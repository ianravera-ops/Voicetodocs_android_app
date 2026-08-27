package com.voicetodocs.cos.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.LocaleHelper
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
import com.voicetodocs.cos.ui.theme.Teal
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    session: CosSession,
    onRecord: () -> Unit,
    onDocs: () -> Unit,
    onCall: () -> Unit,
    onDraft: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.load {
            session.withAccess { viewModel.refreshData() }
        }
    }

    CosScreen {
        CosTitle(stringResource(R.string.home_title))
        state.user?.let {
            CosBody(stringResource(R.string.signed_in_as, it.displayName.ifBlank { it.email }))
        }

        if (state.loading) {
            CosStatusBanner(stringResource(R.string.loading), StatusKind.BUSY)
        }
        state.error?.let { CosStatusBanner(it, StatusKind.ERROR) }

        CosCard {
            Text(stringResource(R.string.home_calendar), fontSize = 24.sp, color = Teal)
            if (state.events.isEmpty() && !state.loading) {
                CosBody(stringResource(R.string.home_no_events))
            } else {
                state.events.take(3).forEach { event ->
                    Text(event.title, fontSize = 24.sp, color = Ink)
                    CosBody(buildString {
                        append(event.whenLabel)
                        if (event.location.isNotBlank()) append(" · ").append(event.location)
                    })
                }
            }
        }

        CosCard {
            Text(stringResource(R.string.home_mail), fontSize = 24.sp, color = Teal)
            if (state.mail.isEmpty() && !state.loading) {
                CosBody(stringResource(R.string.home_no_mail))
            } else {
                state.mail.forEach { thread ->
                    Text(thread.subject, fontSize = 24.sp, color = Ink)
                    CosBody(thread.plainLanguage.ifBlank { thread.snippet })
                    CosSecondaryButton(
                        text = stringResource(R.string.draft_reply),
                        onClick = { onDraft(thread.id) }
                    )
                }
            }
        }

        CosPrimaryButton(
            text = if (state.speaking) {
                stringResource(R.string.hearing_brief)
            } else {
                stringResource(R.string.hear_brief)
            },
            enabled = !state.speaking && !state.loading,
            onClick = {
                viewModel.hearBrief {
                    val lang = session.containerRef.prefs.language()
                    val loc = LocaleHelper.wrap(context, lang)
                    session.withAccess {
                        session.containerRef.gemini.spokenBrief(
                            events = state.events,
                            threads = state.mail,
                            language = lang,
                            emptyFallback = loc.getString(R.string.brief_empty)
                        )
                    }
                }
            }
        )
        CosSecondaryButton(stringResource(R.string.record_note), onClick = onRecord)
        CosSecondaryButton(stringResource(R.string.open_notes), onClick = onDocs)
        CosSecondaryButton(stringResource(R.string.call_someone), onClick = onCall)
        CosTextAction(
            text = if (state.language == AppLanguage.ENGLISH) {
                stringResource(R.string.language_es)
            } else {
                stringResource(R.string.language_en)
            }
        ) {
            val next = if (state.language == AppLanguage.ENGLISH) {
                AppLanguage.SPANISH
            } else {
                AppLanguage.ENGLISH
            }
            viewModel.setLanguage(next) {
                session.withAccess { viewModel.refreshData() }
            }
        }
        CosTextAction(stringResource(R.string.sign_out)) {
            scope.launch {
                try {
                    session.signOut()
                    onSignOut()
                } catch (e: CosException) {
                    // still leave local session
                    onSignOut()
                }
            }
        }
    }
}
