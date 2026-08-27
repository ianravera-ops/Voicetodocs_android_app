package com.voicetodocs.cos.ui.setup

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind
import com.voicetodocs.cos.ui.theme.Ink
import com.voicetodocs.cos.ui.theme.OnTeal
import com.voicetodocs.cos.ui.theme.Teal
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    session: CosSession,
    onReady: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    CosScreen {
        CosTitle(stringResource(R.string.setup_title))
        CosBody(stringResource(R.string.setup_subtitle))

        CosBody(stringResource(R.string.language_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LangChip(
                selected = state.language == AppLanguage.ENGLISH,
                label = stringResource(R.string.language_en),
                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                modifier = Modifier.weight(1f)
            )
            LangChip(
                selected = state.language == AppLanguage.SPANISH,
                label = stringResource(R.string.language_es),
                onClick = { viewModel.setLanguage(AppLanguage.SPANISH) },
                modifier = Modifier.weight(1f)
            )
        }
        CosBody(stringResource(R.string.language_hint))

        CosCard {
            Text(
                text = stringResource(R.string.permissions_title),
                fontSize = 24.sp,
                color = Ink
            )
            CosBody(stringResource(R.string.permissions_body))
        }

        state.user?.let {
            CosStatusBanner(
                text = stringResource(R.string.signed_in_as, it.email),
                kind = StatusKind.OK
            )
        }
        if (state.status.isNotBlank()) {
            CosStatusBanner(state.status, if (state.ready) StatusKind.OK else StatusKind.BUSY)
        }
        state.error?.let { CosStatusBanner(it, StatusKind.ERROR) }

        if (!state.ready) {
            CosPrimaryButton(
                text = when {
                    state.busy && state.user == null -> stringResource(R.string.signing_in)
                    state.busy -> stringResource(R.string.creating_drive)
                    else -> stringResource(R.string.sign_in_google)
                },
                enabled = !state.busy,
                onClick = {
                    scope.launch {
                        viewModel.setBusy(true, context.getString(R.string.signing_in))
                        try {
                            val user = session.signIn()
                            viewModel.onSignedIn(user)
                            session.ensureAccess()
                            viewModel.bootstrapDrive(
                                statusCreating = context.getString(R.string.creating_drive),
                                statusDone = context.getString(R.string.setup_done)
                            )
                        } catch (e: CosException) {
                            viewModel.setBusy(false, error = e.userMessage)
                        } catch (e: Exception) {
                            viewModel.setBusy(
                                false,
                                error = e.message ?: context.getString(R.string.error_sign_in)
                            )
                        }
                    }
                }
            )
        } else {
            CosPrimaryButton(stringResource(R.string.continue_home), onClick = onReady)
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
                fontSize = 22.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        },
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Teal,
            selectedLabelColor = OnTeal,
            labelColor = Ink
        )
    )
}
