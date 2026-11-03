package com.dienstplan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dienstplan.app.data.DienstplanDatenbank
import com.dienstplan.app.data.EinsatzortFarbeEntity
import com.dienstplan.app.data.EintragEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * MVVM ViewModel: Verwaltet den Zustand der App (Monatsauswahl, Einträge, Farben, NRW-Filter)
 * und führt Datenbank-Operationen im Hintergrund aus.
 */
class DienstplanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DienstplanDatenbank.datenbankHolen(application).dienstplanDao()

    // Aktuell ausgewählter Monat (Standard: aktueller Monat)
    private val _aktuellerMonat = MutableStateFlow(YearMonth.now())
    val aktuellerMonat: StateFlow<YearMonth> = _aktuellerMonat.asStateFlow()

    // Schalter für Feiertage und Schulferien in Nordrhein-Westfalen (NRW)
    private val _nrwDatenAktiv = MutableStateFlow(true)
    val nrwDatenAktiv: StateFlow<Boolean> = _nrwDatenAktiv.asStateFlow()

    // Laufend aktualisierte Liste aller Einträge aus der Room-Datenbank
    val alleEintraege: StateFlow<List<EintragEntity>> = dao.alleEintraegeLaden()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Laufend aktualisierte Liste aller Einsatzort-Farben
    val alleFarben: StateFlow<List<EinsatzortFarbeEntity>> = dao.alleFarbenLaden()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun monatWechseln(monateVorOderZurueck: Long) {
        _aktuellerMonat.value = _aktuellerMonat.value.plusMonths(monateVorOderZurueck)
    }

    fun nrwDatenUmschalten() {
        _nrwDatenAktiv.value = !_nrwDatenAktiv.value
    }

    fun eintragSpeichern(eintrag: EintragEntity) {
        viewModelScope.launch {
            dao.eintragSpeichern(eintrag)
        }
    }

    fun mehrereEintraegeSpeichern(eintraege: List<EintragEntity>) {
        viewModelScope.launch {
            dao.mehrereEintraegeSpeichern(eintraege)
        }
    }

    fun eintragLoeschen(eintrag: EintragEntity) {
        viewModelScope.launch {
            dao.eintragLoeschen(eintrag)
        }
    }

    fun farbeSpeichern(einsatzort: String, farbHex: Long) {
        if (einsatzort.isBlank()) return
        viewModelScope.launch {
            dao.farbeSpeichern(EinsatzortFarbeEntity(einsatzort.trim(), farbHex))
        }
    }

    fun farbeLoeschen(farbe: EinsatzortFarbeEntity) {
        viewModelScope.launch {
            dao.farbeLoeschen(farbe)
        }
    }
}
