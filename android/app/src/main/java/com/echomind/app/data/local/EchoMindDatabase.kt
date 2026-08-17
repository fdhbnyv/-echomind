package com.echomind.app.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.echomind.app.data.memory.MemoryEntity

@Database(entities = [NoteEntity::class, MemoryEntity::class], version = 2, exportSchema = false)
abstract class EchoMindDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun memoryDao(): com.echomind.app.data.memory.MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: EchoMindDatabase? = null

        fun getInstance(context: Context): EchoMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoMindDatabase::class.java,
                    "echomind_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS memories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL,
                        type TEXT NOT NULL,
                        tags TEXT NOT NULL DEFAULT '[]',
                        importance INTEGER NOT NULL DEFAULT 3,
                        source TEXT NOT NULL DEFAULT 'manual',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER NOT NULL,
                        accessCount INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
