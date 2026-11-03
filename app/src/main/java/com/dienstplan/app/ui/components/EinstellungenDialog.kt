package com.dienstplan.app.ui.components

import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EinsatzortFarbeEntity
import com.dienstplan.app.util.CloudBackupHelfer
import kotlinx.coroutines.launch

@Composable
fun EinstellungenDialog(
    nrwAktiv: Boolean,
    onNrwUmschalten: () -> Unit,
    gespeicherteFarben: List<EinsatzortFarbeEntity>,
    onFarbeSpeichern: (String, Long) -> Unit,
    onFarbeLoeschen: (EinsatzortFarbeEntity) -> Unit,
    onSchliessen: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var neuerOrt by remember { mutableStateOf("") }
    val farbPalette = listOf(0xFF1E88E5L, 0xFF43A047L, 0xFFE53935L, 0xFFFB8C00L, 0xFF8E24AAL, 0xFF00897BL)
    var gewaehlteFarbe by remember { mutableLongStateOf(farbPalette.first()) }

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Einstellungen & Backup") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NRW Feiertage & Ferien", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = nrwAktiv, onCheckedChange = { onNrwUmschalten() })
                }
                HorizontalDivider()

                Text("Farbcodierung für Einsatzorte", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = neuerOrt,
                    onValueChange = { neuerOrt = it },
                    label = { Text("Einsatzort (z. B. Büro, Filiale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    farbPalette.forEach { farbHex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(farbHex))
                                .border(if (gewaehlteFarbe == farbHex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(eintrag.farbHex)))
                            Text("  ${eintrag.einsatzort}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onFarbeLoeschen(eintrag) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Farbe löschen")
                        }
                    }
                }

                HorizontalDivider()
                Text("Cloud-Backup (Google Drive API)", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val meldung = CloudBackupHelfer.datenbankSichern(context)
                                Toast.makeText(context, meldung, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sichern")
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val meldung = CloudBackupHelfer.datenbankWiederherstellen(context)
                                Toast.makeText(context, meldung, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Laden")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) { Text("Fertig") }
        }
    )
}
