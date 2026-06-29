package com.bodylens.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bodylens.feature.analysis.AnalysisResultScreen
import com.bodylens.feature.capture.CaptureScreen
import com.bodylens.feature.exercise.ExerciseDetailScreen
import com.bodylens.feature.onboarding.OnboardingScreen
import com.bodylens.feature.plan.PlanScreen
import com.bodylens.feature.progress.ProgressScreen
import com.bodylens.feature.settings.SettingsScreen
import com.bodylens.feature.onboarding.StartViewModel

@Composable
fun BodyLensNavHost() {
    val navController = rememberNavController()
    val startVm: StartViewModel = hiltViewModel()
    val onboarded by startVm.onboardingComplete.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (onboarded) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            ProgressScreen(
                onCapture = { navController.navigate(Routes.CAPTURE) },
                onPlan = { navController.navigate(Routes.PLAN) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(
                onAnalysisReady = { navController.navigate(Routes.ANALYSIS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ANALYSIS) {
            AnalysisResultScreen(onDone = {
                navController.popBackStack(Routes.HOME, inclusive = false)
            })
        }
        composable(Routes.PLAN) {
            PlanScreen(
                onBack = { navController.popBackStack() },
                onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
            )
        }
        composable(Routes.EXERCISE_DETAIL) {
            ExerciseDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDataCleared = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
