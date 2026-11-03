package com.dienstplan.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Lokale Room-SQLite-Datenbank der App.
 */
@Database(
    entities = [EintragEntity::class, EinsatzortFarbeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DienstplanDatenbank : RoomDatabase() {

    abstract fun dienstplanDao(): DienstplanDao

    companion object {
        const val DATENBANK_NAME = "dienstplan_db"

        @Volatile
        private var INSTANZ: DienstplanDatenbank? = null

        fun datenbankHolen(context: Context): DienstplanDatenbank {
            return INSTANZ ?: synchronized(this) {
                val instanz = Room.databaseBuilder(
                    context.applicationContext,
                    DienstplanDatenbank::class.java,
                    DATENBANK_NAME
                ).build()
                INSTANZ = instanz
                instanz
            }
        }
    }
}
