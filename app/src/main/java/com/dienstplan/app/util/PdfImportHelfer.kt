package com.dienstplan.app.util

import android.content.Context
import android.net.Uri
import com.dienstplan.app.data.EintragEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.time.LocalDate

object PdfImportHelfer {

    /**
     * Initialisiert PDFBox (muss einmalig aufgerufen werden).
     */
    fun initialisieren(context: Context) {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Liest eine PDF-Datei von einer URI ein und extrahiert Schichten.
     */
    fun pdfEinlesen(context: Context, uri: Uri): List<EintragEntity> {
        initialisieren(context)
        val eintraege = mutableListOf<EintragEntity>()

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val dokument = PDDocument.load(stream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(dokument)
                dokument.close()

                // Einfacher Parser: Durchsucht Zeilen nach Mustern wie "01.11.2026" und Uhrzeiten
                val zeilen = text.split("\n")
                val datumRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
                val zeitRegex = Regex("""(\d{2}:\d{2})\s*[-–]\s*(\d{2}:\d{2})""")

                var aktuellesDatum = LocalDate.now().toString()

                for (zeile in zeilen) {
                    val datumMatch = datumRegex.find(zeile)
                    if (datumMatch != null) {
                        val (tag, monat, jahr) = datumMatch.destructured
                        aktuellesDatum = "$jahr-$monat-$tag"
                    }

                    val zeitMatch = zeitRegex.find(zeile)
                    if (zeitMatch != null) {
                        val (start, ende) = zeitMatch.destructured
                        eintraege.add(
                            EintragEntity(
                                datum = aktuellesDatum,
                                typ = "SCHICHT",
                                titel = "Importierte Schicht",
                                startZeit = start,
                                endZeit = ende,
                                pauseMinuten = 30,
                                stundenlohn = 15.0,
                                einsatzort = "PDF-Import"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return eintraege
    }
}
