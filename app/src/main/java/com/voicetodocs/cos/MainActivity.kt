package com.voicetodocs.cos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.voicetodocs.cos.ui.nav.CosApp
import com.voicetodocs.cos.ui.rememberCosSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session = rememberCosSession()
            CosApp(session)
        }
    }
}
