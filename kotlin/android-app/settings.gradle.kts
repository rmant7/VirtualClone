pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VirtualClone"

include(":app")
include(":core:design")
include(":core:common")
include(":data:db")
include(":data:pdf")
include(":ml:onnx")
include(":feature:chat")
include(":feature:docs")
include(":ml:gemma")
include(":ml:mediapipe")
include(":feature:onboarding")
include(":feature:mediapipe")
include(":feature:llm")
include(":core:domain")
include(":data:modelinference")
include(":data:modeldownload")
include(":data:chat")
include(":data:huggingface")
