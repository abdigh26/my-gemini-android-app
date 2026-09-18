package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ComposeEntryScreen
import com.example.ui.JournalFeedScreen
import com.example.ui.JournalStatsScreen
import com.example.ui.JournalViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JournalApp()
            }
        }
    }
}

@Composable
fun JournalApp() {
    val navController = rememberNavController()
    // Retrieve repository from application context for DI
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as JournalApplication
    val viewModel: JournalViewModel = viewModel(
        factory = JournalViewModel.provideFactory(
            application.repository, 
            application.driveBackupManager,
            application.geminiService,
            application.chatHistoryManager
        )
    )

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            JournalFeedScreen(
                viewModel = viewModel,
                onAddEntry = { navController.navigate("compose") },
                onEditEntry = { id -> navController.navigate("compose?entryId=$id") },
                onNavigateToStats = { navController.navigate("stats") }
            )
        }
        composable("stats") {
            JournalStatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "compose?entryId={entryId}",
            arguments = listOf(navArgument("entryId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            ComposeEntryScreen(
                viewModel = viewModel,
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

