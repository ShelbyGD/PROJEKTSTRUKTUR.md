package com.dienstplan.app.util

import java.time.LocalDate

/**
 * Hilfsklasse für gesetzliche Feiertage und Schulferien in Nordrhein-Westfalen (NRW).
 */
object NrwDatenHelfer {

    /**
     * Gibt den Namen des gesetzlichen Feiertags in NRW zurück oder null, falls kein Feiertag ist.
     */
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

    /**
     * Prüft, ob das Datum in den Schulferien von Nordrhein-Westfalen liegt (2025–2027).
     */
    fun holeSchulferienNrw(datum: LocalDate): String? {
        val ferienZeitraeume = listOf(
            // NRW Schulferien 2025
            Triple(LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 26), "Osterferien NRW"),
            Triple(LocalDate.of(2025, 7, 14), LocalDate.of(2025, 8, 26), "Sommerferien NRW"),
            Triple(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 25), "Herbstferien NRW"),
            Triple(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 6), "Weihnachtsferien NRW"),
            // NRW Schulferien 2026
            Triple(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 11), "Osterferien NRW"),
            Triple(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 9, 1), "Sommerferien NRW"),
            Triple(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 31), "Herbstferien NRW"),
            Triple(LocalDate.of(2026, 12, 23), LocalDate.of(2027, 1, 6), "Weihnachtsferien NRW"),
            // NRW Schulferien 2027
            Triple(LocalDate.of(2027, 3, 22), LocalDate.of(2027, 4, 3), "Osterferien NRW"),
            Triple(LocalDate.of(2027, 7, 19), LocalDate.of(2027, 8, 31), "Sommerferien NRW"),
            Triple(LocalDate.of(2027, 10, 23), LocalDate.of(2027, 11, 6), "Herbstferien NRW"),
            Triple(LocalDate.of(2027, 12, 24), LocalDate.of(2028, 1, 8), "Weihnachtsferien NRW")
        )
        return ferienZeitraeume.firstOrNull { (start, ende, _) ->
            !datum.isBefore(start) && !datum.isAfter(ende)
        }?.third
    }

    /**
     * Gaußsche Osterformel zur Berechnung des Ostersonntags für ein beliebiges Jahr.
     */
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
