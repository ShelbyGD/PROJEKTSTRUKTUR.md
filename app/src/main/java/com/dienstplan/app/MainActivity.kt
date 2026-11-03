package com.dienstplan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dienstplan.app.ui.theme.DienstplanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aktiviert das flache Edge-to-Edge Layout über den gesamten Bildschirm
        enableEdgeToEdge()
        setContent {
            DienstplanTheme {
                DienstplanGrundgeruest()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DienstplanGrundgeruest() {
    // Status für den Wechsel zwischen Monatskalender (true) und Listenansicht (false)
    var istKalenderAnsicht by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Dienstplan",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        // Umschalter (Toggle) zwischen Kalender- und Listenansicht
                        IconButton(onClick = { istKalenderAnsicht = !istKalenderAnsicht }) {
                            Icon(
                                imageVector = if (istKalenderAnsicht) {
                                    Icons.Default.ViewList
                                } else {
                                    Icons.Default.CalendarMonth
                                },
                                contentDescription = if (istKalenderAnsicht) {
                                    "Zur Listenansicht wechseln"
                                } else {
                                    "Zur Kalenderansicht wechseln"
                                }
                            )
                        }
                    }
                )
                // Flacher Divider statt Schatten oder Card-Views
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = if (istKalenderAnsicht) {
                    "Aktive Ansicht: Klassischer Monatskalender"
                } else {
                    "Aktive Ansicht: Flache Listenansicht"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Grundgerüst erfolgreich eingerichtet. Tippe oben rechts auf das Symbol, um die Ansicht umzuschalten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
