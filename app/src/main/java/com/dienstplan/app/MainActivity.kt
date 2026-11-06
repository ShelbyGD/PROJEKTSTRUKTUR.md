package com.dienstplan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dienstplan.app.data.EintragEntity
import com.dienstplan.app.ui.DienstplanViewModel
import com.dienstplan.app.ui.components.EinstellungenDialog
import com.dienstplan.app.ui.components.EintragDialog
import com.dienstplan.app.ui.components.FlacheListenAnsicht
import com.dienstplan.app.ui.components.HamburgerMenuInhalt
import com.dienstplan.app.ui.components.KlassischerMonatsKalender
import com.dienstplan.app.ui.theme.DienstplanTheme
import com.dienstplan.app.util.CloudBackupHelfer
import com.dienstplan.app.util.FehlerLogDialog
import com.dienstplan.app.util.FehlerLogHelfer
import com.dienstplan.app.util.NettoEinstellungen
import com.dienstplan.app.util.NettoRechnerHelfer
import com.dienstplan.app.util.PdfImportHelfer
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun DienstplanViewModel.monatWechseln(offset: Int) { this.monatWechseln(offset.toLong()) }
private fun DienstplanViewModel.monatWechseln(offset: Long) { this.monatWechseln(offset.toInt()) }

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
    val prefs = remember { context.getSharedPreferences("dienstplan_extras", Context.MODE_PRIVATE) }

    val aktuellerMonat by viewModel.aktuellerMonat.collectAsState()
    val nrwAktiv by viewModel.nrwDatenAktiv.collectAsState()
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val alleFarben by viewModel.alleFarben.collectAsState()

    var istKalenderAnsicht by remember { mutableStateOf(true) }
    var zeigeEinstellungen by remember { mutableStateOf(false) }
    var zeigeFehlerLog by remember { mutableStateOf(false) }
    var zeigeSuche by remember { mutableStateOf(false) }
    var zeigeFotoGalerie by remember { mutableStateOf(false) }
    var zeigeStundenlohnDialog by remember { mutableStateOf(false) }
    var zeigeFahrtkostenDialog by remember { mutableStateOf(false) }
    var zeigeMonatLeerenBestaetigung by remember { mutableStateOf(false) }

    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }
    var zielDatumFuerNeuesFoto by remember { mutableStateOf<String?>(null) }
    var groesstesFoto by remember { mutableStateOf<File?>(null) }
    var fotoAktualisierungsZaehler by remember { mutableIntStateOf(0) }

    var nettoConfig by remember { mutableStateOf(NettoRechnerHelfer.ladeEinstellungen(context)) }
    val aktuellerStundenlohn = nettoConfig.stundenlohn

    var kmPauschale by remember { mutableDoubleStateOf(prefs.getFloat("km_pauschale", 0.30f).toDouble()) }
    var hinUndRueckfahrt by remember { mutableStateOf(prefs.getBoolean("hin_rueck", true)) }

    val notizenMap = remember {
        mutableStateMapOf<String, String>().apply {
            prefs.all.forEach { (k, v) -> if (k.startsWith("notiz_") && v is String && v.isNotBlank()) put(k.removePrefix("notiz_"), v) }
        }
    }
    val standortKmMap = remember {
        mutableStateMapOf<String, Double>().apply {
            prefs.all.forEach { (k, v) -> if (k.startsWith("km_") && v is Float && v > 0f) put(k.removePrefix("km_"), v.toDouble()) }
        }
    }
