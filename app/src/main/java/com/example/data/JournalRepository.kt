package com.example.data

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalEntryDao: JournalEntryDao) {
    fun getAllEntriesStream(): Flow<List<JournalEntry>> = journalEntryDao.getAllEntries()
    fun searchEntriesStream(query: String): Flow<List<JournalEntry>> = journalEntryDao.searchEntries(query)
    fun filterByMoodStream(mood: String): Flow<List<JournalEntry>> = journalEntryDao.filterByMood(mood)
    suspend fun getEntryById(id: String): JournalEntry? = journalEntryDao.getEntryById(id)

    suspend fun insertEntry(entry: JournalEntry) = journalEntryDao.insertEntry(entry)
    suspend fun updateEntry(entry: JournalEntry) = journalEntryDao.updateEntry(entry)
    suspend fun deleteEntry(entry: JournalEntry) = journalEntryDao.deleteEntry(entry)
}
