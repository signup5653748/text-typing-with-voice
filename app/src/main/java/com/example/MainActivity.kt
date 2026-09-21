package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.advancedsettings.AdvancedSettingsScreen
import com.example.presentation.categories.SettingsCategoriesScreen
import com.example.presentation.editor.EditorScreen
import com.example.presentation.editor.EditorViewModel
import com.example.presentation.generalsettings.GeneralSettingsScreen
import com.example.presentation.layoutsettings.LayoutSettingsScreen
import com.example.presentation.speechsettings.SpeechSettingsScreen
import com.example.ui.screens.ReadingModeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var handledUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: EditorViewModel = viewModel()
                    val settings by viewModel.settings.collectAsState()
                    var initialNavHandled by remember { mutableStateOf(false) }

                    LaunchedEffect(settings.startOnReadingScreen) {
                        processIntent(intent, viewModel, navController)
                        if (!initialNavHandled) {
                            val openReadingMode = intent?.getBooleanExtra("open_reading_mode", false) ?: false
                            if (openReadingMode || settings.startOnReadingScreen) {
                                navController.navigate("reading_mode") {
                                    launchSingleTop = true
                                }
                            }
                            val startReading = intent?.getBooleanExtra("start_reading", false) ?: false
                            if (startReading) {
                                viewModel.playReadingModeFromTop()
                            }
                            initialNavHandled = true
                        }
                    }

                    DisposableEffect(Unit) {
                        val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                            processIntent(newIntent, viewModel, navController)
                        }
                        addOnNewIntentListener(listener)
                        onDispose {
                            removeOnNewIntentListener(listener)
                        }
                    }

                    NavHost(navController = navController, startDestination = "editor") {
                        composable("editor") {
                            EditorScreen(
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToReadingMode = { navController.navigate("reading_mode") },
                                viewModel = viewModel
                            )
                        }
                        composable("reading_mode") {
                            ReadingModeScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                        composable("settings") {
                            SettingsCategoriesScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToGeneral = { navController.navigate("settings/general") },
                                onNavigateToSpeech = { navController.navigate("settings/speech") },
                                onNavigateToLayout = { navController.navigate("settings/layout") },
                                onNavigateToAdvanced = { navController.navigate("settings/advanced") }
                            )
                        }
                        composable("settings/general") {
                            GeneralSettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings/speech") {
                            SpeechSettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings/layout") {
                            LayoutSettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings/advanced") {
                            AdvancedSettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCustomLayout = { navController.navigate("settings/layout") }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun processIntent(intent: Intent?, viewModel: EditorViewModel, navController: NavController? = null) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            val uri = intent.data
            if (uri != null && uri != handledUri) {
                handledUri = uri
                viewModel.loadFromUri(uri, isFromExternalOrExplicitOpen = true)
            }
        }
        if (intent.getBooleanExtra("open_reading_mode", false)) {
            navController?.navigate("reading_mode") {
                launchSingleTop = true
            }
        }
        if (intent.getBooleanExtra("start_reading", false)) {
            viewModel.playReadingModeFromTop()
        }
    }
}
