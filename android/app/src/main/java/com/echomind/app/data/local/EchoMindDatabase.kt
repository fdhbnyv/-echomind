package com.echomind.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class EchoMindDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: EchoMindDatabase? = null

        fun getInstance(context: Context): EchoMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoMindDatabase::class.java,
                    "echomind_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