// --- START TEIL 2 (MainActivity.kt) ---
    val fotosOrdner = remember { File(context.filesDir, "tages_fotos").apply { if (!exists()) mkdirs() } }
    val alleFotos = remember(fotoAktualisierungsZaehler) {
        fotosOrdner.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()
    }
    val fotosProTagMap = remember(alleFotos) { alleFotos.groupBy { file -> file.name.substringBefore("_") } }

    val speichereFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (uri != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(zielDatei).use { output -> input.copyTo(output) }
                }
                fotoAktualisierungsZaehler++
            }
        }
    }

    val kameraFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (bmp != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(zielDatei).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                fotoAktualisierungsZaehler++
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val rohListe = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
            val mitAktuellemLohn = rohListe.map {
                if (it.typ == "SCHICHT") it.copy(stundenlohn = aktuellerStundenlohn) else it
            }
            if (mitAktuellemLohn.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(mitAktuellemLohn)
            Toast.makeText(context, "${mitAktuellemLohn.size} Schicht(en) mit ${String.format(Locale.GERMAN, "%.2f", aktuellerStundenlohn)} €/Std. importiert", Toast.LENGTH_LONG).show()
        }
    }

    val speichernLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch { Toast.makeText(context, CloudBackupHelfer.backupAufGeraetSpeichern(context, uri), Toast.LENGTH_LONG).show() }
    }
    val ladenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { Toast.makeText(context, CloudBackupHelfer.backupVomGeraetLaden(context, uri), Toast.LENGTH_LONG).show() }
    }

    // WICHTIG: Wendet den eingestellten Stundenlohn direkt auf die Monats-Anzeige an
    val monatsEintraege = remember(alleEintraege, aktuellerMonat, aktuellerStundenlohn) {
        alleEintraege
            .filter { it.datum.startsWith(aktuellerMonat.toString()) }
            .map { if (it.typ == "SCHICHT") it.copy(stundenlohn = aktuellerStundenlohn) else it }
    }
    val farbenMap = alleFarben.associate { it.einsatzort to it.farbHex }

    val monatsFahrtkosten = remember(monatsEintraege, standortKmMap.toMap(), kmPauschale, hinUndRueckfahrt) {
        monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }.sumOf { eintrag ->
            val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
            val einfacheKm = standortKmMap[ort] ?: standortKmMap[eintrag.titel] ?: 0.0
            einfacheKm * (if (hinUndRueckfahrt) 2.0 else 1.0) * kmPauschale
        }
    }

    val schichtenMonat = monatsEintraege.filter { it.typ == "SCHICHT" }
    val gesamtStunden = schichtenMonat.sumOf { it.berechneNettoStunden() }
    val bruttoMonat = gesamtStunden * aktuellerStundenlohn
    val lohnAbrechnung = remember(bruttoMonat, nettoConfig, monatsFahrtkosten) {
        NettoRechnerHelfer.berechneMonatsNetto(bruttoMonat, nettoConfig, monatsFahrtkosten)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = aktuellerMonat.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                aktuellerStundenlohn = aktuellerStundenlohn,
                monatsFahrtkosten = monatsFahrtkosten,
                anzahlFotosGesamt = alleFotos.size,
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                onAnsichtWechseln = { scope.launch { drawerState.close() }; istKalenderAnsicht = !istKalenderAnsicht },
                onSucheOeffnen = { scope.launch { drawerState.close() }; zeigeSuche = true },
                onFotoGalerieOeffnen = { scope.launch { drawerState.close() }; zeigeFotoGalerie = true },
                onStundenlohnOeffnen = { scope.launch { drawerState.close() }; zeigeStundenlohnDialog = true },
                onFahrtkostenOeffnen = { scope.launch { drawerState.close() }; zeigeFahrtkostenDialog = true },
                onEinstellungenOeffnen = { scope.launch { drawerState.close() }; zeigeEinstellungen = true },
                onPdfImport = { scope.launch { drawerState.close() }; pdfLauncher.launch("application/pdf") },
                onMonatLeeren = { scope.launch { drawerState.close() }; zeigeMonatLeerenBestaetigung = true },
                onAufGeraetSpeichern = { scope.launch { drawerState.close() }; speichernLauncher.launch("dienstplan_backup.db") },
                onVomGeraetLaden = { scope.launch { drawerState.close() }; ladenLauncher.launch(arrayOf("*/*")) },
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
                            IconButton(onClick = { zeigeSuche = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Suche")
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Neue kombinierte Monats-, Brutto- & Netto-Leiste (antippbar für Netto-Rechner)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { zeigeStundenlohnDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.monatWechseln(-1L) }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
                        }
                        Text(
                            text = aktuellerMonat.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN))
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.monatWechseln(1L) }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Nächster Monat")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Schichten: ${schichtenMonat.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.GERMAN, "Ist: %.1f Std.", gesamtStunden), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(String.format(Locale.GERMAN, "Brutto: %.2f €", lohnAbrechnung.bruttoLohn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.GERMAN, "Netto: %.2f €", lohnAbrechnung.nettoLohn), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (istKalenderAnsicht) {
                    KlassischerMonatsKalender(
                        monat = aktuellerMonat,
                        eintraege = monatsEintraege,
                        farbenMap = farbenMap,
                        nrwAktiv = nrwAktiv,
                        notizenMap = notizenMap,
                        fotosProTagMap = fotosProTagMap,
                        standortKmMap = standortKmMap,
                        kmPauschale = kmPauschale,
                        hinUndRueckfahrt = hinUndRueckfahrt,
                        onMonatWechseln = { offset -> viewModel.monatWechseln(offset) },
                        onFotoAnsehen = { groesstesFoto = it },
                        onTagKlick = { d, e -> bearbeiteterEintrag = e; dialogDatum = d.toString() },
                        onEintragDirektLoeschen = { viewModel.eintragLoeschen(it) }
                    )
                } else {
                    FlacheListenAnsicht(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { d, e ->
                        bearbeiteterEintrag = e; dialogDatum = d.toString()
                    }
                }
            }
        }
    }
// --- ENDE TEIL 2 (MainActivity.kt) ---
