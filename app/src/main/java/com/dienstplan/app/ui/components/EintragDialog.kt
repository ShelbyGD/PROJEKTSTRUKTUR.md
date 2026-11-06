package com.dienstplan.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EintragEntity
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class SchichtVorlage(
    val name: String,
    val typ: String,
    val titel: String,
    val startZeit: String = "08:00",
    val endZeit: String = "16:30",
    val pauseMinuten: Int = 30,
    val einsatzort: String = ""
)

@Composable
fun EintragDialog(
    initialDatum: String,
    bestehenderEintrag: EintragEntity?,
    alleEintraege: List<EintragEntity> = emptyList(),
    standardStundenlohn: Double = 15.0,
    initialTagesNotiz: String = "",
    initialStandortKm: Double = 0.0,
    tagesFotos: List<File> = emptyList(),
    onDatumGeaendert: (String) -> Unit = {},
    onFotoAusGalerie: (String) -> Unit = {},
    onFotoMitKamera: (String) -> Unit = {},
    onFotoLoeschen: (File) -> Unit = {},
    onFotoAnsehen: (File) -> Unit = {},
    onSpeichern: (EintragEntity, String, Double) -> Unit,
    onLoeschen: ((EintragEntity) -> Unit)?,
    onAbbrechen: () -> Unit
) {
    var datum by remember(initialDatum) { mutableStateOf(bestehenderEintrag?.datum ?: initialDatum) }
    var typ by remember(bestehenderEintrag) { mutableStateOf(bestehenderEintrag?.typ ?: "SCHICHT") }
    var titel by remember(bestehenderEintrag) { mutableStateOf(bestehenderEintrag?.titel ?: "Schicht") }
    var startZeit by remember(bestehenderEintrag) { mutableStateOf(bestehenderEintrag?.startZeit ?: "10:00") }
    var endZeit by remember(bestehenderEintrag) { mutableStateOf(bestehenderEintrag?.endZeit ?: "21:00") }
    var pauseText by remember(bestehenderEintrag) { mutableStateOf((bestehenderEintrag?.pauseMinuten ?: 60).toString()) }
    var lohnText by remember(bestehenderEintrag, standardStundenlohn) {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", bestehenderEintrag?.stundenlohn ?: standardStundenlohn))
    }
    var einsatzort by remember(bestehenderEintrag) { mutableStateOf(bestehenderEintrag?.einsatzort ?: "") }
    var tagesNotiz by remember(initialDatum, initialTagesNotiz) { mutableStateOf(initialTagesNotiz) }
    var kmText by remember(initialDatum, initialStandortKm) {
        mutableStateOf(if (initialStandortKm > 0.0) String.format(Locale.GERMAN, "%.1f", initialStandortKm) else "")
    }
// --- START TEIL 2 (EintragDialog.kt) ---
    val gelernteSchichtVorlagen = remember(alleEintraege) {
        val ausHistorie = alleEintraege
            .filter { it.typ == "SCHICHT" && it.titel.isNotBlank() }
            .distinctBy { "${it.titel}|${it.startZeit}|${it.endZeit}" }
            .take(8)
            .map {
                SchichtVorlage(
                    name = "${it.titel} (${it.startZeit}-${it.endZeit})",
                    typ = "SCHICHT",
                    titel = it.titel,
                    startZeit = it.startZeit,
                    endZeit = it.endZeit,
                    pauseMinuten = it.pauseMinuten,
                    einsatzort = it.einsatzort
                )
            }
        val standardVorlagen = listOf(
            SchichtVorlage("Frei", "FREI", "Frei"),
            SchichtVorlage("Urlaub", "FREI", "Urlaub"),
            SchichtVorlage("Krank", "FREI", "Selber krank"),
            SchichtVorlage("Kind krank", "FREI", "Kind Krank"),
            SchichtVorlage("Arzttermin", "TERMIN", "Arzttermin", "09:00", "10:00", 0)
        )
        ausHistorie + standardVorlagen
    }

    fun datumAendern(tageOffset: Long) {
        runCatching {
            val neuesDatum = LocalDate.parse(datum.trim()).plusDays(tageOffset).toString()
            datum = neuesDatum
            onDatumGeaendert(neuesDatum)
        }
    }

    val formatiertesDatum = remember(datum) {
        runCatching {
            LocalDate.parse(datum.trim()).format(DateTimeFormatter.ofPattern("EE., dd.MM.yyyy", Locale.GERMAN))
        }.getOrDefault(datum)
    }

    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bestehenderEintrag == null) "Neuer Eintrag" else "Eintrag bearbeiten",
                    style = MaterialTheme.typography.titleLarge
                )
                if (bestehenderEintrag != null && onLoeschen != null) {
                    IconButton(onClick = { onLoeschen(bestehenderEintrag) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { datumAendern(-1) }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Vorheriger Tag")
                        }
                        Text(
                            text = formatiertesDatum,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { datumAendern(1) }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Nächster Tag")
                        }
                    }
                }

                Text("Schnellauswahl:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gelernteSchichtVorlagen.forEach { vorlage ->
                        AssistChip(
                            onClick = {
                                typ = vorlage.typ
                                titel = vorlage.titel
                                startZeit = vorlage.startZeit
                                endZeit = vorlage.endZeit
                                pauseText = vorlage.pauseMinuten.toString()
                                einsatzort = if (vorlage.einsatzort.isNotBlank()) vorlage.einsatzort else if (vorlage.typ == "SCHICHT") vorlage.titel else ""
                            },
                            label = { Text(vorlage.name) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SCHICHT" to "Schicht", "FREI" to "Frei", "TERMIN" to "Termin").forEach { (key, label) ->
                        FilterChip(
                            selected = typ == key,
                            onClick = {
                                typ = key
                                if (key == "FREI" && (titel == "Schicht" || titel == "Frühschicht")) titel = "Frei"
                                if (key == "TERMIN" && (titel == "Schicht" || titel == "Frei")) titel = "Arzttermin"
                            },
                            label = { Text(label) }
                        )
                    }
                }
// --- START TEIL 3 (EintragDialog.kt) ---
                OutlinedTextField(
                    value = titel,
                    onValueChange = {
                        titel = it
                        if (typ == "SCHICHT" && einsatzort.isBlank()) einsatzort = it
                    },
                    label = { Text(if (typ == "SCHICHT") "Bezeichnung / Markt" else if (typ == "FREI") "Bezeichnung (z. B. Frei, Urlaub)" else "Termin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (typ == "SCHICHT") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startZeit, onValueChange = { startZeit = it }, label = { Text("Start (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endZeit, onValueChange = { endZeit = it }, label = { Text("Ende (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pauseText, onValueChange = { pauseText = it }, label = { Text("Pause (Min.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = lohnText, onValueChange = { lohnText = it }, label = { Text("Lohn (€/Std.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = einsatzort, onValueChange = { einsatzort = it }, label = { Text("Einsatzort") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        OutlinedTextField(value = kmText, onValueChange = { kmText = it }, label = { Text("Strecke (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.9f))
                    }
                } else if (typ == "TERMIN") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startZeit, onValueChange = { startZeit = it }, label = { Text("Von (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endZeit, onValueChange = { endZeit = it }, label = { Text("Bis (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = einsatzort, onValueChange = { einsatzort = it }, label = { Text("Ort") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        OutlinedTextField(value = kmText, onValueChange = { kmText = it }, label = { Text("Strecke (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.9f))
                    }
                }

                OutlinedTextField(
                    value = tagesNotiz,
                    onValueChange = { tagesNotiz = it },
                    label = { Text("Notiz zum Tag (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fotos (${tagesFotos.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onFotoAusGalerie(datum) }) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Galerie", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onFotoMitKamera(datum) }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Kamera", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (tagesFotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagesFotos.forEach { fotoFile ->
                            val bitmap = remember(fotoFile.absolutePath) {
                                runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull()
                            }
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .clickable { onFotoAnsehen(fotoFile) }
                                ) {
                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                            .clickable { onFotoLoeschen(fotoFile) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val saubererTitel = titel.trim().ifBlank { typ }
                val saubererOrt = einsatzort.trim().ifBlank { if (typ == "SCHICHT") saubererTitel else "" }
                val geparsteKm = kmText.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                val eintrag = EintragEntity(
                    id = bestehenderEintrag?.id ?: 0L,
                    datum = datum.trim(),
                    typ = typ,
                    titel = saubererTitel,
                    startZeit = startZeit.trim(),
                    endZeit = endZeit.trim(),
                    pauseMinuten = if (typ == "SCHICHT") (pauseText.toIntOrNull() ?: 0) else 0,
                    stundenlohn = if (typ == "SCHICHT") (lohnText.replace(",", ".").toDoubleOrNull() ?: standardStundenlohn) else 0.0,
                    einsatzort = saubererOrt
                )
                onSpeichern(eintrag, tagesNotiz.trim(), geparsteKm)
            }) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (bestehenderEintrag != null && onLoeschen != null) {
                    TextButton(onClick = { onLoeschen(bestehenderEintrag) }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
            }
        }
    )
}
