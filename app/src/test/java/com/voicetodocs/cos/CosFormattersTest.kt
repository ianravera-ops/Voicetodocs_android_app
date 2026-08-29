package com.voicetodocs.cos

import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.VoiceMemoAnalysis
import org.junit.Assert.assertTrue
import org.junit.Test

class CosFormattersTest {
    @Test
    fun notesBlockHasSummaryAndTranscriptInOneDoc() {
        val analysis = VoiceMemoAnalysis(
            transcript = "Hola familia, llamo mañana.",
            summary = "Call tomorrow about the weekend."
        )
        val text = CosFormatters.notesBlock(
            nowLabel = "2026-08-29 15:00 PDT",
            analysis = analysis,
            sourceRef = "drive:1",
            language = AppLanguage.ENGLISH
        )
        assertTrue(text.startsWith("=== 2026-08-29 15:00 PDT ==="))
        assertTrue(text.contains("Summary"))
        assertTrue(text.contains("Transcript"))
        assertTrue(text.contains(analysis.summary))
        assertTrue(text.contains(analysis.transcript))
        assertTrue(text.indexOf("Summary") < text.indexOf("Transcript"))
    }

    @Test
    fun notesBlockUsesSpanishHeadings() {
        val text = CosFormatters.notesBlock(
            nowLabel = "2026-08-29 15:00 PDT",
            analysis = VoiceMemoAnalysis(
                transcript = "Need paper for class",
                summary = "Comprar papel."
            ),
            sourceRef = "drive:2",
            language = AppLanguage.SPANISH
        )
        assertTrue(text.contains("Resumen"))
        assertTrue(text.contains("Transcripción"))
        assertTrue(text.contains("Comprar papel."))
        assertTrue(text.contains("Need paper for class"))
    }
}
