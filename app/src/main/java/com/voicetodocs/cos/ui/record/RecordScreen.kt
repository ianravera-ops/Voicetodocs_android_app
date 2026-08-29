package com.voicetodocs.cos.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.VisibleFailure
import com.voicetodocs.cos.data.pipeline.MemoStep
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosSecondaryButton
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind
import com.voicetodocs.cos.ui.theme.Danger
import com.voicetodocs.cos.ui.theme.OnTeal
import com.voicetodocs.cos.ui.theme.Teal
import java.io.File

@Composable
fun RecordSection(
    viewModel: RecordViewModel,
    session: CosSession,
    onOpenNotes: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var micDenied by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micDenied = false
            startRecording(session, viewModel, context.getString(R.string.record_recording)) { pendingFile = it }
        } else {
            micDenied = true
        }
    }

    fun stepLabel(step: MemoStep): String = when (step) {
        MemoStep.SAVING_AUDIO -> context.getString(R.string.status_saving_audio)
        MemoStep.UPLOADING -> context.getString(R.string.status_uploading)
        MemoStep.GEMINI -> context.getString(R.string.status_gemini)
        MemoStep.WRITING_DOC -> context.getString(R.string.status_writing_doc)
        MemoStep.DONE -> context.getString(R.string.status_done)
    }

    fun runPipeline(file: File) {
        viewModel.process(::stepLabel) { onStep ->
            session.withAccess {
                val lang = session.containerRef.prefs.language()
                session.containerRef.memoPipeline.process(
                    audioFile = file,
                    language = lang,
                    emptyRecordingMessage = context.getString(R.string.error_empty_recording),
                    missingFolderMessage = context.getString(R.string.error_missing_folder),
                    onStep = onStep
                )
            }
        }
    }

    CosTitle(stringResource(R.string.record_title))
    CosBody(stringResource(R.string.record_hint))

    val bannerText = when {
        micDenied -> stringResource(R.string.error_permission_mic)
        state.error != null -> state.error!!
        state.status.isNotBlank() -> state.status
        else -> stringResource(R.string.record_hint)
    }
    val kind = when {
        micDenied || state.error != null -> StatusKind.ERROR
        state.done -> StatusKind.OK
        state.recording || state.busy -> StatusKind.BUSY
        else -> StatusKind.INFO
    }
    CosStatusBanner(bannerText, kind)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = {
                if (state.busy) return@Button
                if (state.recording) {
                    val file = try {
                        session.containerRef.recorder.stop()
                    } catch (e: Exception) {
                        viewModel.markError(VisibleFailure.of(e).message)
                        return@Button
                    }
                    pendingFile = file
                    runPipeline(file)
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        startRecording(
                            session,
                            viewModel,
                            context.getString(R.string.record_recording)
                        ) { pendingFile = it }
                    } else {
                        permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            enabled = !state.busy,
            modifier = Modifier.size(220.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.recording) Danger else Teal,
                contentColor = OnTeal
            )
        ) {
            Text(
                text = if (state.recording) {
                    stringResource(R.string.record_stop)
                } else {
                    stringResource(R.string.record_start)
                },
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }

    if (state.done) {
        CosPrimaryButton(stringResource(R.string.open_notes), onClick = onOpenNotes)
    }
    if (state.error != null) {
        CosSecondaryButton(stringResource(R.string.try_again), onClick = {
            val file = pendingFile
            if (file != null && file.exists()) {
                runPipeline(file)
            }
        })
    }
}

private fun startRecording(
    session: CosSession,
    viewModel: RecordViewModel,
    recordingLabel: String,
    onFile: (File) -> Unit
) {
    val file = session.containerRef.recorder.start()
    onFile(file)
    viewModel.markRecording(true, recordingLabel)
}
