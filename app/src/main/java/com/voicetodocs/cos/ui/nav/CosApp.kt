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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicetodocs.cos.CosApplication
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.LocaleHelper
import com.voicetodocs.cos.ui.CosSession
import com.voicetodocs.cos.ui.call.CallFavoritesScreen
import com.voicetodocs.cos.ui.components.CosBody
import com.voicetodocs.cos.ui.components.CosScreen
import com.voicetodocs.cos.ui.docs.OpenDocsScreen
import com.voicetodocs.cos.ui.draft.DraftEmailScreen
import com.voicetodocs.cos.ui.draft.DraftViewModel
import com.voicetodocs.cos.ui.home.HomeScreen
import com.voicetodocs.cos.ui.home.HomeViewModel
import com.voicetodocs.cos.ui.record.RecordScreen
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
        start = if (session.containerRef.prefs.isSetupComplete()) Routes.Home else Routes.Setup
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
                        val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
                        HomeScreen(
                            viewModel = vm,
                            session = session,
                            onRecord = { nav.navigate(Routes.Record) },
                            onDocs = { nav.navigate(Routes.Docs) },
                            onCall = { nav.navigate(Routes.Call) },
                            onDraft = { id -> nav.navigate("draft/$id") },
                            onSignOut = {
                                nav.navigate(Routes.Setup) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.Record) {
                        val vm: RecordViewModel = viewModel(factory = RecordViewModel.factory(app.container))
                        RecordScreen(
                            viewModel = vm,
                            session = session,
                            onBack = { nav.popBackStack() },
                            onOpenNotes = {
                                nav.navigate(Routes.Docs) {
                                    popUpTo(Routes.Home)
                                }
                            }
                        )
                    }
                    composable(Routes.Docs) {
                        OpenDocsScreen(session = session, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.Call) {
                        CallFavoritesScreen(onBack = { nav.popBackStack() })
                    }
                    composable(
                        route = Routes.Draft,
                        arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments?.getString("threadId").orEmpty()
                        val vm: DraftViewModel = viewModel(factory = DraftViewModel.factory(app.container))
                        DraftEmailScreen(
                            threadId = id,
                            viewModel = vm,
                            session = session,
                            onBack = { nav.popBackStack() }
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
    const val Record = "record"
    const val Docs = "docs"
    const val Call = "call"
    const val Draft = "draft/{threadId}"
}
