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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var offenesUntermenue by remember { mutableIntStateOf(1) }
    val lohnInfo = String.format(Locale.GERMAN, "%.2f € • StKl. %d", aktuellerStundenlohn, steuerklasse)
    val fahrtInfo = String.format(Locale.GERMAN, "%.2f €", monatsFahrtkosten)

    ModalDrawerSheet {
        Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dienstplan-Menü", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            UntermenueKopf("1. Ansicht, Suche & Fotos", offenesUntermenue == 1) { offenesUntermenue = if (offenesUntermenue == 1) 0 else 1 }
            if (offenesUntermenue == 1) {
                NavigationDrawerItem(
                    label = { Text(if (istKalenderAnsicht) "Zur Listenansicht wechseln" else "Zur Kalenderansicht wechseln") },
                    selected = false,
                    icon = { Icon(if (istKalenderAnsicht) Icons.Default.ViewList else Icons.Default.CalendarMonth, null) },
                    onClick = onAnsichtWechseln
                )
                NavigationDrawerItem(label = { Text("Suchen im Kalender") }, selected = false, icon = { Icon(Icons.Default.Search, null) }, onClick = onSucheOeffnen)
                NavigationDrawerItem(label = { Text("Alle Tages-Fotos ($anzahlFotosGesamt)") }, selected = false, icon = { Icon(Icons.Default.Image, null) }, onClick = onFotoGalerieOeffnen)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            UntermenueKopf("2. Lohn, Netto & Fahrtkosten", offenesUntermenue == 2) { offenesUntermenue = if (offenesUntermenue == 2) 0 else 2 }
            if (offenesUntermenue == 2) {
                NavigationDrawerItem(label = { Text("Brutto-Netto & Lohn ($lohnInfo)") }, selected = false, icon = { Icon(Icons.Default.Edit, null) }, onClick = onStundenlohnOeffnen)
                NavigationDrawerItem(label = { Text("Fahrtkosten & Standorte ($fahrtInfo)") }, selected = false, icon = { Icon(Icons.Default.Place, null) }, onClick = onFahrtkostenOeffnen)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
// --- ENDE BLOCK 2 VON 3 (HamburgerMenu.kt) ---
// --- START BLOCK 3 VON 3 (HamburgerMenu.kt) ---
            UntermenueKopf("3. Ferien, Feiertage & Farben", offenesUntermenue == 3) { offenesUntermenue = if (offenesUntermenue == 3) 0 else 3 }
            if (offenesUntermenue == 3) {
                NavigationDrawerItem(label = { Text("Feiertage & Schulferien (NRW)") }, selected = false, icon = { Icon(Icons.Default.Event, null) }, onClick = onEinstellungenOeffnen)
                NavigationDrawerItem(label = { Text("Orts-Farben & Cloud-Sync") }, selected = false, icon = { Icon(Icons.Default.Palette, null) }, onClick = onEinstellungenOeffnen)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            UntermenueKopf("4. Dienstplan-Import & Aufräumen", offenesUntermenue == 4) { offenesUntermenue = if (offenesUntermenue == 4) 0 else 4 }
            if (offenesUntermenue == 4) {
                NavigationDrawerItem(label = { Text("PDF-Dienstplan einlesen") }, selected = false, icon = { Icon(Icons.Default.PictureAsPdf, null) }, onClick = onPdfImport)
                NavigationDrawerItem(
                    label = { Text("Monat leeren ($aktuellerMonatName)") },
                    selected = false,
                    icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = onMonatLeeren
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            UntermenueKopf("5. Backup & System-Diagnose", offenesUntermenue == 5) { offenesUntermenue = if (offenesUntermenue == 5) 0 else 5 }
            if (offenesUntermenue == 5) {
                NavigationDrawerItem(label = { Text("Backup speichern (.db)") }, selected = false, icon = { Icon(Icons.Default.Save, null) }, onClick = onAufGeraetSpeichern)
                NavigationDrawerItem(label = { Text("Backup laden (.db)") }, selected = false, icon = { Icon(Icons.Default.Download, null) }, onClick = onVomGeraetLaden)
                NavigationDrawerItem(label = { Text("Diagnose-Log ($anzahlLogs)") }, selected = false, icon = { Icon(Icons.Default.BugReport, null) }, onClick = onFehlerLogOeffnen)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UntermenueKopf(titel: String, istOffen: Boolean, onKlick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onKlick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = titel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Icon(imageVector = if (istOffen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
// --- ENDE BLOCK 3 VON 3 (HamburgerMenu.kt) ---
