package com.dienstplan.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import java.util.Locale

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
    var zeigeSucheDialog by remember { mutableStateOf(false) }
    var zeigeMonatLeerenDialog by remember { mutableStateOf(false) }
    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(
                context,
                "${liste.size} Einträge aus PDF importiert",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val speichernLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(
                context,
                CloudBackupHelfer.backupAufGeraetSpeichern(context, uri),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val ladenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            Toast.makeText(
                context,
                CloudBackupHelfer.backupVomGeraetLaden(context, uri),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val monatsEintraege = alleEintraege.filter { it.datum.startsWith(aktuellerMonat.toString()) }
    val farbenMap = alleFarben.associate { it.einsatzort to it.farbHex }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = aktuellerMonat.toString(),
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                onAnsichtWechseln = {
                    scope.launch { drawerState.close() }
                    istKalenderAnsicht = !istKalenderAnsicht
                },
                onSucheOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeSucheDialog = true
                },
                onPdfImport = {
                    scope.launch { drawerState.close() }
                    pdfLauncher.launch("application/pdf")
                },
                onMonatLeeren = {
                    scope.launch { drawerState.close() }
                    zeigeMonatLeerenDialog = true
                },
                onAufGeraetSpeichern = {
                    scope.launch { drawerState.close() }
                    speichernLauncher.launch("dienstplan_backup.db")
                },
                onVomGeraetLaden = {
                    scope.launch { drawerState.close() }
                    ladenLauncher.launch(arrayOf("*/*"))
                },
                onEinstellungenOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeEinstellungen = true
                },
                onFehlerLogOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeFehlerLog = true
                }
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
                        title = {
                            Text("Dienstplan", style = MaterialTheme.typography.titleLarge)
                        },
                        actions = {
                            IconButton(onClick = { zeigeSucheDialog = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Im Kalender suchen")
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
                    KlassischerMonatsKalender(
                        monat = aktuellerMonat,
                        eintraege = monatsEintraege,
                        farbenMap = farbenMap,
                        nrwAktiv = nrwAktiv,
                        onTagKlick = { d, e ->
                            bearbeiteterEintrag = e
                            dialogDatum = d.toString()
                        },
                        onEintragDirektLoeschen = { eintrag ->
                            viewModel.eintragLoeschen(eintrag)
                            Toast.makeText(context, "${eintrag.titel} gelöscht", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    FlacheListenAnsicht(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { d, e ->
                        bearbeiteterEintrag = e
                        dialogDatum = d.toString()
                    }
                }
            }
        }
    }

    if (dialogDatum != null) {
        EintragDialog(
            initialDatum = dialogDatum!!,
            bestehenderEintrag = bearbeiteterEintrag,
            alleEintraege = alleEintraege,
            onSpeichern = {
                viewModel.eintragSpeichern(it)
                dialogDatum = null
            },
            onLoeschen = {
                viewModel.eintragLoeschen(it)
                dialogDatum = null
            },
            onAbbrechen = { dialogDatum = null }
        )
    }

    if (zeigeSucheDialog) {
        KalenderSucheDialog(
            alleEintraege = alleEintraege,
            onEintragBearbeiten = { eintrag ->
                zeigeSucheDialog = false
                bearbeiteterEintrag = eintrag
                dialogDatum = eintrag.datum
            },
            onEintragLoeschen = { eintrag ->
                viewModel.eintragLoeschen(eintrag)
            },
            onSchliessen = { zeigeSucheDialog = false }
        )
    }

    if (zeigeMonatLeerenDialog) {
        AlertDialog(
            onDismissRequest = { zeigeMonatLeerenDialog = false },
            title = { Text("Monat $aktuellerMonat aufräumen") },
            text = {
                Text(
                    "Im aktuellen Monat sind ${monatsEintraege.size} Einträge gespeichert. " +
                        "Möchtest du nur die 'Frei'-Tage entfernen oder den gesamten Monat leeren?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        monatsEintraege.forEach { viewModel.eintragLoeschen(it) }
                        zeigeMonatLeerenDialog = false
                        Toast.makeText(context, "Monat $aktuellerMonat komplett geleert", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Alle löschen (${monatsEintraege.size})", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    val freiEintraege = monatsEintraege.filter { it.typ == "FREI" }
                    if (freiEintraege.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                freiEintraege.forEach { viewModel.eintragLoeschen(it) }
                                zeigeMonatLeerenDialog = false
                                Toast.makeText(context, "${freiEintraege.size} Frei-Tage entfernt", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Nur 'Frei' (${freiEintraege.size})")
                        }
                    }
                    TextButton(onClick = { zeigeMonatLeerenDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            }
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

@Composable
private fun KalenderSucheDialog(
    alleEintraege: List<EintragEntity>,
    onEintragBearbeiten: (EintragEntity) -> Unit,
    onEintragLoeschen: (EintragEntity) -> Unit,
    onSchliessen: () -> Unit
) {
    var suchbegriff by remember { mutableStateOf("") }
    val treffer = remember(alleEintraege, suchbegriff) {
        val q = suchbegriff.trim().lowercase()
        if (q.isEmpty()) {
            alleEintraege.sortedByDescending { it.datum }.take(30)
        } else {
            alleEintraege.filter {
                it.titel.lowercase().contains(q) ||
                    it.einsatzort.lowercase().contains(q) ||
                    it.datum.contains(q) ||
                    it.typ.lowercase().contains(q)
            }.sortedByDescending { it.datum }
        }
    }
    val gesamtStunden = remember(treffer) {
        treffer.filter { it.typ == "SCHICHT" }.sumOf { it.berechneNettoStunden() }
    }

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Im Kalender suchen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = suchbegriff,
                    onValueChange = { suchbegriff = it },
                    label = { Text("Ort, Termin oder Datum (z. B. Loom, Arzt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = String.format(
                        Locale.GERMAN,
                        "%d Treffer • Summe: %.1f Std.",
                        treffer.size,
                        gesamtStunden
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    treffer.forEach { eintrag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEintragBearbeiten(eintrag) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${eintrag.datum} • ${eintrag.titel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (eintrag.typ == "SCHICHT") {
                                        "${eintrag.startZeit}-${eintrag.endZeit} Uhr (${eintrag.berechneNettoStunden()} Std.)"
                                    } else {
                                        eintrag.typ
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEintragLoeschen(eintrag) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) {
                Text("Schließen")
            }
        }
    )
}
