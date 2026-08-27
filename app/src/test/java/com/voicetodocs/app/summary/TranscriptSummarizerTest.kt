package com.voicetodocs.app.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptSummarizerTest {

    private val summarizer = TranscriptSummarizer(maxSummarySentences = 2)

    @Test
    fun `summary keeps only the configured number of sentences`() {
        val transcript = "First point. Second point. Third point."

        val digest = summarizer.digest(transcript)

        assertEquals("First point. Second point.", digest.summary)
    }

    @Test
    fun `action items are extracted from cue phrases`() {
        val transcript =
            "The launch went well. Please send the recap to the team. Let's schedule a retro."

        val digest = summarizer.digest(transcript)

        assertEquals(
            listOf(
                "Please send the recap to the team",
                "Let's schedule a retro",
            ),
            digest.actionItems,
        )
    }

    @Test
    fun `draft reply lists the action items`() {
        val transcript = "Please remind me to email the client."

        val digest = summarizer.digest(transcript)

        assertTrue(digest.draftReply.contains("Please remind me to email the client"))
    }

    @Test
    fun `draft reply falls back when there are no action items`() {
        val transcript = "The weather was nice today. Everyone had a good time."

        val digest = summarizer.digest(transcript)

        assertTrue(digest.actionItems.isEmpty())
        assertEquals(
            "Thanks for the update. I'll follow up if anything is needed.",
            digest.draftReply,
        )
    }

    @Test
    fun `blank transcript is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            summarizer.digest("   ")
        }
    }
}
