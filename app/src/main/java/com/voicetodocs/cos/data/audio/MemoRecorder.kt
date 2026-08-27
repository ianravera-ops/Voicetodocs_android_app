package com.voicetodocs.cos.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class MemoRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(): File {
        stopQuietly()
        val file = File(context.cacheDir, "memo_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128000)
        rec.setAudioSamplingRate(44100)
        rec.setOutputFile(file.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec
        outputFile = file
        return file
    }

    fun stop(): File {
        val rec = recorder ?: error("Not recording")
        try {
            rec.stop()
        } finally {
            rec.release()
            recorder = null
        }
        return outputFile ?: error("Missing recording file")
    }

    fun cancel() {
        stopQuietly()
        outputFile?.delete()
        outputFile = null
    }

    private fun stopQuietly() {
        recorder?.let { rec ->
            runCatching { rec.stop() }
            rec.release()
        }
        recorder = null
    }
}
