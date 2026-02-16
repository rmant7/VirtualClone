package com.virtualclone.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.design.theme.VirtualCloneTheme
import com.virtualclone.app.feature.chat.ui.ChatRoute
import com.virtualclone.app.feature.chat.ui.ChatViewModel
import com.virtualclone.app.feature.docs.ui.DocumentsScreen
import com.virtualclone.app.feature.onboarding.ModelDownloadRoute
import com.virtualclone.app.feature.onboarding.ModelSelectionRoute
import com.virtualclone.app.ui.AppRoutes
import com.virtualclone.app.ui.askimage.AskImageScreen
import com.virtualclone.app.ui.audioscribe.AudioScribeScreen
import com.virtualclone.app.ui.home.HomeScreen
import com.virtualclone.app.ui.mobileactions.MobileActionsScreen
import com.virtualclone.app.ui.onboarding.OnboardingScreen
import com.virtualclone.app.ui.promptlab.PromptLabScreen
import com.virtualclone.app.ui.tinygarden.TinyGardenScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val tag = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.i("MainActivity onCreate()", tag)

        enableEdgeToEdge()

        setContent {
            VirtualCloneTheme(dynamicColor = false) {
                VirtualCloneApp()
            }
        }
    }
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
private fun VirtualCloneApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home.route
    ) {

        // ────────────────────────────────────────────────────
        // Home
        // ────────────────────────────────────────────────────
        composable(AppRoutes.Home.route) {
            HomeScreen(
                onNavigateToChat = {
                    navController.navigate(AppRoutes.ModelSelection.route)
                },
                onNavigateToAskImage = {
                    navController.navigate(AppRoutes.AskImage.route)
                },
                onNavigateToAudioScribe = {
                    navController.navigate(AppRoutes.AudioScribe.route)
                },
                onNavigateToPromptLab = {
                    navController.navigate(AppRoutes.PromptLab.route)
                },
                onNavigateToTinyGarden = {
                    navController.navigate(AppRoutes.TinyGarden.route)
                },
                onNavigateToMobileActions = {
                    navController.navigate(AppRoutes.MobileActions.route)
                },
                onNavigateToSettings = {
                    navController.navigate(AppRoutes.ModelSettings.route)
                },
                onNavigateToMediaPipe = {
                    navController.navigate(AppRoutes.MediaPipeTasks.route)
                },
                onNavigateToLlm = {
                    navController.navigate(AppRoutes.LlmTasks.route)
                }
            )
        }

        // ────────────────────────────────────────────────────
        // New Feature Screens
        // ────────────────────────────────────────────────────
        composable(AppRoutes.AskImage.route) {
            AskImageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.AudioScribe.route) {
            AudioScribeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PromptLab.route) {
            PromptLabScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.TinyGarden.route) {
            TinyGardenScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.MobileActions.route) {
            MobileActionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ────────────────────────────────────────────────────
        // MediaPipe (existing)
        // ────────────────────────────────────────────────────
        composable(AppRoutes.MediaPipeTasks.route) {
            com.virtualclone.app.feature.mediapipe.ui.MediaPipeTasksScreen(
                onTaskSelected = { taskId ->
                    navController.navigate(AppRoutes.MediaPipeRunner.create(taskId))
                }
            )
        }

        composable(
            route = AppRoutes.MediaPipeRunner.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            com.virtualclone.app.feature.mediapipe.ui.MediaPipeRunnerScreen(taskId = taskId)
        }

        // ────────────────────────────────────────────────────
        // LLM Screens (existing)
        // ────────────────────────────────────────────────────
        composable(AppRoutes.LlmTasks.route) {
            com.virtualclone.app.feature.llm.ui.LlmTasksScreen(
                onTaskSelected = { taskId ->
                    navController.navigate(AppRoutes.LlmRunner.create(taskId))
                }
            )
        }

        composable(
            route = AppRoutes.LlmRunner.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            com.virtualclone.app.feature.llm.ui.LlmRunnerScreen(taskId = taskId)
        }

        // ────────────────────────────────────────────────────
        // Settings
        // ────────────────────────────────────────────────────
        composable(AppRoutes.ModelSettings.route) {
            com.virtualclone.app.ui.settings.ModelSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ────────────────────────────────────────────────────
        // Onboarding
        // ────────────────────────────────────────────────────
        composable(AppRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ────────────────────────────────────────────────────
        // Model Download
        // ────────────────────────────────────────────────────
        composable(AppRoutes.ModelDownload.route) {
            ModelDownloadRoute(
                onOnboardingComplete = {
                    navController.navigate(AppRoutes.ModelSelection.route) {
                        popUpTo(AppRoutes.ModelDownload.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ────────────────────────────────────────────────────
        // Model Selection
        // ────────────────────────────────────────────────────
        composable(AppRoutes.ModelSelection.route) {
            ModelSelectionRoute(
                onModelSelected = { modelUi, conversationId ->
                    navController.navigate(
                        AppRoutes.ChatGraph.create(conversationId)
                    ) {
                        popUpTo(AppRoutes.ModelSelection.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ────────────────────────────────────────────────────
        // Chat Graph (existing)
        // ────────────────────────────────────────────────────
        navigation(
            route = AppRoutes.ChatGraph.route,
            startDestination = AppRoutes.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {

            composable(AppRoutes.Chat.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoutes.ChatGraph.route)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val conversationId =
                    parentEntry.arguments?.getString("conversationId")!!

                ChatRoute(
                    conversationId = conversationId,
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                    onNavigateToDocuments = {
                        navController.navigate(AppRoutes.Documents.route)
                    }
                )
            }

            composable(AppRoutes.Documents.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(AppRoutes.ChatGraph.route)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)

                DocumentsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDocumentSelected = { documentId ->
                        viewModel.selectDocument(documentId)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}