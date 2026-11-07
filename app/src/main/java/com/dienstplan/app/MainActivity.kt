// --- START BLOCK 1 VON 8 (MainActivity.kt) ---
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
// --- ENDE BLOCK 1 VON 8 (MainActivity.kt) ---
// --- START BLOCK 2 VON 8 (MainActivity.kt) ---
private fun DienstplanViewModel.monatWechseln(offset: Int) { this.monatWechseln(offset.toLong()) }
private fun DienstplanViewModel.monatWechseln(offset: Long) { this.monatWechseln(offset.toInt()) }

data class LohnTarifRegel(
    val id: Long,
    val bezeichnung: String,
    val stundenlohn: Double,
    val aktiv: Boolean,
    val gesamterZeitraum: Boolean,
    val vonDatum: String = "",
    val bisDatum: String = ""
)

data class KmStandortRegel(
    val id: Long,
    val standort: String,
    val einfacheKm: Double,
    val aktiv: Boolean,
    val gesamterZeitraum: Boolean,
    val vonDatum: String = "",
    val bisDatum: String = ""
)

private fun parseFlexDatum(text: String): LocalDate? {
    val clean = text.trim()
    if (clean.isEmpty()) return null
    return runCatching { LocalDate.parse(clean) }.getOrElse {
        runCatching { LocalDate.parse(clean, DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)) }.getOrNull()
    }
}

private fun liegtImZeitraum(datumStr: String, gesamt: Boolean, vonStr: String, bisStr: String): Boolean {
    if (gesamt) return true
    val tag = parseFlexDatum(datumStr) ?: return true
    val von = parseFlexDatum(vonStr)
    val bis = parseFlexDatum(bisStr)
    if (von != null && tag.isBefore(von)) return false
    if (bis != null && tag.isAfter(bis)) return false
    return true
}

private fun speichereLohnRegeln(prefs: android.content.SharedPreferences, liste: List<LohnTarifRegel>) {
    val text = liste.joinToString(";;") { "${it.id}|${it.bezeichnung.replace("|", " ")}|${it.stundenlohn}|${it.aktiv}|${it.gesamterZeitraum}|${it.vonDatum}|${it.bisDatum}" }
    prefs.edit().putString("lohn_regeln_v3", text).apply()
}

private fun ladeLohnRegeln(prefs: android.content.SharedPreferences, fallbackLohn: Double): List<LohnTarifRegel> {
    val raw = prefs.getString("lohn_regeln_v3", null)
    if (raw.isNullOrBlank()) return listOf(LohnTarifRegel(1L, "Standard-Tarif", fallbackLohn, true, true))
    return raw.split(";;").mapNotNull { zeile ->
        val p = zeile.split("|")
        if (p.size >= 7) runCatching { LohnTarifRegel(p[0].toLong(), p[1], p[2].toDouble(), p[3].toBoolean(), p[4].toBoolean(), p[5], p[6]) }.getOrNull() else null
    }.ifEmpty { listOf(LohnTarifRegel(1L, "Standard-Tarif", fallbackLohn, true, true)) }
}

private fun speichereKmRegeln(prefs: android.content.SharedPreferences, liste: List<KmStandortRegel>) {
    val text = liste.joinToString(";;") { "${it.id}|${it.standort.replace("|", " ")}|${it.einfacheKm}|${it.aktiv}|${it.gesamterZeitraum}|${it.vonDatum}|${it.bisDatum}" }
    prefs.edit().putString("km_regeln_v3", text).apply()
}

