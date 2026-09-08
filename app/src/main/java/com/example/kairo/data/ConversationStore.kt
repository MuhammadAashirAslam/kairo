package com.example.kairo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class CitationSource(
    val chunkIndex: Int,
    val text: String,
    val score: Float,
    val docName: String = "",
    val breadcrumb: String = "",
    val matchType: String = "Hybrid"
)

data class GenerationMetrics(
    val tokensPerSecond: Float,
    val inputTokens: Int,
    val outputTokens: Int,
    val durationMs: Long = 0L
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<CitationSource> = emptyList(),
    val metrics: GenerationMetrics? = null,
    val isGenerating: Boolean = false,
    val imageUri: String? = null
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isPinned: Boolean = false,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    var attachedDocName: String? = null
)

class ConversationStore(private val context: Context? = null) {

    private val sessions = mutableListOf<ChatSession>()
    private var activeSessionId: String = ""

    private val storageFile: File? by lazy {
        context?.let { File(it.filesDir, "chat_sessions.json") }
    }

    init {
        loadFromDisk()
        if (sessions.isEmpty()) {
            val initial = ChatSession(title = "New Chat")
            sessions.add(initial)
            activeSessionId = initial.id
        } else {
            activeSessionId = sessions.first().id
        }
    }

    @Synchronized
    fun getActiveSession(): ChatSession {
        var session = sessions.find { it.id == activeSessionId }
        if (session == null) {
            session = ChatSession(title = "New Chat")
            sessions.add(0, session)
            activeSessionId = session.id
        }
        return session
    }

    @Synchronized
    fun getSessions(): List<ChatSession> = sessions.toList()

    @Synchronized
    fun createSession(title: String = "New Chat", docName: String? = null): ChatSession {
        val newSession = ChatSession(
            title = title,
            attachedDocName = docName
        )
        sessions.add(0, newSession)
        activeSessionId = newSession.id
        saveToDisk()
        return newSession
    }

    @Synchronized
    fun switchSession(sessionId: String): ChatSession? {
        val session = sessions.find { it.id == sessionId }
        if (session != null) {
            activeSessionId = session.id
        }
        return session
    }

    @Synchronized
    fun deleteSession(sessionId: String) {
        sessions.removeAll { it.id == sessionId }
        if (sessions.isEmpty()) {
            val fresh = ChatSession(title = "New Chat")
            sessions.add(fresh)
            activeSessionId = fresh.id
        } else if (activeSessionId == sessionId) {
            activeSessionId = sessions.first().id
        }
        saveToDisk()
    }

    @Synchronized
    fun renameSession(sessionId: String, newTitle: String) {
        sessions.find { it.id == sessionId }?.let {
            it.title = newTitle.trim()
            it.updatedAt = System.currentTimeMillis()
            saveToDisk()
        }
    }

    @Synchronized
    fun togglePinSession(sessionId: String) {
        sessions.find { it.id == sessionId }?.let {
            it.isPinned = !it.isPinned
            saveToDisk()
        }
    }

    @Synchronized
    fun autoTitleSession(firstUserPrompt: String) {
        val session = getActiveSession()
        if (session.title == "New Chat" && firstUserPrompt.isNotBlank()) {
            val words = firstUserPrompt.trim().split("\\s+".toRegex())
            val candidateTitle = words.take(5).joinToString(" ")
            session.title = if (candidateTitle.length > 30) {
                candidateTitle.take(30).trimEnd() + "..."
            } else {
                candidateTitle
            }
            session.updatedAt = System.currentTimeMillis()
            saveToDisk()
        }
    }

    // --- Active Session Message Operations ---

    @Synchronized
    fun getMessages(): List<ChatMessage> = getActiveSession().messages.toList()

    @Synchronized
    fun addMessage(message: ChatMessage) {
        val session = getActiveSession()
        session.messages.add(message)
        session.updatedAt = System.currentTimeMillis()
        saveToDisk()
    }

    @Synchronized
    fun updateLastAssistantMessage(
        content: String,
        isGenerating: Boolean,
        sources: List<CitationSource> = emptyList(),
        metrics: GenerationMetrics? = null
    ) {
        val session = getActiveSession()
        val lastIdx = session.messages.indexOfLast { it.role == MessageRole.ASSISTANT }
        if (lastIdx != -1) {
            val existing = session.messages[lastIdx]
            session.messages[lastIdx] = existing.copy(
                content = content,
                isGenerating = isGenerating,
                sources = if (sources.isNotEmpty()) sources else existing.sources,
                metrics = metrics ?: existing.metrics
            )
            session.updatedAt = System.currentTimeMillis()
            if (!isGenerating) {
                saveToDisk()
            }
        }
    }

