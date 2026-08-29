package com.voicetodocs.cos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CalendarItem
import com.voicetodocs.cos.data.DayIndex
import com.voicetodocs.cos.data.MailThread
import com.voicetodocs.cos.data.RecordingNote
import com.voicetodocs.cos.data.SignedInUser
import com.voicetodocs.cos.data.VisibleFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val user: SignedInUser? = null,
    val yesterdayNotes: List<RecordingNote> = emptyList(),
    val todayNotes: List<RecordingNote> = emptyList(),
    val openNotes: List<RecordingNote> = emptyList(),
    val mail: List<MailThread> = emptyList(),
    val firstToday: CalendarItem? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun load(run: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                run()
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = VisibleFailure.of(e).message)
                }
            }
        }
    }

    fun showSession(user: SignedInUser?, language: AppLanguage) {
        _state.update { it.copy(user = user, language = language) }
    }

    fun showDay(
        notes: List<RecordingNote>,
        mail: List<MailThread>,
        firstToday: CalendarItem?,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        _state.update {
            it.copy(
                yesterdayNotes = DayIndex.onDate(notes, today.minusDays(1), zone),
                todayNotes = DayIndex.onDate(notes, today, zone),
                openNotes = DayIndex.openItems(notes),
                mail = mail,
                firstToday = firstToday
            )
        }
    }

    fun setLanguage(language: AppLanguage) {
        _state.update { it.copy(language = language) }
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel() as T
        }
    }
}
