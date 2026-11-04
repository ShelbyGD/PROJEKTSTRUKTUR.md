package com.dienstplan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.util.NrwDatenHelfer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monatsFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
private val tagesFormat = DateTimeFormatter.ofPattern("EE, dd.MM.", Locale.GERMAN)

@Composable
fun MonatsUndGehaltsLeiste(
    monat: YearMonth,
    monatsEintraege: List<EintragEntity>,
    onMonatWechsel: (Long) -> Unit
) {
    val anzahlSchichten = monatsEintraege.count { it.typ == "SCHICHT" }
    val gesamtStunden = monatsEintraege.sumOf { it.berechneNettoStunden() }
    val gesamtVerdienst = monatsEintraege.sumOf { it.berechneVerdienst() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonatWechsel(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Monat")
            }
            Text(
                text = monat.format(monatsFormat).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onMonatWechsel(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat")
            }
        }
        // Clean-Übersichtsleiste: Schichten | Netto-Zeit | Gehalt
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Schichten: $anzahlSchichten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.GERMAN, "Ist-Zeit: %.2f Std.", gesamtStunden),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.GERMAN, "Gehalt: %.2f €", gesamtVerdienst),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun FlacheListenAnsicht(
    monat: YearMonth,
    eintraege: List<EintragEntity>,
    farbenMap: Map<String, Long>,
    nrwAktiv: Boolean,
    onTagKlick: (LocalDate, EintragEntity?) -> Unit
) {
    val tageImMonat = (1..monat.lengthOfMonth()).map { monat.atDay(it) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tageImMonat) { datum ->
            val tagesEintraege = eintraege.filter { it.datum == datum.toString() }
            val feiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(datum) else null
            val ferien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(datum) else null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTagKlick(datum, tagesEintraege.firstOrNull()) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = datum.format(tagesFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (feiertag != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (feiertag != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    if (feiertag != null || ferien != null) {
                        Text(
                            text = listOfNotNull(feiertag, ferien).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                tagesEintraege.forEach { eintrag ->
                    val ortFarbe = farbenMap[eintrag.einsatzort]?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ortFarbe))
                            Text(
                                text = "  ${eintrag.titel}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (eintrag.typ == "SCHICHT") {
                                String.format(Locale.GERMAN, "%s-%s • %.1f Std. (%.2f €)", eintrag.startZeit, eintrag.endZeit, eintrag.berechneNettoStunden(), eintrag.berechneVerdienst())
                            } else eintrag.typ,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        }
    }
}
