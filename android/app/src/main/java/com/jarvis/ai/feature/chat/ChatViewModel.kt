package com.jarvis.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.core.language.DetectedLanguage
import com.jarvis.ai.core.language.LanguageDetector
import com.jarvis.ai.data.local.entity.MessageEntity
import com.jarvis.ai.data.repository.ChatRepository
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversationId: String? = null,
    val input: String = "",
    val thinking: Boolean = false,
    val error: String? = null,
    val detection: DetectedLanguage? = null
)

class ChatViewModel(
    private val repo: ChatRepository = Graph.chatRepo
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun onInput(text: String) {
        _state.value = _state.value.copy(input = text, detection = LanguageDetector.detect(text))
    }

    fun send(text: String, image: File? = null) {
        val body = text.trim()
        if (body.isBlank() && image == null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(thinking = true, error = null, input = "")
            try {
                val cid = _state.value.conversationId
                val res = when {
                    image != null -> repo.sendImage(cid, image, body.ifBlank { "Describe this image." })
                    cid == null -> repo.sendNew(body)
                    else -> repo.send(cid, body)
                }
                _state.value = _state.value.copy(
                    conversationId = res.conversationId,
                    thinking = false,
                    detection = LanguageDetector.detect(body)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(thinking = false, error = httpMessage(e))
            }
        }
    }

    fun bindMessages(): kotlinx.coroutines.flow.Flow<List<MessageEntity>> {
        val id = _state.value.conversationId
        return if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else repo.messages(id)
    }
}
