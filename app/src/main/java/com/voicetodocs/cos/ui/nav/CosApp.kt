package com.voicetodocs.cos.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voicetodocs.cos.CosApplication
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.LocaleHelper
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.home.HomeScreen
import com.voicetodocs.cos.ui.home.HomeViewModel
import com.voicetodocs.cos.ui.record.RecordViewModel
import com.voicetodocs.cos.ui.setup.SetupScreen
import com.voicetodocs.cos.ui.setup.SetupViewModel
import com.voicetodocs.cos.ui.theme.CosTheme

@Composable
fun CosApp(session: CosSession) {
    val app = LocalContext.current.applicationContext as CosApplication
    val language by session.containerRef.prefs.languageFlow.collectAsStateWithLifecycle(AppLanguage.ENGLISH)
    var start by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val ready = session.containerRef.prefs.isSetupComplete() &&
            session.containerRef.prefs.driveStructure() != null
        start = if (ready) Routes.Home else Routes.Setup
    }

    val baseContext = LocalContext.current
    val wrapped = remember(language, baseContext) { LocaleHelper.wrap(baseContext, language) }

    CompositionLocalProvider(LocalContext provides wrapped) {
        CosTheme {
            if (start == null) {
                CosScreen { CosBody(wrapped.getString(R.string.loading)) }
            } else {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = start!!) {
                    composable(Routes.Setup) {
                        val vm: SetupViewModel = viewModel(factory = SetupViewModel.factory(app.container))
                        SetupScreen(
                            viewModel = vm,
                            session = session,
                            onReady = {
                                nav.navigate(Routes.Home) {
                                    popUpTo(Routes.Setup) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.Home) {
                        val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory())
                        val recordVm: RecordViewModel = viewModel(factory = RecordViewModel.factory())
                        HomeScreen(
                            homeViewModel = homeVm,
                            recordViewModel = recordVm,
                            session = session,
                            onSignOut = {
                                nav.navigate(Routes.Setup) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

object Routes {
    const val Setup = "setup"
    const val Home = "home"
}
