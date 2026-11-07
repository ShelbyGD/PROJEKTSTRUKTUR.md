// --- START BLOCK 1 VON 6 (MainActivity.kt) ---
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
// --- ENDE BLOCK 1 VON 6 (MainActivity.kt) ---
// --- START BLOCK 2 VON 6 (MainActivity.kt) ---
private fun DienstplanViewModel.monatWechseln(offset: Int) { this.monatWechseln(offset.toLong()) }
private fun DienstplanViewModel.monatWechseln(offset: Long) { this.monatWechseln(offset.toInt()) }

data class NettoErgebnis(
    val brutto: Double,
    val lohnsteuer: Double,
    val kirchensteuer: Double,
    val rv: Double,
    val av: Double,
    val kv: Double,
    val pv: Double,
    val abgabenGesamt: Double,
    val netto: Double,
    val fahrtkosten: Double,
    val auszahlung: Double
)

private fun rundeCent(wert: Double): Double = (wert * 100.0).roundToInt() / 100.0

private fun berechneTarifESt2026(zvE: Double): Double {
    val x = floor(zvE)
    return when {
        x <= 12348.0 -> 0.0
        x <= 17799.0 -> { val y = (x - 12348.0) / 10000.0; floor((914.51 * y + 1400.0) * y) }
        x <= 69878.0 -> { val z = (x - 17799.0) / 10000.0; floor((173.10 * z + 2397.0) * z + 1034.87) }
        x <= 277825.0 -> floor(0.42 * x - 11135.63)
        else -> floor(0.45 * x - 19470.38)
    }
}

