package com.voicetodocs.cos.ui.docs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.DriveStructure
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosSecondaryButton
import com.voicetodocs.cos.ui.components.CosStatusBanner
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.CosTitle
import com.voicetodocs.cos.ui.components.StatusKind

@Composable
fun OpenDocsScreen(
    session: CosSession,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var structure by remember { mutableStateOf<DriveStructure?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        structure = session.containerRef.prefs.driveStructure()
        if (structure == null) {
            error = context.getString(R.string.error_no_docs)
        }
    }

    fun open(url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            error = context.getString(R.string.error_open_doc)
        }
    }

    CosScreen {
        CosTitle(stringResource(R.string.docs_title))
        CosBody(stringResource(R.string.docs_hint))
        error?.let { CosStatusBanner(it, StatusKind.ERROR) }

        CosPrimaryButton(stringResource(R.string.open_transcripts)) {
            val id = structure?.transcriptsDocId
            if (id == null) error = context.getString(R.string.error_no_docs)
            else open("https://docs.google.com/document/d/$id/edit")
        }
        CosSecondaryButton(stringResource(R.string.open_summaries)) {
            val id = structure?.summariesDocId
            if (id == null) error = context.getString(R.string.error_no_docs)
            else open("https://docs.google.com/document/d/$id/edit")
        }
        CosSecondaryButton(stringResource(R.string.open_sheet)) {
            val id = structure?.actionSheetId
            if (id == null) error = context.getString(R.string.error_no_docs)
            else open("https://docs.google.com/spreadsheets/d/$id/edit")
        }
        CosTextAction(stringResource(R.string.back), onClick = onBack)
    }
}
