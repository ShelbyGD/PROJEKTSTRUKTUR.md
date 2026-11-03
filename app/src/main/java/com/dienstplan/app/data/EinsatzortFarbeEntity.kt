package com.dienstplan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Datenbank-Tabelle für die Farbcodierung der Einsatzorte.
 * farbHex speichert den ARGB-Farbwert (z.B. 0xFF2E7D32 für Grün).
 */
@Entity(tableName = "einsatzort_farben")
data class EinsatzortFarbeEntity(
    @PrimaryKey val einsatzort: String,
    val farbHex: Long
)
