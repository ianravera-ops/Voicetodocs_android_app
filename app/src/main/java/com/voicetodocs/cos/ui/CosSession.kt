package com.voicetodocs.cos.ui

import android.app.Activity
import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.voicetodocs.cos.AppContainer
import com.voicetodocs.cos.CosApplication
import com.voicetodocs.cos.data.NeedsUserConsent
import com.voicetodocs.cos.data.SignedInUser
import com.voicetodocs.cos.data.TokenExpiredException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CosSession(
    private val container: AppContainer,
    private val activity: Activity,
    private val requestConsent: (PendingIntent, CancellableContinuation<Unit>) -> Unit
) {
    val containerRef: AppContainer get() = container

    suspend fun signIn(): SignedInUser = container.auth.signIn(activity)

    suspend fun ensureAccess() {
        try {
            container.auth.tokenOrAuthorize(activity)
        } catch (e: NeedsUserConsent) {
            suspendCancellableCoroutine<Unit> { cont ->
                requestConsent(e.pendingIntent, cont)
            }
        }
    }

    suspend fun <T> withAccess(block: suspend () -> T): T {
        ensureAccess()
        return try {
            block()
        } catch (_: TokenExpiredException) {
            container.auth.clearToken()
            ensureAccess()
            block()
        }
    }

    suspend fun signOut() {
        container.auth.signOut()
        container.prefs.clearSession()
    }
}

@Composable
fun rememberCosSession(): CosSession {
    val activity = LocalContext.current as Activity
    val container = (activity.application as CosApplication).container
    val scope = rememberCoroutineScope()
    val waiting = remember { mutableListOf<CancellableContinuation<Unit>>() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val cont = waiting.removeFirstOrNull() ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                container.auth.completeAuthorization(result.data)
                cont.resume(Unit)
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        } else {
            cont.resumeWithException(
                IllegalStateException(activity.getString(com.voicetodocs.cos.R.string.error_sign_in))
            )
        }
    }
    return remember(container) {
        CosSession(container, activity) { pending, cont ->
            waiting.add(cont)
            scope.launch {
                launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            }
        }
    }
}
