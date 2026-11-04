package com.dienstplan.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.dienstplan.app.data.EintragEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Hilfsklasse für die Texterkennung (Kamera & Galerie-Bilder) mit Google ML Kit.
 */
object ScanHelfer {

    /**
     * Scannt ein Kamera-Vorschaubild (Bitmap).
     */
    fun bitmapScannen(
        bitmap: Bitmap,
        standardMonat: String,
        onErfolg: (List<EintragEntity>) -> Unit,
        onFehler: (String) -> Unit
    ) {
        try {
            val bild = InputImage.fromBitmap(bitmap, 0)
            bildVerarbeiten(bild, standardMonat, onErfolg, onFehler)
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Kamera-Scan", "Bitmap konnte nicht geladen werden", e)
            onFehler("Fehler beim Laden des Kamerabildes")
        }
    }

    /**
     * Scannt ein hochauflösendes Bild oder einen Screenshot aus der Galerie (Uri).
     */
    fun bildUriScannen(
        context: Context,
        uri: Uri,
        standardMonat: String,
        onErfolg: (List<EintragEntity>) -> Unit,
        onFehler: (String) -> Unit
    ) {
        try {
            val bild = InputImage.fromFilePath(context, uri)
            bildVerarbeiten(bild, standardMonat, onErfolg, onFehler)
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("Bild-Scan", "Bild-URI konnte nicht gelesen werden: $uri", e)
            onFehler("Bild konnte nicht geöffnet werden")
        }
    }

    private fun bildVerarbeiten(
        bild: InputImage,
        standardMonat: String,
        onErfolg: (List<EintragEntity>) -> Unit,
        onFehler: (String) -> Unit
    ) {
        val erkenner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        erkenner.process(bild)
            .addOnSuccessListener { ergebnis ->
                val volltext = ergebnis.text
                val vorschau = volltext.trim().take(500).ifBlank { "(Kein Text auf dem Bild erkannt)" }
                FehlerLogHelfer.logInfo("ML-Kit Scan-Text", "Erkannter Text:\n$vorschau")

                val eintraege = PdfImportHelfer.textZuEintraegen(volltext, standardMonat, "Kamera-Scan")
                FehlerLogHelfer.logInfo("Scan-Ergebnis", "${eintraege.size} Schicht(en) aus Bild erkannt.")
                onErfolg(eintraege)
            }
            .addOnFailureListener { fehler ->
                FehlerLogHelfer.logFehler("ML-Kit Scan", "Texterkennung fehlgeschlagen", fehler)
                onFehler(fehler.localizedMessage ?: "Fehler bei der Texterkennung")
            }
    }
}
