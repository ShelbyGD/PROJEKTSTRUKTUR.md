package com.dienstplan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    val prefs = remember { context.getSharedPreferences("dienstplan_einstellungen", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val aktuellerMonat by viewModel.aktuellerMonat.collectAsState()
    val nrwAktiv by viewModel.nrwDatenAktiv.collectAsState()
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val alleFarben by viewModel.alleFarben.collectAsState()

    var standardStundenlohn by remember {
        mutableDoubleStateOf(prefs.getFloat("standard_stundenlohn", 15.0f).toDouble())
    }
    var kmPauschale by remember {
        mutableDoubleStateOf(prefs.getFloat("km_pauschale", 0.30f).toDouble())
    }
    var hinUndRueckfahrt by remember {
        mutableStateOf(prefs.getBoolean("hin_und_rueckfahrt", true))
    }
    var notizenMap by remember {
        mutableStateOf(ladeStringMapAusPrefs(prefs, "tages_notizen_json"))
    }
    var standortKmMap by remember {
        mutableStateOf(ladeDoubleMapAusPrefs(prefs, "standort_km_json"))
    }
    var fotoAktualisierungsZaehler by remember { mutableIntStateOf(0) }

    var istKalenderAnsicht by remember { mutableStateOf(true) }
    var zeigeEinstellungen by remember { mutableStateOf(false) }
    var zeigeFehlerLog by remember { mutableStateOf(false) }
    var zeigeSucheDialog by remember { mutableStateOf(false) }
    var zeigeStundenlohnDialog by remember { mutableStateOf(false) }
    var zeigeFahrtkostenDialog by remember { mutableStateOf(false) }
    var zeigeFotoGalerieDialog by remember { mutableStateOf(false) }
    var zeigeMonatLeerenDialog by remember { mutableStateOf(false) }
    var vorschauFotoFile by remember { mutableStateOf<File?>(null) }

    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }
    var zielDatumFuerNeuesFoto by remember { mutableStateOf<String?>(null) }

    val alleFotos = remember(fotoAktualisierungsZaehler) {
        holeAlleTagesFotos(context)
    }
    val fotosProTagMap = remember(alleFotos) {
        alleFotos.groupBy { extrahiereDatumAusFotoName(it.name) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val rohListe = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
            val angepassteListe = rohListe.map { eintrag ->
                if (eintrag.typ == "SCHICHT") eintrag.copy(stundenlohn = standardStundenlohn) else eintrag
            }
            if (angepassteListe.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(angepassteListe)
            Toast.makeText(
                context,
                "${angepassteListe.size} Einträge aus PDF importiert",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val tagesFotoGalerieLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val datum = zielDatumFuerNeuesFoto ?: dialogDatum
        if (uri != null && datum != null) {
            val gespeichert = speichereTagesFotoAusUri(context, uri, datum)
            if (gespeichert) {
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "Foto zum Tag $datum hinzugefügt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val tagesFotoKameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        val datum = zielDatumFuerNeuesFoto ?: dialogDatum
        if (bmp != null && datum != null) {
            val gespeichert = speichereTagesFotoAusBitmap(context, bmp, datum)
            if (gespeichert) {
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "Kamerafoto zum Tag $datum gespeichert", Toast.LENGTH_SHORT).show()
            }
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

    // Monats-Fahrtkosten berechnen
    val monatsGesamtKm = remember(monatsEintraege, standortKmMap, hinUndRueckfahrt) {
        monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }.sumOf { eintrag ->
            val ortKey = eintrag.einsatzort.ifBlank { eintrag.titel }
            val einfacheStrecke = standortKmMap[ortKey] ?: standortKmMap[eintrag.titel] ?: 0.0
            if (hinUndRueckfahrt) einfacheStrecke * 2.0 else einfacheStrecke
        }
    }
    val monatsFahrtkosten = monatsGesamtKm * kmPauschale

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = aktuellerMonat.toString(),
                aktuellerStundenlohn = standardStundenlohn,
                monatsFahrtkosten = monatsFahrtkosten,
                anzahlFotosGesamt = alleFotos.size,
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                onAnsichtWechseln = {
                    scope.launch { drawerState.close() }
                    istKalenderAnsicht = !istKalenderAnsicht
                },
                onSucheOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeSucheDialog = true
                },
                onFotoGalerieOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeFotoGalerieDialog = true
                },
                onStundenlohnOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeStundenlohnDialog = true
                },
                onFahrtkostenOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeFahrtkostenDialog = true
                },
                onEinstellungenOeffnen = {
                    scope.launch { drawerState.close() }
                    zeigeEinstellungen = true
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
                        notizenMap = notizenMap,
                        fotosProTagMap = fotosProTagMap,
                        standortKmMap = standortKmMap,
                        kmPauschale = kmPauschale,
                        hinUndRueckfahrt = hinUndRueckfahrt,
                        onMonatWechseln = { viewModel.monatWechseln(it) },
                        onFotoAnsehen = { vorschauFotoFile = it },
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
        val aktDatum = dialogDatum!!
        val ortName = (bearbeiteterEintrag?.einsatzort?.ifBlank { bearbeiteterEintrag?.titel }).orEmpty()
        val gespeicherteKm = if (ortName.isNotBlank()) (standortKmMap[ortName] ?: 0.0) else 0.0

        EintragDialog(
            initialDatum = aktDatum,
            bestehenderEintrag = bearbeiteterEintrag,
            alleEintraege = alleEintraege,
            standardStundenlohn = standardStundenlohn,
            initialTagesNotiz = notizenMap[aktDatum].orEmpty(),
            initialStandortKm = gespeicherteKm,
            tagesFotos = fotosProTagMap[aktDatum].orEmpty(),
            onDatumGeaendert = { neuesDatum ->
                dialogDatum = neuesDatum
                bearbeiteterEintrag = alleEintraege.firstOrNull { it.datum == neuesDatum }
            },
            onFotoAusGalerie = { zielDatum ->
                zielDatumFuerNeuesFoto = zielDatum
                tagesFotoGalerieLauncher.launch("image/*")
            },
            onFotoMitKamera = { zielDatum ->
                zielDatumFuerNeuesFoto = zielDatum
                tagesFotoKameraLauncher.launch(null)
            },
            onFotoLoeschen = { file ->
                if (file.delete()) {
                    fotoAktualisierungsZaehler++
                }
            },
            onFotoAnsehen = { vorschauFotoFile = it },
            onSpeichern = { eintrag, notizText, distanzKm ->
                viewModel.eintragSpeichern(eintrag)

                // Tages-Notiz speichern
                val neueNotizen = notizenMap.toMutableMap()
                if (notizText.isBlank()) {
                    neueNotizen.remove(eintrag.datum)
                } else {
                    neueNotizen[eintrag.datum] = notizText
                }
                notizenMap = neueNotizen
                speichereStringMapInPrefs(prefs, "tages_notizen_json", neueNotizen)

                // Standort-Kilometer speichern
                val standortSchluessel = eintrag.einsatzort.ifBlank { eintrag.titel }
                if (standortSchluessel.isNotBlank() && distanzKm >= 0.0) {
                    val neueKmMap = standortKmMap.toMutableMap()
                    if (distanzKm > 0.0) {
                        neueKmMap[standortSchluessel] = distanzKm
                        neueKmMap[eintrag.titel] = distanzKm
                    }
                    standortKmMap = neueKmMap
                    speichereDoubleMapInPrefs(prefs, "standort_km_json", neueKmMap)
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
        val monatsStunden = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneNettoStunden() }
        val monatsLohnBrutto = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneVerdienst() }

        StundenlohnUndGehaltDialog(
            aktuellerLohn = standardStundenlohn,
            aktuellerMonatName = aktuellerMonat.toString(),
            anzahlMonatsSchichten = monatsEintraege.count { it.typ == "SCHICHT" },
            monatsStunden = monatsStunden,
            monatsLohnBrutto = monatsLohnBrutto,
            monatsFahrtkosten = monatsFahrtkosten,
            onSpeichern = { neuerLohn, aufAktuellenMonatAnwenden, aufAlleSchichtenAnwenden ->
                standardStundenlohn = neuerLohn
                prefs.edit().putFloat("standard_stundenlohn", neuerLohn.toFloat()).apply()

                if (aufAlleSchichtenAnwenden) {
                    alleEintraege.filter { it.typ == "SCHICHT" }.forEach { schicht ->
                        viewModel.eintragSpeichern(schicht.copy(stundenlohn = neuerLohn))
                    }
                } else if (aufAktuellenMonatAnwenden) {
                    monatsEintraege.filter { it.typ == "SCHICHT" }.forEach { schicht ->
                        viewModel.eintragSpeichern(schicht.copy(stundenlohn = neuerLohn))
                    }
                }

                zeigeStundenlohnDialog = false
                Toast.makeText(
                    context,
                    String.format(Locale.GERMAN, "Stundenlohn auf %.2f € gespeichert", neuerLohn),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onAbbrechen = { zeigeStundenlohnDialog = false }
        )
    }

    if (zeigeFahrtkostenDialog) {
        val bekannteStandorte = remember(alleEintraege, standortKmMap) {
            val ausSchichten = alleEintraege
                .filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }
                .map { it.einsatzort.ifBlank { it.titel } }
                .filter { it.isNotBlank() }
            (ausSchichten + standortKmMap.keys).distinct().sorted()
        }

        FahrtkostenUndStandorteDialog(
            aktuellerMonatName = aktuellerMonat.toString(),
            monatsEintraege = monatsEintraege,
            bekannteStandorte = bekannteStandorte,
            standortKmMap = standortKmMap,
            initialKmPauschale = kmPauschale,
            initialHinUndRueck = hinUndRueckfahrt,
            onSpeichern = { neueKmMap, neuePauschale, neuHinUndRueck ->
                standortKmMap = neueKmMap
                kmPauschale = neuePauschale
                hinUndRueckfahrt = neuHinUndRueck
                speichereDoubleMapInPrefs(prefs, "standort_km_json", neueKmMap)
                prefs.edit()
                    .putFloat("km_pauschale", neuePauschale.toFloat())
                    .putBoolean("hin_und_rueckfahrt", neuHinUndRueck)
                    .apply()
                zeigeFahrtkostenDialog = false
                Toast.makeText(context, "Fahrtkosten & Standorte gespeichert", Toast.LENGTH_SHORT).show()
            },
            onSchliessen = { zeigeFahrtkostenDialog = false }
        )
    }

    if (zeigeFotoGalerieDialog) {
        FotoGesamtAnsichtDialog(
            alleFotos = alleFotos,
            alleEintraege = alleEintraege,
            notizenMap = notizenMap,
            onFotoGrossAnsehen = { vorschauFotoFile = it },
            onZumTagSpringen = { datumString ->
                zeigeFotoGalerieDialog = false
                bearbeiteterEintrag = alleEintraege.firstOrNull { it.datum == datumString }
                dialogDatum = datumString
            },
            onFotoLoeschen = { file ->
                if (file.delete()) {
                    fotoAktualisierungsZaehler++
                }
            },
            onSchliessen = { zeigeFotoGalerieDialog = false }
        )
    }

    if (vorschauFotoFile != null) {
        val file = vorschauFotoFile!!
        val datumString = extrahiereDatumAusFotoName(file.name)
        val bmp = remember(file.absolutePath) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { vorschauFotoFile = null },
            title = { Text("Foto vom $datumString") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Vollbild-Foto",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 420.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    val notiz = notizenMap[datumString]
                    if (!notiz.isNullOrBlank()) {
                        Text(
                            text = "Notiz: $notiz",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vorschauFotoFile = null }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (file.delete()) {
                            fotoAktualisierungsZaehler++
                        }
                        vorschauFotoFile = null
                    }
                ) {
                    Text("Foto löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (zeigeSucheDialog) {
        KalenderSucheDialog(
            alleEintraege = alleEintraege,
            notizenMap = notizenMap,
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
private fun StundenlohnUndGehaltDialog(
    aktuellerLohn: Double,
    aktuellerMonatName: String,
    anzahlMonatsSchichten: Int,
    monatsStunden: Double,
    monatsLohnBrutto: Double,
    monatsFahrtkosten: Double,
    onSpeichern: (Double, Boolean, Boolean) -> Unit,
    onAbbrechen: () -> Unit
) {
    var lohnEingabe by remember {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", aktuellerLohn))
    }
    var aufMonatAnwenden by remember { mutableStateOf(true) }
    var aufAlleAnwenden by remember { mutableStateOf(false) }

    val vorschauLohn = lohnEingabe.replace(",", ".").trim().toDoubleOrNull() ?: aktuellerLohn
    val vorschauMonatsGehalt = if (aufMonatAnwenden || aufAlleAnwenden) {
        monatsStunden * vorschauLohn
    } else {
        monatsLohnBrutto
    }

    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text("Stundenlohn & Gehalt") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Monatsübersicht ($aktuellerMonatName)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Arbeitszeit: %.2f Std. (%d Schichten)", monatsStunden, anzahlMonatsSchichten),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Gehalt (Brutto): %.2f €", vorschauMonatsGehalt),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Fahrtkosten: %.2f €", monatsFahrtkosten),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Gesamt inkl. Fahrtkosten: %.2f €", vorschauMonatsGehalt + monatsFahrtkosten),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = lohnEingabe,
                    onValueChange = { lohnEingabe = it },
                    label = { Text("Stundenlohn in € (z. B. 15,50)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aufMonatAnwenden = !aufMonatAnwenden },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = aufMonatAnwenden,
                        onCheckedChange = { aufMonatAnwenden = it }
                    )
                    Text(
                        text = "Auf alle $anzahlMonatsSchichten Schichten im Monat $aktuellerMonatName anwenden",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aufAlleAnwenden = !aufAlleAnwenden },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = aufAlleAnwenden,
                        onCheckedChange = { aufAlleAnwenden = it }
                    )
                    Text(
                        text = "Auf alle gespeicherten Monate anwenden",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val geparst = lohnEingabe.replace(",", ".").trim().toDoubleOrNull() ?: aktuellerLohn
                    onSpeichern(geparst, aufMonatAnwenden, aufAlleAnwenden)
                }
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onAbbrechen) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun FahrtkostenUndStandorteDialog(
    aktuellerMonatName: String,
    monatsEintraege: List<EintragEntity>,
    bekannteStandorte: List<String>,
    standortKmMap: Map<String, Double>,
    initialKmPauschale: Double,
    initialHinUndRueck: Boolean,
    onSpeichern: (Map<String, Double>, Double, Boolean) -> Unit,
    onSchliessen: () -> Unit
) {
    var pauschaleText by remember {
        mutableStateOf(String.format(Locale.GERMAN, "%.2f", initialKmPauschale))
    }
    var hinUndRueck by remember { mutableStateOf(initialHinUndRueck) }
    var bearbeiteteKmMap by remember { mutableStateOf(standortKmMap.toMutableMap()) }
    var neuerStandortName by remember { mutableStateOf("") }
    var neuerStandortKm by remember { mutableStateOf("") }

    val alleOrteListe = remember(bekannteStandorte, bearbeiteteKmMap.keys.toList()) {
        (bekannteStandorte + bearbeiteteKmMap.keys).distinct().sorted()
    }

    val aktPauschale = pauschaleText.replace(",", ".").toDoubleOrNull() ?: initialKmPauschale
    val faktor = if (hinUndRueck) 2.0 else 1.0

    val monatsFahrten = monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }
    val gesamtMonatsKm = monatsFahrten.sumOf { eintrag ->
        val ortKey = eintrag.einsatzort.ifBlank { eintrag.titel }
        val einfach = bearbeiteteKmMap[ortKey] ?: bearbeiteteKmMap[eintrag.titel] ?: 0.0
        einfach * faktor
    }
    val gesamtMonatsKosten = gesamtMonatsKm * aktPauschale

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Fahrtkosten & Standorte (km)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Fahrtkosten im Monat $aktuellerMonatName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Gefahrene Strecke: %.1f km", gesamtMonatsKm),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Fahrtkosten gesamt: %.2f €", gesamtMonatsKosten),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pauschaleText,
                        onValueChange = { pauschaleText = it },
                        label = { Text("€/km (z. B. 0,30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable { hinUndRueck = !hinUndRueck },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hinUndRueck,
                            onCheckedChange = { hinUndRueck = it }
                        )
                        Text(
                            text = "Hin- & Rückweg (2x)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "Entfernung pro Standort (einfache Strecke in km):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                alleOrteListe.forEach { ort ->
                    var kmEingabe by remember(ort, bearbeiteteKmMap[ort]) {
                        val w = bearbeiteteKmMap[ort] ?: 0.0
                        mutableStateOf(if (w > 0.0) String.format(Locale.GERMAN, "%.1f", w) else "")
                    }
                    val anzahlEinsaetze = monatsFahrten.count {
                        it.einsatzort.equals(ort, ignoreCase = true) || it.titel.equals(ort, ignoreCase = true)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = ort,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$anzahlEinsaetzex im Monat $aktuellerMonatName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = kmEingabe,
                            onValueChange = { neuerText ->
                                kmEingabe = neuerText
                                val wert = neuerText.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                                val kopie = bearbeiteteKmMap.toMutableMap()
                                kopie[ort] = wert
                                bearbeiteteKmMap = kopie
                            },
                            label = { Text("km (einfach)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "Weiteren Standort manuell hinzufügen:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = neuerStandortName,
                        onValueChange = { neuerStandortName = it },
                        label = { Text("Standort") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = neuerStandortKm,
                        onValueChange = { neuerStandortKm = it },
                        label = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    TextButton(
                        onClick = {
                            val name = neuerStandortName.trim()
                            val km = neuerStandortKm.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                val kopie = bearbeiteteKmMap.toMutableMap()
                                kopie[name] = km
                                bearbeiteteKmMap = kopie
                                neuerStandortName = ""
                                neuerStandortKm = ""
                            }
                        }
                    ) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pauschale = pauschaleText.replace(",", ".").trim().toDoubleOrNull() ?: initialKmPauschale
                    onSpeichern(bearbeiteteKmMap, pauschale, hinUndRueck)
                }
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSchliessen) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun FotoGesamtAnsichtDialog(
    alleFotos: List<File>,
    alleEintraege: List<EintragEntity>,
    notizenMap: Map<String, String>,
    onFotoGrossAnsehen: (File) -> Unit,
    onZumTagSpringen: (String) -> Unit,
    onFotoLoeschen: (File) -> Unit,
    onSchliessen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Alle Tages-Fotos (${alleFotos.size})") },
        text = {
            if (alleFotos.isEmpty()) {
                Text(
                    text = "Noch keine Fotos vorhanden. Tippe im Kalender auf einen Tag und wähle 'Galerie' oder 'Kamera', um ein Foto zum Tag hinzuzufügen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    alleFotos.forEach { fotoFile ->
                        val datum = extrahiereDatumAusFotoName(fotoFile.name)
                        val tagesSchicht = alleEintraege.firstOrNull { it.datum == datum }
                        val tagesNotiz = notizenMap[datum]
                        val bmp = remember(fotoFile.absolutePath) {
                            runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { onFotoGrossAnsehen(fotoFile) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Foto vom $datum",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onZumTagSpringen(datum) }
                            ) {
                                Text(
                                    text = "Tag: $datum",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (tagesSchicht != null) {
                                    Text(
                                        text = "${tagesSchicht.titel} (${tagesSchicht.startZeit}-${tagesSchicht.endZeit})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!tagesNotiz.isNullOrBlank()) {
                                    Text(
                                        text = "Notiz: $tagesNotiz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onFotoLoeschen(fotoFile) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Foto löschen",
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

@Composable
private fun KalenderSucheDialog(
    alleEintraege: List<EintragEntity>,
    notizenMap: Map<String, String>,
    onEintragBearbeiten: (EintragEntity) -> Unit,
    onEintragLoeschen: (EintragEntity) -> Unit,
    onSchliessen: () -> Unit
) {
    var suchbegriff by remember { mutableStateOf("") }
    val treffer = remember(alleEintraege, notizenMap, suchbegriff) {
        val q = suchbegriff.trim().lowercase()
        if (q.isEmpty()) {
            alleEintraege.sortedByDescending { it.datum }.take(30)
        } else {
            alleEintraege.filter {
                val notizZumTag = notizenMap[it.datum].orEmpty().lowercase()
                it.titel.lowercase().contains(q) ||
                    it.einsatzort.lowercase().contains(q) ||
                    it.datum.contains(q) ||
                    it.typ.lowercase().contains(q) ||
                    notizZumTag.contains(q)
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
                    label = { Text("Ort, Notiz, Termin oder Datum") },
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
                        val notiz = notizenMap[eintrag.datum]
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
                                if (!notiz.isNullOrBlank()) {
                                    Text(
                                        text = "Notiz: $notiz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { onEintragLoeschen(eintrag) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
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

private fun holeFotoOrdner(context: Context): File {
    val ordner = File(context.filesDir, "tages_fotos")
    if (!ordner.exists()) ordner.mkdirs()
    return ordner
}

private fun holeAlleTagesFotos(context: Context): List<File> {
    val ordner = holeFotoOrdner(context)
    return ordner.listFiles()
        ?.filter { it.isFile && it.name.startsWith("foto_") }
        ?.sortedByDescending { it.name }
        ?: emptyList()
}

private fun extrahiereDatumAusFotoName(dateiName: String): String {
    return dateiName.removePrefix("foto_").take(10)
}

private fun speichereTagesFotoAusUri(context: Context, uri: Uri, datum: String): Boolean {
    return runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return false
        speichereTagesFotoAusBitmap(context, original, datum)
    }.getOrDefault(false)
}

private fun speichereTagesFotoAusBitmap(context: Context, bitmap: Bitmap, datum: String): Boolean {
    return runCatching {
        val zielDatei = File(holeFotoOrdner(context), "foto_${datum}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(zielDatei).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        true
    }.getOrDefault(false)
}

private fun ladeStringMapAusPrefs(prefs: android.content.SharedPreferences, key: String): Map<String, String> {
    val raw = prefs.getString(key, null) ?: return emptyMap()
    return runCatching {
        val json = JSONObject(raw)
        val map = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = json.optString(k, "")
        }
        map
    }.getOrDefault(emptyMap())
}

private fun speichereStringMapInPrefs(prefs: android.content.SharedPreferences, key: String, map: Map<String, String>) {
    val json = JSONObject()
    map.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString(key, json.toString()).apply()
}

private fun ladeDoubleMapAusPrefs(prefs: android.content.SharedPreferences, key: String): Map<String, Double> {
    val raw = prefs.getString(key, null) ?: return emptyMap()
    return runCatching {
        val json = JSONObject(raw)
        val map = mutableMapOf<String, Double>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = json.optDouble(k, 0.0)
        }
        map
    }.getOrDefault(emptyMap())
}

private fun speichereDoubleMapInPrefs(prefs: android.content.SharedPreferences, key: String, map: Map<String, Double>) {
    val json = JSONObject()
    map.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString(key, json.toString()).apply()
