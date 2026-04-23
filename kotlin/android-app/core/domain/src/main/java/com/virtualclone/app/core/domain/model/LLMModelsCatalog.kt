package com.virtualclone.app.core.domain.model

object LLMModelsCatalog {

    val all: List<LLMModel> = listOf(

        // =========================================================
        // QWEN 2.5 FAMILY (RECOMMENDED)
        // =========================================================

        LLMModel(
            id = "QWEN2_5_0_5B_INSTRUCT",
            path = "/data/local/tmp/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "https://huggingface.co/Qwen",
            needsAuth = false,
            defaultTemperature = 0.9f,
            defaultTopK = 40,
            defaultTopP = 1.0f,
            preferredBackend = "CPU",
            thinking = false
        ),

        LLMModel(
            id = "QWEN2_5_1_5B_INSTRUCT",
            path = "/data/local/tmp/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "https://huggingface.co/Qwen",
            needsAuth = false,
            defaultTemperature = 0.8f,
            defaultTopK = 40,
            defaultTopP = 1.0f,
            preferredBackend = "CPU",
            thinking = false
        ),

        // =========================================================
        // REASONING MODEL
        // =========================================================

        LLMModel(
            id = "DEEPSEEK_R1_DISTILL_QWEN_1_5B",
            path = "/data/local/tmp/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "",
            needsAuth = false,
            defaultTemperature = 0.6f,
            defaultTopK = 40,
            defaultTopP = 0.7f,
            preferredBackend = "CPU",
            thinking = true
        ),

        // =========================================================
        // FAST / SMALL MODELS
        // =========================================================

        LLMModel(
            id = "PHI_4_MINI_INSTRUCT",
            path = "/data/local/tmp/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "",
            needsAuth = false,
            defaultTemperature = 0.6f,
            defaultTopK = 40,
            defaultTopP = 1.0f,
            preferredBackend = "CPU",
            thinking = false
        ),

        LLMModel(
            id = "SMOLLM_135M_INSTRUCT",
            path = "/data/local/tmp/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "",
            needsAuth = false,
            defaultTemperature = 0.9f,
            defaultTopK = 40,
            defaultTopP = 1.0f,
            preferredBackend = "CPU",
            thinking = false
        ),

        LLMModel(
            id = "TINYLLAMA_1_1B_CHAT",
            path = "/data/local/tmp/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            licenseUrl = "",
            needsAuth = false,
            defaultTemperature = 0.9f,
            defaultTopK = 40,
            defaultTopP = 1.0f,
            preferredBackend = "CPU",
            thinking = false
        ),

        LLMModel(
    id = "GEMMA3_1B_IT",
    path = "/data/local/tmp/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
    url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
    licenseUrl = "https://ai.google.dev/gemma/terms",
    needsAuth = true,
    defaultTemperature = 0.9f,
    defaultTopK = 64,
    defaultTopP = 1.0f,
    preferredBackend = "CPU",
    thinking = false,
    inferenceEngine = InferenceEngine.GEMMA
),

LLMModel(
    id = "GEMMA3_4B_IT",
    path = "/data/local/tmp/Gemma3-4B-IT_multi-prefill-seq_q4_ekv2048.task",
    url = "https://huggingface.co/litert-community/Gemma3-4B-IT/resolve/main/Gemma3-4B-IT_multi-prefill-seq_q4_ekv2048.task",
    licenseUrl = "https://ai.google.dev/gemma/terms",
    needsAuth = true,
    defaultTemperature = 0.9f,
    defaultTopK = 64,
    defaultTopP = 1.0f,
    preferredBackend = "GPU",
    thinking = false,
    inferenceEngine = InferenceEngine.GEMMA
)
    )

    fun find(modelId: String): LLMModel =
        all.firstOrNull { it.id == modelId }
            ?: throw IllegalArgumentException("Unknown modelId=$modelId")
}