fun berechneNetto2026(
    brutto: Double,
    steuerklasse: Int,
    kvZusatzProzent: Double,
    kirchensteuerAktiv: Boolean,
    kinderUnter25: Int,
    fahrtkosten: Double
): NettoErgebnis {
    if (brutto <= 0.0) return NettoErgebnis(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, fahrtkosten, fahrtkosten)
    val rv = rundeCent(brutto * 0.093)
    val av = rundeCent(brutto * 0.013)
    val kvSatz = 0.073 + (kvZusatzProzent / 100.0 / 2.0)
    val kv = rundeCent(brutto * kvSatz)
    val pvSatz = when {
        kinderUnter25 <= 0 -> 0.024
        kinderUnter25 == 1 -> 0.018
        else -> max(0.008, 0.018 - (minOf(kinderUnter25, 5) - 1) * 0.0025)
    }
    val pv = rundeCent(brutto * pvSatz)

    val jahresBrutto = brutto * 12.0
    val vspRv = jahresBrutto * 0.093
    val vspKvPv = jahresBrutto * (0.07 + (kvZusatzProzent / 100.0 / 2.0) + pvSatz)
    val vorsorge = ceil(vspRv + vspKvPv)
    val pauschbetraege = when (steuerklasse) {
        2 -> 1230.0 + 36.0 + 4260.0
        3 -> 1230.0 + 72.0
        5, 6 -> 0.0
        else -> 1230.0 + 36.0
    }
    val zvE = max(0.0, floor(jahresBrutto - pauschbetraege - vorsorge))
    val jahresSteuer = if (steuerklasse == 3) 2.0 * berechneTarifESt2026(zvE / 2.0) else berechneTarifESt2026(zvE)
    val lohnsteuer = rundeCent(jahresSteuer / 12.0)
    val kirche = if (kirchensteuerAktiv) rundeCent(lohnsteuer * 0.09) else 0.0
    val abgaben = rundeCent(lohnsteuer + kirche + rv + av + kv + pv)
    val netto = max(0.0, rundeCent(brutto - abgaben))
    return NettoErgebnis(brutto, lohnsteuer, kirche, rv, av, kv, pv, abgaben, netto, fahrtkosten, rundeCent(netto + fahrtkosten))
}
// --- ENDE BLOCK 2 VON 6 (MainActivity.kt) ---
// --- START BLOCK 3 VON 6 (MainActivity.kt) ---
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
    val alleEintraegeRoh by viewModel.alleEintraege.collectAsState()
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

    var aktuellerStundenlohn by remember { mutableDoubleStateOf(prefs.getFloat("global_stundenlohn", 15.75f).toDouble()) }
    var lohnGlobalErzwingen by remember { mutableStateOf(prefs.getBoolean("lohn_erzwingen", true)) }
    var steuerklasse by remember { mutableIntStateOf(prefs.getInt("steuerklasse", 1)) }
    var kvZusatz by remember { mutableDoubleStateOf(prefs.getFloat("kv_zusatz", 3.20f).toDouble()) }
    var kirchensteuer by remember { mutableStateOf(prefs.getBoolean("kirchensteuer", false)) }
    var kinderUnter25 by remember { mutableIntStateOf(prefs.getInt("kinder_anzahl", 2)) }

    var kmPauschale by remember { mutableDoubleStateOf(prefs.getFloat("km_pauschale", 0.30f).toDouble()) }
    var hinUndRueckfahrt by remember { mutableStateOf(prefs.getBoolean("hin_rueck", true)) }

    val alleEintraege = remember(alleEintraegeRoh, aktuellerStundenlohn, lohnGlobalErzwingen) {
        if (lohnGlobalErzwingen) {
            alleEintraegeRoh.map { if (it.typ == "SCHICHT") it.copy(stundenlohn = aktuellerStundenlohn) else it }
        } else alleEintraegeRoh
    }

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
// --- ENDE BLOCK 3 VON 6 (MainActivity.kt) ---
// --- START BLOCK 4 VON 6 (MainActivity.kt) ---
    val fotosOrdner = remember { File(context.filesDir, "tages_fotos").apply { if (!exists()) mkdirs() } }
    val alleFotos = remember(fotoAktualisierungsZaehler) {
        fotosOrdner.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()
    }
    val fotosProTagMap = remember(alleFotos) { alleFotos.groupBy { it.name.substringBefore("_") } }

    val speichereFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (uri != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(zielDatei).use { input.copyTo(it) } }
                fotoAktualisierungsZaehler++
            }
        }
    }

    val kameraFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (bmp != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(zielDatei).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                fotoAktualisierungsZaehler++
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
                .map { if (it.typ == "SCHICHT") it.copy(stundenlohn = aktuellerStundenlohn) else it }
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(context, "${liste.size} Schicht(en) importiert", Toast.LENGTH_LONG).show()
        }
    }

    val speichernLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch { Toast.makeText(context, CloudBackupHelfer.backupAufGeraetSpeichern(context, uri), Toast.LENGTH_LONG).show() }
    }
    val ladenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { Toast.makeText(context, CloudBackupHelfer.backupVomGeraetLaden(context, uri), Toast.LENGTH_LONG).show() }
    }

    val monatsEintraege = alleEintraege.filter { it.datum.startsWith(aktuellerMonat.toString()) }
    val farbenMap = alleFarben.associate { it.einsatzort to it.farbHex }

    val monatsFahrtkosten = remember(monatsEintraege, standortKmMap.toMap(), kmPauschale, hinUndRueckfahrt) {
        monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }.sumOf { eintrag ->
            val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
            val einfacheKm = standortKmMap[ort] ?: standortKmMap[eintrag.titel] ?: 0.0
            einfacheKm * (if (hinUndRueckfahrt) 2.0 else 1.0) * kmPauschale
        }
    }
    val monatsBrutto = remember(monatsEintraege) { monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneVerdienst() } }
    val nettoDaten = remember(monatsBrutto, steuerklasse, kvZusatz, kirchensteuer, kinderUnter25, monatsFahrtkosten) {
        berechneNetto2026(monatsBrutto, steuerklasse, kvZusatz, kirchensteuer, kinderUnter25, monatsFahrtkosten)
    }
