package com.voicetodocs.cos

import com.voicetodocs.cos.data.ActionItemDraft
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.Domain
import com.voicetodocs.cos.data.VoiceMemoAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CosFormattersTest {
    @Test
    fun domainParseAcceptsExactAndMessyValues() {
        assertEquals(Domain.FAMILY, Domain.parse("FAMILY"))
        assertEquals(Domain.SIDE_WORK, Domain.parse("side-work"))
        assertEquals(Domain.FINANCE, Domain.parse(" finance "))
        assertEquals(Domain.PERSONAL, Domain.parse("unknown"))
    }

    @Test
    fun financeRequiresHumanReview() {
        val analysis = VoiceMemoAnalysis(
            transcript = "Need to check the bill",
            domain = "FINANCE",
            bluf = "A bill needs a look.",
            is_actionable = false
        )
        assertTrue(analysis.requiresHuman())
        val rows = CosFormatters.actionRows(
            analysis = analysis.copy(
                action_items = listOf(ActionItemDraft(title = "Review bill"))
            ),
            source = "voice_memo",
            sourceRef = "drive:abc",
            masterLogRef = "docs:xyz",
            createdAt = "now"
        )
        assertEquals("TRUE", rows.single()[14])
        assertEquals("FINANCE", rows.single()[4])
    }

    @Test
    fun executiveSummaryIsBlufFirstAndNewestStyle() {
        val analysis = VoiceMemoAnalysis(
            transcript = "Hola familia",
            domain = "FAMILY",
            bluf = "Call tomorrow.",
            action_items = listOf(ActionItemDraft(title = "Call Ana")),
            strategic_notes = "Birthday weekend",
            clarifications_or_risks = "Date unclear",
            is_actionable = true
        )
        val text = CosFormatters.executiveSummaryBlock(
            nowLabel = "2026-08-26 09:00 PDT",
            analysis = analysis,
            sourceRef = "drive:1",
            language = AppLanguage.ENGLISH
        )
        assertTrue(text.startsWith("=== 2026-08-26 09:00 PDT ==="))
        assertTrue(text.contains("BLUF"))
        assertTrue(text.contains("ACTIONS"))
        assertTrue(text.contains("CONTEXT"))
        assertTrue(text.contains("RISKS"))
        assertTrue(text.indexOf("BLUF") < text.indexOf("ACTIONS"))
    }

    @Test
    fun sheetHeadersMatchSpec() {
        assertEquals(
            listOf(
                "id", "created_at", "source", "source_ref", "domain", "priority", "status",
                "title", "notes", "due_date", "people", "bluf", "draft_link", "master_log_ref",
                "requires_human"
            ),
            CosFormatters.SHEET_HEADERS
        )
    }
}
