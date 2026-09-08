package com.example.kairo

import android.app.Application
import com.example.kairo.data.ChunkStore
import com.example.kairo.data.ConversationStore
import com.example.kairo.data.DocumentRepository
import com.example.kairo.data.ImageOcrRepository
import com.example.kairo.domain.Chunker
import com.example.kairo.domain.EmbeddingService
import com.example.kairo.domain.RagPromptBuilder
import com.example.kairo.domain.RetrievalService
import com.runanywhere.sdk.llm.llamacpp.LlamaCPP
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.api.InferenceFramework
import com.runanywhere.sdk.public.api.ModelRegistration
import com.runanywhere.sdk.public.api.models
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.kairo.data.KairoPreferences
import kotlinx.coroutines.launch

class KairoApp : Application() {

    companion object {
        lateinit var instance: KairoApp
            private set

        val isInitialized: Boolean
            get() = ::instance.isInitialized

        const val MODEL_SMOLLM_360M_Q4 = "smollm2-360m-instruct-q4_k_m"
        const val MODEL_SMOLLM_360M = "smollm2-360m-instruct-q8_0"
        const val MODEL_LLAMA_1B = "llama-3.2-1b-instruct-q4_k_m"
        const val MODEL_QWEN_CODER_1_5B = "qwen2.5-coder-1.5b-instruct-q4_k_m"

        var selectedModelId: String
            get() = if (::instance.isInitialized) instance.preferences.selectedModelId else MODEL_SMOLLM_360M_Q4
            set(value) { if (::instance.isInitialized) instance.preferences.selectedModelId = value }

        var topKRetrieval: Int
            get() = if (::instance.isInitialized) instance.preferences.topKRetrieval else 3
            set(value) { if (::instance.isInitialized) instance.preferences.topKRetrieval = value }

        var similarityThreshold: Float
            get() = if (::instance.isInitialized) instance.preferences.similarityThreshold else 0.65f
            set(value) { if (::instance.isInitialized) instance.preferences.similarityThreshold = value }

        var maxOutputTokens: Int
            get() = if (::instance.isInitialized) instance.preferences.maxOutputTokens else 1024
            set(value) { if (::instance.isInitialized) instance.preferences.maxOutputTokens = value }
    }

    lateinit var preferences: KairoPreferences
        private set
    lateinit var documentRepository: DocumentRepository
        private set
    lateinit var chunkStore: ChunkStore
        private set
    lateinit var conversationStore: ConversationStore
        private set
    lateinit var chunker: Chunker
        private set
    lateinit var embeddingService: EmbeddingService
        private set
    lateinit var retrievalService: RetrievalService
        private set
    lateinit var promptBuilder: RagPromptBuilder
        private set
    lateinit var imageOcrRepository: ImageOcrRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = KairoPreferences(this)

        // Initialize PDFBox for on-device PDF extraction
        try {
            PDFBoxResourceLoader.init(this)
        } catch (_: Exception) {}

        // Initialize Services & Stores
        documentRepository = DocumentRepository(this)
        chunkStore = ChunkStore()
        conversationStore = ConversationStore(this)
        chunker = Chunker(targetChunkWords = 250, overlapWords = 50)
        embeddingService = EmbeddingService()
        retrievalService = RetrievalService(chunkStore, embeddingService)
        promptBuilder = RagPromptBuilder()
        imageOcrRepository = ImageOcrRepository()

        // Initialize RunAnywhere SDK
        RunAnywhere.initialize(this)

        // Register LlamaCPP backend & Models in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LlamaCPP.register()

                // Primary Default: SmolLM2 360M Q4_K_M (Ultra Fast ~12-18 tok/s, light memory bandwidth)
                RunAnywhere.models.register(
                    ModelRegistration.url(
                        id = MODEL_SMOLLM_360M_Q4,
                        name = "SmolLM2 360M Instruct Q4_K_M",
                        url = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
                        framework = InferenceFramework.INFERENCE_FRAMEWORK_LLAMA_CPP,
                        memoryBytes = 280_000_000L,
                        downloadBytes = 270_590_880L,
                    )
                )

                // High Precision 8-bit SmolLM2
                RunAnywhere.models.register(
                    ModelRegistration.url(
                        id = MODEL_SMOLLM_360M,
                        name = "SmolLM2 360M Instruct Q8_0",
                        url = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                        framework = InferenceFramework.INFERENCE_FRAMEWORK_LLAMA_CPP,
                        memoryBytes = 400_000_000L,
                        downloadBytes = 388_527_104L,
                    )
                )

                // 1B Candidate: Llama-3.2-1B-Instruct Q4_K_M (~700MB)
                RunAnywhere.models.register(
                    ModelRegistration.url(
                        id = MODEL_LLAMA_1B,
                        name = "Llama 3.2 1B Instruct Q4_K_M",
                        url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                        framework = InferenceFramework.INFERENCE_FRAMEWORK_LLAMA_CPP,
                        memoryBytes = 750_000_000L,
                        downloadBytes = 747_112_000L,
                    )
                )

                // Qwen 2.5 Coder 1.5B Instruct Q4_K_M (Code & Technical Reasoning, ~1.1GB)
                RunAnywhere.models.register(
                    ModelRegistration.url(
                        id = MODEL_QWEN_CODER_1_5B,
                        name = "Qwen 2.5 Coder 1.5B Instruct Q4_K_M",
                        url = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
                        framework = InferenceFramework.INFERENCE_FRAMEWORK_LLAMA_CPP,
                        memoryBytes = 1_200_000_000L,
                        downloadBytes = 1_117_320_768L,
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
