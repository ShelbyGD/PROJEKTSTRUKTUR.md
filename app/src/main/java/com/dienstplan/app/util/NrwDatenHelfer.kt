package com.dienstplan.app.util

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FerienZeitraum(
    val start: LocalDate,
    val ende: LocalDate,
    val name: String,
    val istManuell: Boolean = false
)

object NrwDatenHelfer {

    private const val PREFS_NAME = "nrw_ferien_prefs"
    private const val KEY_MANUELL = "manuelle_ferien_daten"
    private const val KEY_API = "api_ferien_daten"

    val manuelleFerienListe = mutableStateListOf<FerienZeitraum>()
    val apiFerienListe = mutableStateListOf<FerienZeitraum>()
    private var istInitialisiert = false

    private val standardFerienNrw = listOf(
        // 2025
        FerienZeitraum(LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 26), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 8, 26), "Sommerferien NRW"),
        FerienZeitraum(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 25), "Herbstferien NRW"),
        FerienZeitraum(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 6), "Weihnachtsferien NRW"),
        // 2026
        FerienZeitraum(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 11), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2026, 5, 26), LocalDate.of(2026, 5, 26), "Pfingstferien NRW"),
        FerienZeitraum(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 9, 1), "Sommerferien NRW"),
        FerienZeitraum(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 31), "Herbstferien NRW"),
        FerienZeitraum(LocalDate.of(2026, 12, 23), LocalDate.of(2027, 1, 6), "Weihnachtsferien NRW"),
        // 2027
        FerienZeitraum(LocalDate.of(2027, 3, 22), LocalDate.of(2027, 4, 3), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2027, 5, 18), LocalDate.of(2027, 5, 18), "Pfingstferien NRW"),
        FerienZeitraum(LocalDate.of(2027, 7, 19), LocalDate.of(2027, 8, 31), "Sommerferien NRW"),
        FerienZeitraum(LocalDate.of(2027, 10, 23), LocalDate.of(2027, 11, 6), "Herbstferien NRW"),
        FerienZeitraum(LocalDate.of(2027, 12, 24), LocalDate.of(2028, 1, 8), "Weihnachtsferien NRW"),
        // 2028
        FerienZeitraum(LocalDate.of(2028, 4, 10), LocalDate.of(2028, 4, 22), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2028, 6, 6), LocalDate.of(2028, 6, 6), "Pfingstferien NRW"),
        FerienZeitraum(LocalDate.of(2028, 7, 10), LocalDate.of(2028, 8, 22), "Sommerferien NRW"),
        FerienZeitraum(LocalDate.of(2028, 10, 23), LocalDate.of(2028, 11, 4), "Herbstferien NRW"),
        FerienZeitraum(LocalDate.of(2028, 12, 21), LocalDate.of(2029, 1, 5), "Weihnachtsferien NRW"),
        // 2029
        FerienZeitraum(LocalDate.of(2029, 3, 26), LocalDate.of(2029, 4, 7), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2029, 5, 22), LocalDate.of(2029, 5, 22), "Pfingstferien NRW"),
        FerienZeitraum(LocalDate.of(2029, 7, 2), LocalDate.of(2029, 8, 14), "Sommerferien NRW"),
        FerienZeitraum(LocalDate.of(2029, 10, 15), LocalDate.of(2029, 10, 27), "Herbstferien NRW"),
        FerienZeitraum(LocalDate.of(2029, 12, 21), LocalDate.of(2030, 1, 4), "Weihnachtsferien NRW"),
        // 2030
        FerienZeitraum(LocalDate.of(2030, 4, 15), LocalDate.of(2030, 4, 27), "Osterferien NRW"),
        FerienZeitraum(LocalDate.of(2030, 6, 11), LocalDate.of(2030, 6, 11), "Pfingstferien NRW"),
        FerienZeitraum(LocalDate.of(2030, 7, 15), LocalDate.of(2030, 8, 27), "Sommerferien NRW")
    )

    private fun stelleInitialisierungSicher(context: Context? = null) {
        if (istInitialisiert) return
        val appContext = context ?: runCatching {
            val clazz = Class.forName("android.app.ActivityThread")
            clazz.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: return
        initialisiere(appContext)
    }
// --- START TEIL 2 (NrwDatenHelfer.kt) ---
    fun initialisiere(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        manuelleFerienListe.clear()
        manuelleFerienListe.addAll(deserialisiereListe(prefs.getString(KEY_MANUELL, ""), true))
        apiFerienListe.clear()
        apiFerienListe.addAll(deserialisiereListe(prefs.getString(KEY_API, ""), false))
        istInitialisiert = true
    }

    fun holeFeiertagNrw(datum: LocalDate): String? {
        val jahr = datum.year
        val ostern = berechneOstersonntag(jahr)

        return when (datum) {
            LocalDate.of(jahr, 1, 1) -> "Neujahr"
            ostern.minusDays(2) -> "Karfreitag"
            ostern.plusDays(1) -> "Ostermontag"
            LocalDate.of(jahr, 5, 1) -> "Tag der Arbeit"
            ostern.plusDays(39) -> "Christi Himmelfahrt"
            ostern.plusDays(50) -> "Pfingstmontag"
            ostern.plusDays(60) -> "Fronleichnam (NRW)"
            LocalDate.of(jahr, 10, 3) -> "Tag der Deutschen Einheit"
            LocalDate.of(jahr, 11, 1) -> "Allerheiligen (NRW)"
            LocalDate.of(jahr, 12, 25) -> "1. Weihnachtstag"
            LocalDate.of(jahr, 12, 26) -> "2. Weihnachtstag"
            else -> null
        }
    }

    fun holeSchulferienNrw(datum: LocalDate): String? {
        stelleInitialisierungSicher()
        manuelleFerienListe.firstOrNull { !datum.isBefore(it.start) && !datum.isAfter(it.ende) }?.let {
            return it.name
        }
        apiFerienListe.firstOrNull { !datum.isBefore(it.start) && !datum.isAfter(it.ende) }?.let {
            return it.name
        }
        return standardFerienNrw.firstOrNull { !datum.isBefore(it.start) && !datum.isAfter(it.ende) }?.name
    }

    fun holeAlleFeiertageFuerJahr(jahr: Int): List<Pair<LocalDate, String>> {
        val ostern = berechneOstersonntag(jahr)
        return listOf(
            LocalDate.of(jahr, 1, 1) to "Neujahr",
            ostern.minusDays(2) to "Karfreitag",
            ostern.plusDays(1) to "Ostermontag",
            LocalDate.of(jahr, 5, 1) to "Tag der Arbeit",
            ostern.plusDays(39) to "Christi Himmelfahrt",
            ostern.plusDays(50) to "Pfingstmontag",
            ostern.plusDays(60) to "Fronleichnam (NRW)",
            LocalDate.of(jahr, 10, 3) to "Tag der Deutschen Einheit",
            LocalDate.of(jahr, 11, 1) to "Allerheiligen (NRW)",
            LocalDate.of(jahr, 12, 25) to "1. Weihnachtstag",
            LocalDate.of(jahr, 12, 26) to "2. Weihnachtstag"
        ).sortedBy { it.first }
    }

    fun holeFerienFuerJahr(jahr: Int): List<FerienZeitraum> {
        stelleInitialisierungSicher()
        val basis = if (apiFerienListe.any { it.start.year == jahr || it.ende.year == jahr }) {
            apiFerienListe
        } else {
            standardFerienNrw
        }
        return (manuelleFerienListe + basis)
            .filter { it.start.year == jahr || it.ende.year == jahr }
            .distinctBy { "${it.start}_${it.ende}_${it.name}" }
            .sortedBy { it.start }
    }

    fun manuelleFerienHinzufuegen(context: Context, startText: String, endeText: String, titel: String): Boolean {
        val startDatum = parseDatumFlexibel(startText) ?: return false
        val endeDatum = if (endeText.isBlank()) startDatum else (parseDatumFlexibel(endeText) ?: return false)
        val sortiertStart = if (endeDatum.isBefore(startDatum)) endeDatum else startDatum
        val sortiertEnde = if (endeDatum.isBefore(startDatum)) startDatum else endeDatum
        val saubererTitel = titel.trim().ifBlank { "Beweglicher Ferientag" }

        manuelleFerienListe.add(FerienZeitraum(sortiertStart, sortiertEnde, saubererTitel, true))
        speichereListe(context, KEY_MANUELL, manuelleFerienListe)
        return true
    }

    fun manuelleFerienLoeschen(context: Context, zeitraum: FerienZeitraum) {
        manuelleFerienListe.remove(zeitraum)
        speichereListe(context, KEY_MANUELL, manuelleFerienListe)
    }
// --- START TEIL 3 (NrwDatenHelfer.kt) ---
    suspend fun synchronisiereOpenHolidaysApi(context: Context, startJahr: Int): String = withContext(Dispatchers.IO) {
        runCatching {
            val endeJahr = startJahr + 2
            val urlString = "https://openholidaysapi.org/SchoolHolidays?countryIsoCode=DE&subdivisionCode=DE-NW&languageIsoCode=DE&validFrom=$startJahr-01-01&validTo=$endeJahr-12-31"
            val verbindung = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            if (verbindung.responseCode != 200) {
                return@withContext "API-Fehler: HTTP ${verbindung.responseCode}"
            }

            val jsonText = verbindung.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)
            val geladeneFerien = mutableListOf<FerienZeitraum>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val start = LocalDate.parse(obj.getString("startDate"))
                val ende = LocalDate.parse(obj.getString("endDate"))
                val namenArray = obj.optJSONArray("name")
                var ferienName = "Schulferien NRW"
                if (namenArray != null && namenArray.length() > 0) {
                    ferienName = namenArray.getJSONObject(0).optString("text", "Schulferien NRW")
                }
                geladeneFerien.add(FerienZeitraum(start, ende, "$ferienName (NRW)", false))
            }

            withContext(Dispatchers.Main) {
                apiFerienListe.clear()
                apiFerienListe.addAll(geladeneFerien)
                speichereListe(context, KEY_API, apiFerienListe)
            }
            "${geladeneFerien.size} Ferien-Zeiträume ($startJahr–$endeJahr) online geladen!"
        }.getOrElse { fehler ->
            "Online-Abruf fehlgeschlagen (${fehler.localizedMessage ?: "Keine Verbindung"}). Offline-Daten aktiv."
        }
    }

    private fun parseDatumFlexibel(eingabe: String): LocalDate? {
        val text = eingabe.trim()
        if (text.isEmpty()) return null
        return runCatching { LocalDate.parse(text) }.getOrElse {
            runCatching {
                LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
            }.getOrNull()
        }
    }

    private fun speichereListe(context: Context, key: String, liste: List<FerienZeitraum>) {
        val serialisiert = liste.joinToString(";;") { "${it.start}|${it.ende}|${it.name.replace("|", " ")}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, serialisiert).apply()
    }

    private fun deserialisiereListe(daten: String?, istManuell: Boolean): List<FerienZeitraum> {
        if (daten.isNullOrBlank()) return emptyList()
        return daten.split(";;").mapNotNull { eintrag ->
            val teile = eintrag.split("|")
            if (teile.size >= 3) {
                runCatching {
                    FerienZeitraum(LocalDate.parse(teile[0]), LocalDate.parse(teile[1]), teile[2], istManuell)
                }.getOrNull()
            } else null
        }
    }

    private fun berechneOstersonntag(jahr: Int): LocalDate {
        val a = jahr % 19
        val b = jahr / 100
        val c = jahr % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val monat = (h + l - 7 * m + 114) / 31
        val tag = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(jahr, monat, tag)
    }
}