private fun ladeKmRegeln(prefs: android.content.SharedPreferences): List<KmStandortRegel> {
    val raw = prefs.getString("km_regeln_v3", null)
    if (raw.isNullOrBlank()) {
        val alt = mutableListOf<KmStandortRegel>()
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("km_") && k != "km_pauschale" && k != "km_regeln_v3" && v is Float && v > 0f) {
                alt.add(KmStandortRegel(System.nanoTime(), k.removePrefix("km_"), v.toDouble(), true, true))
            }
        }
        return alt
    }
    return raw.split(";;").mapNotNull { zeile ->
        val p = zeile.split("|")
        if (p.size >= 7) runCatching { KmStandortRegel(p[0].toLong(), p[1], p[2].toDouble(), p[3].toBoolean(), p[4].toBoolean(), p[5], p[6]) }.getOrNull() else null
    }
}
// --- ENDE BLOCK 2 VON 8 (MainActivity.kt) ---
// --- START BLOCK 3 VON 8 (MainActivity.kt) ---
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
    val vorsorge = ceil(jahresBrutto * 0.093 + jahresBrutto * (0.07 + (kvZusatzProzent / 100.0 / 2.0) + pvSatz))
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DienstplanTheme { DienstplanHauptbildschirm() } }
    }
}
// --- ENDE BLOCK 3 VON 8 (MainActivity.kt) ---
// --- START BLOCK 4 VON 8 (MainActivity.kt) ---
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
    var zeigeVersionDialog by remember { mutableStateOf(false) }
    var zeigeMonatLeerenBestaetigung by remember { mutableStateOf(false) }

    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }
    var zielDatumFuerNeuesFoto by remember { mutableStateOf<String?>(null) }
    var groesstesFoto by remember { mutableStateOf<File?>(null) }
    var fotoAktualisierungsZaehler by remember { mutableIntStateOf(0) }

    var aktuellerStundenlohn by remember { mutableDoubleStateOf(prefs.getFloat("global_stundenlohn", 15.75f).toDouble()) }
    val lohnRegeln = remember { mutableStateListOf<LohnTarifRegel>().apply { addAll(ladeLohnRegeln(prefs, aktuellerStundenlohn)) } }
    val kmRegeln = remember { mutableStateListOf<KmStandortRegel>().apply { addAll(ladeKmRegeln(prefs)) } }

    var steuerklasse by remember { mutableIntStateOf(prefs.getInt("steuerklasse", 1)) }
    var kvZusatz by remember { mutableDoubleStateOf(prefs.getFloat("kv_zusatz", 3.20f).toDouble()) }
    var kirchensteuer by remember { mutableStateOf(prefs.getBoolean("kirchensteuer", false)) }
    var kinderUnter25 by remember { mutableIntStateOf(prefs.getInt("kinder_anzahl", 2)) }
    var kmPauschale by remember { mutableDoubleStateOf(prefs.getFloat("km_pauschale", 0.30f).toDouble()) }
    var hinUndRueckfahrt by remember { mutableStateOf(prefs.getBoolean("hin_rueck", true)) }

    fun holeTarifLohnFuerDatum(datumStr: String): Double {
        val zeitraumTreffer = lohnRegeln.firstOrNull { it.aktiv && !it.gesamterZeitraum && liegtImZeitraum(datumStr, false, it.vonDatum, it.bisDatum) }
        if (zeitraumTreffer != null) return zeitraumTreffer.stundenlohn
        val gesamtTreffer = lohnRegeln.firstOrNull { it.aktiv && it.gesamterZeitraum }
        return gesamtTreffer?.stundenlohn ?: aktuellerStundenlohn
    }

    fun holeKmFuerOrtUndDatum(ort: String, titel: String, datumStr: String): Double {
        val aktiv = kmRegeln.filter { it.aktiv && (it.standort.equals(ort, true) || it.standort.equals(titel, true)) }
        val zeitraumTreffer = aktiv.firstOrNull { !it.gesamterZeitraum && liegtImZeitraum(datumStr, false, it.vonDatum, it.bisDatum) }
        if (zeitraumTreffer != null) return zeitraumTreffer.einfacheKm
        return aktiv.firstOrNull { it.gesamterZeitraum }?.einfacheKm ?: 0.0
    }

    val alleEintraege = remember(alleEintraegeRoh, lohnRegeln.toList(), aktuellerStundenlohn) {
        alleEintraegeRoh.map { if (it.typ == "SCHICHT") it.copy(stundenlohn = holeTarifLohnFuerDatum(it.datum)) else it }
    }

    val notizenMap = remember {
        mutableStateMapOf<String, String>().apply {
            prefs.all.forEach { (k, v) -> if (k.startsWith("notiz_") && v is String && v.isNotBlank()) put(k.removePrefix("notiz_"), v) }
        }
    }
