package com.dienstplan.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HamburgerMenuInhalt(
    anzahlLogs: Int,
    onPdfImport: () -> Unit,
    onBildAusGalerieScannen: () -> Unit,
    onKameraFotoScannen: () -> Unit,
    onAufGeraetSpeichern: () -> Unit,
    onVomGeraetLaden: () -> Unit,
    onEinstellungenOeffnen: () -> Unit,
    onFehlerLogOeffnen: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dienstplan-Menü",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Import & Scannen",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = { Text("PDF-Dienstplan einlesen") },
                selected = false,
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = onPdfImport
            )
            NavigationDrawerItem(
                label = { Text("Bild / Screenshot scannen (HD)") },
                selected = false,
                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                onClick = onBildAusGalerieScannen
            )
            NavigationDrawerItem(
                label = { Text("Direktfoto scannen (Kamera)") },
                selected = false,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                onClick = onKameraFotoScannen
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Geräte-Speicherung & Backup",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = { Text("Auf Gerät speichern (.db)") },
                selected = false,
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                onClick = onAufGeraetSpeichern
            )
            NavigationDrawerItem(
                label = { Text("Vom Gerät laden (.db)") },
                selected = false,
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = onVomGeraetLaden
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Anpassung & Diagnose",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = { Text("Einstellungen, Farben & Cloud") },
                selected = false,
                icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                onClick = onEinstellungenOeffnen
            )
            NavigationDrawerItem(
                label = { Text("Fehler- & Diagnose-Log ($anzahlLogs)") },
                selected = false,
                icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                onClick = onFehlerLogOeffnen
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
