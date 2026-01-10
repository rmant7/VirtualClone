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
    object Documents : AppRoutes("documents")
}
