package com.dienstplan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.util.NrwDatenHelfer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
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
    val wochenFeld = WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear()

    Column(modifier = Modifier.fillMaxSize()) {
        // Wochentags-Kopfzeile mit dezenter KW-Spalte
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KW",
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
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

        // Vollbild-Kalenderraster im Clean-Design
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            wochenZeilen.forEach { woche ->
                val erstesDatumDerWoche = woche.filterNotNull().firstOrNull()
                val kw = erstesDatumDerWoche?.get(wochenFeld)?.toString() ?: ""

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight()
                            .border(0.25.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = kw,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    for (i in 0 until 7) {
                        val datum = woche.getOrNull(i)
                        if (datum == null) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight())
                        } else {
                            val tagesEintraege = eintraege.filter { it.datum == datum.toString() }
                            val feiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(datum) else null
                            val ferien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(datum) else null
                            val istAusgewaehlt = datum == ausgewaehlterTag

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
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
                                            modifier = Modifier.size(5.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.tertiary)
                                        )
                                    }
                                }
                                tagesEintraege.take(2).forEach { eintrag ->
                                    val tagFarbe = when {
                                        farbenMap.containsKey(eintrag.einsatzort) -> Color(farbenMap[eintrag.einsatzort]!!)
                                        eintrag.typ == "FREI" -> MaterialTheme.colorScheme.secondary
                                        eintrag.typ == "TERMIN" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp)
                                            .background(tagFarbe.copy(alpha = 0.22f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = eintrag.titel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = tagFarbe,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 10.sp
                                        )
                                        if (eintrag.typ == "SCHICHT") {
                                            Text(
                                                text = "${eintrag.startZeit}-${eintrag.endZeit}",
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                lineHeight = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Kompakte Detailansicht unten
        val ausgewaehlteEintraege = eintraege.filter { it.datum == ausgewaehlterTag.toString() }
        val tagFeiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(ausgewaehlterTag) else null
        val tagFerien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(ausgewaehlterTag) else null

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 150.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Am ${ausgewaehlterTag.dayOfMonth}.${ausgewaehlterTag.monthValue}.${ausgewaehlterTag.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onTagKlick(ausgewaehlterTag, null) }) { Text("+ Eintrag") }
            }
            if (tagFeiertag != null || tagFerien != null) {
                Text(
                    text = listOfNotNull(tagFeiertag, tagFerien).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            ausgewaehlteEintraege.forEach { eintrag ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTagKlick(ausgewaehlterTag, eintrag) }.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = eintrag.titel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (eintrag.typ == "SCHICHT") {
                            String.format(Locale.GERMAN, "%s-%s (%.1f Std. • %.2f €)", eintrag.startZeit, eintrag.endZeit, eintrag.berechneNettoStunden(), eintrag.berechneVerdienst())
                        } else eintrag.typ,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
