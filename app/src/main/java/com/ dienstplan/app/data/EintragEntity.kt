package com.dienstplan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalTime

/**
 * Datenbank-Tabelle für alle Einträge im Dienstplan.
 * typ kann sein: "SCHICHT", "FREI" oder "TERMIN"
 * datum wird im ISO-Format gespeichert: "YYYY-MM-DD" (z.B. "2026-11-03")
 */
@Entity(tableName = "eintraege")
data class EintragEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: String,
    val typ: String,
    val titel: String,
    val startZeit: String = "08:00",
    val endZeit: String = "16:30",
    val pauseMinuten: Int = 30,
    val stundenlohn: Double = 15.0,
    val einsatzort: String = "",
    val notiz: String = ""
) {
    /**
     * Berechnet die Netto-Arbeitsstunden (Arbeitszeit abzüglich Pause in Stunden).
     * Unterstützt auch Nachtschichten über Mitternacht hinaus.
     */
    fun berechneNettoStunden(): Double {
        if (typ != "SCHICHT") return 0.0
        return try {
            val start = LocalTime.parse(startZeit)
            val ende = LocalTime.parse(endZeit)
            var minuten = Duration.between(start, ende).toMinutes()
            if (minuten < 0) {
                minuten += 24 * 60 // Nachtschicht über Mitternacht
            }
            val nettoMinuten = (minuten - pauseMinuten).coerceAtLeast(0)
            nettoMinuten / 60.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Berechnet den Brutto-Verdienst für diese Schicht (Netto-Arbeitszeit x Stundenlohn).
     */
    fun berechneVerdienst(): Double {
        return berechneNettoStunden() * stundenlohn
    }
}

