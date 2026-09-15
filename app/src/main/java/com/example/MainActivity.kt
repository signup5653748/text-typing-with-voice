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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.EditorViewModel
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.LayoutSettingsScreen
import com.example.ui.screens.SettingsCategoriesScreen
import com.example.ui.screens.SpeechSettingsScreen
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
                    
                    LaunchedEffect(Unit) {
                        processIntent(intent, viewModel)
                    }
                    
                    DisposableEffect(Unit) {
                        val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                            processIntent(newIntent, viewModel)
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
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                        composable("settings/speech") {
                            SpeechSettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                        composable("settings/layout") {
                            LayoutSettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                        composable("settings/advanced") {
                            AdvancedSettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCustomLayout = { navController.navigate("settings/layout") },
                                viewModel = viewModel
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

    private fun processIntent(intent: Intent?, viewModel: EditorViewModel) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            val uri = intent.data
            if (uri != null && uri != handledUri) {
                handledUri = uri
                viewModel.loadFromUri(uri, isFromExternalOrExplicitOpen = true)
            }
        }
    }
}
