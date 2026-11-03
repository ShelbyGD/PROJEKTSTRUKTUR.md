package com.dienstplan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EinsatzortFarbeEntity

@Composable
fun EinstellungenDialog(
    nrwAktiv: Boolean,
    onNrwUmschalten: () -> Unit,
    gespeicherteFarben: List<EinsatzortFarbeEntity>,
    onFarbeSpeichern: (String, Long) -> Unit,
    onFarbeLoeschen: (EinsatzortFarbeEntity) -> Unit,
    onSchliessen: () -> Unit
) {
    var neuerOrt by remember { mutableStateOf("") }
    val farbPalette = listOf(
        0xFF1E88E5L, // Blau
        0xFF43A047L, // Grün
        0xFFE53935L, // Rot
        0xFFFB8C00L, // Orange
        0xFF8E24AAL, // Lila
        0xFF00897BL  // Türkis
    )
    var gewaehlteFarbe by remember { mutableLongStateOf(farbPalette.first()) }

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Einstellungen & Farben") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NRW Feiertage & Schulferien Schalter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NRW Feiertage & Ferien",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = nrwAktiv, onCheckedChange = { onNrwUmschalten() })
                }
                HorizontalDivider()

                Text(
                    text = "Farbcodierung für Einsatzorte",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = neuerOrt,
                    onValueChange = { neuerOrt = it },
                    label = { Text("Einsatzort (z. B. Büro, Filiale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    farbPalette.forEach { farbHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(farbHex))
                                .border(
                                    width = if (gewaehlteFarbe == farbHex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { gewaehlteFarbe = farbHex }
                        )
                    }
                }
                Button(
                    onClick = {
                        if (neuerOrt.isNotBlank()) {
                            onFarbeSpeichern(neuerOrt, gewaehlteFarbe)
                            neuerOrt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Einsatzort-Farbe speichern")
                }

                gespeicherteFarben.forEach { eintrag ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(eintrag.farbHex))
                            )
                            Text(text = "  ${eintrag.einsatzort}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onFarbeLoeschen(eintrag) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Farbe löschen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) {
                Text("Fertig")
            }
        }
    )
}
