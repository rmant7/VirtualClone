package com.virtualclone.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.core.design.theme.VirtualCloneTheme
import com.virtualclone.app.feature.chat.ui.ChatRoute
import com.virtualclone.app.feature.chat.ui.ChatViewModel
import com.virtualclone.app.feature.docs.ui.DocumentsScreen
import com.virtualclone.app.feature.onboarding.ModelDownloadRoute
import com.virtualclone.app.feature.onboarding.ModelSelectionRoute
import com.virtualclone.app.ui.AppRoutes
import com.virtualclone.app.ui.onboarding.OnboardingScreen
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
    val tag = MainActivity::class.java.simpleName


    NavHost(
        navController = navController,
        startDestination = AppRoutes.Onboarding.route
    ) {

        // ------------------------------------------------------------
        // Onboarding
        // ------------------------------------------------------------
        composable(AppRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(AppRoutes.ModelDownload.route) {
                        popUpTo(AppRoutes.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ------------------------------------------------------------
        // Model Download
        // ------------------------------------------------------------
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

        // ------------------------------------------------------------
        // Model Selection
        // ------------------------------------------------------------
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

        navigation(
            route = AppRoutes.ChatGraph.route,
            startDestination = AppRoutes.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {

            // ---------------- CHAT ----------------
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

            // ---------------- DOCUMENTS ----------------
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