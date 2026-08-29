package com.voicetodocs.cos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CalendarItem
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.DayIndex
import com.voicetodocs.cos.data.MailThread
import com.voicetodocs.cos.data.RecordingNote
import com.voicetodocs.cos.data.VisibleFailure
import com.voicetodocs.cos.data.digest.VipDigestRunner
import com.voicetodocs.cos.data.digest.VipDigestScheduler
import com.voicetodocs.cos.data.digest.VipNotifier
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosCard
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosSecondaryButton
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind
import com.voicetodocs.cos.ui.theme.Ink
import com.voicetodocs.cos.ui.theme.OnTeal
import com.voicetodocs.cos.ui.theme.Teal
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    session: CosSession,
    onRecord: () -> Unit,
    onPeople: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun loadDay() {
        homeViewModel.load {
            val lang = session.containerRef.prefs.language()
            val user = session.containerRef.prefs.user()
            homeViewModel.showSession(user, lang)
            val notes = session.containerRef.prefs.recordings()
            val errors = mutableListOf<String>()
            var mail = emptyList<MailThread>()
            var first: CalendarItem? = null
            session.withAccess {
                try {
                    val raw = session.containerRef.gmailCalendar.importantThreads()
                    mail = session.containerRef.gemini.plainLanguageEmails(raw, lang)
                } catch (e: Exception) {
                    errors += VisibleFailure.of(e).message
                }
                try {
                    val events = session.containerRef.gmailCalendar.upcomingEvents(
                        lang,
                        context.getString(R.string.all_day)
                    )
                    first = DayIndex.firstEventToday(events, LocalDate.now())
                } catch (e: Exception) {
                    errors += VisibleFailure.of(e).message
                }
            }
            homeViewModel.showDay(notes, mail, first)
            homeViewModel.showDigest(
                session.containerRef.prefs.digestItems(),
                session.containerRef.prefs.digestError()
            )
            if (errors.isNotEmpty()) {
                throw CosException(errors.joinToString("\n"))
            }
        }
    }

    fun retryDigest() {
        homeViewModel.load {
            try {
                session.withAccess {
                    VipDigestRunner(session.containerRef, VipNotifier(context)).run(notify = true)
                }
            } finally {
                homeViewModel.showDigest(
                    session.containerRef.prefs.digestItems(),
                    session.containerRef.prefs.digestError()
                )
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            loadDay()
        }
    }

    CosScreen {
        CosTitle(stringResource(R.string.home_title))
        state.user?.let {
            CosBody(stringResource(R.string.signed_in_as, it.displayName.ifBlank { it.email }))
        }

        CosBody(stringResource(R.string.language_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LangChip(
                selected = state.language == AppLanguage.ENGLISH,
                label = stringResource(R.string.language_en),
                onClick = {
                    homeViewModel.setLanguage(AppLanguage.ENGLISH)
                    scope.launch {
                        session.containerRef.prefs.setLanguage(AppLanguage.ENGLISH)
                        loadDay()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            LangChip(
                selected = state.language == AppLanguage.SPANISH,
                label = stringResource(R.string.language_es),
                onClick = {
                    homeViewModel.setLanguage(AppLanguage.SPANISH)
                    scope.launch {
                        session.containerRef.prefs.setLanguage(AppLanguage.SPANISH)
                        loadDay()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        CosPrimaryButton(stringResource(R.string.record_note), onClick = onRecord)
        CosSecondaryButton(stringResource(R.string.vip_choose_people), onClick = onPeople)

        if (state.loading) {
            CosStatusBanner(stringResource(R.string.loading), StatusKind.BUSY)
        }
        state.error?.let {
            CosStatusBanner(it, StatusKind.ERROR)
            CosSecondaryButton(stringResource(R.string.try_again), onClick = { loadDay() })
        }

        CosCard {
            Text(stringResource(R.string.home_yesterday), fontSize = 18.sp, color = Teal)
            if (state.yesterdayNotes.isEmpty()) {
                CosBody(stringResource(R.string.home_no_yesterday_notes))
            } else {
                state.yesterdayNotes.forEach { note ->
                    NoteRow(note)
                }
            }
            Text(stringResource(R.string.home_vip), fontSize = 16.sp, color = Teal)
            state.digestError?.let {
                CosStatusBanner(it, StatusKind.ERROR)
                CosSecondaryButton(stringResource(R.string.try_again), onClick = { retryDigest() })
            }
            if (state.vipItems.isEmpty() && state.digestError == null) {
                CosBody(stringResource(R.string.home_vip_empty))
            } else {
                state.vipItems.forEach { item ->
                    Text(item.subject, fontSize = 17.sp, color = Ink)
                    CosBody(item.from)
                    CosBody(item.summary)
                }
            }

            Text(stringResource(R.string.home_mail), fontSize = 16.sp, color = Teal)
            if (!state.loading && state.mail.isEmpty() && state.error == null) {
                CosBody(stringResource(R.string.home_no_mail))
            }
            state.mail.forEach { thread ->
                Text(thread.subject, fontSize = 17.sp, color = Ink)
                CosBody(thread.from)
                CosBody(thread.plainLanguage.ifBlank { thread.snippet })
            }
        }

        CosCard {
            Text(stringResource(R.string.home_today), fontSize = 18.sp, color = Teal)
            Text(stringResource(R.string.home_first_today), fontSize = 16.sp, color = Teal)
            val first = state.firstToday
            if (first == null) {
                CosBody(stringResource(R.string.home_no_calendar))
            } else {
                Text(first.title, fontSize = 17.sp, color = Ink)
                CosBody(
                    buildString {
                        append(first.whenLabel)
                        if (first.location.isNotBlank()) append(" · ").append(first.location)
                    }
                )
            }

            Text(stringResource(R.string.home_still_open), fontSize = 16.sp, color = Teal)
            val openOlder = state.openNotes.filter { note ->
                state.todayNotes.none { it.id == note.id }
            }
            if (openOlder.isEmpty() && state.mail.isEmpty()) {
                CosBody(stringResource(R.string.home_no_open))
            } else {
                openOlder.forEach { note ->
                    NoteRow(note)
                    CosTextAction(stringResource(R.string.mark_done)) {
                        scope.launch {
                            session.containerRef.prefs.setRecordingOpen(note.id, false)
                            val notes = session.containerRef.prefs.recordings()
                            homeViewModel.showDay(notes, state.mail, state.firstToday)
                        }
                    }
                }
                state.mail.forEach { thread ->
                    Text(thread.subject, fontSize = 17.sp, color = Ink)
                    CosBody(thread.plainLanguage.ifBlank { thread.snippet })
                }
            }

            Text(stringResource(R.string.home_today_notes), fontSize = 16.sp, color = Teal)
            if (state.todayNotes.isEmpty()) {
                CosBody(stringResource(R.string.home_no_today_notes))
            } else {
                state.todayNotes.forEach { note ->
                    NoteRow(note)
                    if (note.open) {
                        CosTextAction(stringResource(R.string.mark_done)) {
                            scope.launch {
                                session.containerRef.prefs.setRecordingOpen(note.id, false)
                                val notes = session.containerRef.prefs.recordings()
                                homeViewModel.showDay(notes, state.mail, state.firstToday)
                            }
                        }
                    }
                }
            }
        }

        CosTextAction(stringResource(R.string.sign_out)) {
            scope.launch {
                try {
                    session.signOut()
                } catch (_: CosException) {
                    // leave local session even if Google sign-out fails
                }
                VipDigestScheduler.cancel(context)
                onSignOut()
            }
        }
    }
}

@Composable
private fun NoteRow(note: RecordingNote) {
    val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.getDefault()
    Text(
        text = CosFormatters.timeOfDay(note.createdAtMillis, locale = locale),
        fontSize = 14.sp,
        color = Teal
    )
    CosBody(note.summary.ifBlank { "—" })
}

@Composable
private fun LangChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Teal,
            selectedLabelColor = OnTeal,
            labelColor = Ink
        )
    )
}
