package com.dienstplan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.util.NrwDatenHelfer
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun KlassischerMonatsKalender(
    monat: YearMonth,
    eintraege: List<EintragEntity>,
    farbenMap: Map<String, Long>,
    nrwAktiv: Boolean,
    onTagKlick: (LocalDate, EintragEntity?) -> Unit
) {
    var ausgewaehlterTag by remember(monat) { mutableStateOf(monat.atDay(1)) }
    val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val ersterWochentagOffset = monat.atDay(1).dayOfWeek.value - 1
    val zellen = List(ersterWochentagOffset) { null } + (1..monat.lengthOfMonth()).map { monat.atDay(it) }
    val wochenZeilen = zellen.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Kopfzeile mit den Wochentagen (Mo - So)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            wochentage.forEach { tagName ->
                Text(
                    text = tagName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Flaches Kalenderraster
        wochenZeilen.forEach { woche ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val datum = woche.getOrNull(i)
                    if (datum == null) {
                        Box(modifier = Modifier.weight(1f).height(64.dp))
                    } else {
                        val tagesEintraege = eintraege.filter { it.datum == datum.toString() }
                        val feiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(datum) else null
                        val ferien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(datum) else null
                        val istAusgewaehlt = datum == ausgewaehlterTag

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .border(
                                    width = if (istAusgewaehlt) 1.5.dp else 0.25.dp,
                                    color = if (istAusgewaehlt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable {
                                    ausgewaehlterTag = datum
                                    onTagKlick(datum, tagesEintraege.firstOrNull())
                                }
                                .padding(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = datum.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (feiertag != null || istAusgewaehlt) FontWeight.Bold else FontWeight.Normal,
                                    color = if (feiertag != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                if (ferien != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                }
                            }
                            tagesEintraege.take(2).forEach { eintrag ->
                                val farbe = farbenMap[eintrag.einsatzort]?.let { Color(it) }
                                    ?: MaterialTheme.colorScheme.primary
                                Text(
                                    text = eintrag.titel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = farbe,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailbereich unter dem Kalender für den ausgewählten Tag
        val ausgewaehlteEintraege = eintraege.filter { it.datum == ausgewaehlterTag.toString() }
        val tagFeiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(ausgewaehlterTag) else null
        val tagFerien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(ausgewaehlterTag) else null

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Details am ${ausgewaehlterTag.dayOfMonth}.${ausgewaehlterTag.monthValue}.${ausgewaehlterTag.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onTagKlick(ausgewaehlterTag, null) }) {
                    Text("+ Weiterer Eintrag")
                }
            }
            if (tagFeiertag != null || tagFerien != null) {
                Text(
                    text = listOfNotNull(tagFeiertag, tagFerien).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            if (ausgewaehlteEintraege.isEmpty()) {
                Text(
                    text = "Keine Einträge an diesem Tag. Tippe auf einen Tag im Kalender, um eine Schicht oder einen Termin anzulegen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ausgewaehlteEintraege.forEach { eintrag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagKlick(ausgewaehlterTag, eintrag) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${eintrag.titel} ${if (eintrag.einsatzort.isNotBlank()) "(${eintrag.einsatzort})" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (eintrag.typ == "SCHICHT") {
                                String.format(Locale.GERMAN, "%s-%s • %.2f €", eintrag.startZeit, eintrag.endZeit, eintrag.berechneVerdienst())
                            } else eintrag.typ,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}
