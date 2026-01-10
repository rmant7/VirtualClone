package com.virtualclone.app.feature.chat.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.toMutableStateList
import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.domain.model.*

class ChatUiState(
    conversationId: String = "",
    supportsThinking: Boolean = false,
    documentContext: String? = null,
    messages: List<ChatMessage> = emptyList()
) {

    private val tag = "ChatUiState"

    private val _messages = messages.toMutableStateList()
    val messages: List<ChatMessage> = _messages.asReversed()

    private var currentModelMessageId: String? = null

    var supportsThinking: Boolean = supportsThinking
        private set

    var conversationId: String = conversationId
        private set

    var selectedDocumentId by mutableStateOf<String?>(null)
        private set

    var selectedDocumentName by mutableStateOf<String?>(null)
        private set
    var documentContext by mutableStateOf(documentContext)
        private set

    /* ---------------- Config ---------------- */

    fun setSupportsThinking(value: Boolean) {
        supportsThinking = value
        Logger.i("supportsThinking=$value", tag)
    }

    /* ---------------- Conversation ---------------- */

    fun setConversation(id: String) {
        if (conversationId == id) return
        Logger.i("setConversation: $id", tag)
        conversationId = id
        clearMessagesOnly()
    }

    fun clearMessagesOnly() {
        Logger.w(
            "clearMessagesOnly() called — currentModelMessageId=$currentModelMessageId",
            tag
        )
        _messages.clear()
        currentModelMessageId = null
    }

    fun setSelectedDocument(
        id: String?,
        name: String?,
        context: String? = null
    ) {
        Logger.i("setSelectedDocument: id=$id name=$name", tag)
        selectedDocumentId = id
        selectedDocumentName = name
        documentContext = context
    }

    fun hasActiveModelMessage(): Boolean {
        return currentModelMessageId != null
    }

    /* ---------------- Messages ---------------- */

    fun addUserMessage(text: String) {
        Logger.i("addUserMessage: '$text'", tag)
        _messages.add(
            ChatMessage(
                author = USER_PREFIX,
                text = text,
                phase = ModelPhase.DONE
            )
        )
    }

    fun createModelMessage() {
        val initialPhase =
            if (supportsThinking) ModelPhase.THINKING else ModelPhase.STREAMING

        val msg = ChatMessage(
            author = MODEL_PREFIX,
            phase = initialPhase,
            isLoading = true,
            text = ""
        )

        _messages.add(msg)
        currentModelMessageId = msg.id

        Logger.i(
            "createModelMessage: id=${msg.id}, phase=$initialPhase, loading=true",
            tag
        )
    }

    /**
     * CORE RULES:
     * - Model message is CREATED immediately
     * - isLoading=true until FIRST visible token
     * - </think> is NEVER rendered
     */
    fun appendModelChunk(chunk: String) {
        val id = currentModelMessageId ?: return
        val index = _messages.indexOfFirst { it.id == id }
        if (index == -1) return

        val current = _messages[index]

        // ─────────────────────────────────────────────
        // THINKING PHASE — SHOW IT
        // ─────────────────────────────────────────────
        if (current.phase == ModelPhase.THINKING) {

            if (chunk.contains(THINKING_MARKER_END)) {
                Logger.i("THINKING → STREAMING transition", tag)

                val afterThink = chunk
                    .substringAfter(THINKING_MARKER_END)
                    .trimStart()              // 🔥 IMPORTANT

                _messages[index] = current.copy(
                    text = afterThink,
                    phase = ModelPhase.STREAMING,
                    isLoading = afterThink.isBlank()
                )
                return
            }

            // Append thinking text ONLY if visible
            if (chunk.isVisibleText()) {
                _messages[index] = current.copy(
                    text = current.text + chunk,
                    isLoading = false
                )
            }
            return
        }

        // ─────────────────────────────────────────────
        // STREAMING PHASE — CLEAN OUTPUT
        // ─────────────────────────────────────────────
        if (!chunk.isVisibleText()) return

        _messages[index] = current.copy(
            text = current.text + chunk,
            isLoading = false
        )
    }

    fun finishStreaming() {
        val id = currentModelMessageId ?: return
        val index = _messages.indexOfFirst { it.id == id }
        if (index == -1) return

        Logger.i("finishStreaming: id=$id", tag)

        val msg = _messages[index]
        _messages[index] = msg.copy(
            phase = ModelPhase.DONE,
            isLoading = false
        )

        currentModelMessageId = null
    }

    fun removeCurrentModelMessage() {
        val id = currentModelMessageId ?: return
        Logger.w("removeCurrentModelMessage: id=$id", tag)
        _messages.removeAll { it.id == id }
        currentModelMessageId = null
    }

    fun addPersistedMessage(message: ChatMessage) {
        Logger.i("addPersistedMessage: id=${message.id}", tag)
        _messages.add(message)
    }

    private fun String.isVisibleText(): Boolean {
        return this.any { !it.isWhitespace() }
    }
}
