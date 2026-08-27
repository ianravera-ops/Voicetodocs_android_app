package com.voicetodocs.cos.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicetodocs.cos.AppContainer
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.MailThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DraftUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val thread: MailThread? = null,
    val draft: String = "",
    val error: String? = null,
    val sent: Boolean = false
)

class DraftViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(DraftUiState())
    val state: StateFlow<DraftUiState> = _state

    fun load(block: suspend () -> Pair<MailThread, String>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, sent = false) }
            try {
                val (thread, draft) = block()
                _state.update { it.copy(loading = false, thread = thread, draft = draft) }
            } catch (e: CosException) {
                _state.update { it.copy(loading = false, error = e.userMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: e.toString()) }
            }
        }
    }

    fun updateDraft(text: String) {
        _state.update { it.copy(draft = text) }
    }

    fun send(block: suspend (MailThread, String) -> Unit) {
        val thread = _state.value.thread ?: return
        val body = _state.value.draft
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            try {
                block(thread, body)
                _state.update { it.copy(sending = false, sent = true) }
            } catch (e: CosException) {
                _state.update { it.copy(sending = false, error = e.userMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, error = e.message ?: e.toString()) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DraftViewModel(container) as T
        }
    }
}
