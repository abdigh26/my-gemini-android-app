package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val mood: String,
    val tags: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = timestamp
)
