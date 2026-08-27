package com.voicetodocs.cos.ui.draft

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosCard
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind
import com.voicetodocs.cos.ui.theme.Ink
import com.voicetodocs.cos.ui.theme.Teal

@Composable
fun DraftEmailScreen(
    threadId: String,
    viewModel: DraftViewModel,
    session: CosSession,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(threadId) {
        viewModel.load {
            session.withAccess {
                val thread = session.containerRef.gmailCalendar.fetchThread(threadId)
                    ?: throw CosException(context.getString(R.string.home_no_mail))
                val lang = session.containerRef.prefs.language()
                val reply = session.containerRef.gemini.suggestReply(thread, lang)
                thread to reply
            }
        }
    }

    CosScreen {
        CosTitle(stringResource(R.string.draft_title))
        CosBody(stringResource(R.string.draft_never_auto))

        if (state.loading) {
            CosStatusBanner(stringResource(R.string.loading), StatusKind.BUSY)
        }
        state.error?.let { CosStatusBanner(it, StatusKind.ERROR) }
        if (state.sent) {
            CosStatusBanner(stringResource(R.string.draft_sent), StatusKind.OK)
        }

        state.thread?.let { thread ->
            CosCard {
                Text(stringResource(R.string.draft_from, thread.from), fontSize = 20.sp, color = Ink)
                Text(stringResource(R.string.draft_subject, thread.subject), fontSize = 22.sp, color = Teal)
                Text(stringResource(R.string.draft_original), fontSize = 22.sp, color = Ink)
                CosBody(thread.plainLanguage.ifBlank { thread.snippet })
            }
        }

        if (!state.loading && !state.sent) {
            Text(stringResource(R.string.draft_suggested), fontSize = 24.sp, color = Ink)
            OutlinedTextField(
                value = state.draft,
                onValueChange = viewModel::updateDraft,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
                shape = RoundedCornerShape(16.dp)
            )
            CosPrimaryButton(
                text = stringResource(R.string.draft_send),
                enabled = state.draft.isNotBlank() && !state.sending,
                onClick = { confirm = true }
            )
        }

        CosTextAction(stringResource(R.string.back), onClick = onBack)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.draft_confirm_title), fontSize = 26.sp) },
            text = { Text(stringResource(R.string.draft_confirm_body), fontSize = 22.sp, lineHeight = 30.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.send { thread, body ->
                        session.withAccess {
                            val user = session.containerRef.prefs.user()
                                ?: throw CosException(context.getString(R.string.error_sign_in))
                            try {
                                session.containerRef.gmailCalendar.sendReply(user.email, thread, body)
                            } catch (e: CosException) {
                                throw CosException(
                                    context.getString(R.string.error_send, e.userMessage),
                                    e
                                )
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.draft_send_yes), fontSize = 22.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(stringResource(R.string.draft_send_no), fontSize = 22.sp)
                }
            }
        )
    }
}
