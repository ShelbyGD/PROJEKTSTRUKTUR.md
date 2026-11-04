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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dienstplan.app.ui.components.HamburgerMenuInhalt
import com.dienstplan.app.ui.components.KlassischerMonatsKalender
import com.dienstplan.app.ui.components.MonatsUndGehaltsLeiste
import com.dienstplan.app.ui.theme.DienstplanTheme
import com.dienstplan.app.util.CloudBackupHelfer
import com.dienstplan.app.util.FehlerLogDialog
import com.dienstplan.app.util.FehlerLogHelfer
import com.dienstplan.app.util.PdfImportHelfer
import com.dienstplan.app.util.ScanHelfer
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DienstplanTheme { DienstplanHauptbildschirm() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DienstplanHauptbildschirm(viewModel: DienstplanViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val aktuellerMonat by viewModel.aktuellerMonat.collectAsState()
    val nrwAktiv by viewModel.nrwDatenAktiv.collectAsState()
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val alleFarben by viewModel.alleFarben.collectAsState()

    var istKalenderAnsicht by remember { mutableStateOf(true) }
    var zeigeEinstellungen by remember { mutableStateOf(false) }
    var zeigeFehlerLog by remember { mutableStateOf(false) }
    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(context, "${liste.size} Schicht(en) erkannt (Details im Fehler-Log)", Toast.LENGTH_LONG).show()
        }
    }

    val bildScanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ScanHelfer.bildUriScannen(context, uri, aktuellerMonat.toString(),
            onErfolg = { liste ->
                if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
                Toast.makeText(context, "${liste.size} Schicht(en) gescannt (Details im Log)", Toast.LENGTH_LONG).show()
            },
            onFehler = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        )
    }

    val kameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) ScanHelfer.bitmapScannen(bmp, aktuellerMonat.toString(),
            onErfolg = { liste ->
                if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
                Toast.makeText(context, "${liste.size} Schicht(en) gescannt (Details im Log)", Toast.LENGTH_LONG).show()
            },
            onFehler = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        )
    }

    val speichernLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(context, CloudBackupHelfer.backupAufGeraetSpeichern(context, uri), Toast.LENGTH_LONG).show()
        }
    }

    val ladenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(context, CloudBackupHelfer.backupVomGeraetLaden(context, uri), Toast.LENGTH_LONG).show()
        }
    }

    val monatsEintraege = alleEintraege.filter { it.datum.startsWith(aktuellerMonat.toString()) }
    val farbenMap = alleFarben.associate { it.einsatzort to it.farbHex }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                onPdfImport = { scope.launch { drawerState.close() }; pdfLauncher.launch("application/pdf") },
                onBildAusGalerieScannen = { scope.launch { drawerState.close() }; bildScanLauncher.launch("image/*") },
                onKameraFotoScannen = { scope.launch { drawerState.close() }; kameraLauncher.launch(null) },
                onAufGeraetSpeichern = { scope.launch { drawerState.close() }; speichernLauncher.launch("dienstplan_backup.db") },
                onVomGeraetLaden = { scope.launch { drawerState.close() }; ladenLauncher.launch(arrayOf("*/*")) },
                onEinstellungenOeffnen = { scope.launch { drawerState.close() }; zeigeEinstellungen = true },
                onFehlerLogOeffnen = { scope.launch { drawerState.close() }; zeigeFehlerLog = true }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                            }
                        },
                        title = { Text("Dienstplan", style = MaterialTheme.typography.titleLarge) },
                        actions = {
                            IconButton(onClick = { bearbeiteterEintrag = null; dialogDatum = LocalDate.now().toString() }) {
                                Icon(Icons.Default.Add, contentDescription = "Neuer Eintrag")
                            }
                            IconButton(onClick = { bildScanLauncher.launch("image/*") }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Bild/Foto scannen")
                            }
                            IconButton(onClick = { istKalenderAnsicht = !istKalenderAnsicht }) {
                                Icon(if (istKalenderAnsicht) Icons.Default.ViewList else Icons.Default.CalendarMonth, "Ansicht")
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                MonatsUndGehaltsLeiste(aktuellerMonat, monatsEintraege) { viewModel.monatWechseln(it) }
                if (istKalenderAnsicht) {
                    KlassischerMonatsKalender(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { d, e ->
                        bearbeiteterEintrag = e; dialogDatum = d.toString()
                    }
                } else {
                    FlacheListenAnsicht(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { d, e ->
                        bearbeiteterEintrag = e; dialogDatum = d.toString()
                    }
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
    if (zeigeFehlerLog) {
        FehlerLogDialog(onSchliessen = { zeigeFehlerLog = false })
    }
}
