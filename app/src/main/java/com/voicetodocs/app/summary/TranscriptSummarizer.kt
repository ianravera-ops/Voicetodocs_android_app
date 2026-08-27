package com.voicetodocs.app.summary

/**
 * Lightweight, offline transcript processing used by the "chief of staff" flow.
 *
 * This is deliberately dependency-free so it can be unit tested on the JVM without a
 * device or network. It turns a raw transcript into a short summary, a list of action
 * items, and a suggested reply draft.
 */
class TranscriptSummarizer(
    private val maxSummarySentences: Int = 3,
) {

    data class Digest(
        val summary: String,
        val actionItems: List<String>,
        val draftReply: String,
    )

    fun digest(transcript: String): Digest {
        val cleaned = transcript.trim()
        require(cleaned.isNotEmpty()) { "Transcript must not be blank" }

        val sentences = splitIntoSentences(cleaned)
        val summary = sentences.take(maxSummarySentences).joinToString(" ")
        val actionItems = extractActionItems(sentences)
        val draft = draftReply(actionItems)

        return Digest(summary = summary, actionItems = actionItems, draftReply = draft)
    }

    internal fun splitIntoSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    internal fun extractActionItems(sentences: List<String>): List<String> =
        sentences
            .filter { sentence ->
                ACTION_CUES.any { cue -> sentence.contains(cue, ignoreCase = true) }
            }
            .map { it.removeSuffix(".").trim() }

    private fun draftReply(actionItems: List<String>): String =
        if (actionItems.isEmpty()) {
            "Thanks for the update. I'll follow up if anything is needed."
        } else {
            buildString {
                append("Thanks for the note. Here's what I'll take care of:\n")
                actionItems.forEach { append("- ").append(it).append('\n') }
            }.trimEnd()
        }

    companion object {
        private val ACTION_CUES = listOf(
            "please",
            "need to",
            "let's",
            "can you",
            "follow up",
            "schedule",
            "send",
            "remind",
        )
    }
}
