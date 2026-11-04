```kotlin
package com.dienstplan.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HamburgerMenuInhalt(
    istKalenderAnsicht: Boolean,
    aktuellerMonatName: String,
    anzahlLogs: Int,
    onAnsichtWechseln: () -> Unit,
    onSucheOeffnen: () -> Unit,
    onPdfImport: () -> Unit,
    onMonatLeeren: () -> Unit,
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
                text = "Dienstplan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 1. Ansicht & Suche (aufgeräumt und direkt erreichbar)
            Text(
                text = "Ansicht & Suche",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = {
                    Text(
                        if (istKalenderAnsicht) "Zur Listenansicht wechseln"
                        else "Zur Kalenderansicht wechseln"
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        if (istKalenderAnsicht) Icons.Default.ViewList else Icons.Default.CalendarMonth,
                        contentDescription = null
                    )
                },
                onClick = onAnsichtWechseln
            )
            NavigationDrawerItem(
                label = { Text("Suchen im Kalender") },
                selected = false,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = onSucheOeffnen
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 2. Dienstplan & Verwaltung
            Text(
                text = "Dienstplan & Verwaltung",
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
                label = { Text("Einstellungen & Orts-Farben") },
                selected = false,
                icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                onClick = onEinstellungenOeffnen
            )
            NavigationDrawerItem(
                label = { Text("Monat leeren ($aktuellerMonatName)") },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = onMonatLeeren
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. Backup & Diagnose
            Text(
                text = "Sichern & Diagnose",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = { Text("Backup speichern (.db)") },
                selected = false,
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                onClick = onAufGeraetSpeichern
            )
            NavigationDrawerItem(
                label = { Text("Backup laden (.db)") },
                selected = false,
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = onVomGeraetLaden
            )
            NavigationDrawerItem(
                label = { Text("Diagnose-Log ($anzahlLogs)") },
                selected = false,
                icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                onClick = onFehlerLogOeffnen
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```
