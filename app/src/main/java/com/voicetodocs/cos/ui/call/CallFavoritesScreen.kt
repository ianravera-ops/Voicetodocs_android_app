package com.voicetodocs.cos.ui.call

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voicetodocs.cos.R
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosPrimaryButton
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.components.CosSecondaryButton
import com.voicetodocs.cos.ui.components.CosTextAction
import com.voicetodocs.cos.ui.components.CosTitle

@Composable
fun CallFavoritesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    }
    CosScreen {
        CosTitle(stringResource(R.string.call_title))
        CosBody(stringResource(R.string.call_hint))
        CosPrimaryButton(stringResource(R.string.call_ana), onClick = { dial("5550100100") })
        CosSecondaryButton(stringResource(R.string.call_luis), onClick = { dial("5550100200") })
        CosTextAction(stringResource(R.string.back), onClick = onBack)
    }
}
