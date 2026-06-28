package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.HomeForgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: HomeForgeViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAnalysis = {
                                navController.navigate("analysis")
                            },
                            onNavigateToPlanner = { roomId ->
                                navController.navigate("planner/$roomId")
                            }
                        )
                    }

                    composable("analysis") {
                        AnalysisScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToPlanner = { roomId ->
                                // Pop the analysis screen so that pressing back on Planner returns to Dashboard
                                navController.navigate("planner/$roomId") {
                                    popUpTo("dashboard")
                                }
                            }
                        )
                    }

                    composable(
                        route = "planner/{roomId}",
                        arguments = listOf(navArgument("roomId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getInt("roomId") ?: 0
                        
                        // Sync viewmodel active roomId if needed
                        LaunchedEffect(roomId) {
                            viewModel.setActiveRoomId(roomId)
                        }

                        PlannerScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