// --- ENDE BLOCK 4 VON 6 (MainActivity.kt) ---
// --- START BLOCK 5 VON 6 (MainActivity.kt) ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = aktuellerMonat.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                aktuellerStundenlohn = aktuellerStundenlohn,
                steuerklasse = steuerklasse,
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
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menü") } },
                        title = { Text("Dienstplan", style = MaterialTheme.typography.titleLarge) },
                        actions = { IconButton(onClick = { zeigeSuche = true }) { Icon(Icons.Default.Search, contentDescription = "Suche") } }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                MonatsUndGehaltsLeiste(aktuellerMonat, monatsEintraege) { viewModel.monatWechseln(it) }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().clickable { zeigeStundenlohnDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.GERMAN, "Netto (StKl. %d): %.2f €", steuerklasse, nettoDaten.netto),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "+ Fahrtk.: %.2f € = Ausz.: %.2f €", monatsFahrtkosten, nettoDaten.auszahlung),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
// --- ENDE BLOCK 5 VON 6 (MainActivity.kt) ---
// --- START BLOCK 6 VON 6 (MainActivity.kt) ---
    if (dialogDatum != null) {
        val dStr = dialogDatum!!
        EintragDialog(
            initialDatum = dStr,
            bestehenderEintrag = bearbeiteterEintrag,
            alleEintraege = alleEintraege,
            standardStundenlohn = aktuellerStundenlohn,
            initialTagesNotiz = notizenMap[dStr].orEmpty(),
            initialStandortKm = bearbeiteterEintrag?.let { standortKmMap[it.einsatzort.ifBlank { it.titel }] ?: 0.0 } ?: 0.0,
            tagesFotos = fotosProTagMap[dStr].orEmpty(),
            onDatumGeaendert = { dialogDatum = it },
            onFotoAusGalerie = { d -> zielDatumFuerNeuesFoto = d; speichereFotoLauncher.launch("image/*") },
            onFotoMitKamera = { d -> zielDatumFuerNeuesFoto = d; kameraFotoLauncher.launch(null) },
            onFotoLoeschen = { fotoFile -> runCatching { fotoFile.delete() }; fotoAktualisierungsZaehler++ },
            onFotoAnsehen = { groesstesFoto = it },
            onSpeichern = { eintrag, neueNotiz, neueKm ->
                viewModel.eintragSpeichern(eintrag)
                if (neueNotiz.isNotBlank()) { notizenMap[dStr] = neueNotiz; prefs.edit().putString("notiz_$dStr", neueNotiz).apply() }
                else { notizenMap.remove(dStr); prefs.edit().remove("notiz_$dStr").apply() }
                val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
                if (ort.isNotBlank() && neueKm > 0.0) { standortKmMap[ort] = neueKm; prefs.edit().putFloat("km_$ort", neueKm.toFloat()).apply() }
                dialogDatum = null
            },
            onLoeschen = { viewModel.eintragLoeschen(it); dialogDatum = null },
            onAbbrechen = { dialogDatum = null }
        )
    }

    if (zeigeStundenlohnDialog) {
        var lohnInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", aktuellerStundenlohn)) }
        var kvInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kvZusatz)) }
        var klWahl by remember { mutableIntStateOf(steuerklasse) }
        var kircheWahl by remember { mutableStateOf(kirchensteuer) }
        var kinderWahl by remember { mutableIntStateOf(kinderUnter25) }
        var erzwingenWahl by remember { mutableStateOf(lohnGlobalErzwingen) }

        val vLohn = lohnInput.replace(",", ".").toDoubleOrNull() ?: aktuellerStundenlohn
        val vKv = kvInput.replace(",", ".").toDoubleOrNull() ?: kvZusatz
        val stdSumme = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneNettoStunden() }
        val vNetto = berechneNetto2026(stdSumme * vLohn, klWahl, vKv, kircheWahl, kinderWahl, monatsFahrtkosten)

        AlertDialog(
            onDismissRequest = { zeigeStundenlohnDialog = false },
            title = { Text("Brutto-Netto-Rechner 2026") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lohnInput, onValueChange = { lohnInput = it }, label = { Text("Stundenlohn Brutto (€/Std.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = erzwingenWahl, onCheckedChange = { erzwingenWahl = it })
                        Text("Für alle Schichten übernehmen", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Steuerklasse:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..6).forEach { kl -> FilterChip(selected = klWahl == kl, onClick = { klWahl = kl }, label = { Text("Kl. $kl") }) }
                    }
                    Text("Kinder unter 25 (Pflegevers.-Rabatt):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..5).forEach { k -> FilterChip(selected = kinderWahl == k, onClick = { kinderWahl = k }, label = { Text(if (k == 0) "Keine" else "$k Kind(er)") }) }
                    }
                    Text("Krankenkasse (Zusatzbeitrag 2026):", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DAK" to "3.20", "TK" to "2.45", "AOK NW" to "2.99", "Barmer" to "3.29", "IKK" to "3.10").forEach { (kasse, satz) ->
                            FilterChip(selected = kvInput == satz, onClick = { kvInput = satz }, label = { Text("$kasse ($satz%)") })
                        }
                    }
                    OutlinedTextField(value = kvInput, onValueChange = { kvInput = it }, label = { Text("KV-Zusatzbeitrag (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Kirchensteuer (9 % NRW)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = kircheWahl, onCheckedChange = { kircheWahl = it })
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(String.format(Locale.GERMAN, "Brutto pro Monat: %.2f €", vNetto.brutto), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Lohnsteuer: -%.2f €", vNetto.lohnsteuer), style = MaterialTheme.typography.bodySmall)
                            if (vNetto.kirchensteuer > 0) Text(String.format(Locale.GERMAN, "Kirchensteuer: -%.2f €", vNetto.kirchensteuer), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Rentenvers.: -%.2f € | Arbeitslosenv.: -%.2f €", vNetto.rv, vNetto.av), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Krankenvers.: -%.2f € | Pflegevers.: -%.2f €", vNetto.kv, vNetto.pv), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Abgaben gesamt: -%.2f €", vNetto.abgabenGesamt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))
                            Text(String.format(Locale.GERMAN, "Netto: %.2f €", vNetto.netto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(String.format(Locale.GERMAN, "+ Fahrtkosten: %.2f € = Auszahlung: %.2f €", vNetto.fahrtkosten, vNetto.auszahlung), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aktuellerStundenlohn = vLohn; kvZusatz = vKv; steuerklasse = klWahl; kirchensteuer = kircheWahl; kinderUnter25 = kinderWahl; lohnGlobalErzwingen = erzwingenWahl
                    prefs.edit().putFloat("global_stundenlohn", vLohn.toFloat()).putFloat("kv_zusatz", vKv.toFloat()).putInt("steuerklasse", klWahl).putBoolean("kirchensteuer", kircheWahl).putInt("kinder_anzahl", kinderWahl).putBoolean("lohn_erzwingen", erzwingenWahl).apply()
                    val aktualisiert = alleEintraegeRoh.filter { it.typ == "SCHICHT" }.map { it.copy(stundenlohn = vLohn) }
                    if (aktualisiert.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(aktualisiert)
                    zeigeStundenlohnDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { zeigeStundenlohnDialog = false }) { Text("Abbrechen") } }
        )
    }

    if (zeigeFahrtkostenDialog) {
        var pauschaleInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kmPauschale)) }
        var hinRueck by remember { mutableStateOf(hinUndRueckfahrt) }
        var ausgewaehlterOrt by remember { mutableStateOf("") }
        var ortKmInput by remember { mutableStateOf("") }
        val bekannteOrte = remember(alleEintraege) { alleEintraege.map { it.einsatzort.ifBlank { it.titel } }.filter { it.isNotBlank() }.distinct() }

        AlertDialog(
            onDismissRequest = { zeigeFahrtkostenDialog = false },
            title = { Text("Fahrtkosten & Standorte") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pauschaleInput, onValueChange = { pauschaleInput = it }, label = { Text("km-Pauschale (€/km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth().clickable { hinRueck = !hinRueck }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Hin- & Rückweg (2x km)")
                        Checkbox(checked = hinRueck, onCheckedChange = { hinRueck = it })
                    }
                    HorizontalDivider()
                    bekannteOrte.forEach { ort ->
                        val km = standortKmMap[ort] ?: 0.0
                        Row(modifier = Modifier.fillMaxWidth().clickable { ausgewaehlterOrt = ort; ortKmInput = if (km > 0.0) km.toString() else "" }.padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ort, style = MaterialTheme.typography.bodySmall)
                            Text(if (km > 0.0) "$km km" else "Eintragen", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(value = ausgewaehlterOrt, onValueChange = { ausgewaehlterOrt = it }, label = { Text("Standort") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ortKmInput, onValueChange = { ortKmInput = it }, label = { Text("Einfache Strecke (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    kmPauschale = pauschaleInput.replace(",", ".").toDoubleOrNull() ?: 0.30; hinUndRueckfahrt = hinRueck
                    prefs.edit().putFloat("km_pauschale", kmPauschale.toFloat()).putBoolean("hin_rueck", hinUndRueckfahrt).apply()
                    val kmWert = ortKmInput.replace(",", ".").toDoubleOrNull()
                    if (ausgewaehlterOrt.isNotBlank() && kmWert != null) { standortKmMap[ausgewaehlterOrt.trim()] = kmWert; prefs.edit().putFloat("km_${ausgewaehlterOrt.trim()}", kmWert.toFloat()).apply() }
                    zeigeFahrtkostenDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { zeigeFahrtkostenDialog = false }) { Text("Schließen") } }
        )
    }

    if (zeigeSuche) {
        var suchText by remember { mutableStateOf("") }
        val treffer = remember(suchText, alleEintraege) {
            if (suchText.isBlank()) emptyList()
            else alleEintraege.filter { it.titel.contains(suchText, true) || it.einsatzort.contains(suchText, true) || it.datum.contains(suchText) }.sortedByDescending { it.datum }
        }
        AlertDialog(
            onDismissRequest = { zeigeSuche = false },
            title = { Text("Im Kalender suchen") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = suchText, onValueChange = { suchText = it }, label = { Text("Ort, Titel oder Datum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        treffer.forEach { e ->
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().clickable { zeigeSuche = false; bearbeiteterEintrag = e; dialogDatum = e.datum }) {
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
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alleFotos.isEmpty()) Text("Noch keine Fotos gespeichert.")
                    alleFotos.forEach { fotoFile ->
                        val tag = fotoFile.name.substringBefore("_")
                        val bmp = remember(fotoFile.absolutePath) { runCatching { BitmapFactory.decodeFile(fotoFile.absolutePath) }.getOrNull() }
                        if (bmp != null) {
                            Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().clickable { groesstesFoto = fotoFile }) {
                                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)))
                                        Column {
                                            Text("Datum: $tag", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text(notizenMap[tag] ?: "Tippen zum Vergrößern", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(onClick = { runCatching { fotoFile.delete() }; fotoAktualisierungsZaehler++ }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Foto löschen", tint = MaterialTheme.colorScheme.error)
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
        val foto = groesstesFoto!!
        val bmp = remember(foto.absolutePath) { runCatching { BitmapFactory.decodeFile(foto.absolutePath) }.getOrNull() }
        AlertDialog(
            onDismissRequest = { groesstesFoto = null },
            title = { Text(foto.name.substringBefore("_")) },
            text = { if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 400.dp)) },
            confirmButton = { TextButton(onClick = { groesstesFoto = null }) { Text("Schließen") } },
            dismissButton = {
                TextButton(onClick = { runCatching { foto.delete() }; groesstesFoto = null; fotoAktualisierungsZaehler++ }) {
                    Text("Foto löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (zeigeMonatLeerenBestaetigung) {
        AlertDialog(
            onDismissRequest = { zeigeMonatLeerenBestaetigung = false },
            title = { Text("Monat leeren?") },
            text = { Text("Alle Einträge des aktuellen Monats löschen?") },
            confirmButton = { TextButton(onClick = { monatsEintraege.forEach { viewModel.eintragLoeschen(it) }; zeigeMonatLeerenBestaetigung = false }) { Text("Löschen", color = MaterialTheme.colorScheme.error) } },
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
    if (zeigeFehlerLog) { FehlerLogDialog(onSchliessen = { zeigeFehlerLog = false }) }
}
// --- ENDE BLOCK 6 VON 6 (MainActivity.kt) ---
