package com.voicetodocs.cos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetodocs.cos.ui.theme.CardBg
import com.voicetodocs.cos.ui.theme.Cream
import com.voicetodocs.cos.ui.theme.Danger
import com.voicetodocs.cos.ui.theme.Ink
import com.voicetodocs.cos.ui.theme.OkGreen
import com.voicetodocs.cos.ui.theme.OnTeal
import com.voicetodocs.cos.ui.theme.Teal

@Composable
fun CosScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun CosTitle(text: String) {
    Text(text = text, fontSize = 26.sp, color = Ink, lineHeight = 32.sp)
}

@Composable
fun CosBody(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 17.sp, color = Ink, lineHeight = 24.sp, modifier = modifier)
}

@Composable
fun CosPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = OnTeal),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(text = text, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

@Composable
fun CosSecondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal)
    ) {
        Text(text = text, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun CosTextAction(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = text, fontSize = 16.sp, color = Teal)
    }
}

@Composable
fun CosStatusBanner(text: String, kind: StatusKind) {
    val bg = when (kind) {
        StatusKind.INFO -> Teal.copy(alpha = 0.12f)
        StatusKind.OK -> OkGreen.copy(alpha = 0.15f)
        StatusKind.ERROR -> Danger.copy(alpha = 0.12f)
        StatusKind.BUSY -> Teal.copy(alpha = 0.12f)
    }
    val fg = when (kind) {
        StatusKind.OK -> OkGreen
        StatusKind.ERROR -> Danger
        else -> Ink
    }
    Text(
        text = text,
        fontSize = 16.sp,
        color = fg,
        lineHeight = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    )
}

enum class StatusKind { INFO, OK, ERROR, BUSY }

@Composable
fun CosCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun CosSpacer() {
    Spacer(Modifier.height(8.dp))
}
