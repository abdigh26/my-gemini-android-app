package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JournalEntry::class], version = 2, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var Instance: JournalDatabase? = null

        fun getDatabase(context: Context): JournalDatabase {
            return Instance ?: synchronized(this) {
                // SECURITY NOTE (SQLCipher):
                // To encrypt the Room database for privacy, you would typically use SQLCipher.
                // 1. Add dependency: implementation("net.zetetic:android-database-sqlcipher:4.5.4")
                // 2. Add dependency: implementation("androidx.sqlite:sqlite-ktx:2.4.0")
                // 3. Initialize a SupportFactory with your secure passphrase:
                // val factory = SupportFactory("your_secure_passphrase".toByteArray())
                // 4. Pass the factory to the database builder:
                // Room.databaseBuilder(context, JournalDatabase::class.java, "journal_database")
                //     .openHelperFactory(factory)
                //     .build()
                Room.databaseBuilder(context, JournalDatabase::class.java, "journal_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
