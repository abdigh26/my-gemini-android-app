package com.example.data

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long,
    val messages: List<GeminiContent>
)

class ChatHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("ai_chat_history_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, ChatSession::class.java)
    private val adapter = moshi.adapter<List<ChatSession>>(type)

    fun getChatSessions(): List<ChatSession> {
        val json = prefs.getString("chat_sessions", null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChatSessions(sessions: List<ChatSession>) {
        val json = adapter.toJson(sessions)
        prefs.edit().putString("chat_sessions", json).apply()
    }
}
