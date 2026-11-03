package com.dienstplan.app.util

import android.graphics.Bitmap
import com.dienstplan.app.data.EintragEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate

/**
 * Hilfsklasse für die Kamera-Texterkennung mit Google ML Kit.
 */
object ScanHelfer {

    /**
     * Scannt ein Kamerabild (Bitmap) nach Dienstplan-Zeiten und Datumsangaben.
     */
    fun bildScannen(
        bitmap: Bitmap,
        onErfolg: (List<EintragEntity>) -> Unit,
        onFehler: (String) -> Unit
    ) {
        val bild = InputImage.fromBitmap(bitmap, 0)
        val erkenner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        erkenner.process(bild)
            .addOnSuccessListener { erkannterText ->
                val gefundeneEintraege = textAuswerten(erkannterText.text)
                onErfolg(gefundeneEintraege)
            }
            .addOnFailureListener { fehler ->
                onFehler(fehler.localizedMessage ?: "Fehler bei der Texterkennung")
            }
    }

    /**
     * Extrahiert Datum (TT.MM.JJJJ), Start-/Endzeit (HH:MM - HH:MM) und Ort aus dem erkannten Text.
     */
    private fun textAuswerten(volltext: String): List<EintragEntity> {
        val liste = mutableListOf<EintragEntity>()
        val datumRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        val zeitRegex = Regex("""(\d{2}:\d{2})\s*[-–]\s*(\d{2}:\d{2})""")
        var aktuellesDatum = LocalDate.now().toString()

        for (zeile in volltext.split("\n")) {
            datumRegex.find(zeile)?.let { match ->
                val (tag, monat, jahr) = match.destructured
                aktuellesDatum = "$jahr-$monat-$tag"
            }

            zeitRegex.find(zeile)?.let { match ->
                val (start, ende) = match.destructured
                // Text hinter der Uhrzeit als Einsatzort übernehmen (falls vorhanden)
                val restText = zeile.substringAfter(match.value).trim()
                val ort = restText.ifBlank { "Kamera-Scan" }

                liste.add(
                    EintragEntity(
                        datum = aktuellesDatum,
                        typ = "SCHICHT",
                        titel = "Gescannte Schicht",
                        startZeit = start,
                        endZeit = ende,
                        pauseMinuten = 30,
                        stundenlohn = 15.0,
                        einsatzort = ort
                    )
                )
            }
        }
        return liste
    }
}
