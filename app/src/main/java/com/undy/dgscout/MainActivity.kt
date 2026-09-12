package com.undy.dgscout

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
import com.undy.dgscout.ui.CourseDetailScreen
import com.undy.dgscout.ui.CourseListScreen
import com.undy.dgscout.ui.CourseViewModel
import com.undy.dgscout.ui.HoleDetailScreen
import com.undy.dgscout.ui.theme.DGScoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DGScoutTheme {
                DGScoutApp()
            }
        }
    }
}

@Composable
fun DGScoutApp() {
    val navController = rememberNavController()
    val viewModel: CourseViewModel = viewModel()

    NavHost(navController = navController, startDestination = "courses") {
        composable("courses") {
            CourseListScreen(
                viewModel = viewModel,
                onOpenCourse = { courseId -> navController.navigate("course/$courseId") }
            )
        }
        composable(
            route = "course/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
            CourseDetailScreen(
                viewModel = viewModel,
                courseId = courseId,
                onOpenHole = { holeNumber -> navController.navigate("course/$courseId/hole/$holeNumber") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "course/{courseId}/hole/{holeNumber}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("holeNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
            val holeNumber = backStackEntry.arguments?.getInt("holeNumber") ?: 1
            HoleDetailScreen(
                viewModel = viewModel,
                courseId = courseId,
                holeNumber = holeNumber,
                onBack = { navController.popBackStack() },
                onNavigateToHole = { newHoleNumber ->
                    navController.navigate("course/$courseId/hole/$newHoleNumber") {
                        popUpTo("course/$courseId/hole/$holeNumber") { inclusive = true }
                    }
                }
            )
        }
    }
}
