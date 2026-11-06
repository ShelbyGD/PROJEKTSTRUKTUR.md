package com.dienstplan.app.util

import android.content.Context
import kotlin.math.max
import kotlin.math.min

data class KrankenkasseInfo(
    val name: String,
    val zusatzbeitragProzent: Double
)

data class NettoEinstellungen(
    val stundenlohn: Double = 15.0,
    val steuerklasse: Int = 1,
    val krankenkasseName: String = "TK (Techniker)",
    val kvZusatzbeitrag: Double = 2.45,
    val kinderAnzahl: Int = 0,
    val kirchensteuer: Boolean = false,
    val istMinijob: Boolean = false
)

data class LohnabrechnungErgebnis(
    val bruttoLohn: Double,
    val lohnsteuer: Double,
    val kirchensteuerBetrag: Double,
    val krankenversicherung: Double,
    val pflegeversicherung: Double,
    val rentenversicherung: Double,
    val arbeitslosenversicherung: Double,
    val sozialabgabenGesamt: Double,
    val nettoLohn: Double,
    val fahrtkostenSteuerfrei: Double,
    val auszahlungGesamt: Double
)

object NettoRechnerHelfer {

    private const val PREFS_NAME = "dienstplan_extras"

    val krankenkassenListe = listOf(
        KrankenkasseInfo("TK (Techniker)", 2.45),
        KrankenkasseInfo("Barmer", 3.29),
        KrankenkasseInfo("DAK-Gesundheit", 2.80),
        KrankenkasseInfo("AOK NordWest (NRW)", 2.79),
        KrankenkasseInfo("IKK classic", 2.60),
        KrankenkasseInfo("hkk Krankenkasse", 2.19),
        KrankenkasseInfo("BKK Firmus", 1.84),
        KrankenkasseInfo("Audi BKK", 2.40),
        KrankenkasseInfo("Knappschaft", 3.25)
    )

    fun ladeEinstellungen(context: Context): NettoEinstellungen {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NettoEinstellungen(
            stundenlohn = prefs.getFloat("global_stundenlohn", 15.0f).toDouble(),
            steuerklasse = prefs.getInt("netto_steuerklasse", 1),
            krankenkasseName = prefs.getString("netto_kk_name", "TK (Techniker)") ?: "TK (Techniker)",
            kvZusatzbeitrag = prefs.getFloat("netto_kv_zusatz", 2.45f).toDouble(),
            kinderAnzahl = prefs.getInt("netto_kinder", 0),
            kirchensteuer = prefs.getBoolean("netto_kirche", false),
            istMinijob = prefs.getBoolean("netto_minijob", false)
        )
    }

    fun speichereEinstellungen(context: Context, einstellungen: NettoEinstellungen) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat("global_stundenlohn", einstellungen.stundenlohn.toFloat())
            .putInt("netto_steuerklasse", einstellungen.steuerklasse)
            .putString("netto_kk_name", einstellungen.krankenkasseName)
            .putFloat("netto_kv_zusatz", einstellungen.kvZusatzbeitrag.toFloat())
            .putInt("netto_kinder", einstellungen.kinderAnzahl)
            .putBoolean("netto_kirche", einstellungen.kirchensteuer)
            .putBoolean("netto_minijob", einstellungen.istMinijob)
            .apply()
    }
