package com.chambita.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chambita.app.data.local.dao.LocalAddressDao
import com.chambita.app.data.local.dao.UserSessionDao
import com.chambita.app.data.local.entities.LocalAddressEntity
import com.chambita.app.data.local.entities.UserSessionEntity

@Database(entities = [UserSessionEntity::class, LocalAddressEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userSessionDao(): UserSessionDao
    abstract fun localAddressDao(): LocalAddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chambita_db"
                )
                .fallbackToDestructiveMigration() // ESTO EVITA EL CRASH POR VERSIÓN
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


