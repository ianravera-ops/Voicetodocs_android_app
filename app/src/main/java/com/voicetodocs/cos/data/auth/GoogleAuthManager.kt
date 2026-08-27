package com.voicetodocs.cos.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.voicetodocs.cos.BuildConfig
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.NeedsUserConsent
import com.voicetodocs.cos.data.SignedInUser
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

object CosScopes {
    const val GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    const val GMAIL_SEND = "https://www.googleapis.com/auth/gmail.send"
    const val CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    const val DOCUMENTS = "https://www.googleapis.com/auth/documents"
    const val SPREADSHEETS = "https://www.googleapis.com/auth/spreadsheets"

    val all = listOf(
        GMAIL_READONLY,
        GMAIL_SEND,
        CALENDAR_READONLY,
        DRIVE_FILE,
        DOCUMENTS,
        SPREADSHEETS
    )
}

class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    @Volatile
    var accessToken: String? = null
        private set

    fun webClientIdConfigured(): Boolean {
        val id = BuildConfig.WEB_CLIENT_ID
        return id.isNotBlank() && !id.startsWith("YOUR_")
    }

    suspend fun signIn(activity: Activity): SignedInUser {
        if (!webClientIdConfigured()) {
            throw CosException(context.getString(com.voicetodocs.cos.R.string.error_missing_oauth))
        }
        val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonceStr = Base64.encodeToString(nonce, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
            .setNonce(nonceStr)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                return SignedInUser(
                    email = google.id,
                    displayName = google.displayName.orEmpty()
                )
            }
            throw CosException(context.getString(com.voicetodocs.cos.R.string.error_sign_in))
        } catch (e: CosException) {
            throw e
        } catch (e: Exception) {
            throw CosException(context.getString(com.voicetodocs.cos.R.string.error_sign_in), e)
        }
    }

    suspend fun authorize(activity: Activity): String {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(CosScopes.all.map { Scope(it) })
            .build()
        val result = Identity.getAuthorizationClient(activity)
            .authorize(request)
            .await()
        if (result.hasResolution()) {
            val pending = result.pendingIntent
                ?: throw CosException(context.getString(com.voicetodocs.cos.R.string.error_sign_in))
            throw NeedsUserConsent(pending)
        }
        val token = result.accessToken
            ?: throw CosException(context.getString(com.voicetodocs.cos.R.string.error_sign_in))
        accessToken = token
        return token
    }

    fun completeAuthorization(data: Intent?): String {
        val result = Identity.getAuthorizationClient(context)
            .getAuthorizationResultFromIntent(data)
        val token = result.accessToken
            ?: throw CosException(context.getString(com.voicetodocs.cos.R.string.error_sign_in))
        accessToken = token
        return token
    }

    fun clearToken() {
        accessToken = null
    }

    suspend fun signOut() {
        accessToken = null
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    suspend fun tokenOrAuthorize(activity: Activity): String {
        return accessToken ?: authorize(activity)
    }
}
