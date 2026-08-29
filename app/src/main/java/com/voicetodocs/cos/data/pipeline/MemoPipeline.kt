package com.voicetodocs.cos.data.pipeline

import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.CosPreferences
import com.voicetodocs.cos.data.VoiceMemoAnalysis
import com.voicetodocs.cos.data.gemini.GeminiService
import com.voicetodocs.cos.data.google.DocsWriter
import com.voicetodocs.cos.data.google.DriveWorkspace
import java.io.File
import java.time.Instant

enum class MemoStep {
    SAVING_AUDIO,
    UPLOADING,
    GEMINI,
    WRITING_DOC,
    DONE
}

class MemoPipeline(
    private val prefs: CosPreferences,
    private val drive: DriveWorkspace,
    private val docs: DocsWriter,
    private val gemini: GeminiService
) {
    suspend fun process(
        audioFile: File,
        language: AppLanguage,
        emptyRecordingMessage: String,
        missingFolderMessage: String,
        onStep: suspend (MemoStep) -> Unit
    ): VoiceMemoAnalysis {
        onStep(MemoStep.SAVING_AUDIO)
        val bytes = audioFile.readBytes()
        if (bytes.isEmpty()) {
            throw CosException(emptyRecordingMessage)
        }

        val structure = prefs.driveStructure()
            ?: throw CosException(missingFolderMessage)

        onStep(MemoStep.UPLOADING)
        val fileName = audioFile.name
        val audioId = drive.uploadAudio(structure.audioInboxId, fileName, bytes)
        val sourceRef = "drive:$audioId"

        onStep(MemoStep.GEMINI)
        val analysis = gemini.analyzeVoiceMemo(bytes, language)
        val nowLabel = CosFormatters.timestamp(Instant.now())

        onStep(MemoStep.WRITING_DOC)
        docs.prependDocument(
            structure.notesDocId,
            CosFormatters.notesBlock(nowLabel, analysis, sourceRef, language)
        )

        onStep(MemoStep.DONE)
        return analysis
    }
}
