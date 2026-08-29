package com.voicetodocs.cos.ui.vip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.digest.VipDigestPolicy
import com.voicetodocs.cos.data.digest.VipDigestScheduler
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosCard
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind
import kotlinx.coroutines.launch

@Composable
fun VipPeopleScreen(
    session: CosSession,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emails by remember { mutableStateOf(listOf<String>()) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notifyDenied by remember { mutableStateOf(false) }

    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifyDenied = !granted
    }

    fun hasNotifyPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        emails = session.containerRef.prefs.vipEmails()
    }

    fun persist(next: List<String>) {
        scope.launch {
            session.containerRef.prefs.setVipEmails(next)
            emails = next
            if (next.isNotEmpty()) {
                VipDigestScheduler.ensure(context)
            }
        }
    }

    fun add() {
        val normalized = VipDigestPolicy.normalizeEmail(draft)
        error = when {
            !VipDigestPolicy.isValidEmail(normalized) ->
                context.getString(R.string.vip_error_email)
            emails.contains(normalized) ->
                context.getString(R.string.vip_error_duplicate)
            emails.size >= VipDigestPolicy.MAX_PEOPLE ->
                context.getString(R.string.vip_error_max)
            else -> null
        }
        if (error != null) return
        draft = ""
        persist(emails + normalized)
        if (!hasNotifyPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CosScreen {
        CosTitle(stringResource(R.string.vip_title))
        CosBody(stringResource(R.string.vip_hint))

        CosCard {
            if (emails.isEmpty()) {
                CosBody(stringResource(R.string.vip_empty))
            } else {
                emails.forEach { address ->
                    CosBody(address)
                    CosTextAction(stringResource(R.string.vip_remove)) {
                        persist(emails.filterNot { it == address })
                    }
                }
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.vip_field_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { add() })
        )
        CosPrimaryButton(
            text = stringResource(R.string.vip_add),
            enabled = emails.size < VipDigestPolicy.MAX_PEOPLE,
            onClick = { add() }
        )

        CosBody(stringResource(R.string.vip_notify_rationale))
        if (notifyDenied) {
            CosStatusBanner(stringResource(R.string.vip_notify_denied), StatusKind.ERROR)
        }

        error?.let { CosStatusBanner(it, StatusKind.ERROR) }
        CosTextAction(stringResource(R.string.back), onClick = onBack)
    }
}
