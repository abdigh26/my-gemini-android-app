package com.example

import android.app.Application
import com.example.data.DriveBackupManager
import com.example.data.GeminiService
import com.example.data.ChatHistoryManager
import com.example.data.JournalDatabase
import com.example.data.JournalRepository

class JournalApplication : Application() {
    // Lazy initialization acts as our AppContainer for Constructor Dependency Injection
    // instead of Hilt, ensuring cleaner builds without KSP mismatches.
    val database by lazy { JournalDatabase.getDatabase(this) }
    val repository by lazy { JournalRepository(database.journalEntryDao()) }
    val driveBackupManager by lazy { DriveBackupManager(this) }
    val geminiService by lazy { GeminiService() }
    val chatHistoryManager by lazy { ChatHistoryManager(this) }
}