    @Synchronized
    fun clearCurrentSession() {
        val session = getActiveSession()
        session.messages.clear()
        session.updatedAt = System.currentTimeMillis()
        saveToDisk()
    }

    @Synchronized
    fun clear() = clearCurrentSession()

    // --- JSON Persistence ---

    @Synchronized
    fun saveToDisk() {
        val file = storageFile ?: return
        try {
            val rootArray = JSONArray()
            sessions.forEach { s ->
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("title", s.title)
                sObj.put("createdAt", s.createdAt)
                sObj.put("updatedAt", s.updatedAt)
                sObj.put("isPinned", s.isPinned)
                sObj.put("attachedDocName", s.attachedDocName ?: JSONObject.NULL)

                val mArray = JSONArray()
                s.messages.forEach { m ->
                    val mObj = JSONObject()
                    mObj.put("id", m.id)
                    mObj.put("role", m.role.name)
                    mObj.put("content", m.content)
                    mObj.put("timestamp", m.timestamp)
                    mObj.put("imageUri", m.imageUri ?: JSONObject.NULL)

                    if (m.metrics != null) {
                        val metObj = JSONObject()
                        metObj.put("tokensPerSecond", m.metrics.tokensPerSecond.toDouble())
                        metObj.put("inputTokens", m.metrics.inputTokens)
                        metObj.put("outputTokens", m.metrics.outputTokens)
                        metObj.put("durationMs", m.metrics.durationMs)
                        mObj.put("metrics", metObj)
                    }

                    if (m.sources.isNotEmpty()) {
                        val srcArray = JSONArray()
                        m.sources.forEach { src ->
                            val srcObj = JSONObject()
                            srcObj.put("chunkIndex", src.chunkIndex)
                            srcObj.put("text", src.text)
                            srcObj.put("score", src.score.toDouble())
                            srcObj.put("docName", src.docName)
                            srcObj.put("breadcrumb", src.breadcrumb)
                            srcObj.put("matchType", src.matchType)
                            srcArray.put(srcObj)
                        }
                        mObj.put("sources", srcArray)
                    }
                    mArray.put(mObj)
                }
                sObj.put("messages", mArray)
                rootArray.put(sObj)
            }
            file.writeText(rootArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val jsonText = file.readText()
            if (jsonText.isBlank()) return
            val rootArray = JSONArray(jsonText)
            sessions.clear()

            for (i in 0 until rootArray.length()) {
                val sObj = rootArray.getJSONObject(i)
                val session = ChatSession(
                    id = sObj.optString("id", UUID.randomUUID().toString()),
                    title = sObj.optString("title", "Chat"),
                    createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                    isPinned = sObj.optBoolean("isPinned", false),
                    attachedDocName = if (sObj.isNull("attachedDocName")) null else sObj.optString("attachedDocName")
                )

                val mArray = sObj.optJSONArray("messages")
                if (mArray != null) {
                    for (j in 0 until mArray.length()) {
                        val mObj = mArray.getJSONObject(j)
                        val roleStr = mObj.optString("role", "USER")
                        val role = try { MessageRole.valueOf(roleStr) } catch (_: Exception) { MessageRole.USER }

                        var metrics: GenerationMetrics? = null
                        val metObj = mObj.optJSONObject("metrics")
                        if (metObj != null) {
                            metrics = GenerationMetrics(
                                tokensPerSecond = metObj.optDouble("tokensPerSecond", 0.0).toFloat(),
                                inputTokens = metObj.optInt("inputTokens", 0),
                                outputTokens = metObj.optInt("outputTokens", 0),
                                durationMs = metObj.optLong("durationMs", 0L)
                            )
                        }

                        val sourcesList = mutableListOf<CitationSource>()
                        val srcArray = mObj.optJSONArray("sources")
                        if (srcArray != null) {
                            for (k in 0 until srcArray.length()) {
                                val srcObj = srcArray.getJSONObject(k)
                                sourcesList.add(
                                    CitationSource(
                                        chunkIndex = srcObj.optInt("chunkIndex", 0),
                                        text = srcObj.optString("text", ""),
                                        score = srcObj.optDouble("score", 0.0).toFloat(),
                                        docName = srcObj.optString("docName", ""),
                                        breadcrumb = srcObj.optString("breadcrumb", ""),
                                        matchType = srcObj.optString("matchType", "Hybrid")
                                    )
                                )
                            }
                        }

                        session.messages.add(
                            ChatMessage(
                                id = mObj.optString("id", UUID.randomUUID().toString()),
                                role = role,
                                content = mObj.optString("content", ""),
                                timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                                sources = sourcesList,
                                metrics = metrics,
                                isGenerating = false,
                                imageUri = if (mObj.isNull("imageUri")) null else mObj.optString("imageUri")
                            )
                        )
                    }
                }
                sessions.add(session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
