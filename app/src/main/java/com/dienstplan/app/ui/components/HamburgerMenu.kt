// --- START BLOCK 1 VON 3 (HamburgerMenu.kt) ---
package com.dienstplan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
// --- ENDE BLOCK 1 VON 3 (HamburgerMenu.kt) ---
// --- START BLOCK 2 VON 3 (HamburgerMenu.kt) ---
@Composable
fun HamburgerMenuInhalt(
    istKalenderAnsicht: Boolean,
    aktuellerMonatName: String,
    aktuellerStundenlohn: Double,
    steuerklasse: Int = 1,
    monatsFahrtkosten: Double,
    anzahlFotosGesamt: Int,
    anzahlLogs: Int,
    onAnsichtWechseln: () -> Unit,
    onSucheOeffnen: () -> Unit,
    onFotoGalerieOeffnen: () -> Unit,
    onStundenlohnOeffnen: () -> Unit,
    onFahrtkostenOeffnen: () -> Unit,
    onEinstellungenOeffnen: () -> Unit,
    onPdfImport: () -> Unit,
    onMonatLeeren: () -> Unit,
    onAufGeraetSpeichern: () -> Unit,
    onVomGeraetLaden: () -> Unit,
    onFehlerLogOeffnen: () -> Unit
) {
    var systemOffen by remember { mutableStateOf(false) }
    val lohnSub = String.format(Locale.GERMAN, "%.2f €/Std. • Steuerklasse %d", aktuellerStundenlohn, steuerklasse)
    val fahrtSub = String.format(Locale.GERMAN, "Monatssumme: %.2f €", monatsFahrtkosten)

    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dienstplan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(aktuellerMonatName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // 2x2 SCHNELLZUGRIFF-KACHELN OBEN
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SchnellKachel(Modifier.weight(1f), Icons.Default.PictureAsPdf, "PDF-Import", true, onPdfImport)
                SchnellKachel(
                    Modifier.weight(1f),
                    if (istKalenderAnsicht) Icons.Default.ViewList else Icons.Default.CalendarMonth,
                    if (istKalenderAnsicht) "Listenansicht" else "Kalender",
                    false,
                    onAnsichtWechseln
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SchnellKachel(Modifier.weight(1f), Icons.Default.Search, "Suche", false, onSucheOeffnen)
                SchnellKachel(Modifier.weight(1f), Icons.Default.Image, "Fotos ($anzahlFotosGesamt)", false, onFotoGalerieOeffnen)
            }

            // KARTE 1: FINANZEN & RECHNER
            Text("Finanzen & Rechner", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                Column {
                    MenueZeile(Icons.Default.Edit, "Brutto-Netto & Stundenlohn", lohnSub, onClick = onStundenlohnOeffnen)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MenueZeile(Icons.Default.Place, "Fahrtkosten & Standorte", fahrtSub, onClick = onFahrtkostenOeffnen)
                }
            }
// --- ENDE BLOCK 2 VON 3 (HamburgerMenu.kt) ---
// --- START BLOCK 3 VON 3 (HamburgerMenu.kt) ---
            // KARTE 2: KALENDER & DARSTELLUNG
            Text("Kalender & Darstellung", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                Column {
                    MenueZeile(Icons.Default.Event, "Feiertage & Schulferien", "NRW-Daten, Online-API & eigene Tage", onClick = onEinstellungenOeffnen)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MenueZeile(Icons.Default.Palette, "Einsatzort-Farben & Cloud", "Farbcodierung für Märkte verwalten", onClick = onEinstellungenOeffnen)
                }
            }

            // KARTE 3: SYSTEM, BACKUP & AUFRÄUMEN (AUSKLAPPBAR)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { systemOffen = !systemOffen }.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System, Backup & Aufräumen", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Icon(if (systemOffen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    if (systemOffen) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        MenueZeile(Icons.Default.Save, "Backup speichern (.db)", "Datenbank lokal sichern", onClick = onAufGeraetSpeichern)
                        MenueZeile(Icons.Default.Download, "Backup laden (.db)", "Sicherung wiederherstellen", onClick = onVomGeraetLaden)
                        MenueZeile(Icons.Default.BugReport, "Diagnose-Log ($anzahlLogs)", "Protokoll für Fehlersuche", onClick = onFehlerLogOeffnen)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        MenueZeile(Icons.Default.Delete, "Monat leeren ($aktuellerMonatName)", "Alle Einträge im Monat löschen", MaterialTheme.colorScheme.error, onMonatLeeren)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SchnellKachel(modifier: Modifier, icon: ImageVector, titel: String, hervorgehoben: Boolean, onClick: () -> Unit) {
    val bg = if (hervorgehoben) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val fg = if (hervorgehoben) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = modifier.clickable { onClick() }) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(titel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
        }
    }
}

@Composable
private fun MenueZeile(icon: ImageVector, titel: String, untertitel: String, farbe: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (farbe == MaterialTheme.colorScheme.error) farbe else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = farbe)
            Text(untertitel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
// --- ENDE BLOCK 3 VON 3 (HamburgerMenu.kt) ---
