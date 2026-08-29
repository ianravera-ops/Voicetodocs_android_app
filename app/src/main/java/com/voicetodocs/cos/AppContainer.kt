package com.voicetodocs.cos

import android.app.Application
import com.voicetodocs.cos.data.CosPreferences
import com.voicetodocs.cos.data.TokenExpiredException
import com.voicetodocs.cos.data.audio.MemoRecorder
import com.voicetodocs.cos.data.auth.GoogleAuthManager
import com.voicetodocs.cos.data.gemini.GeminiService
import com.voicetodocs.cos.data.google.DocsWriter
import com.voicetodocs.cos.data.google.DriveWorkspace
import com.voicetodocs.cos.data.google.GmailCalendarClient
import com.voicetodocs.cos.data.google.GoogleHttp
import com.voicetodocs.cos.data.digest.VipDigestScheduler
import com.voicetodocs.cos.data.pipeline.MemoPipeline

class CosApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        VipDigestScheduler.ensure(this)
    }
}

class AppContainer(app: Application) {
    val prefs = CosPreferences(app)
    val auth = GoogleAuthManager(app)
    val recorder = MemoRecorder(app)

    private val http = GoogleHttp(
        tokenProvider = { auth.accessToken },
        missingNetworkMessage = app.getString(R.string.error_network),
        httpErrorTemplate = app.getString(R.string.error_http)
    )

    val drive = DriveWorkspace(http)
    val docs = DocsWriter(http)
    val gmailCalendar = GmailCalendarClient(http)
    val gemini = GeminiService(
        missingKeyMessage = app.getString(R.string.error_missing_gemini),
        networkMessage = app.getString(R.string.error_network),
        geminiErrorTemplate = app.getString(R.string.error_gemini)
    )
    val memoPipeline = MemoPipeline(prefs, drive, docs, gemini)

    suspend fun <T> withFreshToken(block: suspend () -> T): T {
        return try {
            block()
        } catch (_: TokenExpiredException) {
            auth.clearToken()
            throw TokenExpiredException()
        }
    }
}
