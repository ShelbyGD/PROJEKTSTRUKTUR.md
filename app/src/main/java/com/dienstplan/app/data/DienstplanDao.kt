package com.dienstplan.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für alle Datenbank-Abfragen.
 */
@Dao
interface DienstplanDao {

    @Query("SELECT * FROM eintraege ORDER BY datum ASC, startZeit ASC")
    fun alleEintraegeLaden(): Flow<List<EintragEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun eintragSpeichern(eintrag: EintragEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mehrereEintraegeSpeichern(eintraege: List<EintragEntity>)

    @Delete
    suspend fun eintragLoeschen(eintrag: EintragEntity)

    @Query("SELECT * FROM einsatzort_farben ORDER BY einsatzort ASC")
    fun alleFarbenLaden(): Flow<List<EinsatzortFarbeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun farbeSpeichern(farbe: EinsatzortFarbeEntity)

    @Delete
    suspend fun farbeLoeschen(farbe: EinsatzortFarbeEntity)
}
