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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dienstplan.app.data.EinsatzortFarbeEntity
import com.dienstplan.app.util.CloudBackupHelfer
import com.dienstplan.app.util.NrwDatenHelfer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val datumsFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }

    LaunchedEffect(Unit) {
        NrwDatenHelfer.initialisiere(context)
    }

    var aktiverTab by remember { mutableIntStateOf(0) }
    var vorschauJahr by remember { mutableIntStateOf(LocalDate.now().year) }
    var ferienTitel by remember { mutableStateOf("") }
    var ferienStart by remember { mutableStateOf("") }
    var ferienEnde by remember { mutableStateOf("") }
    var syncLaeuft by remember { mutableStateOf(false) }

    var neuerOrt by remember { mutableStateOf("") }
    val farbPalette = listOf(0xFF1E88E5L, 0xFF43A047L, 0xFFE53935L, 0xFFFB8C00L, 0xFF8E24AAL, 0xFF00897BL)
    var gewaehlteFarbe by remember { mutableLongStateOf(farbPalette.first()) }
// --- START TEIL 2 (EinstellungenDialog.kt) ---
    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Einstellungen & NRW-Daten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = aktiverTab == 0,
                        onClick = { aktiverTab = 0 },
                        label = { Text("Feiertage & Ferien") }
                    )
                    FilterChip(
                        selected = aktiverTab == 1,
                        onClick = { aktiverTab = 1 },
                        label = { Text("Farben & Cloud") }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (aktiverTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NRW Feiertage & Schulferien", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Im Kalender & in der Tagesleiste anzeigen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = nrwAktiv, onCheckedChange = { onNrwUmschalten() })
                    }

                    OutlinedButton(
                        onClick = {
                            if (!syncLaeuft) {
                                syncLaeuft = true
                                coroutineScope.launch {
                                    val status = NrwDatenHelfer.synchronisiereOpenHolidaysApi(context, vorschauJahr)
                                    syncLaeuft = false
                                    Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(if (syncLaeuft) " Aktualisiere über OpenHolidaysAPI..." else " Schulferien online aktualisieren (API)")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Eigene Ferientage / Schließtage hinzufügen", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = ferienTitel,
                        onValueChange = { ferienTitel = it },
                        label = { Text("Bezeichnung (z. B. Beweglicher Ferientag, KiTa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ferienStart,
                            onValueChange = { ferienStart = it },
                            label = { Text("Von (TT.MM.JJJJ)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ferienEnde,
                            onValueChange = { ferienEnde = it },
                            label = { Text("Bis (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val erfolg = NrwDatenHelfer.manuelleFerienHinzufuegen(context, ferienStart, ferienEnde, ferienTitel)
                            if (erfolg) {
                                ferienTitel = ""
                                ferienStart = ""
                                ferienEnde = ""
                                Toast.makeText(context, "Eigener Ferientag gespeichert", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Bitte gültiges Datum eingeben (z. B. 15.05.2026)", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Eigenen Zeitraum speichern")
                    }
// --- START TEIL 3 (EinstellungenDialog.kt) ---
                    if (NrwDatenHelfer.manuelleFerienListe.isNotEmpty()) {
                        Text("Gespeicherte eigene Tage:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        NrwDatenHelfer.manuelleFerienListe.forEach { eintrag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val zeitText = if (eintrag.start == eintrag.ende) eintrag.start.format(datumsFormat) else "${eintrag.start.format(datumsFormat)} – ${eintrag.ende.format(datumsFormat)}"
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(eintrag.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(zeitText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { NrwDatenHelfer.manuelleFerienLoeschen(context, eintrag) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Jahresübersicht $vorschauJahr", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { vorschauJahr-- }) { Text("${vorschauJahr - 1}") }
                            TextButton(onClick = { vorschauJahr++ }) { Text("${vorschauJahr + 1}") }
                        }
                    }
                    NrwDatenHelfer.holeFerienFuerJahr(vorschauJahr).forEach { f ->
                        val z = if (f.start == f.ende) f.start.format(datumsFormat) else "${f.start.format(datumsFormat)} – ${f.ende.format(datumsFormat)}"
                        Text("• ${f.name}: $z", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    val alleFarbOptionen = listOf(
                        0xFF1E88E5L, 0xFF00ACC1L, 0xFF43A047L, 0xFF7CB342L,
                        0xFFFDD835L, 0xFFFB8C00L, 0xFFE53935L, 0xFFD81B60L,
                        0xFF8E24AAL, 0xFF5E35B1L, 0xFF546E7AL, 0xFF6D4C41L
                    )
                    Text("Optionale Farben für ALLES (Märkte, Frei, Urlaub, Termine)", style = MaterialTheme.typography.titleSmall)
                    Text("Tippe auf einen Schnellbegriff oder gib einen eigenen Namen ein:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Frei", "Urlaub", "Krank", "Termin", "SCHICHT").forEach { vorschlag ->
                            FilterChip(selected = neuerOrt == vorschlag, onClick = { neuerOrt = vorschlag }, label = { Text(vorschlag) })
                        }
                    }

                    OutlinedTextField(
                        value = neuerOrt,
                        onValueChange = { neuerOrt = it },
                        label = { Text("Begriff / Ort / Typ (z. B. Bielefeld, Loom oder Frei)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        alleFarbOptionen.take(6).forEach { farbHex ->
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(farbHex))
                                    .border(if (gewaehlteFarbe == farbHex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { gewaehlteFarbe = farbHex }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        alleFarbOptionen.drop(6).forEach { farbHex ->
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(farbHex))
                                    .border(if (gewaehlteFarbe == farbHex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { gewaehlteFarbe = farbHex }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (neuerOrt.isNotBlank()) {
                                onFarbeSpeichern(neuerOrt.trim(), gewaehlteFarbe)
                                neuerOrt = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Farbe speichern (optional)") }

                    gespeicherteFarben.forEach { eintrag ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(eintrag.farbHex)))
                                Text("  ${eintrag.einsatzort}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { onFarbeLoeschen(eintrag) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Farbe löschen")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Cloud-Backup (Google Drive API)", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { coroutineScope.launch { Toast.makeText(context, CloudBackupHelfer.datenbankSichern(context), Toast.LENGTH_LONG).show() } }, modifier = Modifier.weight(1f)) { Text("Sichern") }
                        OutlinedButton(onClick = { coroutineScope.launch { Toast.makeText(context, CloudBackupHelfer.datenbankWiederherstellen(context), Toast.LENGTH_LONG).show() } }, modifier = Modifier.weight(1f)) { Text("Laden") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSchliessen) { Text("Fertig") } }
    )
}
// --- ENDE TEIL 3 (EinstellungenDialog.kt) ---
