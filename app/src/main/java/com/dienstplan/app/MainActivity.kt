package com.dienstplan.app

import android.content.Context
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
import com.dienstplan.app.util.NrwDatenHelfer
import com.dienstplan.app.util.PdfImportHelfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
data class NettoErgebnis(
    val brutto: Double,
    val lohnsteuer: Double,
    val kirchensteuer: Double,
    val sozialabgaben: Double,
    val netto: Double,
    val fahrtkosten: Double,
    val auszahlungGesamt: Double
)

fun berechneMonatsNetto2026(
    bruttoMonat: Double,
    steuerklasse: Int,
    kvZusatzbeitragProzent: Double,
    kirchensteuerAktiv: Boolean,
    hatKinder: Boolean,
    steuerfreiFahrtkosten: Double
): NettoErgebnis {
    if (bruttoMonat <= 0.0) {
        return NettoErgebnis(0.0, 0.0, 0.0, 0.0, 0.0, steuerfreiFahrtkosten, steuerfreiFahrtkosten)
    }
    // Arbeitnehmer-Sozialversicherungsanteile 2026
    val rvSatz = 0.093 // 9,3 % Rentenversicherung (AN-Anteil)
    val avSatz = 0.013 // 1,3 % Arbeitslosenversicherung (AN-Anteil)
    val kvSatz = 0.073 + (kvZusatzbeitragProzent / 100.0 / 2.0) // 7,3 % + halber Zusatzbeitrag
    val pvSatz = if (hatKinder) 0.018 else 0.024 // 1,8 % mit Kind, 2,4 % ohne Kind (inkl. Zuschlag)

    val sozialabgaben = bruttoMonat * (rvSatz + avSatz + kvSatz + pvSatz)

    // Jahreshochschätzung für Lohnsteuer 2026 (§ 32a EStG orientiert)
    val jahresBrutto = bruttoMonat * 12.0
    val werbungskostenPauschale = 1230.0
    val sonderausgabenPauschale = 36.0
    val vorsorgePauschale = sozialabgaben * 12.0 * 0.88

    val freibetragNachKlasse = when (steuerklasse) {
        1 -> 12348.0
        2 -> 12348.0 + 4260.0 // inkl. Entlastungsbetrag Alleinerziehende
        3 -> 24696.0
        4 -> 12348.0
        5 -> 0.0
        6 -> 0.0
        else -> 12348.0
    }

    val zvE = max(0.0, jahresBrutto - werbungskostenPauschale - sonderausgabenPauschale - vorsorgePauschale - freibetragNachKlasse)
    val jahresSteuer = when {
        zvE <= 0.0 -> 0.0
        zvE <= 17000.0 -> zvE * (0.14 + (zvE / 17000.0) * 0.09)
        zvE <= 66760.0 -> (17000.0 * 0.23) + (zvE - 17000.0) * 0.30
        else -> (17000.0 * 0.23) + (49760.0 * 0.30) + (zvE - 66760.0) * 0.42
    }

    val lohnsteuerMonat = jahresSteuer / 12.0
    val kirchensteuerMonat = if (kirchensteuerAktiv) lohnsteuerMonat * 0.09 else 0.0 // 9 % NRW
    val nettoMonat = max(0.0, bruttoMonat - sozialabgaben - lohnsteuerMonat - kirchensteuerMonat)

    return NettoErgebnis(
        brutto = bruttoMonat,
        lohnsteuer = lohnsteuerMonat,
        kirchensteuer = kirchensteuerMonat,
        sozialabgaben = sozialabgaben,
        netto = nettoMonat,
        fahrtkosten = steuerfreiFahrtkosten,
        auszahlungGesamt = nettoMonat + steuerfreiFahrtkosten
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DienstplanTheme { DienstplanHauptbildschirm() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DienstplanHauptbildschirm(viewModel: DienstplanViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val prefs = remember { context.getSharedPreferences("dienstplan_zusatz_prefs", Context.MODE_PRIVATE) }

    val aktuellerMonat by viewModel.aktuellerMonat.collectAsState()
    val nrwAktiv by viewModel.nrwDatenAktiv.collectAsState()
    val alleEintraegeRoh by viewModel.alleEintraege.collectAsState()
    val alleFarben by viewModel.alleFarben.collectAsState()

    var standardLohn by remember { mutableDoubleStateOf(prefs.getString("std_lohn", "15.75")?.toDoubleOrNull() ?: 15.75) }
    var lohnImmerErzwingen by remember { mutableStateOf(prefs.getBoolean("lohn_erzwingen", true)) }
    var steuerklasse by remember { mutableIntStateOf(prefs.getInt("steuerklasse", 1)) }
    var kvZusatzbeitrag by remember { mutableDoubleStateOf(prefs.getString("kv_zusatz", "1.7")?.toDoubleOrNull() ?: 1.7) }
    var kirchensteuerAktiv by remember { mutableStateOf(prefs.getBoolean("kirchensteuer", false)) }
    var hatKinder by remember { mutableStateOf(prefs.getBoolean("hat_kinder", true)) }

    var kmPauschale by remember { mutableDoubleStateOf(prefs.getString("km_pauschale", "0.30")?.toDoubleOrNull() ?: 0.30) }
    var hinUndRueck by remember { mutableStateOf(prefs.getBoolean("hin_und_rueck", true)) }

    var notizenMap by remember { mutableStateOf(ladeStringMap(prefs, "notizen_json")) }
    var fotosMap by remember { mutableStateOf(ladeListeMap(prefs, "fotos_json")) }
    var fahrtkostenMap by remember { mutableStateOf(ladeDoubleMap(prefs, "fahrtkosten_json")) }
    var eigeneFerienMap by remember { mutableStateOf(ladeStringMap(prefs, "eigene_ferien_json")) }

    // WICHTIG: Überschreibt auf Wunsch sofort alle Schichten mit dem eingestellten Stundenlohn!
    val alleEintraege = remember(alleEintraegeRoh, standardLohn, lohnImmerErzwingen) {
        if (lohnImmerErzwingen) {
            alleEintraegeRoh.map { if (it.typ == "SCHICHT") it.copy(stundenlohn = standardLohn) else it }
        } else alleEintraegeRoh
    }
var istKalenderAnsicht by remember { mutableStateOf(true) }
    var zeigeEinstellungen by remember { mutableStateOf(false) }
    var zeigeFehlerLog by remember { mutableStateOf(false) }
    var zeigeSuche by remember { mutableStateOf(false) }
    var zeigeMonatBereinigen by remember { mutableStateOf(false) }
    var zeigeFotoGalerie by remember { mutableStateOf(false) }
    var zeigeLohnDialog by remember { mutableStateOf(false) }
    var zeigeFahrtkostenDialog by remember { mutableStateOf(false) }
    var zeigeFeiertageFerienDialog by remember { mutableStateOf(false) }
    var dialogDatum by remember { mutableStateOf<String?>(null) }
    var bearbeiteterEintrag by remember { mutableStateOf<EintragEntity?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val liste = PdfImportHelfer.pdfEinlesen(context, uri, aktuellerMonat.toString())
                .map { if (it.typ == "SCHICHT") it.copy(stundenlohn = standardLohn) else it }
            if (liste.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(liste)
            Toast.makeText(context, "${liste.size} Schicht(en) mit ${String.format(Locale.GERMAN, "%.2f", standardLohn)} €/Std. importiert", Toast.LENGTH_LONG).show()
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
    val monatsName = "${aktuellerMonat.month.getDisplayName(TextStyle.FULL, Locale.GERMAN).replaceFirstChar { it.uppercase() }} ${aktuellerMonat.year}"

    val monatsBrutto = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneVerdienst() }
    val monatsKmGesamt = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { eintrag ->
        val ortKey = eintrag.einsatzort.ifBlank { eintrag.titel }
        val einfacheKm = fahrtkostenMap[ortKey] ?: fahrtkostenMap[eintrag.titel] ?: 0.0
        if (hinUndRueck) einfacheKm * 2.0 else einfacheKm
    }
    val monatsFahrtkosten = monatsKmGesamt * kmPauschale
    val nettoDaten = berechneMonatsNetto2026(monatsBrutto, steuerklasse, kvZusatzbeitrag, kirchensteuerAktiv, hatKinder, monatsFahrtkosten)
    val anzahlFotosGesamt = fotosMap.values.sumOf { it.size }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HamburgerMenuInhalt(
                istKalenderAnsicht = istKalenderAnsicht,
                aktuellerMonatName = monatsName,
                anzahlFotosGesamt = anzahlFotosGesamt,
                standardStundenlohn = standardLohn,
                steuerklasse = steuerklasse,
                monatsFahrtkosten = monatsFahrtkosten,
                anzahlLogs = FehlerLogHelfer.eintraege.size,
                onAnsichtWechseln = { scope.launch { drawerState.close() }; istKalenderAnsicht = !istKalenderAnsicht },
                onSucheOeffnen = { scope.launch { drawerState.close() }; zeigeSuche = true },
                onFotoGalerieOeffnen = { scope.launch { drawerState.close() }; zeigeFotoGalerie = true },
                onLohnUndNettoOeffnen = { scope.launch { drawerState.close() }; zeigeLohnDialog = true },
                onFahrtkostenOeffnen = { scope.launch { drawerState.close() }; zeigeFahrtkostenDialog = true },
                onFeiertageFerienOeffnen = { scope.launch { drawerState.close() }; zeigeFeiertageFerienDialog = true },
                onEinstellungenOeffnen = { scope.launch { drawerState.close() }; zeigeEinstellungen = true },
                onPdfImport = { scope.launch { drawerState.close() }; pdfLauncher.launch("application/pdf") },
                onMonatBereinigenOeffnen = { scope.launch { drawerState.close() }; zeigeMonatBereinigen = true },
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
                                Icon(Icons.Default.Menu, contentDescription = "Menü")
                            }
                        },
                        title = { Text("Dienstplan", style = MaterialTheme.typography.titleLarge) },
                        actions = {
                            IconButton(onClick = { zeigeSuche = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Suchen")
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                MonatsUndGehaltsLeiste(aktuellerMonat, monatsEintraege) { viewModel.monatWechseln(it) }

                // LIVE BRUTTO-NETTO & FAHRTKOSTEN-LEISTE (Antippbar zum Öffnen des Rechners)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().clickable { zeigeLohnDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.GERMAN, "Netto (StKl. %d): ~%.2f €", steuerklasse, nettoDaten.netto),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.GERMAN, "Fahrtk.: %.2f € • Ausz.: %.2f €", monatsFahrtkosten, nettoDaten.auszahlungGesamt),
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
                        fotosMap = fotosMap,
                        fahrtkostenMap = fahrtkostenMap,
                        kmPauschale = kmPauschale,
                        hinUndRueck = hinUndRueck,
                        eigeneFerienMap = eigeneFerienMap,
                        onMonatSwipe = { richtung -> viewModel.monatWechseln(aktuellerMonat.plusMonths(richtung.toLong())) },
                        onTagKlick = { d, e -> bearbeiteterEintrag = e; dialogDatum = d.toString() },
                        onEintragDirektLoeschen = { e -> viewModel.eintragLoeschen(e) }
                    )
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
            onSpeichern = { eintrag ->
                val angepassterEintrag = if (eintrag.typ == "SCHICHT" && (lohnImmerErzwingen || eintrag.stundenlohn == 15.0)) {
                    eintrag.copy(stundenlohn = standardLohn)
                } else eintrag
                viewModel.eintragSpeichern(angepassterEintrag)
                dialogDatum = null
            },
            onLoeschen = { viewModel.eintragLoeschen(it); dialogDatum = null },
            onAbbrechen = { dialogDatum = null }
        )
    }

    // DIALOG 1: BRUTTO-NETTO-RECHNER & STUNDENLOHN
    if (zeigeLohnDialog) {
        var lohnInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", standardLohn)) }
        var kvInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kvZusatzbeitrag)) }
        var klAuswahl by remember { mutableIntStateOf(steuerklasse) }
        var kircheAuswahl by remember { mutableStateOf(kirchensteuerAktiv) }
        var kinderAuswahl by remember { mutableStateOf(hatKinder) }
        var erzwingenAuswahl by remember { mutableStateOf(lohnImmerErzwingen) }

        val vorschauLohn = lohnInput.replace(",", ".").toDoubleOrNull() ?: standardLohn
        val vorschauKv = kvInput.replace(",", ".").toDoubleOrNull() ?: kvZusatzbeitrag
        val gesamtStunden = monatsEintraege.filter { it.typ == "SCHICHT" }.sumOf { it.berechneNettoStunden() }
        val vorschauBrutto = gesamtStunden * vorschauLohn
        val vorschauNetto = berechneMonatsNetto2026(vorschauBrutto, klAuswahl, vorschauKv, kircheAuswahl, kinderAuswahl, monatsFahrtkosten)

        AlertDialog(
            onDismissRequest = { zeigeLohnDialog = false },
            title = { Text("Brutto-Netto-Rechner & Lohn") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lohnInput,
                        onValueChange = { lohnInput = it },
                        label = { Text("Stundenlohn Brutto (€/Std.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = erzwingenAuswahl, onCheckedChange = { erzwingenAuswahl = it })
                        Text("Diesen Stundenlohn sofort für alle Schichten übernehmen", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Lohnsteuerklasse wählen:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..6).forEach { kl ->
                            FilterChip(selected = klAuswahl == kl, onClick = { klAuswahl = kl }, label = { Text("Kl. $kl") })
                        }
                    }
                    Text("Krankenkasse (Zusatzbeitrag %):", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TK" to "1.20", "AOK NW" to "1.89", "Barmer" to "2.19", "DAK" to "1.70").forEach { (name, satz) ->
                            FilterChip(selected = kvInput == satz, onClick = { kvInput = satz }, label = { Text("$name ($satz%)") })
                        }
                    }
                    OutlinedTextField(
                        value = kvInput,
                        onValueChange = { kvInput = it },
                        label = { Text("KV-Zusatzbeitrag in %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Elterneigenschaft / Kinder (PV-Rabatt)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = kinderAuswahl, onCheckedChange = { kinderAuswahl = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Kirchensteuer (9 % NRW)", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = kircheAuswahl, onCheckedChange = { kircheAuswahl = it })
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Monats-Abrechnung ($monatsName):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.GERMAN, "Brutto (%.1f Std.): %.2f €", gesamtStunden, vorschauNetto.brutto), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Sozialabgaben (KV/RV/AV/PV): -%.2f €", vorschauNetto.sozialabgaben), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "Lohn- & Kirchensteuer: -%.2f €", vorschauNetto.lohnsteuer + vorschauNetto.kirchensteuer), style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))
                            Text(String.format(Locale.GERMAN, "Netto-Gehalt: %.2f €", vorschauNetto.netto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(String.format(Locale.GERMAN, "+ Fahrtkosten (steuerfrei): %.2f €", vorschauNetto.fahrtkosten), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.GERMAN, "= Auszahlung Gesamt: %.2f €", vorschauNetto.auszahlungGesamt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    standardLohn = vorschauLohn
                    kvZusatzbeitrag = vorschauKv
                    steuerklasse = klAuswahl
                    kirchensteuerAktiv = kircheAuswahl
                    hatKinder = kinderAuswahl
                    lohnImmerErzwingen = erzwingenAuswahl
                    prefs.edit()
                        .putString("std_lohn", standardLohn.toString())
                        .putString("kv_zusatz", kvZusatzbeitrag.toString())
                        .putInt("steuerklasse", steuerklasse)
                        .putBoolean("kirchensteuer", kirchensteuerAktiv)
                        .putBoolean("hat_kinder", hatKinder)
                        .putBoolean("lohn_erzwingen", lohnImmerErzwingen)
                        .apply()
                    if (erzwingenAuswahl) {
                        val aktualisiert = alleEintraegeRoh.map { if (it.typ == "SCHICHT") it.copy(stundenlohn = standardLohn) else it }
                        if (aktualisiert.isNotEmpty()) viewModel.mehrereEintraegeSpeichern(aktualisiert)
                    }
                    zeigeLohnDialog = false
                    Toast.makeText(context, "Stundenlohn (${String.format(Locale.GERMAN, "%.2f", standardLohn)} €) & Netto-Rechner gespeichert!", Toast.LENGTH_SHORT).show()
                }) { Text("Speichern & Übernehmen") }
            },
            dismissButton = { TextButton(onClick = { zeigeLohnDialog = false }) { Text("Abbrechen") } }
        )
    }

    // DIALOG 2: FEIERTAGE & SCHULFERIEN (NRW + API + EIGENE FERIENTAGE)
    if (zeigeFeiertageFerienDialog) {
        var neuesDatum by remember { mutableStateOf("") }
        var neuerFerienName by remember { mutableStateOf("Beweglicher Ferientag") }
        var ladeStatus by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { zeigeFeiertageFerienDialog = false },
            title = { Text("Feiertage & Schulferien (NRW)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Im Kalender farbig markieren", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(checked = nrwAktiv, onCheckedChange = { viewModel.nrwDatenUmschalten() })
                    }
                    OutlinedButton(
                        onClick = {
                            ladeStatus = "Lade NRW-Schulferien von OpenHolidaysAPI..."
                            scope.launch {
                                val (ok, msg, geladen) = ladeOpenHolidaysNrw(aktuellerMonat.year)
                                if (ok && geladen.isNotEmpty()) {
                                    val neu = eigeneFerienMap.toMutableMap().apply { putAll(geladen) }
                                    eigeneFerienMap = neu
                                    speichereStringMap(prefs, "eigene_ferien_json", neu)
                                }
                                ladeStatus = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("NRW-Ferien ${aktuellerMonat.year} online aktualisieren (API)") }
                    if (ladeStatus != null) {
                        Text(ladeStatus!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider()
                    Text("Eigenen Ferientag / Brückentag hinzufügen:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = neuesDatum, onValueChange = { neuesDatum = it }, label = { Text("Datum (JJJJ-MM-TT, z. B. 2026-05-15)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = neuerFerienName, onValueChange = { neuerFerienName = it }, label = { Text("Bezeichnung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (neuesDatum.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                val neu = eigeneFerienMap.toMutableMap().apply { put(neuesDatum.trim(), neuerFerienName.trim()) }
                                eigeneFerienMap = neu
                                speichereStringMap(prefs, "eigene_ferien_json", neu)
                                neuesDatum = ""
                            } else {
                                Toast.makeText(context, "Bitte Format JJJJ-MM-TT nutzen", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Ferientag eintragen") }
                }
            },
            confirmButton = { TextButton(onClick = { zeigeFeiertageFerienDialog = false }) { Text("Fertig") } }
        )
    }

    // DIALOG 3: FAHRTKOSTEN & STANDORTE
    if (zeigeFahrtkostenDialog) {
        val bekannteOrte = (alleEintraege.filter { it.typ == "SCHICHT" }.map { it.einsatzort.ifBlank { it.titel } } + fahrtkostenMap.keys)
            .filter { it.isNotBlank() }.distinct().sorted()
        var pauschaleInput by remember { mutableStateOf(String.format(Locale.GERMAN, "%.2f", kmPauschale)) }
        var neuerOrt by remember { mutableStateOf("") }
        var neueKm by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { zeigeFahrtkostenDialog = false },
            title = { Text("Fahrtkosten & Standorte") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pauschaleInput, onValueChange = { pauschaleInput = it }, label = { Text("Kilometerpauschale (€/km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hinUndRueck, onCheckedChange = { hinUndRueck = it; prefs.edit().putBoolean("hin_und_rueck", it).apply() })
                        Text("Hin- und Rückweg berechnen (2x Strecke)", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    bekannteOrte.forEach { ort ->
                        var ortKmText by remember(ort, fahrtkostenMap[ort]) { mutableStateOf(fahrtkostenMap[ort]?.toString() ?: "") }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(ort, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = ortKmText,
                                onValueChange = {
                                    ortKmText = it
                                    val kmVal = it.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val neu = fahrtkostenMap.toMutableMap().apply { put(ort, kmVal) }
                                    fahrtkostenMap = neu
                                    speichereDoubleMap(prefs, "fahrtkosten_json", neu)
                                },
                                label = { Text("km (einfach)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(115.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = neuerOrt, onValueChange = { neuerOrt = it }, label = { Text("Neuer Ort") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = neueKm, onValueChange = { neueKm = it }, label = { Text("km") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.width(80.dp))
                    }
                    OutlinedButton(onClick = {
                        val kmVal = neueKm.replace(",", ".").toDoubleOrNull()
                        if (neuerOrt.isNotBlank() && kmVal != null) {
                            val neu = fahrtkostenMap.toMutableMap().apply { put(neuerOrt.trim(), kmVal) }
                            fahrtkostenMap = neu
                            speichereDoubleMap(prefs, "fahrtkosten_json", neu)
                            neuerOrt = ""; neueKm = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("+ Standort speichern") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    kmPauschale = pauschaleInput.replace(",", ".").toDoubleOrNull() ?: 0.30
                    prefs.edit().putString("km_pauschale", kmPauschale.toString()).apply()
                    zeigeFahrtkostenDialog = false
                }) { Text("Fertig") }
            }
        )
    }

    if (zeigeMonatBereinigen) {
        AlertDialog(
            onDismissRequest = { zeigeMonatBereinigen = false },
            title = { Text("$monatsName aufräumen") },
            text = { Text("Möchtest du alle ${monatsEintraege.size} Einträge in diesem Monat löschen?") },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = {
                    monatsEintraege.forEach { viewModel.eintragLoeschen(it) }
                    zeigeMonatBereinigen = false
                }) { Text("Monat komplett leeren") }
            },
            dismissButton = { TextButton(onClick = { zeigeMonatBereinigen = false }) { Text("Abbrechen") } }
        )
    }

    if (zeigeSuche) {
        var suchbegriff by remember { mutableStateOf("") }
        val treffer = remember(suchbegriff, alleEintraege) {
            if (suchbegriff.isBlank()) emptyList()
            else alleEintraege.filter { it.titel.contains(suchbegriff, true) || it.einsatzort.contains(suchbegriff, true) || it.datum.contains(suchbegriff) }.sortedByDescending { it.datum }
        }
        AlertDialog(
            onDismissRequest = { zeigeSuche = false },
            title = { Text("Suchen im Kalender") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = suchbegriff, onValueChange = { suchbegriff = it }, label = { Text("Ort, Schicht oder Datum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(treffer) { e ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                runCatching { viewModel.monatWechseln(YearMonth.from(LocalDate.parse(e.datum))) }
                                bearbeiteterEintrag = e; dialogDatum = e.datum; zeigeSuche = false
                            }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(e.titel, fontWeight = FontWeight.SemiBold)
                                Text(e.datum, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { zeigeSuche = false }) { Text("Schließen") } }
        )
    }

    if (zeigeFotoGalerie) {
        val alleFotos = fotosMap.entries.sortedByDescending { it.key }.flatMap { (d, l) -> l.map { d to it } }
        AlertDialog(
            onDismissRequest = { zeigeFotoGalerie = false },
            title = { Text("Alle Tages-Fotos (${alleFotos.size})") },
            text = {
                if (alleFotos.isEmpty()) Text("Noch keine Tages-Fotos vorhanden.")
                else LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(alleFotos) { (d, pfad) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AsyncImage(model = File(pfad), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)))
                            Text(d, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { zeigeFotoGalerie = false }) { Text("Schließen") } }
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

private suspend fun ladeOpenHolidaysNrw(jahr: Int): Triple<Boolean, String, Map<String, String>> = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://openholidaysapi.org/SchoolHolidays?countryIsoCode=DE&subdivisionCode=DE-NW&languageIsoCode=DE&validFrom=$jahr-01-01&validTo=$jahr-12-31")
        val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 6000; readTimeout = 6000 }
        if (conn.responseCode != 200) return@withContext Triple(false, "API-Fehler: HTTP ${conn.responseCode}", emptyMap())
        val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
        val ergebnis = mutableMapOf<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val start = LocalDate.parse(obj.getString("startDate"))
            val ende = LocalDate.parse(obj.getString("endDate"))
            val nameArr = obj.optJSONArray("name")
            val titel = if (nameArr != null && nameArr.length() > 0) nameArr.getJSONObject(0).optString("text", "Ferien NRW") else "Ferien NRW"
            var d = start
            while (!d.isAfter(ende)) { ergebnis[d.toString()] = titel; d = d.plusDays(1) }
        }
        Triple(true, "${arr.length()} NRW-Ferienzeiträume für $jahr geladen!", ergebnis)
    } catch (e: Exception) {
        Triple(false, "Offline-Daten aktiv (${e.localizedMessage ?: "Kein Netz"})", emptyMap())
    }
}

private fun ladeStringMap(prefs: android.content.SharedPreferences, key: String): Map<String, String> = runCatching {
    val json = JSONObject(prefs.getString(key, "{}") ?: "{}")
    json.keys().asSequence().associateWith { json.getString(it) }
}.getOrDefault(emptyMap())

private fun speichereStringMap(prefs: android.content.SharedPreferences, key: String, map: Map<String, String>) {
    val json = JSONObject(); map.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString(key, json.toString()).apply()
}

private fun ladeDoubleMap(prefs: android.content.SharedPreferences, key: String): Map<String, Double> = runCatching {
    val json = JSONObject(prefs.getString(key, "{}") ?: "{}")
    json.keys().asSequence().associateWith { json.getDouble(it) }
}.getOrDefault(emptyMap())

private fun speichereDoubleMap(prefs: android.content.SharedPreferences, key: String, map: Map<String, Double>) {
    val json = JSONObject(); map.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString(key, json.toString()).apply()
}

private fun ladeListeMap(prefs: android.content.SharedPreferences, key: String): Map<String, List<String>> = runCatching {
    val json = JSONObject(prefs.getString(key, "{}") ?: "{}")
    json.keys().asSequence().associateWith { k ->
        val arr = json.getJSONArray(k)
        (0 until arr.length()).map { arr.getString(it) }
    }
}.getOrDefault(emptyMap())
