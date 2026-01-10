package com.virtualclone.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.common.UiEvent
import com.virtualclone.app.core.domain.model.*
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.core.domain.usecase.chat.*
import com.virtualclone.app.feature.onboarding.mapper.ModelUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_DOC_TOKENS = 300
private const val MODEL_MAX_TOKENS = 1024
private const val SAFE_MAX_TOKENS = 900 // buffer

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val saveChatMessage: SaveChatMessageUseCase,
    private val observeChatMessages: ObserveChatMessagesUseCase,
    private val getDocumentContext: GetDocumentContextUseCase,
    private val clearConversation: ClearConversationUseCase
) : ViewModel() {

    private val tag = "ChatViewModel"

    private var hasRehydrated = false
    private var initializedConversationId: String? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _inputEnabled = MutableStateFlow(false)
    val isTextInputEnabled: StateFlow<Boolean> = _inputEnabled.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()
    private val _draftMessage = MutableStateFlow("")
    val draftMessage: StateFlow<String> = _draftMessage

    init {
        initializeModel()
    }


    /* ---------------- INIT ---------------- */

    fun initialize(conversationId: String) {
        if (initializedConversationId == conversationId) return
        initializedConversationId = conversationId

        Logger.i("initialize: conversationId=$conversationId", tag)
        initializeConversation(conversationId)
    }

    private fun initializeModel() {
        viewModelScope.launch {
            when (val result = modelRepository.getActiveModel()) {
                is Result.Success -> {
                    val model = result.data
                    val uiModel = ModelUiMapper.toUi(model)

                    Logger.i("Active model=${model.id}", tag)

                    _uiState.value.setSupportsThinking(model.thinking)
                    _modelName.value = uiModel.displayName

                    modelRepository.initializeModel(model.id)

                    _inputEnabled.value = true
                    recomputeTokens("")
                }

                is Result.Error -> {
                    Logger.e("No active model", result.exception, tag)
                    _events.emit(UiEvent.ShowError("No active model"))
                }

                else -> Unit
            }
        }
    }

    private fun initializeConversation(conversationId: String) {
        hasRehydrated = false
        _uiState.value.setConversation(conversationId)

        viewModelScope.launch {
            observeChatMessages(conversationId).collectLatest { messages ->

                if (_uiState.value.hasActiveModelMessage()) {
                    Logger.v(
                        "Skipping rehydration: active model message id=${_uiState.value}",
                        tag
                    )
                    return@collectLatest
                }

                // ❌ Rehydrate only once
                if (hasRehydrated) {
                    Logger.v("Skipping rehydration: already done", tag)
                    return@collectLatest
                }

                Logger.i("Rehydrating ${messages.size} messages", tag)
                _uiState.value.clearMessagesOnly()
                messages.forEach {
                    _uiState.value.addPersistedMessage(it)
                }

                hasRehydrated = true
            }
        }
    }

    private fun updateSelectedDocument(documentId: String?) {
        if (documentId == null) {
            _uiState.value.setSelectedDocument(null, null, null)
            return
        }

        viewModelScope.launch {
            when (val result = getDocumentContext(documentId)) {
                is Result.Success -> {
                    val name = result.data.first?.name
                    val context = buildString {
                        var tokens = 0

                        for (chunk in result.data.second) {
                            val chunkTokens = chunk.text.length / 4
                            if (tokens + chunkTokens > MAX_DOC_TOKENS) break
                            append(chunk.text)
                            append("\n")
                            tokens += chunkTokens
                        }
                    }

                    Logger.i("updateSelectedDocument(): Selected document: id=$documentId name=$name context=$context", tag)

                    _uiState.value.setSelectedDocument(
                        documentId,
                        name,
                        context
                    )
                }
                else -> Unit
            }
        }
    }

    fun selectDocument(documentId: String) {
        Logger.i("selectDocument: $documentId", tag)
        updateSelectedDocument(documentId)
    }
    /* ---------------- INPUT ---------------- */

    private fun recomputeTokens(text: String) {
        val remaining =
            modelRepository.estimateTokensRemaining(
                text,
                _uiState.value.messages
            )
        Logger.v("Tokens remaining=$remaining", tag)
        _tokensRemaining.value = remaining
    }

    /* ---------------- SEND MESSAGE ---------------- */

    fun sendMessage(text: String) {
        if (_tokensRemaining.value == 0) {
            viewModelScope.launch {
                _events.emit(
                    UiEvent.ShowMessage(
                        "Context limit reached. Please reset the session."
                    )
                )
            }
            return
        }

        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) {
            Logger.e("sendMessage: blank conversationId", tag = tag)
            return
        }

        viewModelScope.launch {
            Logger.i("sendMessage: '$text'", tag)

            _inputEnabled.value = false

            // UI first
            _uiState.value.addUserMessage(text)
            _uiState.value.createModelMessage()

            // Persist user message
            saveChatMessage(
                conversationId,
                ChatMessage(author = USER_PREFIX, text = text)
            )

            val prompt = buildPrompt(text)
            Logger.i("Prompt length=${prompt.length}", tag)

            val estimatedPromptTokens = estimateTokens(prompt)
            if (estimatedPromptTokens >= SAFE_MAX_TOKENS) {
                Logger.w("Prompt too large: estimatedTokens=$estimatedPromptTokens", tag)

                _uiState.value.removeCurrentModelMessage()
                _inputEnabled.value = true

                _events.emit(
                    UiEvent.ShowMessage(
                        "Context limit reached. Please reset the session."
                    )
                )
                return@launch
            }

            val responseBuffer = StringBuilder()
            var receivedAnyChunk = false

            try {
                modelRepository.generateResponseAsync(prompt) { chunk, done ->

                    Logger.v(
                        "modelChunk: done=$done chunk='${chunk.take(40)}'",
                        tag
                    )

                    if (chunk.isNotEmpty()) {
                        receivedAnyChunk = true
                        responseBuffer.append(chunk)
                        _uiState.value.appendModelChunk(chunk)
                    }

                    if (done) {
                        Logger.i("Model finished response", tag)

                        viewModelScope.launch {

                            if (!receivedAnyChunk) {
                                Logger.w(
                                    "Model returned no output (likely context overflow)",
                                    tag
                                )
                                _uiState.value.removeCurrentModelMessage()
                                _events.emit(
                                    UiEvent.ShowMessage(
                                        "Context limit reached. Please reset the session."
                                    )
                                )
                            } else {
                                val finalText = responseBuffer
                                    .toString()
                                    .substringAfter(
                                        THINKING_MARKER_END,
                                        responseBuffer.toString()
                                    )
                                    .trim()

                                saveChatMessage(
                                    conversationId,
                                    ChatMessage(
                                        author = MODEL_PREFIX,
                                        text = finalText
                                    )
                                )
                                _uiState.value.finishStreaming()
                            }

                            _inputEnabled.value = true
                            recomputeTokens("")
                        }
                    }
                }
            } catch (e: IllegalStateException) {

                Logger.e("LLM native failure", e, tag)

                _uiState.value.removeCurrentModelMessage()
                _inputEnabled.value = true

                _events.emit(
                    UiEvent.ShowMessage(
                        "Context limit exceeded. Please reset the session."
                    )
                )
            }
        }
    }

    /* ---------------- PROMPT ---------------- */

    // do not change important things in here
    private fun buildPrompt(userMessage: String): String {

        Logger.i(
            "buildPrompt: userMessage='$userMessage' conversationId=${_uiState.value.conversationId}",
            tag
        )

        val sb = StringBuilder()

        // ─────────────────────────────────────────────
        // SYSTEM PROMPT (controls behavior)
        // ─────────────────────────────────────────────
        sb.append(
            """
        You are a helpful, intelligent AI assistant.
        Answer concisely and clearly.
        Keep responses short and simple unless the user explicitly asks for a detailed explanation.
        Do not mention internal reasoning or thinking steps.
        """.trimIndent()
        )
        sb.append("\n\n")

        // ─────────────────────────────────────────────
        // DOCUMENT CONTEXT (if selected)
        // ─────────────────────────────────────────────
        _uiState.value.documentContext?.let { context ->
            Logger.i("Context of document selected: $context", tag)
            sb.append("Context:\n")
            sb.append(context)
            sb.append("\n\n")
        }

        // ─────────────────────────────────────────────
        // CONVERSATION HISTORY
        // (reverse because UI list is reversed)
        // ─────────────────────────────────────────────
        _uiState.value.messages
            .asReversed()
            .takeLast(3) // keep last 6 messages only (safe default)
            .forEach { msg ->
                when (msg.author) {
                    USER_PREFIX ->
                        sb.append("User: ${msg.text}\n")
                    MODEL_PREFIX ->
                        sb.append("Assistant: ${msg.text}\n")
                }
            }

        // ─────────────────────────────────────────────
        // CURRENT USER MESSAGE
        // ─────────────────────────────────────────────
        sb.append("User: $userMessage\n")
        sb.append("Assistant:")

        return sb.toString()
    }

    /* ---------------- RESET ---------------- */

    fun resetSession() {
        val conversationId = _uiState.value.conversationId
        Logger.i("resetSession: $conversationId", tag)

        viewModelScope.launch {
            clearConversation(conversationId)
            modelRepository.resetSession()
            _uiState.value.clearMessagesOnly()
            clearSelectedDocument()
            recomputeTokens("")
        }
    }

    private fun estimateTokens(text: String): Int {
        return text.length / 4
    }

    fun clearSelectedDocument() {
        Logger.i("clearSelectedDocument()", tag)
        _uiState.value.setSelectedDocument(null, null, null)
        recomputeTokens(_draftMessage.value)
    }

    override fun onCleared() {
        Logger.i("onCleared(), documentname=${_uiState.value.selectedDocumentName}", tag)
        viewModelScope.launch { modelRepository.closeModel() }
        super.onCleared()
    }

    fun onDraftChanged(text: String) {
        _draftMessage.value = text
        recomputeTokens(text)
    }

    fun clearDraft() {
        _draftMessage.value = ""
    }
}