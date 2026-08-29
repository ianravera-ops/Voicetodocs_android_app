package com.voicetodocs.cos

import com.voicetodocs.cos.data.MailThread
import com.voicetodocs.cos.data.digest.VipDigestPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class VipDigestPolicyTest {
    private fun thread(id: String, at: Long) = MailThread(
        id = id,
        from = "sender@example.com",
        subject = "Note",
        snippet = "",
        plainLanguage = "Main point $id",
        messageIdHeader = "",
        toAddress = "sender@example.com",
        internalDateMillis = at
    )

    @Test
    fun windowIsSevenToNineEasternAllWeek() {
        val saturdayMorning = ZonedDateTime.of(2026, 8, 29, 7, 0, 0, 0, VipDigestPolicy.ZONE)
        val saturdayNight = ZonedDateTime.of(2026, 8, 29, 21, 0, 0, 0, VipDigestPolicy.ZONE)
        val sundayAfternoon = ZonedDateTime.of(2026, 8, 30, 15, 0, 0, 0, VipDigestPolicy.ZONE)
        val beforeDawn = ZonedDateTime.of(2026, 8, 29, 6, 59, 0, 0, VipDigestPolicy.ZONE)
        assertTrue(VipDigestPolicy.isInWindow(saturdayMorning))
        assertTrue(VipDigestPolicy.isInWindow(sundayAfternoon))
        assertFalse(VipDigestPolicy.isInWindow(saturdayNight))
        assertFalse(VipDigestPolicy.isInWindow(beforeDawn))
    }

    @Test
    fun noNotifyWhenNothingNewerThanWatermark() {
        val old = listOf(thread("a", 100), thread("b", 200))
        val fresh = VipDigestPolicy.newSince(old, 200)
        assertTrue(fresh.isEmpty())
        assertFalse(VipDigestPolicy.shouldNotify(fresh))
    }

    @Test
    fun accumulatesNewSinceWatermarkIntoOneBatch() {
        val mixed = listOf(thread("old", 100), thread("n1", 300), thread("n2", 400))
        val fresh = VipDigestPolicy.newSince(mixed, 200)
        assertEquals(listOf("n1", "n2"), fresh.map { it.id })
        assertTrue(VipDigestPolicy.shouldNotify(fresh))
        assertEquals(400L, VipDigestPolicy.nextWatermark(200, fresh))
    }

    @Test
    fun emailValidationAcceptsPlainAddressesOnly() {
        assertTrue(VipDigestPolicy.isValidEmail("  person@example.com "))
        assertEquals("person@example.com", VipDigestPolicy.normalizeEmail("  Person@Example.com "))
        assertFalse(VipDigestPolicy.isValidEmail("not-an-email"))
        assertFalse(VipDigestPolicy.isValidEmail(""))
    }
}
