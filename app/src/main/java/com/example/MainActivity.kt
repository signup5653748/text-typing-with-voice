package com.example

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
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.EditorViewModel
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
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
                val currentIntent = intent
                if (currentIntent?.action == android.content.Intent.ACTION_VIEW || currentIntent?.action == android.content.Intent.ACTION_EDIT) {
                    currentIntent.data?.let { viewModel.loadFromUri(it) }
                }
            }
            
            DisposableEffect(Unit) {
                val listener = androidx.core.util.Consumer<android.content.Intent> { newIntent ->
                    if (newIntent.action == android.content.Intent.ACTION_VIEW || newIntent.action == android.content.Intent.ACTION_EDIT) {
                        newIntent.data?.let { viewModel.loadFromUri(it) }
                    }
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
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
            }
        }
      }
    }
  }
}