// --- ENDE BLOCK 4 VON 8 (MainActivity.kt) ---
// --- START BLOCK 5 VON 8 (MainActivity.kt) ---
    val fotosOrdner = remember { File(context.filesDir, "tages_fotos").apply { if (!exists()) mkdirs() } }
    val alleFotos = remember(fotoAktualisierungsZaehler) {
        fotosOrdner.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()
    }
    val fotosProTagMap = remember(alleFotos) { alleFotos.groupBy { it.name.substringBefore("_") } }

    // HD-Galerie-Import: Kopiert die Originaldatei zu 100 % verlustfrei!
    val speichereFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (uri != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(zielDatei).use { input.copyTo(it) } }
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "HD-Foto gespeichert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Kamera-Foto mit 100 % Qualität speichern
    val kameraFotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        val zielTag = zielDatumFuerNeuesFoto ?: dialogDatum
        if (bmp != null && zielTag != null) {
            runCatching {
                val zielDatei = File(fotosOrdner, "${zielTag}_${System.currentTimeMillis()}.png")
                FileOutputStream(zielDatei).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                fotoAktualisierungsZaehler++
                Toast.makeText(context, "Foto gespeichert (Tipp: Für 4K-Schärfe über Galerie wählen)", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
                .map { if (it.typ == "SCHICHT") it.copy(stundenlohn = holeTarifLohnFuerDatum(it.datum)) else it }
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(context, "${liste.size} Schicht(en) mit Tariflohn importiert", Toast.LENGTH_LONG).show()
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
    val aktiveKmMapFuerKalender = remember(kmRegeln.toList(), aktuellerMonat) {
        val probeTag = "${aktuellerMonat}-15"
        kmRegeln.filter { it.aktiv && liegtImZeitraum(probeTag, it.gesamterZeitraum, it.vonDatum, it.bisDatum) }
            .associate { it.standort to it.einfacheKm }
    }

    val monatsFahrtkosten = remember(monatsEintraege, kmRegeln.toList(), kmPauschale, hinUndRueckfahrt) {
        monatsEintraege.filter { it.typ == "SCHICHT" || it.typ == "TERMIN" }.sumOf { eintrag ->
            val ort = eintrag.einsatzort.ifBlank { eintrag.titel }
            val einfacheKm = holeKmFuerOrtUndDatum(ort, eintrag.titel, eintrag.datum)
            einfacheKm * (if (hinUndRueckfahrt) 2.0 else 1.0) * kmPauschale
        }
    }
    val monatsBrutto = remember(monatsEintraege) { monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneVerdienst() } }
    val aktiverMonatsLohn = remember(aktuellerMonat, lohnRegeln.toList(), aktuellerStundenlohn) { holeTarifLohnFuerDatum("${aktuellerMonat}-15") }
    val nettoDaten = remember(monatsBrutto, steuerklasse, kvZusatz, kirchensteuer, kinderUnter25, monatsFahrtkosten) {
        berechneNetto2026(monatsBrutto, steuerklasse, kvZusatz, kirchensteuer, kinderUnter25, monatsFahrtkosten)
    }
// --- ENDE BLOCK 5 VON 8 (MainActivity.kt) ---
// --- START BLOCK 6 VON 8 (MainActivity.kt) ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = aktuellerMonat.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                aktuellerStundenlohn = aktiverMonatsLohn,
                steuerklasse = steuerklasse,
                monatsFahrtkosten = monatsFahrtkosten,
                anzahlFotosGesamt = alleFotos.size,
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                appVersion = "v2.4.0",
                onAnsichtWechseln = { scope.launch { drawerState.close() }; istKalenderAnsicht = !istKalenderAnsicht },
                onSucheOeffnen = { scope.launch { drawerState.close() }; zeigeSuche = true },
                onFotoGalerieOeffnen = { scope.launch { drawerState.close() }; zeigeFotoGalerie = true },
                onStundenlohnOeffnen = { scope.launch { drawerState.close() }; zeigeStundenlohnDialog = true },
                onFahrtkostenOeffnen = { scope.launch { drawerState.close() }; zeigeFahrtkostenDialog = true },
                onEinstellungenOeffnen = { scope.launch { drawerState.close() }; zeigeEinstellungen = true },
                onVersionOeffnen = { scope.launch { drawerState.close() }; zeigeVersionDialog = true },
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
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format(Locale.GERMAN, "Netto (StKl. %d): %.2f €", steuerklasse, nettoDaten.netto), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(String.format(Locale.GERMAN, "+ Fahrtk.: %.2f € = Ausz.: %.2f €", monatsFahrtkosten, nettoDaten.auszahlung), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        standortKmMap = aktiveKmMapFuerKalender,
                        kmPauschale = kmPauschale,
                        hinUndRueckfahrt = hinUndRueckfahrt,
                        onMonatWechseln = { offset -> viewModel.monatWechseln(offset) },
                        onFotoAnsehen = { groesstesFoto = it },
                        onTagKlick = { d, e -> bearbeiteterEintrag = e; dialogDatum = d.toString() },
                        onEintragDirektLoeschen = { viewModel.eintragLoeschen(it) }
                    )
                } else {
                    FlacheListenAnsicht(aktuellerMonat, monatsEintraege, farbenMap, nrwAktiv) { d, e -> bearbeiteterEintrag = e; dialogDatum = d.toString() }
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
            standardStundenlohn = holeTarifLohnFuerDatum(dStr),
            initialTagesNotiz = notizenMap[dStr].orEmpty(),
            initialStandortKm = bearbeiteterEintrag?.let { holeKmFuerOrtUndDatum(it.einsatzort.ifBlank { it.titel }, it.titel, dStr) } ?: 0.0,
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
                if (ort.isNotBlank() && neueKm > 0.0) {
                    kmRegeln.removeAll { it.standort.equals(ort, true) && it.gesamterZeitraum }
                    kmRegeln.add(KmStandortRegel(System.currentTimeMillis(), ort, neueKm, true, true))
                    speichereKmRegeln(prefs, kmRegeln)
                }
                dialogDatum = null
            },
            onLoeschen = { viewModel.eintragLoeschen(it); dialogDatum = null },
            onAbbrechen = { dialogDatum = null }
        )
    }
