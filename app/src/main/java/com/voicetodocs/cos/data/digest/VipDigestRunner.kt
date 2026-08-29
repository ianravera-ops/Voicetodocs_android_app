package com.voicetodocs.cos.data.digest

import com.voicetodocs.cos.AppContainer
import com.voicetodocs.cos.data.TokenExpiredException
import com.voicetodocs.cos.data.VipDigestItem
import com.voicetodocs.cos.data.VisibleFailure
import java.time.ZonedDateTime

sealed class VipDigestOutcome {
    data object Skipped : VipDigestOutcome()
    data object NoNewMail : VipDigestOutcome()
    data class Digest(val items: List<VipDigestItem>) : VipDigestOutcome()
}

class VipDigestRunner(
    private val container: AppContainer,
    private val notifier: VipNotifier
) {
    suspend fun run(
        notify: Boolean,
        now: ZonedDateTime = ZonedDateTime.now(VipDigestPolicy.ZONE)
    ): VipDigestOutcome {
        val emails = container.prefs.vipEmails()
        if (emails.isEmpty() || !VipDigestPolicy.isInWindow(now)) {
            return VipDigestOutcome.Skipped
        }
        return try {
            ensureToken()
            val watermark = container.prefs.vipWatermark()
            val language = container.prefs.language()
            val raw = container.gmailCalendar.threadsFromSendersSince(emails, watermark)
            val fresh = VipDigestPolicy.newSince(raw, watermark)
            if (!VipDigestPolicy.shouldNotify(fresh)) {
                container.prefs.setDigestError(null)
                return VipDigestOutcome.NoNewMail
            }
            val summarized = container.gemini.plainLanguageEmails(fresh, language)
            val items = summarized.map {
                VipDigestItem(
                    threadId = it.id,
                    from = it.from,
                    subject = it.subject,
                    summary = it.plainLanguage.ifBlank { it.snippet },
                    internalDateMillis = it.internalDateMillis
                )
            }
            container.prefs.addDigestItems(items)
            container.prefs.setVipWatermark(VipDigestPolicy.nextWatermark(watermark, fresh))
            container.prefs.setDigestError(null)
            if (notify) notifier.show(items, language)
            VipDigestOutcome.Digest(items)
        } catch (e: Exception) {
            container.prefs.setDigestError(VisibleFailure.of(e).message)
            throw e
        }
    }

    private suspend fun ensureToken() {
        try {
            if (container.auth.accessToken == null) {
                container.auth.authorizeQuietly()
            }
        } catch (_: TokenExpiredException) {
            container.auth.clearToken()
            container.auth.authorizeQuietly()
        }
    }
}
