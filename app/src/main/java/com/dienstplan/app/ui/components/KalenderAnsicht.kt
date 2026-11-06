// --- START MINI-BLOCK 1 VON 5 (KalenderAnsicht.kt) ---
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
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
// --- START MINI-BLOCK 2 VON 5 (KalenderAnsicht.kt) ---
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
    val context = LocalContext.current
    LaunchedEffect(Unit) { NrwDatenHelfer.initialisiere(context) }

    val heute = LocalDate.now()
    var ausgewaehlterTag by remember(monat) {
        mutableStateOf(if (YearMonth.from(heute) == monat) heute else monat.atDay(1))
    }
    val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val ersterOffset = monat.atDay(1).dayOfWeek.value - 1
    val zellen = List(ersterOffset) { null } + (1..monat.lengthOfMonth()).map { monat.atDay(it) }
    val wochenZeilen = zellen.chunked(7)
    val wochenFeld = WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("KW", modifier = Modifier.width(26.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            wochentage.forEach { tagName ->
                Text(tagName, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(monat) {
                var wischWeg = 0f
                detectHorizontalDragGestures(
                    onDragStart = { wischWeg = 0f },
                    onHorizontalDrag = { change, dragAmount -> change.consume(); wischWeg += dragAmount },
                    onDragEnd = {
                        if (wischWeg > 95f) onMonatWechseln(-1L)
                        else if (wischWeg < -95f) onMonatWechseln(1L)
                    }
                )
            }
        ) {
            wochenZeilen.forEach { woche ->
                val kw = woche.filterNotNull().firstOrNull()?.get(wochenFeld)?.toString() ?: ""
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier.width(26.dp).fillMaxHeight().border(0.25.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(kw, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                    }
                    for (i in 0 until 7) {
                        val datum = woche.getOrNull(i)
                        if (datum == null) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight())
                        } else {
                            val key = datum.toString()
                            KalenderTagZelle(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                datum = datum,
                                istAusgewaehlt = datum == ausgewaehlterTag,
                                istHeute = datum == heute,
                                tagesEintraege = eintraege.filter { it.datum == key },
                                farbenMap = farbenMap,
                                feiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(datum) else null,
                                ferien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(datum) else null,
                                hatNotiz = !notizenMap[key].isNullOrBlank(),
                                hatFotos = !fotosProTagMap[key].isNullOrEmpty(),
                                onKlick = {
                                    val liste = eintraege.filter { it.datum == key }
                                    if (ausgewaehlterTag == datum) onTagKlick(datum, liste.firstOrNull())
                                    else ausgewaehlterTag = datum
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        KalenderTagesDetails(
            tag = ausgewaehlterTag,
            eintraege = eintraege.filter { it.datum == ausgewaehlterTag.toString() },
            nrwAktiv = nrwAktiv,
            tagesNotiz = notizenMap[ausgewaehlterTag.toString()].orEmpty(),
            tagesFotos = fotosProTagMap[ausgewaehlterTag.toString()].orEmpty(),
            standortKmMap = standortKmMap,
            kmPauschale = kmPauschale,
            hinUndRueckfahrt = hinUndRueckfahrt,
            onFotoAnsehen = onFotoAnsehen,
            onTagKlick = onTagKlick,
            onEintragDirektLoeschen = onEintragDirektLoeschen
        )
    }
}
// --- START MINI-BLOCK 3 VON 5 (KalenderAnsicht.kt) ---
@Composable
private fun KalenderTagZelle(
    modifier: Modifier,
    datum: LocalDate,
    istAusgewaehlt: Boolean,
    istHeute: Boolean,
    tagesEintraege: List<EintragEntity>,
    farbenMap: Map<String, Long>,
    feiertag: String?,
    ferien: String?,
    hatNotiz: Boolean,
    hatFotos: Boolean,
    onKlick: () -> Unit
) {
    val hintergrund = when {
        istAusgewaehlt -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        feiertag != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)
        ferien != null -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
        else -> Color.Transparent
    }
    val rahmenFarbe = if (istAusgewaehlt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .background(hintergrund)
            .border(if (istAusgewaehlt) 1.6.dp else 0.25.dp, rahmenFarbe)
            .clickable { onKlick() }
    ) {
        if (feiertag != null) {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error).padding(horizontal = 2.dp, vertical = 1.dp)) {
                Text(feiertag.replace(" (NRW)", ""), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 8.sp)
            }
        } else if (ferien != null) {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiary).padding(horizontal = 2.dp, vertical = 1.dp)) {
                Text(ferien.replace(" NRW", "").replace(" (NRW)", ""), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 8.sp)
            }
        }

        Column(modifier = Modifier.padding(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = datum.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (feiertag != null || ferien != null || istAusgewaehlt || istHeute) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        feiertag != null -> MaterialTheme.colorScheme.error
                        istHeute -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hatNotiz) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    if (hatFotos) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
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
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).background(tagFarbe.copy(alpha = 0.22f), RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(eintrag.titel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = tagFarbe, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 10.sp)
                    if (eintrag.typ == "SCHICHT" || eintrag.typ == "TERMIN") {
                        Text("${eintrag.startZeit}-${eintrag.endZeit}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, lineHeight = 9.sp)
                    }
                }
            }
        }
    }
}
// --- START MINI-BLOCK 4 VON 5 (KalenderAnsicht.kt) ---
@Composable
private fun KalenderTagesDetails(
    tag: LocalDate,
    eintraege: List<EintragEntity>,
    nrwAktiv: Boolean,
    tagesNotiz: String,
    tagesFotos: List<File>,
    standortKmMap: Map<String, Double>,
    kmPauschale: Double,
    hinUndRueckfahrt: Boolean,
    onFotoAnsehen: (File) -> Unit,
    onTagKlick: (LocalDate, EintragEntity?) -> Unit,
    onEintragDirektLoeschen: ((EintragEntity) -> Unit)?
) {
    val tagFeiertag = if (nrwAktiv) NrwDatenHelfer.holeFeiertagNrw(tag) else null
    val tagFerien = if (nrwAktiv) NrwDatenHelfer.holeSchulferienNrw(tag) else null

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp, max = 175.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Am ${tag.dayOfMonth}.${tag.monthValue}.${tag.year}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (tagFeiertag != null || tagFerien != null) {
                    Text(
                        text = listOfNotNull(tagFeiertag, tagFerien).joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tagFeiertag != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            TextButton(onClick = { onTagKlick(tag, null) }) { Text("+ Eintrag") }
        }

        if (tagesNotiz.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onTagKlick(tag, eintraege.firstOrNull()) }
            ) {
                Text("Notiz: $tagesNotiz", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        if (tagesFotos.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tagesFotos.forEach { fotoFile ->
                    val bmp = remember(fotoFile.absolutePath) { runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull() }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Tagesfoto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)).clickable { onFotoAnsehen(fotoFile) }
                        )
                    }
                }
            }
        }

        eintraege.forEach { eintrag ->
            KalenderEintragZeile(
                eintrag = eintrag,
                standortKmMap = standortKmMap,
                kmPauschale = kmPauschale,
                hinUndRueckfahrt = hinUndRueckfahrt,
                onBearbeiten = { onTagKlick(tag, eintrag) },
                onLoeschen = onEintragDirektLoeschen
            )
        }
    }
}
// --- START MINI-BLOCK 5 VON 5 (KalenderAnsicht.kt) ---
@Composable
private fun KalenderEintragZeile(
    eintrag: EintragEntity,
    standortKmMap: Map<String, Double>,
    kmPauschale: Double,
    hinUndRueckfahrt: Boolean,
    onBearbeiten: () -> Unit,
    onLoeschen: ((EintragEntity) -> Unit)?
) {
    val ortKey = eintrag.einsatzort.ifBlank { eintrag.titel }
    val einfacheKm = standortKmMap[ortKey] ?: standortKmMap[eintrag.titel] ?: 0.0
    val gesamtKm = if (hinUndRueckfahrt) einfacheKm * 2.0 else einfacheKm
    val fahrtkosten = gesamtKm * kmPauschale

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onBearbeiten() }.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(eintrag.titel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text(basisInfo + kmInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBearbeiten, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            if (onLoeschen != null) {
                IconButton(onClick = { onLoeschen(eintrag) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