// --- START TEIL 2 (NettoRechnerHelfer.kt) ---
    /**
     * Berechnet Lohnsteuer, Sozialversicherung (KV, PV, RV, AV) und Netto für 2026 (NRW).
     */
    fun berechneMonatsNetto(
        bruttoMonat: Double,
        einstellungen: NettoEinstellungen,
        fahrtkosten: Double = 0.0
    ): LohnabrechnungErgebnis {
        if (bruttoMonat <= 0.0) {
            return LohnabrechnungErgebnis(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, fahrtkosten, fahrtkosten)
        }

        if (einstellungen.istMinijob) {
            return LohnabrechnungErgebnis(
                bruttoLohn = bruttoMonat,
                lohnsteuer = 0.0,
                kirchensteuerBetrag = 0.0,
                krankenversicherung = 0.0,
                pflegeversicherung = 0.0,
                rentenversicherung = 0.0,
                arbeitslosenversicherung = 0.0,
                sozialabgabenGesamt = 0.0,
                nettoLohn = bruttoMonat,
                fahrtkostenSteuerfrei = fahrtkosten,
                auszahlungGesamt = bruttoMonat + fahrtkosten
            )
        }

        // Arbeitnehmer-Anteile Sozialversicherung 2026
        val kvSatzAn = (7.3 + (einstellungen.kvZusatzbeitrag / 2.0)) / 100.0
        val rvSatzAn = 0.093
        val avSatzAn = 0.013

        // Pflegeversicherung 2026 inkl. Kinderstaffelung
        val pvSatzAn = when {
            einstellungen.kinderAnzahl <= 0 -> 0.024 // 1,8 % + 0,6 % Kinderlosenzuschlag
            einstellungen.kinderAnzahl == 1 -> 0.018
            else -> {
                val abschlag = min(einstellungen.kinderAnzahl - 1, 4) * 0.0025
                max(0.008, 0.018 - abschlag)
            }
        }

        val kvBetrag = bruttoMonat * kvSatzAn
        val pvBetrag = bruttoMonat * pvSatzAn
        val rvBetrag = bruttoMonat * rvSatzAn
        val avBetrag = bruttoMonat * avSatzAn
        val sozialGesamt = kvBetrag + pvBetrag + rvBetrag + avBetrag

        // Lohnsteuer-Berechnung 2026 nach Jahreshochschätzung & Steuerklasse
        val jahresBrutto = bruttoMonat * 12.0
        val jahresSozial = sozialGesamt * 12.0
        val werbungskosten = 1230.0
        val sonderausgaben = 36.0

        val grundfreibetrag2026 = when (einstellungen.steuerklasse) {
            1, 4 -> 12348.0
            2 -> 12348.0 + 4260.0 // inkl. Entlastungsbetrag Alleinerziehende
            3 -> 24696.0
            5, 6 -> 0.0
            else -> 12348.0
        }

        val zuVersteuernJahr = max(0.0, jahresBrutto - jahresSozial - werbungskosten - sonderausgaben - grundfreibetrag2026)

        val jahresSteuer = when (einstellungen.steuerklasse) {
            3 -> berechneProgressionsSteuer(zuVersteuernJahr / 2.0) * 2.0
            5 -> max(0.0, (jahresBrutto - jahresSozial) * 0.22)
            6 -> max(0.0, (jahresBrutto - jahresSozial) * 0.25)
            else -> berechneProgressionsSteuer(zuVersteuernJahr)
        }

        val monatsLohnsteuer = max(0.0, jahresSteuer / 12.0)
        val kirchensteuerBetrag = if (einstellungen.kirchensteuer) monatsLohnsteuer * 0.09 else 0.0
        val netto = max(0.0, bruttoMonat - sozialGesamt - monatsLohnsteuer - kirchensteuerBetrag)

        return LohnabrechnungErgebnis(
            bruttoLohn = bruttoMonat,
            lohnsteuer = monatsLohnsteuer,
            kirchensteuerBetrag = kirchensteuerBetrag,
            krankenversicherung = kvBetrag,
            pflegeversicherung = pvBetrag,
            rentenversicherung = rvBetrag,
            arbeitslosenversicherung = avBetrag,
            sozialabgabenGesamt = sozialGesamt,
            nettoLohn = netto,
            fahrtkostenSteuerfrei = fahrtkosten,
            auszahlungGesamt = netto + fahrtkosten
        )
    }

    private fun berechneProgressionsSteuer(betragUeberFreibetrag: Double): Double {
        if (betragUeberFreibetrag <= 0.0) return 0.0
        val stufe1 = min(betragUeberFreibetrag, 5500.0) * 0.16
        val stufe2 = max(0.0, min(betragUeberFreibetrag - 5500.0, 48000.0)) * 0.24
        val stufe3 = max(0.0, betragUeberFreibetrag - 53500.0) * 0.42
        return stufe1 + stufe2 + stufe3
    }
}
