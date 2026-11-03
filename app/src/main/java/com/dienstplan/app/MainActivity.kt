package com.dienstplan.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.ui.DienstplanViewModel
import com.dienstplan.app.ui.components.EinstellungenDialog
import com.dienstplan.app.ui.components.EintragDialog
import com.dienstplan.app.ui.components.FlacheListenAnsicht
import com.dienstplan.app.ui.components.KlassischerMonatsKalender
import com.dienstplan.app.ui.components.MonatsUndGehaltsLeiste
import com.dienstplan.app.ui.theme.DienstplanTheme
import com.dienstplan.app.util.PdfImportHelfer
import com.dienstplan.app.util.ScanHelfer
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DienstplanTheme {
                DienstplanHauptbildschirm()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DienstplanHauptbildschirm(viewModel: DienstplanViewModel = viewModel()) {
    val context = LocalContext.current
    val aktuellerMonat by viewModel.aktuellerMonat.collectAsState()
    val nrwAktiv by viewModel.nrwDatenAktiv.collectAsState()
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val alleFarben by viewModel.alleFarben.collectAsState()

    var istKalenderAnsicht by remember { mutableStateOf(true) }
    var zeigeEinstellungen by remember { mutableStateOf(false) }
    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }

    // Dateiauswahl für statische PDF-Dienstpläne
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val importiert = PdfImportHelfer.pdfEinlesen(context, uri)
            if (importiert.isNotEmpty()) {
                viewModel.mehrereEintraegeSpeichern(importiert)
                Toast.makeText(context, "${importiert.size} Schicht(en) aus PDF importiert!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Keine Schichten im PDF erkannt.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Kamera-Starter für Google ML Kit Texterkennung
    val kameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            ScanHelfer.bildScannen(
                bitmap = bitmap,
                onErfolg = { gescannt ->
                    if (gescannt.isNotEmpty()) {
                        viewModel.mehrereEintraegeSpeichern(gescannt)
                        Toast.makeText(context, "${gescannt.size} Eintrag/Einträge gescannt!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Keine Uhrzeiten (HH:MM - HH:MM) erkannt.", Toast.LENGTH_SHORT).show()
                    }
                },
                onFehler = { fehler ->
                    Toast.makeText(context, fehler, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val monatsEintraege = alleEintraege.filter { it.datum.startsWith(aktuellerMonat.toString()) }
    val farbenMap = alleFarben.associate { it.einsatzort to it.farbHex }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Dienstplan", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF importieren")
                        }
                        IconButton(onClick = { kameraLauncher.launch(null) }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Dienstplan scannen")
                        }
                        IconButton(onClick = { istKalenderAnsicht = !istKalenderAnsicht }) {
                            Icon(
                                imageVector = if (istKalenderAnsicht) Icons.Default.ViewList else Icons.Default.CalendarMonth,
                                contentDescription = "Ansicht wechseln"
                            )
                        }
                        IconButton(onClick = { zeigeEinstellungen = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                bearbeiteterEintrag = null
                dialogDatum = LocalDate.now().toString()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Neuer Eintrag")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MonatsUndGehaltsLeiste(aktuellerMonat, monatsEintraege) { viewModel.monatWechseln(it) }
            if (istKalenderAnsicht) {
                KlassischerMonatsKalender(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { datum, eintrag ->
                    bearbeiteterEintrag = eintrag
                    dialogDatum = datum.toString()
                }
            } else {
                FlacheListenAnsicht(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { datum, eintrag ->
                    bearbeiteterEintrag = eintrag
                    dialogDatum = datum.toString()
                }
            }
        }
    }

    if (dialogDatum != null) {
        EintragDialog(
            initialDatum = dialogDatum!!,
            bestehenderEintrag = bearbeiteterEintrag,
            onSpeichern = { viewModel.eintragSpeichern(it); dialogDatum = null },
            onLoeschen = { viewModel.eintragLoeschen(it); dialogDatum = null },
            onAbbrechen = { dialogDatum = null }
        )
    }

    if (zeigeEinstellungen) {
        EinstellungenDialog(
            nrwAktiv = nrwAktiv,
            onNrwUmschalten = { viewModel.nrwDatenUmschalten() },
            gespeicherteFarben = alleFarben,
            onFarbeSpeichern = { ort, farbe -> viewModel.farbeSpeichern(ort, farbe) },
            onFarbeLoeschen = { viewModel.farbeLoeschen(it) },
            onSchliessen = { zeigeEinstellungen = false }
        )
    }
}
