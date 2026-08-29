package com.voicetodocs.cos.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicetodocs.cos.data.VisibleFailure
import com.voicetodocs.cos.data.pipeline.MemoStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordUiState(
    val recording: Boolean = false,
    val busy: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val done: Boolean = false
)

class RecordViewModel : ViewModel() {
    private val _state = MutableStateFlow(RecordUiState())
    val state: StateFlow<RecordUiState> = _state

    fun markRecording(recording: Boolean, status: String) {
        _state.update { it.copy(recording = recording, status = status, error = null, done = false) }
    }

    fun markError(message: String) {
        _state.update { it.copy(recording = false, busy = false, error = message, status = "") }
    }

    fun process(
        statusFor: (MemoStep) -> String,
        run: suspend (onStep: suspend (MemoStep) -> Unit) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, recording = false, error = null, done = false) }
            try {
                run { step ->
                    _state.update { it.copy(status = statusFor(step), busy = step != MemoStep.DONE) }
                }
                _state.update { it.copy(busy = false, done = true, status = statusFor(MemoStep.DONE)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(busy = false, error = VisibleFailure.of(e).message, status = "")
                }
            }
        }
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RecordViewModel() as T
        }
    }
}
