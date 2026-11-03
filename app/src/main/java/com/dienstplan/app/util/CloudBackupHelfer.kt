package com.dienstplan.app.util

import android.content.Context
import com.dienstplan.app.data.DienstplanDatenbank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Hilfsklasse für das Sichern und Wiederherstellen der lokalen Room/SQLite-Datenbank
 * über die Google Drive REST API v3.
 */
object CloudBackupHelfer {

    // =========================================================================
    // PLATZHALTER FÜR GOOGLE DRIVE API KEYS (Hier eigene Schlüssel eintragen)
    // =========================================================================
    const val GOOGLE_DRIVE_CLIENT_ID = "HIER_GOOGLE_CLIENT_ID_EINTRAGEN"
    const val GOOGLE_DRIVE_API_KEY = "HIER_GOOGLE_API_KEY_EINTRAGEN"
    const val GOOGLE_DRIVE_ACCESS_TOKEN = "HIER_OAUTH_ACCESS_TOKEN_EINTRAGEN"

    private const val BACKUP_DATEINAME = "dienstplan_backup.db"

    /**
     * Erstellt ein Backup der aktuellen Room-Datenbank.
     * Falls die Google-Keys noch Platzhalter sind, wird eine lokale Backup-Datei
     * im App-Speicher angelegt, damit die Funktion sofort getestet werden kann.
     */
    suspend fun datenbankSichern(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val dbDatei: File = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            if (!dbDatei.exists()) {
                return@withContext "Noch keine Datenbank zum Sichern vorhanden."
            }

            // Lokale Sicherheitskopie im App-Verzeichnis erstellen
            val lokalesBackup = File(context.filesDir, BACKUP_DATEINAME)
            dbDatei.copyTo(lokalesBackup, overwrite = true)

            // Prüfen, ob echte Google Drive Keys eingetragen wurden
            if (GOOGLE_DRIVE_ACCESS_TOKEN.startsWith("HIER_")) {
                return@withContext "Lokales Backup erstellt (Google Drive API-Key ist noch Platzhalter)."
            }

            // Upload zur Google Drive API v3
            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=media&key=$GOOGLE_DRIVE_API_KEY")
            val verbindung = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $GOOGLE_DRIVE_ACCESS_TOKEN")
                setRequestProperty("Content-Type", "application/x-sqlite3")
            }

            verbindung.outputStream.use { output ->
                dbDatei.inputStream().use { input -> input.copyTo(output) }
            }

            if (verbindung.responseCode in 200..299) {
                "Backup erfolgreich in Google Drive hochgeladen!"
            } else {
                "Google Drive Fehler (Code ${verbindung.responseCode}). Lokales Backup wurde gespeichert."
            }
        } catch (e: Exception) {
            "Fehler beim Backup: ${e.localizedMessage ?: "Unbekannter Fehler"}"
        }
    }

    /**
     * Stellt die Room-SQLite-Datenbank aus dem Backup wieder her.
     */
    suspend fun datenbankWiederherstellen(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val dbDatei: File = context.getDatabasePath(DienstplanDatenbank.DATENBANK_NAME)
            val lokalesBackup = File(context.filesDir, BACKUP_DATEINAME)

            if (!GOOGLE_DRIVE_ACCESS_TOKEN.startsWith("HIER_")) {
                // Download von Google Drive API v3 (sobald echte Keys hinterlegt sind)
                val url = URL("https://www.googleapis.com/drive/v3/files/$BACKUP_DATEINAME?alt=media&key=$GOOGLE_DRIVE_API_KEY")
                val verbindung = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $GOOGLE_DRIVE_ACCESS_TOKEN")
                }
                if (verbindung.responseCode in 200..299) {
                    verbindung.inputStream.use { input ->
                        dbDatei.outputStream().use { output -> input.copyTo(output) }
                    }
                    return@withContext "Datenbank aus Google Drive wiederhergestellt! Bitte App neu starten."
                }
            }

            // Fallback auf das gespeicherte Backup
            if (lokalesBackup.exists()) {
                lokalesBackup.copyTo(dbDatei, overwrite = true)
                "Backup erfolgreich wiederhergestellt!"
            } else {
                "Kein Backup zum Wiederherstellen gefunden."
            }
        } catch (e: Exception) {
            "Fehler bei Wiederherstellung: ${e.localizedMessage ?: "Unbekannter Fehler"}"
        }
    }
}
