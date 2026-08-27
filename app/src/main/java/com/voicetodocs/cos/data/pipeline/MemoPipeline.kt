package com.voicetodocs.cos.data.pipeline

import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.CosPreferences
import com.voicetodocs.cos.data.VoiceMemoAnalysis
import com.voicetodocs.cos.data.gemini.GeminiService
import com.voicetodocs.cos.data.google.DocsSheetsWriter
import com.voicetodocs.cos.data.google.DriveWorkspace
import java.io.File
import java.time.Instant

enum class MemoStep {
    SAVING_AUDIO,
    UPLOADING,
    GEMINI,
    TRANSCRIPT,
    SUMMARY,
    ACTIONS,
    DONE
}

class MemoPipeline(
    private val prefs: CosPreferences,
    private val drive: DriveWorkspace,
    private val docsSheets: DocsSheetsWriter,
    private val gemini: GeminiService
) {
    suspend fun process(
        audioFile: File,
        language: AppLanguage,
        onStep: suspend (MemoStep) -> Unit
    ): VoiceMemoAnalysis {
        onStep(MemoStep.SAVING_AUDIO)
        val bytes = audioFile.readBytes()
        if (bytes.isEmpty()) {
            throw CosException("The recording was empty. Please try again.")
        }

        val structure = prefs.driveStructure()
            ?: throw CosException("Notes folder is missing. Sign in again from the first screen.")

        onStep(MemoStep.UPLOADING)
        val fileName = audioFile.name
        val audioId = drive.uploadAudio(structure.audioInboxId, fileName, bytes)
        val sourceRef = "drive:$audioId"

        onStep(MemoStep.GEMINI)
        val analysis = gemini.analyzeVoiceMemo(bytes, language)
        val nowLabel = CosFormatters.timestamp(Instant.now())

        onStep(MemoStep.TRANSCRIPT)
        docsSheets.prependDocument(
            structure.transcriptsDocId,
            CosFormatters.transcriptBlock(nowLabel, analysis.transcript, sourceRef)
        )

        onStep(MemoStep.SUMMARY)
        docsSheets.prependDocument(
            structure.summariesDocId,
            CosFormatters.executiveSummaryBlock(nowLabel, analysis, sourceRef, language)
        )

        onStep(MemoStep.ACTIONS)
        val rows = CosFormatters.actionRows(
            analysis = analysis,
            source = "voice_memo",
            sourceRef = sourceRef,
            masterLogRef = "docs:${structure.transcriptsDocId}",
            createdAt = nowLabel
        )
        docsSheets.insertActionRows(structure.actionSheetId, rows)

        onStep(MemoStep.DONE)
        return analysis
    }
}
