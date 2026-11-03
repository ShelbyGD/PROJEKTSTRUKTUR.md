package com.dienstplan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EintragEntity

@Composable
fun EintragDialog(
    initialDatum: String,
    bestehenderEintrag: EintragEntity?,
    onSpeichern: (EintragEntity) -> Unit,
    onLoeschen: ((EintragEntity) -> Unit)?,
    onAbbrechen: () -> Unit
) {
    var datum by remember { mutableStateOf(bestehenderEintrag?.datum ?: initialDatum) }
    var typ by remember { mutableStateOf(bestehenderEintrag?.typ ?: "SCHICHT") }
    var titel by remember { mutableStateOf(bestehenderEintrag?.titel ?: "Frühschicht") }
    var startZeit by remember { mutableStateOf(bestehenderEintrag?.startZeit ?: "08:00") }
    var endZeit by remember { mutableStateOf(bestehenderEintrag?.endZeit ?: "16:30") }
    var pauseText by remember { mutableStateOf((bestehenderEintrag?.pauseMinuten ?: 30).toString()) }
    var lohnText by remember { mutableStateOf((bestehenderEintrag?.stundenlohn ?: 15.0).toString()) }
    var einsatzort by remember { mutableStateOf(bestehenderEintrag?.einsatzort ?: "") }

    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = {
            Text(if (bestehenderEintrag == null) "Neuer Eintrag" else "Eintrag bearbeiten")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auswahl zwischen Schicht, Freier Tag und Termin
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SCHICHT" to "Schicht", "FREI" to "Frei", "TERMIN" to "Termin").forEach { (key, label) ->
                        FilterChip(
                            selected = typ == key,
                            onClick = {
                                typ = key
                                if (key == "FREI" && titel == "Frühschicht") titel = "Freier Tag"
                                if (key == "TERMIN" && titel == "Frühschicht") titel = "Arzttermin"
                            },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = datum,
                    onValueChange = { datum = it },
                    label = { Text("Datum (JJJJ-MM-TT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titel,
                    onValueChange = { titel = it },
                    label = { Text("Bezeichnung / Titel") },
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
                        label = { Text("Einsatzort (für Farbcodierung)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val eintrag = EintragEntity(
                    id = bestehenderEintrag?.id ?: 0L,
                    datum = datum.trim(),
                    typ = typ,
                    titel = titel.ifBlank { typ },
                    startZeit = startZeit.trim(),
                    endZeit = endZeit.trim(),
                    pauseMinuten = pauseText.toIntOrNull() ?: 0,
                    stundenlohn = lohnText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    einsatzort = einsatzort.trim()
                )
                onSpeichern(eintrag)
            }) {
                Text("Speichern")
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
