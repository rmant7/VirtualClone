package com.virtualclone.app.ui

sealed class AppRoutes(val route: String) {
    object Onboarding : AppRoutes("onboarding")
    object ModelDownload : AppRoutes("model_download")
    object ModelSelection : AppRoutes("model_selection")

    object ChatGraph : AppRoutes("chat_graph/{conversationId}") {
        fun create(conversationId: String) =
            "chat_graph/$conversationId"
    }

    object Chat : AppRoutes("chat")
    object Home : AppRoutes("home")
    object Documents : AppRoutes("documents")
    object MediaPipeTasks : AppRoutes("mediapipe_tasks")
    object MediaPipeRunner : AppRoutes("mediapipe_runner/{taskId}") {
        fun create(taskId: String) = "mediapipe_runner/$taskId"
    }
    object LlmTasks : AppRoutes("llm_tasks")
    object LlmRunner : AppRoutes("llm_runner/{taskId}") {
        fun create(taskId: String) = "llm_runner/$taskId"
    }
    object ModelSettings : AppRoutes("model_settings")

    // ── New Feature Routes ──
    object AskImage : AppRoutes("ask_image")
    object AudioScribe : AppRoutes("audio_scribe")
    object PromptLab : AppRoutes("prompt_lab")
    object TinyGarden : AppRoutes("tiny_garden")
    object MobileActions : AppRoutes("mobile_actions")
}
