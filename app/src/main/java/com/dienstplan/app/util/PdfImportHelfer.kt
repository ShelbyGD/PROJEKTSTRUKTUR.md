package com.dienstplan.app.util

import android.content.Context
import android.net.Uri
import com.dienstplan.app.data.EintragEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToInt

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
                    FehlerLogHelfer.logFehler("PDF-Import", "Datei konnte nicht geöffnet werden: $uri")
                    return@withContext emptyList()
                }

                val text = stream.use { input ->
                    PDDocument.load(input).use { dokument ->
                        PDFTextStripper().getText(dokument)
                    }
                }

                val vorschau = text.trim().take(600).ifBlank { "(Kein Text im PDF gefunden)" }
                FehlerLogHelfer.logInfo("PDF-Rohtext", "Länge: ${text.length} Zeichen.\nVorschau:\n$vorschau")

                eintraege.addAll(textZuEintraegen(text, standardMonat, "PDF-Import"))
                val schichten = eintraege.count { it.typ == "SCHICHT" }
                val freieTage = eintraege.count { it.typ == "FREI" }
                FehlerLogHelfer.logInfo("PDF-Ergebnis", "$schichten Schicht(en) und $freieTage freie(r) Tag(e) erkannt.")
            } catch (e: Exception) {
                FehlerLogHelfer.logFehler("PDF-Import", "Absturz beim Lesen der PDF-Datei", e)
            }

            eintraege
        }

    fun textZuEintraegen(volltext: String, standardMonat: String, quelle: String): List<EintragEntity> {
        val liste = mutableListOf<EintragEntity>()
        // Fußzeile ab "Summe der Stunden" abschneiden, damit der Druck-Zeitstempel ignoriert wird
        val bereinigterVolltext = volltext.substringBefore("Summe der Stunden")

        val vollDatumRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        val zeitRegex = Regex("""(\d{1,2})[:.](\d{2})\s*(?:->|[-–bis]+|\s+)\s*(\d{1,2})[:.](\d{2})(?:\s+(\d{1,2}[,.]\d{1,3}))?""")

        // Teilt den Text zuverlässig an jedem Wochentag ("So,", "Mo,", "Di," usw.) auf
        val tagesBloecke = bereinigterVolltext.split(Regex("""(?=(?:Mo|Di|Mi|Do|Fr|Sa|So),\s*)"""))

        for (rohBlock in tagesBloecke) {
            val zeile = rohBlock.replace("\n", " ").replace("\r", " ").trim()
            val datumMatch = vollDatumRegex.find(zeile) ?: continue
            val (tag, monat, jahr) = datumMatch.destructured
            val datumIso = "$jahr-$monat-$tag"

            val zeileOhneDatum = zeile.replaceFirst(datumMatch.value, " ")

            // 1. Fall: Freier Tag ("----------" oder "00:00 -> 24:00")
            if (zeileOhneDatum.contains("-----") || zeileOhneDatum.contains("00:00") && zeileOhneDatum.contains("24:00")) {
                liste.add(
                    EintragEntity(
                        datum = datumIso,
                        typ = "FREI",
                        titel = "Frei",
                        startZeit = "00:00",
                        endZeit = "00:00",
                        pauseMinuten = 0,
                        stundenlohn = 0.0
                    )
                )
                continue
            }

            // 2. Fall: Arbeitsschicht (z.B. "10:00 21:00 10,000")
            val zeitMatch = zeitRegex.find(zeileOhneDatum) ?: continue
            val start = "${zeitMatch.groupValues[1].padStart(2, '0')}:${zeitMatch.groupValues[2]}"
            val ende = "${zeitMatch.groupValues[3].padStart(2, '0')}:${zeitMatch.groupValues[4]}"
            val stdString = zeitMatch.groupValues.getOrNull(5)?.replace(",", ".")

            val pauseMin = berechnePauseAusPdf(start, ende, stdString)
            val saubererOrt = extrahiereEinsatzort(zeile.substringBefore(datumMatch.value), quelle)

            liste.add(
                EintragEntity(
                    datum = datumIso,
                    typ = "SCHICHT",
                    titel = saubererOrt,
                    startZeit = start,
                    endZeit = ende,
                    pauseMinuten = pauseMin,
                    stundenlohn = 15.0,
                    einsatzort = saubererOrt,
                    notiz = "REWE Nord"
                )
            )
        }
        return liste
    }

    private fun berechnePauseAusPdf(startZeit: String, endZeit: String, nettoStdString: String?): Int {
        val nettoStd = nettoStdString?.toDoubleOrNull() ?: return 30
        return try {
            var gesamtMin = Duration.between(LocalTime.parse(startZeit), LocalTime.parse(endZeit)).toMinutes()
            if (gesamtMin < 0) gesamtMin += 24 * 60
            val nettoMin = (nettoStd * 60.0).roundToInt()
            (gesamtMin - nettoMin).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            30
        }
    }

    private fun extrahiereEinsatzort(roherVortext: String, fallback: String): String {
        return roherVortext
            .replace(Regex("""^(Mo|Di|Mi|Do|Fr|Sa|So),?\s*"""), "")
            .replace("->", " ")
            .replace(Regex("""^\s*\d{4}\s+"""), "") // Entfernt Marktnummer wie 0410 oder 0171
            .replace(Regex("""\s+DE[T]?\s+REWE.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+REWE\s+Nord.*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(28)
            .ifBlank { fallback }
    }
}
