package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JournalEntry
import com.example.data.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.example.data.DriveBackupManager
import com.example.data.GeminiService
import com.example.data.GeminiContent
import com.example.data.GeminiPart
import com.example.data.ChatHistoryManager
import com.example.data.ChatSession
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JournalStats(
    val totalEntries: Int,
    val moodCounts: Map<String, Int>,
    val topTags: List<Pair<String, Int>>
)

class JournalViewModel(
    private val repository: JournalRepository,
    private val driveBackupManager: DriveBackupManager,
    private val geminiService: GeminiService,
    private val chatHistoryManager: ChatHistoryManager
) : ViewModel() {

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<GeminiContent>>(emptyList())
    val chatHistory: StateFlow<List<GeminiContent>> = _chatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMood = MutableStateFlow<String?>(null)
    val filterMood: StateFlow<String?> = _filterMood.asStateFlow()

    private val _googleAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val googleAccount: StateFlow<GoogleSignInAccount?> = _googleAccount.asStateFlow()

    init {
        loadChatSessions()
    }

    fun loadChatSessions() {
        _chatSessions.value = chatHistoryManager.getChatSessions().sortedByDescending { it.timestamp }
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _chatHistory.value = emptyList()
    }

    fun selectSession(sessionId: String) {
        val session = _chatSessions.value.find { it.id == sessionId }
        if (session != null) {
            _currentSessionId.value = sessionId
            _chatHistory.value = session.messages
        }
    }

    fun deleteSession(sessionId: String) {
        val updated = _chatSessions.value.filterNot { it.id == sessionId }
        _chatSessions.value = updated
        chatHistoryManager.saveChatSessions(updated)
        if (_currentSessionId.value == sessionId) {
            startNewChat()
        }
    }

    fun setGoogleAccount(account: GoogleSignInAccount?) {
        _googleAccount.value = account
    }

    val statsState: StateFlow<JournalStats> = repository.getAllEntriesStream().map { entries ->
        val totalEntries = entries.size
        val moodCounts = entries.groupingBy { it.mood }.eachCount()
        val tagCounts = entries.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
        val topTags = tagCounts.toList().sortedByDescending { it.second }.take(5)
        JournalStats(totalEntries, moodCounts, topTags)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JournalStats(0, emptyMap(), emptyList())
    )

    // Observe changes based on search and filters.
    // combine and flatMapLatest creates a reactive data stream that updates UI seamlessly.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<List<JournalEntry>> = combine(_searchQuery, _filterMood) { query, mood ->
        Pair(query, mood)
    }.flatMapLatest { (query, mood) ->
        when {
            query.isNotEmpty() -> repository.searchEntriesStream(query)
            mood != null -> repository.filterByMoodStream(mood)
            else -> repository.getAllEntriesStream()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _filterMood.value = null // clear filter if searching
    }

    fun updateFilterMood(mood: String?) {
        if (_filterMood.value == mood) {
            _filterMood.value = null // toggle off
        } else {
            _filterMood.value = mood
        }
        _searchQuery.value = "" // clear search if filtering
    }

    fun addEntry(text: String, mood: String, tags: String) {
        viewModelScope.launch {
            val entry = JournalEntry(text = text, mood = mood, tags = tags)
            repository.insertEntry(entry)
            _googleAccount.value?.let { account ->
                driveBackupManager.backupEntry(account, entry)
            }
        }
    }

    fun updateEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
            _googleAccount.value?.let { account ->
                driveBackupManager.backupEntry(account, entry)
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    suspend fun getEntry(id: String): JournalEntry? {
        return repository.getEntryById(id)
    }

    private fun formatJournalEntriesContext(entries: List<JournalEntry>): String {
        if (entries.isEmpty()) {
            return "No journal entries found."
        }
        return entries.take(50).joinToString("\n") { entry ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
            "- Date: $dateStr, Mood: ${entry.mood}, Tags: [${entry.tags}], Text: ${entry.text}"
        }
    }

    fun askAi(prompt: String, entries: List<JournalEntry>) {
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            val userContent = GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
            val currentHistory = _chatHistory.value + userContent
            _chatHistory.value = currentHistory
            _isAiLoading.value = true

            var sessionId = _currentSessionId.value
            val isNewSession = sessionId == null
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString()
                _currentSessionId.value = sessionId
            }

            val journalContext = formatJournalEntriesContext(entries)
            val systemInstructionText = """
                You are a friendly, empathetic, and insight-driven AI Journal Assistant.
                Below are the user's saved journal entries. Use them to answer their questions about patterns, trends, specific dates, or advice.
                
                User's Journal Entries:
                $journalContext
                
                CRITICAL RULE:
                You MUST ALWAYS provide extremely short, brief, and concise answers (1-2 sentences maximum, under 45 words) unless the user explicitly asks for more detail, a list, a detailed breakdown, or a longer explanation.
                If the user asks "tell me more" or requests details, you may then provide a longer, thorough, and structured answer.
                Keep your tone supportive, reflective, and encouraging. Never invent entries that do not exist.
            """.trimIndent()

            val aiResponseText = geminiService.generateResponse(currentHistory, systemInstructionText)
            
            val modelContent = GeminiContent(role = "model", parts = listOf(GeminiPart(text = aiResponseText)))
            val finalHistory = _chatHistory.value + modelContent
            _chatHistory.value = finalHistory
            _isAiLoading.value = false

            // Save the session
            val title = if (isNewSession) {
                if (prompt.length > 25) prompt.take(25) + "..." else prompt
            } else {
                _chatSessions.value.find { it.id == sessionId }?.title ?: (if (prompt.length > 25) prompt.take(25) + "..." else prompt)
            }

            val newSession = ChatSession(
                id = sessionId!!,
                title = title,
                timestamp = System.currentTimeMillis(),
                messages = finalHistory
            )

            val otherSessions = _chatSessions.value.filterNot { it.id == sessionId }
            val updatedSessions = listOf(newSession) + otherSessions
            _chatSessions.value = updatedSessions
            chatHistoryManager.saveChatSessions(updatedSessions)
        }
    }

    fun clearChat() {
        val sessionId = _currentSessionId.value
        if (sessionId != null) {
            deleteSession(sessionId)
        } else {
            _chatHistory.value = emptyList()
        }
    }

    companion object {
        fun provideFactory(
            repository: JournalRepository,
            driveBackupManager: DriveBackupManager,
            geminiService: GeminiService,
            chatHistoryManager: ChatHistoryManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                        return JournalViewModel(repository, driveBackupManager, geminiService, chatHistoryManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
