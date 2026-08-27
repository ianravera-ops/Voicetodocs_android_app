package com.voicetodocs.cos.ui.home

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicetodocs.cos.AppContainer
import com.voicetodocs.cos.CosApplication
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CalendarItem
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.LocaleHelper
import com.voicetodocs.cos.data.MailThread
import com.voicetodocs.cos.data.SignedInUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class HomeUiState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val user: SignedInUser? = null,
    val events: List<CalendarItem> = emptyList(),
    val mail: List<MailThread> = emptyList(),
    val loading: Boolean = false,
    val speaking: Boolean = false,
    val error: String? = null,
    val status: String = ""
)

class HomeViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state
    private var tts: TextToSpeech? = null
    private var pendingSpeak: String? = null

    fun load(loader: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val lang = container.prefs.language()
                val user = container.prefs.user()
                _state.update { it.copy(language = lang, user = user) }
                loader()
            } catch (e: CosException) {
                _state.update { it.copy(loading = false, error = e.userMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: e.toString()) }
            }
        }
    }

    fun setLanguage(language: AppLanguage, loader: suspend () -> Unit) {
        viewModelScope.launch {
            container.prefs.setLanguage(language)
            _state.update { it.copy(language = language) }
            load(loader)
        }
    }

    suspend fun refreshData() {
        val lang = container.prefs.language()
        val ctx = LocaleHelper.wrap(getApplication(), lang)
        val events = container.gmailCalendar.upcomingEvents(lang, ctx.getString(R.string.all_day))
        val rawMail = container.gmailCalendar.importantThreads()
        val mail = container.gemini.plainLanguageEmails(rawMail, lang)
        _state.update {
            it.copy(
                language = lang,
                events = events,
                mail = mail,
                loading = false,
                error = null
            )
        }
    }

    fun hearBrief(loader: suspend () -> String) {
        viewModelScope.launch {
            _state.update { it.copy(speaking = true, error = null) }
            try {
                val text = loader()
                speak(text, _state.value.language)
            } catch (e: CosException) {
                _state.update { it.copy(speaking = false, error = e.userMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(speaking = false, error = e.message ?: e.toString()) }
            }
        }
    }

    private fun speak(text: String, language: AppLanguage) {
        pendingSpeak = text
        val app = getApplication<Application>()
        if (tts == null) {
            tts = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.forLanguageTag(language.bcp47)
                    pendingSpeak?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "today-brief") }
                    pendingSpeak = null
                    _state.update { it.copy(speaking = false) }
                } else {
                    _state.update { it.copy(speaking = false, error = text) }
                }
            }
        } else {
            tts?.language = Locale.forLanguageTag(language.bcp47)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "today-brief")
            _state.update { it.copy(speaking = false) }
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    companion object {
        fun factory(app: CosApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app, app.container) as T
        }
    }
}
