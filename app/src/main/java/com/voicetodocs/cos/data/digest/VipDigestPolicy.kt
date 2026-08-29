package com.voicetodocs.cos.data.digest

import com.voicetodocs.cos.data.MailThread
import java.time.ZoneId
import java.time.ZonedDateTime

object VipDigestPolicy {
    val ZONE: ZoneId = ZoneId.of("America/New_York")
    const val MAX_PEOPLE = 5
    const val WINDOW_START_HOUR = 7
    const val WINDOW_END_HOUR = 21

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isInWindow(now: ZonedDateTime): Boolean {
        val local = now.withZoneSameInstant(ZONE)
        val hour = local.hour
        return hour >= WINDOW_START_HOUR && hour < WINDOW_END_HOUR
    }

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun isValidEmail(raw: String): Boolean = EMAIL.matches(normalizeEmail(raw))

    fun newSince(threads: List<MailThread>, watermarkMillis: Long): List<MailThread> {
        return threads
            .filter { it.internalDateMillis > watermarkMillis }
            .sortedBy { it.internalDateMillis }
    }

    fun nextWatermark(watermarkMillis: Long, threads: List<MailThread>): Long {
        val newest = threads.maxOfOrNull { it.internalDateMillis } ?: return watermarkMillis
        return maxOf(watermarkMillis, newest)
    }

    fun shouldNotify(newThreads: List<MailThread>): Boolean = newThreads.isNotEmpty()
}
