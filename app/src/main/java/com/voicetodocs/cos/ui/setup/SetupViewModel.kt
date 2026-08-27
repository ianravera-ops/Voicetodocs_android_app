package com.voicetodocs.cos.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicetodocs.cos.AppContainer
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.SignedInUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val user: SignedInUser? = null,
    val busy: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val ready: Boolean = false
)

class SetupViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    init {
        viewModelScope.launch {
            val lang = container.prefs.language()
            val user = container.prefs.user()
            val ready = container.prefs.isSetupComplete() && container.prefs.driveStructure() != null
            _state.update { it.copy(language = lang, user = user, ready = ready) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            container.prefs.setLanguage(language)
            _state.update { it.copy(language = language) }
        }
    }

    fun onSignedIn(user: SignedInUser) {
        viewModelScope.launch {
            container.prefs.setUser(user)
            _state.update { it.copy(user = user, error = null) }
        }
    }

    fun bootstrapDrive(statusCreating: String, statusDone: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, status = statusCreating) }
            try {
                val existing = container.prefs.driveStructure()
                val structure = container.drive.ensureStructure(existing)
                container.prefs.saveDriveStructure(structure)
                container.prefs.setSetupComplete(true)
                _state.update { it.copy(busy = false, ready = true, status = statusDone) }
            } catch (e: CosException) {
                _state.update { it.copy(busy = false, error = e.userMessage, status = "") }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: e.toString(), status = "") }
            }
        }
    }

    fun setBusy(busy: Boolean, status: String = "", error: String? = null) {
        _state.update { it.copy(busy = busy, status = status, error = error) }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SetupViewModel(container) as T
        }
    }
}
