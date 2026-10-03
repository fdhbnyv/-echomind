package com.echomind.app.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.echomind.app.data.memory.MemoryEntity

@Database(
    entities = [NoteEntity::class, MemoryEntity::class, CloudDeleteEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class EchoMindDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun memoryDao(): com.echomind.app.data.memory.MemoryDao
    abstract fun cloudDeleteDao(): CloudDeleteDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

        /**
         * v3：为 notes / memories 增加云端同步元数据（uuid 主键、更新时间、
         * 脏标记），并新增删除墓碑表。存量行补齐 uuid（32 位随机 hex，与
         * 客户端 UUID.randomUUID().replace("-","") 同格式）和 updatedAt。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN cloudSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE notes SET uuid = lower(hex(randomblob(16))) WHERE uuid = ''")
                database.execSQL("UPDATE notes SET updatedAt = createdAt WHERE updatedAt = 0")

                database.execSQL("ALTER TABLE memories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE memories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE memories ADD COLUMN cloudSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE memories SET uuid = lower(hex(randomblob(16))) WHERE uuid = ''")
                database.execSQL("UPDATE memories SET updatedAt = createdAt WHERE updatedAt = 0")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cloud_deletes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        uuid TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
