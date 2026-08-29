package com.voicetodocs.cos.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosCard
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosSecondaryButton
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.StatusKind
import com.voicetodocs.cos.ui.record.RecordSection
import com.voicetodocs.cos.ui.record.RecordViewModel
import com.voicetodocs.cos.ui.theme.Ink
import com.voicetodocs.cos.ui.theme.OnTeal
import com.voicetodocs.cos.ui.theme.Teal
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    recordViewModel: RecordViewModel,
    session: CosSession,
    onSignOut: () -> Unit
) {
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openError by remember { mutableStateOf<String?>(null) }

    fun loadMail() {
        homeViewModel.load {
            val lang = session.containerRef.prefs.language()
            val user = session.containerRef.prefs.user()
            homeViewModel.showSession(user, lang)
            session.withAccess {
                val raw = session.containerRef.gmailCalendar.importantThreads()
                val mail = session.containerRef.gemini.plainLanguageEmails(raw, lang)
                homeViewModel.showMail(mail)
            }
        }
    }

    fun openNotes() {
        scope.launch {
            val id = session.containerRef.prefs.driveStructure()?.notesDocId
            if (id == null) {
                openError = context.getString(R.string.error_no_docs)
                return@launch
            }
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://docs.google.com/document/d/$id/edit")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                openError = null
            } catch (_: ActivityNotFoundException) {
                openError = context.getString(R.string.error_open_doc)
            }
        }
    }

    LaunchedEffect(Unit) { loadMail() }

    CosScreen {
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
                        loadMail()
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
                        loadMail()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        RecordSection(
            viewModel = recordViewModel,
            session = session,
            onOpenNotes = ::openNotes
        )

        openError?.let { CosStatusBanner(it, StatusKind.ERROR) }
        CosSecondaryButton(stringResource(R.string.open_notes), onClick = ::openNotes)

        CosCard {
            Text(stringResource(R.string.home_mail), fontSize = 18.sp, color = Teal)
            if (state.loading) {
                CosStatusBanner(stringResource(R.string.loading), StatusKind.BUSY)
            }
            state.error?.let { CosStatusBanner(it, StatusKind.ERROR) }
            if (!state.loading && state.mail.isEmpty() && state.error == null) {
                CosBody(stringResource(R.string.home_no_mail))
            }
            state.mail.forEach { thread ->
                Text(thread.subject, fontSize = 17.sp, color = Ink)
                CosBody(thread.from)
                CosBody(thread.plainLanguage.ifBlank { thread.snippet })
            }
            if (state.error != null) {
                CosSecondaryButton(stringResource(R.string.try_again), onClick = { loadMail() })
            }
        }

        CosTextAction(stringResource(R.string.sign_out)) {
            scope.launch {
                try {
                    session.signOut()
                } catch (_: CosException) {
                    // leave local session even if Google sign-out fails
                }
                onSignOut()
            }
        }
    }
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
