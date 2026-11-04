package com.dienstplan.app.util

import android.content.Context
import android.net.Uri
import com.dienstplan.app.data.EintragEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfImportHelfer {

    private fun initialisieren(context: Context) {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
        } catch (e: Exception) {
            FehlerLogHelfer.logFehler("PDF-Init", "PDFBox konnte nicht initialisiert werden", e)
        }
    }

    suspend fun pdfEinlesen(context: Context, uri: Uri, standardMonat: String): List<EintragEntity> =
        withContext(Dispatchers.IO) {
            initialisieren(context)
            val eintraege = mutableListOf<EintragEntity>()

            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    FehlerLogHelfer.logFehler("PDF-Import", "Datei-Stream konnte nicht geöffnet werden: $uri")
                    return@withContext emptyList()
                }

                val text = stream.use { input ->
                    PDDocument.load(input).use { dokument ->
                        PDFTextStripper().getText(dokument)
                    }
                }

                val vorschau = text.trim().take(500).ifBlank { "(Kein Text im PDF gefunden – evtl. reines Bild-PDF)" }
                FehlerLogHelfer.logInfo("PDF-Rohtext", "Länge: ${text.length} Zeichen.\nVorschau:\n$vorschau")

                eintraege.addAll(textZuEintraegen(text, standardMonat, "PDF-Import"))
                FehlerLogHelfer.logInfo("PDF-Ergebnis", "${eintraege.size} Schicht(en) erfolgreich erkannt.")
            } catch (e: Exception) {
                FehlerLogHelfer.logFehler("PDF-Import", "Absturz beim Lesen der PDF-Datei", e)
            }

            eintraege
        }

    fun textZuEintraegen(volltext: String, standardMonat: String, quelle: String): List<EintragEntity> {
        val liste = mutableListOf<EintragEntity>()
        val vollDatumRegex = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{2,4})""")
        val kurzDatumRegex = Regex("""(\d{1,2})\.(\d{1,2})\.""")
        val zeitRegex = Regex("""(\d{1,2})[:.](\d{2})\s*[-–bis]+\s*(\d{1,2})[:.](\d{2})""")

        var aktuellesDatum = "$standardMonat-01"
        val standardJahr = standardMonat.substringBefore("-")

        for (zeile in volltext.lines()) {
            vollDatumRegex.find(zeile)?.let { m ->
                val tag = m.groupValues[1].padStart(2, '0')
                val monat = m.groupValues[2].padStart(2, '0')
                val jahrRoh = m.groupValues[3]
                val jahr = if (jahrRoh.length == 2) "20$jahrRoh" else jahrRoh
                aktuellesDatum = "$jahr-$monat-$tag"
            } ?: kurzDatumRegex.find(zeile)?.let { m ->
                val tag = m.groupValues[1].padStart(2, '0')
                val monat = m.groupValues[2].padStart(2, '0')
                aktuellesDatum = "$standardJahr-$monat-$tag"
            }

            zeitRegex.find(zeile)?.let { m ->
                val start = "${m.groupValues[1].padStart(2, '0')}:${m.groupValues[2]}"
                val ende = "${m.groupValues[3].padStart(2, '0')}:${m.groupValues[4]}"
                val rest = zeile.substringAfter(m.value).trim().take(25)
                liste.add(
                    EintragEntity(
                        datum = aktuellesDatum,
                        typ = "SCHICHT",
                        titel = if (quelle == "PDF-Import") "PDF-Schicht" else "Scan-Schicht",
                        startZeit = start,
                        endZeit = ende,
                        pauseMinuten = 30,
                        stundenlohn = 15.0,
                        einsatzort = rest.ifBlank { quelle }
                    )
                )
            }
        }
        return liste
    }
}
