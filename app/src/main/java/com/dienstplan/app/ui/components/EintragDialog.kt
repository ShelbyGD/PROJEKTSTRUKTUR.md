package com.dienstplan.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EintragEntity
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
    onSpeichern: (EintragEntity) -> Unit,
    onLoeschen: ((EintragEntity) -> Unit)?,
    onAbbrechen: () -> Unit
) {
    var datum by remember { mutableStateOf(bestehenderEintrag?.datum ?: initialDatum) }
    var typ by remember { mutableStateOf(bestehenderEintrag?.typ ?: "SCHICHT") }
    var titel by remember { mutableStateOf(bestehenderEintrag?.titel ?: "Schicht") }
    var startZeit by remember { mutableStateOf(bestehenderEintrag?.startZeit ?: "10:00") }
    var endZeit by remember { mutableStateOf(bestehenderEintrag?.endZeit ?: "21:00") }
    var pauseText by remember { mutableStateOf((bestehenderEintrag?.pauseMinuten ?: 60).toString()) }
    var lohnText by remember { mutableStateOf((bestehenderEintrag?.stundenlohn ?: 15.0).toString()) }
    var einsatzort by remember { mutableStateOf(bestehenderEintrag?.einsatzort ?: "") }

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
            val geparst = LocalDate.parse(datum.trim())
            datum = geparst.plusDays(tageOffset).toString()
        }
    }

    val formatiertesDatum = remember(datum) {
        runCatching {
            val geparst = LocalDate.parse(datum.trim())
            geparst.format(DateTimeFormatter.ofPattern("EE., dd.MM.yyyy", Locale.GERMAN))
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
                    text = if (bestehenderEintrag == null) "Eintrag planen" else "Eintrag bearbeiten",
                    style = MaterialTheme.typography.titleLarge
                )
                if (bestehenderEintrag != null && onLoeschen != null) {
                    IconButton(onClick = { onLoeschen(bestehenderEintrag) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eintrag löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { datumAendern(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }
                        Text(
                            text = formatiertesDatum,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { datumAendern(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Tag")
                        }
                    }
                }

                Text(
                    text = "Schnellauswahl (1-Klick-Vorlagen):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                                if (vorlage.einsatzort.isNotBlank()) {
                                    einsatzort = vorlage.einsatzort
                                }
                            },
                            label = { Text(vorlage.name) }
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "SCHICHT" to "Schicht",
                        "FREI" to "Frei / Urlaub",
                        "TERMIN" to "Termin"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = typ == key,
                            onClick = {
                                typ = key
                                if (key == "FREI" && (titel == "Schicht" || titel == "Frühschicht")) {
                                    titel = "Frei"
                                }
                                if (key == "TERMIN" && (titel == "Schicht" || titel == "Frei")) {
                                    titel = "Arzttermin"
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = titel,
                    onValueChange = {
                        titel = it
                        if (typ == "SCHICHT" && einsatzort.isBlank()) {
                            einsatzort = it
                        }
                    },
                    label = {
                        Text(
                            when (typ) {
                                "SCHICHT" -> "Markt / Schicht (z. B. Bielefeld, Loom)"
                                "FREI" -> "Grund (z. B. Frei, Urlaub, Krank)"
                                else -> "Termin-Bezeichnung (z. B. Zahnarzt Lio)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (typ == "SCHICHT") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startZeit,
                            onValueChange = { startZeit = it },
                            label = { Text("Start (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endZeit,
                            onValueChange = { endZeit = it },
                            label = { Text("Ende (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pauseText,
                            onValueChange = { pauseText = it },
                            label = { Text("Pause (Min.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lohnText,
                            onValueChange = { lohnText = it },
                            label = { Text("Lohn (€/Std.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = einsatzort,
                        onValueChange = { einsatzort = it },
                        label = { Text("Farb-Kategorie / Ort (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (typ == "TERMIN") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startZeit,
                            onValueChange = { startZeit = it },
                            label = { Text("Von (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endZeit,
                            onValueChange = { endZeit = it },
                            label = { Text("Bis (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = einsatzort,
                        onValueChange = { einsatzort = it },
                        label = { Text("Adresse / Notiz zum Termin (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = einsatzort,
                        onValueChange = { einsatzort = it },
                        label = { Text("Notiz / Zusatzinfo (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val saubererTitel = titel.trim().ifBlank { typ }
                    val saubererOrt = einsatzort.trim().ifBlank {
                        if (typ == "SCHICHT") saubererTitel else ""
                    }
                    val eintrag = EintragEntity(
                        id = bestehenderEintrag?.id ?: 0L,
                        datum = datum.trim(),
                        typ = typ,
                        titel = saubererTitel,
                        startZeit = startZeit.trim(),
                        endZeit = endZeit.trim(),
                        pauseMinuten = if (typ == "SCHICHT") (pauseText.toIntOrNull() ?: 0) else 0,
                        stundenlohn = if (typ == "SCHICHT") (lohnText.replace(",", ".").toDoubleOrNull() ?: 0.0) else 0.0,
                        einsatzort = saubererOrt
                    )
                    onSpeichern(eintrag)
                }
            ) {
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
                TextButton(onClick = onAbbrechen) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
```