// --- ENDE BLOCK 6 VON 8 (MainActivity.kt) ---
// --- START BLOCK 7 VON 8 (MainActivity.kt) ---
    if (zeigeStundenlohnDialog) {
        var tarifName by remember { mutableStateOf("") }
        var lohnInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", aktiverMonatsLohn)) }
        var gesamtZeit by remember { mutableStateOf(true) }
        var vonInput by remember { mutableStateOf("01.${aktuellerMonat.monthValue.toString().padStart(2, '0')}.${aktuellerMonat.year}") }
        var bisInput by remember { mutableStateOf("") }
        var kvInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kvZusatz)) }
        var klWahl by remember { mutableIntStateOf(steuerklasse) }
        var kircheWahl by remember { mutableStateOf(kirchensteuer) }
        var kinderWahl by remember { mutableIntStateOf(kinderUnter25) }

        val vKv = kvInput.replace(",", ".").toDoubleOrNull() ?: kvZusatz
        val vNetto = berechneNetto2026(monatsBrutto, klWahl, vKv, kircheWahl, kinderWahl, monatsFahrtkosten)

        AlertDialog(
            onDismissRequest = { zeigeStundenlohnDialog = false },
            title = { Text("Tarif-Stundenlöhne & Netto") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gespeicherte Tarif-Stundenlöhne (an-/abwählbar):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    lohnRegeln.forEachIndexed { idx, regel ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Checkbox(checked = regel.aktiv, onCheckedChange = { aktivNeu ->
                                    lohnRegeln[idx] = regel.copy(aktiv = aktivNeu)
                                    speichereLohnRegeln(prefs, lohnRegeln)
                                })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${regel.bezeichnung}: ${String.format(Locale.GERMAN, "%.2f €", regel.stundenlohn)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    val zeitInfo = if (regel.gesamterZeitraum) "Gesamter Zeitraum" else "${regel.vonDatum.ifBlank { "..." }} bis ${regel.bisDatum.ifBlank { "offen" }}"
                                    Text(zeitInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { lohnRegeln.removeAt(idx); speichereLohnRegeln(prefs, lohnRegeln) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Neuen Tarif-Stundenlohn speichern:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = tarifName, onValueChange = { tarifName = it }, label = { Text("Tarif-Name") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        OutlinedTextField(value = lohnInput, onValueChange = { lohnInput = it }, label = { Text("€/Std.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.8f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = gesamtZeit, onClick = { gesamtZeit = true }, label = { Text("Gesamter Zeitraum") })
                        FilterChip(selected = !gesamtZeit, onClick = { gesamtZeit = false }, label = { Text("Zeitraum Von–Bis") })
                    }
                    if (!gesamtZeit) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = vonInput, onValueChange = { vonInput = it }, label = { Text("Von (TT.MM.JJJJ)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = bisInput, onValueChange = { bisInput = it }, label = { Text("Bis (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                    Button(onClick = {
                        val betrag = lohnInput.replace(",", ".").toDoubleOrNull()
                        if (betrag != null) {
                            val name = tarifName.trim().ifBlank { if (gesamtZeit) "Basis-Tarif" else "Tarif ab $vonInput" }
                            lohnRegeln.add(0, LohnTarifRegel(System.currentTimeMillis(), name, betrag, true, gesamtZeit, vonInput.trim(), bisInput.trim()))
                            speichereLohnRegeln(prefs, lohnRegeln)
                            aktuellerStundenlohn = betrag
                            tarifName = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Tarif-Stundenlohn hinzufügen")
                    }

                    HorizontalDivider()
                    Text("Brutto-Netto-Parameter (2026):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..6).forEach { kl -> FilterChip(selected = klWahl == kl, onClick = { klWahl = kl }, label = { Text("StKl. $kl") }) }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..5).forEach { k -> FilterChip(selected = kinderWahl == k, onClick = { kinderWahl = k }, label = { Text(if (k == 0) "Keine Kinder" else "$k Kind(er)") }) }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DAK" to "3.20", "TK" to "2.45", "AOK NW" to "2.99", "Barmer" to "3.29").forEach { (kasse, satz) ->
                            FilterChip(selected = kvInput == satz, onClick = { kvInput = satz }, label = { Text("$kasse ($satz%)") })
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(String.format(Locale.GERMAN, "Brutto: %.2f € | Abgaben: -%.2f €", vNetto.brutto, vNetto.abgabenGesamt), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Netto: %.2f € (+ %.2f € Fahrtk. = %.2f €)", vNetto.netto, vNetto.fahrtkosten, vNetto.auszahlung), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    kvZusatz = vKv; steuerklasse = klWahl; kirchensteuer = kircheWahl; kinderUnter25 = kinderWahl
                    prefs.edit().putFloat("kv_zusatz", vKv.toFloat()).putInt("steuerklasse", klWahl).putBoolean("kirchensteuer", kircheWahl).putInt("kinder_anzahl", kinderWahl).apply()
                    val aktualisiert = alleEintraegeRoh.filter { it.typ == "SCHICHT" }.map { it.copy(stundenlohn = holeTarifLohnFuerDatum(it.datum)) }
                    if (aktualisiert.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(aktualisiert)
                    zeigeStundenlohnDialog = false
                }) { Text("Fertig & Übernehmen") }
            }
        )
    }
// --- ENDE BLOCK 7 VON 8 (MainActivity.kt) ---
// --- START BLOCK 8 VON 8 (MainActivity.kt) ---
    if (zeigeFahrtkostenDialog) {
        var pauschaleInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kmPauschale)) }
        var hinRueck by remember { mutableStateOf(hinUndRueckfahrt) }
        var ausgewaehlterOrt by remember { mutableStateOf("") }
        var ortKmInput by remember { mutableStateOf("") }
        var kmGesamtZeit by remember { mutableStateOf(true) }
        var kmVonInput by remember { mutableStateOf("") }
        var kmBisInput by remember { mutableStateOf("") }
        val bekannteOrte = remember(alleEintraege) { alleEintraege.map { it.einsatzort.ifBlank { it.titel } }.filter { it.isNotBlank() }.distinct() }

        AlertDialog(
            onDismissRequest = { zeigeFahrtkostenDialog = false },
            title = { Text("Kilometer & Standorte (Zeitraum)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = pauschaleInput, onValueChange = { pauschaleInput = it }, label = { Text("€/km") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hinRueck = !hinRueck }) {
                            Checkbox(checked = hinRueck, onCheckedChange = { hinRueck = it })
                            Text("Hin- & Rückfahrt (2x)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    Text("Gespeicherte Kilometer-Regeln (an-/abwählbar):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    kmRegeln.forEachIndexed { idx, regel ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Checkbox(checked = regel.aktiv, onCheckedChange = { aktivNeu ->
                                    kmRegeln[idx] = regel.copy(aktiv = aktivNeu)
                                    speichereKmRegeln(prefs, kmRegeln)
                                })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${regel.standort}: ${regel.einfacheKm} km", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    val zText = if (regel.gesamterZeitraum) "Gesamter Zeitraum" else "${regel.vonDatum.ifBlank { "..." }} bis ${regel.bisDatum.ifBlank { "offen" }}"
                                    Text(zText, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { kmRegeln.removeAt(idx); speichereKmRegeln(prefs, kmRegeln) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("Standort antippen oder neu eingeben:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bekannteOrte.forEach { ort -> FilterChip(selected = ausgewaehlterOrt == ort, onClick = { ausgewaehlterOrt = ort }, label = { Text(ort) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = ausgewaehlterOrt, onValueChange = { ausgewaehlterOrt = it }, label = { Text("Standort") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        OutlinedTextField(value = ortKmInput, onValueChange = { ortKmInput = it }, label = { Text("km (einfach)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.8f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = kmGesamtZeit, onClick = { kmGesamtZeit = true }, label = { Text("Gesamter Zeitraum") })
                        FilterChip(selected = !kmGesamtZeit, onClick = { kmGesamtZeit = false }, label = { Text("Zeitraum Von–Bis") })
                    }
                    if (!kmGesamtZeit) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = kmVonInput, onValueChange = { kmVonInput = it }, label = { Text("Von (TT.MM.JJJJ)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = kmBisInput, onValueChange = { kmBisInput = it }, label = { Text("Bis (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                    Button(onClick = {
                        val kmWert = ortKmInput.replace(",", ".").toDoubleOrNull()
                        if (ausgewaehlterOrt.isNotBlank() && kmWert != null) {
                            kmRegeln.add(0, KmStandortRegel(System.currentTimeMillis(), ausgewaehlterOrt.trim(), kmWert, true, kmGesamtZeit, kmVonInput.trim(), kmBisInput.trim()))
                            speichereKmRegeln(prefs, kmRegeln)
                            ortKmInput = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("+ Kilometer-Regel speichern") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    kmPauschale = pauschaleInput.replace(",", ".").toDoubleOrNull() ?: 0.30; hinUndRueckfahrt = hinRueck
                    prefs.edit().putFloat("km_pauschale", kmPauschale.toFloat()).putBoolean("hin_rueck", hinUndRueckfahrt).apply()
                    zeigeFahrtkostenDialog = false
                }) { Text("Fertig") }
            }
        )
    }

    if (zeigeVersionDialog) {
        AlertDialog(
            onDismissRequest = { zeigeVersionDialog = false },
            title = { Text("Dienstplan App • Version v2.4.0") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neu in Version 2.4.0 (Aktuell):", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("• Tarif-Stundenlöhne: Mehrere Löhne speicherbar, einzeln per Haken aktivierbar & mit Zeitraum (Von–Bis oder Gesamt).\n• Kilometer-Regeln: Standorte separat mit Zeitraum oder Gesamt speicherbar & an-/abwählbar.\n• HD-Foto-Zoom: Verlustfreier Galerie-Import & stufenloser 4x Pinch-to-Zoom.\n• Farben für alles: Freiwillige Farbcodierung für Märkte, Frei, Urlaub, Krank & Termine.\n• BMF Netto-Rechner 2026: Centgenaue Berechnung inkl. Kinder-PV-Abschlag & Krankenkassen.", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Text("Version 2.3.0:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("• Neues Material-3 Kachel-Menü & randloses Adaptive-Icon.\n• Durchgehende Ferien- & Feiertagsbalken im Kalender (inkl. OpenHolidaysAPI).", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Text("Version 2.0 – 2.2:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("• Direkter PDF-Dienstplan-Import, Tages-Notizen, Monats-Swipe & lokales Datenbank-Backup (.db).", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { zeigeVersionDialog = false }) { Text("Schließen") } }
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
                                    if (e.typ == "SCHICHT") Text("${e.startZeit}-${eintragZeitEnde(e)}", style = MaterialTheme.typography.bodySmall)
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
                                            Text("Tippen für HD-Zoom", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(onClick = { runCatching { fotoFile.delete() }; fotoAktualisierungsZaehler++ }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
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

    // HD-FOTO-ANSICHT MIT 4X PINCH-TO-ZOOM & VERSCHIEBEN
    if (groesstesFoto != null) {
        val foto = groesstesFoto!!
        val bmp = remember(foto.absolutePath) { runCatching { BitmapFactory.decodeFile(foto.absolutePath) }.getOrNull() }
        var zoom by remember(foto) { mutableFloatStateOf(1f) }
        var offsetX by remember(foto) { mutableFloatStateOf(0f) }
        var offsetY by remember(foto) { mutableFloatStateOf(0f) }

        AlertDialog(
            onDismissRequest = { groesstesFoto = null },
            title = { Text("${foto.name.substringBefore("_")} (2-Finger-Zoom)") },
            text = {
                if (bmp != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 460.dp).clip(RoundedCornerShape(8.dp))
                            .pointerInput(foto) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 4.5f)
                                    if (zoom > 1f) { offsetX += pan.x; offsetY += pan.y } else { offsetX = 0f; offsetY = 0f }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offsetX, translationY = offsetY)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { groesstesFoto = null }) { Text("Schließen") } },
            dismissButton = {
                Row {
                    if (zoom > 1f) TextButton(onClick = { zoom = 1f; offsetX = 0f; offsetY = 0f }) { Text("100%") }
                    TextButton(onClick = { runCatching { foto.delete() }; groesstesFoto = null; fotoAktualisierungsZaehler++ }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
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

private fun eintragZeitEnde(e: EintragEntity): String = e.endZeit
// --- ENDE BLOCK 8 VON 8 (MainActivity.kt) ---
