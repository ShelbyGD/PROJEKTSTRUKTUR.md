package com.dienstplan.app.util

import android.content.Context
import android.net.Uri
import com.dienstplan.app.data.DienstplanDatenbank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object CloudBackupHelfer {

    const val GOOGLE_DRIVE_API_KEY = "HIER_GOOGLE_API_KEY_EINTRAGEN"
    const val GOOGLE_DRIVE_ACCESS_TOKEN = "HIER_OAUTH_ACCESS_TOKEN_EINTRAGEN"
    private const val BACKUP_DATEINAME = "dienstplan_backup.db"

    /**
     * Schreibt alle offenen Änderungen aus dem Zwischenspeicher (WAL) fest in die Datenbank-Datei.
     */
    private fun datenbankSynchronisieren(context: Context) {
        try {
            val db = DienstplanDatenbank.datenbankHolen(context).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("DB-Sync", "WAL-Checkpoint Hinweis", e)
        }
    }

    /**
     * Speichert die Datenbank-Datei direkt an einem vom Nutzer gewählten Ort auf dem Smartphone.
     */
    suspend fun backupAufGeraetSpeichern(context: Context, zielUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            datenbankSynchronisieren(context)
            val dbDatei = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            if (!dbDatei.exists()) return@withContext "Noch keine Datenbank vorhanden."

            context.contentResolver.openOutputStream(zielUri)?.use { output ->
                dbDatei.inputStream().use { input -> input.copyTo(output) }
            }
            FehlerLogHelfer.logInfo("Geräte-Backup", "Erfolgreich gespeichert unter: $zielUri")
            "Dienstplan erfolgreich auf dem Gerät gespeichert!"
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Geräte-Backup", "Speichern auf Gerät fehlgeschlagen", e)
            "Fehler beim Speichern: ${e.localizedMessage}"
        }
    }

    /**
     * Lädt eine gespeicherte Datenbank-Datei vom Smartphone.
     */
    suspend fun backupVomGeraetLaden(context: Context, quellUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dbDatei = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            File(dbDatei.path + "-wal").delete()
            File(dbDatei.path + "-shm").delete()

            context.contentResolver.openInputStream(quellUri)?.use { input ->
                dbDatei.outputStream().use { output -> input.copyTo(output) }
            }
            FehlerLogHelfer.logInfo("Geräte-Restore", "Backup geladen von: $quellUri")
            "Backup geladen! Bitte App einmal kurz neu starten."
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Geräte-Restore", "Laden vom Gerät fehlgeschlagen", e)
            "Fehler beim Laden: ${e.localizedMessage}"
        }
    }

    suspend fun datenbankSichern(context: Context): String = withContext(Dispatchers.IO) {
        try {
            datenbankSynchronisieren(context)
            val dbDatei = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            val lokalesBackup = File(context.getExternalFilesDir(null) ?: context.filesDir, BACKUP_DATEINAME)
            dbDatei.copyTo(lokalesBackup, overwrite = true)

            if (GOOGLE_DRIVE_ACCESS_TOKEN.startsWith("HIER_")) {
                FehlerLogHelfer.logInfo("Backup", "Intern gesichert: ${lokalesBackup.absolutePath}")
                return@withContext "Im App-Ordner gesichert! (Tipp: Nutze im Menü 'Auf Gerät speichern' für freie Ordnerwahl)."
            }

            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=media&key=$GOOGLE_DRIVE_API_KEY")
            val verbindung = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $GOOGLE_DRIVE_ACCESS_TOKEN")
                setRequestProperty("Content-Type", "application/x-sqlite3")
            }
            verbindung.outputStream.use { out -> dbDatei.inputStream().use { it.copyTo(out) } }
            "Cloud-Backup Status: ${verbindung.responseCode}"
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Cloud-Backup", "Fehler beim Sichern", e)
            "Fehler: ${e.localizedMessage}"
        }
    }

    suspend fun datenbankWiederherstellen(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val dbDatei = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            val lokalesBackup = File(context.getExternalFilesDir(null) ?: context.filesDir, BACKUP_DATEINAME)
            if (lokalesBackup.exists()) {
                File(dbDatei.path + "-wal").delete()
                File(dbDatei.path + "-shm").delete()
                lokalesBackup.copyTo(dbDatei, overwrite = true)
                "Aus App-Ordner wiederhergestellt! Bitte App neu starten."
            } else {
                "Kein internes Backup gefunden."
            }
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Restore", "Wiederherstellung fehlgeschlagen", e)
            "Fehler: ${e.localizedMessage}"
        }
    }
}
