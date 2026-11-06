package com.dienstplan.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.util.NrwDatenHelfer
import java.io.File
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
    notizenMap: Map<String, String> = emptyMap(),
    fotosProTagMap: Map<String, List<File>> = emptyMap(),
    standortKmMap: Map<String, Double> = emptyMap(),
    kmPauschale: Double = 0.30,
    hinUndRueckfahrt: Boolean = true,
    onMonatWechseln: (Long) -> Unit = {},
    onFotoAnsehen: (File) -> Unit = {},
    onTagKlick: (LocalDate, EintragEntity?) -> Unit,
    onEintragDirektLoeschen: ((EintragEntity) -> Unit)? = null
) {
    val heute = LocalDate.now()
    var ausgewaehlterTag by remember(monat) {
        mutableStateOf(if (YearMonth.from(heute) == monat) heute else monat.atDay(1))
    }
    val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val ersterWochentagOffset = monat.atDay(1).dayOfWeek.value - 1
    val zellen = List(ersterWochentagOffset) { null } + (1..monat.lengthOfMonth()).map { monat.atDay(it) }
    val wochenZeilen = zellen.chunked(7)
    val wochenFeld = WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear()

    Column(modifier = Modifier.fillMaxSize()) {
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
// --- START TEIL 2 (KalenderAnsicht.kt) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(monat) {
                    var gesamtWischWeg = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { gesamtWischWeg = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            gesamtWischWeg += dragAmount
                        },
                        onDragEnd = {
                            if (gesamtWischWeg > 95f) onMonatWechseln(-1L)
                            else if (gesamtWischWeg < -95f) onMonatWechseln(1L)
                        }
                    )
                }
        ) {
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
                        Text(text = kw, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                    }

                    for (i in 0 until 7) {
                        val datum = woche.getOrNull(i)
                        if (datum == null) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight())
                        } else {
                            val datumKey = datum.toString()
                            val tagesEintraege = eintraege.filter { it.datum == datumKey }
                            val hatNotiz = !notizenMap[datumKey].isNullOrBlank()
                            val hatFotos = !fotosProTagMap[datumKey].isNullOrEmpty()
                            val feiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(datum) else null
                            val ferien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(datum) else null
                            val istAusgewaehlt = datum == ausgewaehlterTag
                            val istHeute = datum == heute

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(
                                        width = if (istAusgewaehlt) 1.5.dp else 0.25.dp,
                                        color = if (istAusgewaehlt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .clickable {
                                        if (ausgewaehlterTag == datum) onTagKlick(datum, tagesEintraege.firstOrNull())
                                        else ausgewaehlterTag = datum
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
                                        fontWeight = if (feiertag != null || istAusgewaehlt || istHeute) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            feiertag != null -> MaterialTheme.colorScheme.error
                                            istHeute -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (hatNotiz) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                        if (hatFotos) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        if (ferien != null) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                                    }
                                }
                                tagesEintraege.take(2).forEach { eintrag ->
                                    val tagFarbe = when {
                                        farbenMap.containsKey(eintrag.einsatzort) -> Color(farbenMap[eintrag.einsatzort]!!)
                                        farbenMap.containsKey(eintrag.titel) -> Color(farbenMap[eintrag.titel]!!)
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
                                        if (eintrag.typ == "SCHICHT" || eintrag.typ == "TERMIN") {
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
// --- START TEIL 3 (KalenderAnsicht.kt) ---
        val tagKey = ausgewaehlterTag.toString()
        val ausgewaehlteEintraege = eintraege.filter { it.datum == tagKey }
        val tagesNotiz = notizenMap[tagKey].orEmpty()
        val tagesFotos = fotosProTagMap[tagKey].orEmpty()
        val tagFeiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(ausgewaehlterTag) else null
        val tagFerien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(ausgewaehlterTag) else null

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp, max = 175.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Am ${ausgewaehlterTag.dayOfMonth}.${ausgewaehlterTag.monthValue}.${ausgewaehlterTag.year}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (tagFeiertag != null || tagFerien != null) {
                        Text(
                            text = listOfNotNull(tagFeiertag, tagFerien).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                TextButton(onClick = { onTagKlick(ausgewaehlterTag, null) }) {
                    Text("+ Eintrag")
                }
            }

            if (tagesNotiz.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onTagKlick(ausgewaehlterTag, ausgewaehlteEintraege.firstOrNull()) }
                ) {
                    Text(
                        text = "Notiz: $tagesNotiz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (tagesFotos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tagesFotos.forEach { fotoFile ->
                        val bmp = remember(fotoFile.absolutePath) {
                            runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull()
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Tagesfoto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                    .clickable { onFotoAnsehen(fotoFile) }
                            )
                        }
                    }
                }
            }

            ausgewaehlteEintraege.forEach { eintrag ->
                val ortKey = eintrag.einsatzort.ifBlank { eintrag.titel }
                val einfacheKm = standortKmMap[ortKey] ?: standortKmMap[eintrag.titel] ?: 0.0
                val gesamtKm = if (hinUndRueckfahrt) einfacheKm * 2.0 else einfacheKm
                val fahrtkosten = gesamtKm * kmPauschale

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTagKlick(ausgewaehlterTag, eintrag) }.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = eintrag.titel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val basisInfo = when (eintrag.typ) {
                            "SCHICHT" -> String.format(
                                Locale.GERMAN,
                                "%s-%s (%.1f Std. • %.2f €)",
                                eintrag.startZeit,
                                eintrag.endZeit,
                                eintrag.berechneNettoStunden(),
                                eintrag.berechneVerdienst()
                            )
                            "TERMIN" -> "${eintrag.startZeit}-${eintrag.endZeit} Uhr" +
                                if (eintrag.einsatzort.isNotBlank()) " • ${eintrag.einsatzort}" else ""
                            else -> eintrag.typ
                        }
                        val kmInfo = if (einfacheKm > 0.0 && (eintrag.typ == "SCHICHT" || eintrag.typ == "TERMIN")) {
                            String.format(Locale.GERMAN, " • %.0f km (%.2f €)", gesamtKm, fahrtkosten)
                        } else ""

                        Text(text = basisInfo + kmInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onTagKlick(ausgewaehlterTag, eintrag) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        if (onEintragDirektLoeschen != null) {
                            IconButton(onClick = { onEintragDirektLoeschen(eintrag) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
