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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dienstplan.app.ui.components.MonatsUndGehaltsLeiste
import com.dienstplan.app.ui.theme.DienstplanTheme
import com.dienstplan.app.util.CloudBackupHelfer
import com.dienstplan.app.util.FehlerLogDialog
import com.dienstplan.app.util.FehlerLogHelfer
import com.dienstplan.app.util.PdfImportHelfer
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    var aktuellerStundenlohn by remember {
        mutableDoubleStateOf(prefs.getFloat("global_stundenlohn", 15.0f).toDouble())
    }
    var kmPauschale by remember {
        mutableDoubleStateOf(prefs.getFloat("km_pauschale", 0.30f).toDouble())
    }
    var hinUndRueckfahrt by remember {
        mutableStateOf(prefs.getBoolean("hin_rueck", true))
    }

    val notizenMap = remember {
        mutableStateMapOf<String, String>().apply {
            prefs.all.forEach { (k, v) ->
                if (k.startsWith("notiz_") && v is String && v.isNotBlank()) {
                    put(k.removePrefix("notiz_"), v)
                }
            }
        }
    }

    val standortKmMap = remember {
        mutableStateMapOf<String, Double>().apply {
            prefs.all.forEach { (k, v) ->
                if (k.startsWith("km_") && v is Float && v > 0f) {
                    put(k.removePrefix("km_"), v.toDouble())
                }
            }
        }
    }
        val fotosOrdner = remember {
        File(context.filesDir, "tages_fotos").apply { if (!exists()) mkdirs() }
    }

    val alleFotos = remember(fotoAktualisierungsZaehler) {
        fotosOrdner.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()
    }

    val fotosProTagMap = remember(alleFotos) {
        alleFotos.groupBy { file -> file.name.substringBefore("_") }
    }

    val speichereFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (uri != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(zielDatei).use { output -> input.copyTo(output) }
                }
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "Foto zum Tag $zielTag gespeichert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val kameraFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (bmp != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(zielDatei).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "Kamerafoto zu $zielTag gespeichert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(context, "${liste.size} Schicht(en) importiert", Toast.LENGTH_LONG).show()
        }
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

    val monatsFahrtkosten = remember(monatsEintraege, standortKmMap.toMap(), kmPauschale, hinUndRueckfahrt) {
        monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }.sumOf { eintrag ->
            val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
            val einfacheKm = standortKmMap[ort] ?: standortKmMap[eintrag.titel] ?: 0.0
            val faktor = if (hinUndRueckfahrt) 2.0 else 1.0
            einfacheKm * faktor * kmPauschale
        }
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
                MonatsUndGehaltsLeiste(aktuellerMonat, monatsEintraege) { viewModel.monatWechseln(it) }
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
                        onMonatWechseln = { offset ->
                            if (offset < 0) viewModel.monatWechseln(-1) else viewModel.monatWechseln(1)
                        },
                        onFotoAnsehen = { groesstesFoto = it },
                        onTagKlick = { d, e ->
                            bearbeiteterEintrag = e
                            dialogDatum = d.toString()
                        },
                        onEintragDirektLoeschen = { viewModel.eintragLoeschen(it) }
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
        val dStr = dialogDatum!!
        EintragDialog(
            initialDatum = dStr,
            bestehenderEintrag = bearbeiteterEintrag,
            alleEintraege = alleEintraege,
            standardStundenlohn = aktuellerStundenlohn,
            initialTagesNotiz = notizenMap[dStr].orEmpty(),
            initialStandortKm = bearbeiteterEintrag?.let {
                val ort = it.einsatzort.ifBlank { it.titel }
                standortKmMap[ort] ?: 0.0
            } ?: 0.0,
            tagesFotos = fotosProTagMap[dStr].orEmpty(),
            onDatumGeaendert = { dialogDatum = it },
            onFotoAusGalerie = { d ->
                zielDatumFuerNeuesFoto = d
                speichereFotoLauncher.launch("image/*")
            },
            onFotoMitKamera = { d ->
                zielDatumFuerNeuesFoto = d
                kameraFotoLauncher.launch(null)
            },
            onFotoLoeschen = { fotoFile ->
                runCatching { fotoFile.delete() }
                fotoAktualisierungsZaehler++
            },
            onFotoAnsehen = { groesstesFoto = it },
            onSpeichern = { eintrag, neueNotiz, neueKm ->
                viewModel.eintragSpeichern(eintrag)
                if (neueNotiz.isNotBlank()) {
                    notizenMap[dStr] = neueNotiz
                    prefs.edit().putString("notiz_$dStr", neueNotiz).apply()
                } else {
                    notizenMap.remove(dStr)
                    prefs.edit().remove("notiz_$dStr").apply()
                }
                val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
                if (ort.isNotBlank() && neueKm > 0.0) {
                    standortKmMap[ort] = neueKm
                    prefs.edit().putFloat("km_$ort", neueKm.toFloat()).apply()
                }
                dialogDatum = null
            },
            onLoeschen = {
                viewModel.eintragLoeschen(it)
                dialogDatum = null
            },
            onAbbrechen = { dialogDatum = null }
        )
    }

    if (zeigeStundenlohnDialog) {
        var lohnInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", aktuellerStundenlohn)) }
        AlertDialog(
            onDismissRequest = { zeigeStundenlohnDialog = false },
            title = { Text("Stundenlohn & Gehalt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lohnInput,
                        onValueChange = { lohnInput = it },
                        label = { Text("Standard-Stundenlohn (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = String.format(Locale.GERMAN, "Fahrtkosten diesen Monat: %.2f €", monatsFahrtkosten),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val neuerLohn = lohnInput.replace(",", ".").toDoubleOrNull() ?: aktuellerStundenlohn
                        aktuellerStundenlohn = neuerLohn
                        prefs.edit().putFloat("global_stundenlohn", neuerLohn.toFloat()).apply()
                        val aktualisiert = monatsEintraege.filter { it.typ == "SCHICHT" }.map { it.copy(stundenlohn = neuerLohn) }
                        if (aktualisiert.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(aktualisiert)
                        zeigeStundenlohnDialog = false
                        Toast.makeText(context, "Stundenlohn gespeichert", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Speichern (inkl. Monat)") }
            },
            dismissButton = {
                TextButton(onClick = { zeigeStundenlohnDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    if (zeigeFahrtkostenDialog) {
        var pauschaleInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kmPauschale)) }
        var hinRueck by remember { mutableStateOf(hinUndRueckfahrt) }
        var ausgewaehlterOrt by remember { mutableStateOf("") }
        var ortKmInput by remember { mutableStateOf("") }
        val bekannteOrte = remember(alleEintraege) {
            alleEintraege.map { it.einsatzort.ifBlank { it.titel } }.filter { it.isNotBlank() }.distinct()
        }

        AlertDialog(
            onDismissRequest = { zeigeFahrtkostenDialog = false },
            title = { Text("Fahrtkosten & Standorte") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pauschaleInput,
                        onValueChange = { pauschaleInput = it },
                        label = { Text("km-Pauschale (€/km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { hinRueck = !hinRueck },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hin- & Rückweg (2x km)")
                        Checkbox(checked = hinRueck, onCheckedChange = { hinRueck = it })
                    }
                    HorizontalDivider()
                    bekannteOrte.forEach { ort ->
                        val km = standortKmMap[ort] ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                ausgewaehlterOrt = ort
                                ortKmInput = if (km > 0.0) km.toString() else ""
                            }.padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ort, style = MaterialTheme.typography.bodySmall)
                            Text(if (km > 0.0) "$km km" else "Tippen zum Eintragen", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = ausgewaehlterOrt,
                        onValueChange = { ausgewaehlterOrt = it },
                        label = { Text("Standort (z. B. Bielefeld, Loom)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ortKmInput,
                        onValueChange = { ortKmInput = it },
                        label = { Text("Einfache Strecke (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        kmPauschale = pauschaleInput.replace(",", ".").toDoubleOrNull() ?: 0.30
                        hinUndRueckfahrt = hinRueck
                        prefs.edit().putFloat("km_pauschale", kmPauschale.toFloat()).putBoolean("hin_rueck", hinUndRueckfahrt).apply()
                        val kmWert = ortKmInput.replace(",", ".").toDoubleOrNull()
                        if (ausgewaehlterOrt.isNotBlank() && kmWert != null) {
                            standortKmMap[ausgewaehlterOrt.trim()] = kmWert
                            prefs.edit().putFloat("km_${ausgewaehlterOrt.trim()}", kmWert.toFloat()).apply()
                        }
                        zeigeFahrtkostenDialog = false
                    }
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { zeigeFahrtkostenDialog = false }) { Text("Schließen") }
            }
        )
    }

    if (zeigeSuche) {
        var suchText by remember { mutableStateOf("") }
        val treffer = remember(suchText, alleEintraege) {
            if (suchText.isBlank()) emptyList()
            else alleEintraege.filter {
                it.titel.contains(suchText, true) || it.einsatzort.contains(suchText, true) || it.datum.contains(suchText)
            }.sortedByDescending { it.datum }
        }
        AlertDialog(
            onDismissRequest = { zeigeSuche = false },
            title = { Text("Im Kalender suchen") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = suchText,
                        onValueChange = { suchText = it },
                        label = { Text("Ort, Titel oder Datum") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        treffer.forEach { e ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    zeigeSuche = false
                                    bearbeiteterEintrag = e
                                    dialogDatum = e.datum
                                }
                            ) {
                                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${e.datum}: ${e.titel}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if (e.typ == "SCHICHT") Text("${e.startZeit}-${e.endZeit}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { zeigeSuche = false }) { Text("Schließen") } }
        )
    }

    if (zeigeFotoGalerie) {
        AlertDialog(
            onDismissRequest = { zeigeFotoGalerie = false },
            title = { Text("Alle Tages-Fotos (${alleFotos.size})") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alleFotos.isEmpty()) Text("Noch keine Fotos gespeichert.")
                    alleFotos.forEach { fotoFile ->
                        val tag = fotoFile.name.substringBefore("_")
                        val bmp = remember(fotoFile.absolutePath) { runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull() }
                        if (bmp != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().clickable { groesstesFoto = fotoFile }
                            ) {
                                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)))
                                    Column {
                                        Text("Datum: $tag", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(notizenMap[tag] ?: "Tippen zum Vergrößern", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { zeigeFotoGalerie = false }) { Text("Schließen") } }
        )
    }

    if (groesstesFoto != null) {
        val bmp = remember(groesstesFoto!!.absolutePath) { runCatching { BitmapFactory.decodeFile(groesstesFoto!!.absolutePath) }.getOrNull() }
        AlertDialog(
            onDismissRequest = { groesstesFoto = null },
            title = { Text(groesstesFoto!!.name.substringBefore("_")) },
            text = {
                if (bmp != null) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 420.dp))
                }
            },
            confirmButton = { TextButton(onClick = { groesstesFoto = null }) { Text("Schließen") } }
        )
    }

    if (zeigeMonatLeerenBestaetigung) {
        AlertDialog(
            onDismissRequest = { zeigeMonatLeerenBestaetigung = false },
            title = { Text("Monat leeren?") },
            text = { Text("Alle Einträge des aktuellen Monats löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    monatsEintraege.forEach { viewModel.eintragLoeschen(it) }
                    zeigeMonatLeerenBestaetigung = false
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { zeigeMonatLeerenBestaetigung = false }) { Text("Abbrechen") } }
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
