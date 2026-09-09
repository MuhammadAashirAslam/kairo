package com.example.kairo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.kairo.data.ChatMessage
import com.example.kairo.data.ChatSession
import com.example.kairo.data.CitationSource
import com.example.kairo.data.GenerationMetrics
import com.example.kairo.data.LoadedDocumentMetadata
import com.example.kairo.data.MessageRole
import com.example.kairo.domain.MatchType
import com.example.kairo.domain.retrieval.Bm25SearchEngine
import com.example.kairo.presentation.chat.ChatScreen
import com.example.kairo.presentation.components.KairoSidebarDrawer
import com.example.kairo.presentation.ingestion.IngestionSheet
import com.example.kairo.presentation.ingestion.IngestionStage
import com.example.kairo.presentation.ingestion.IngestionState
import com.example.kairo.presentation.settings.SettingsScreen
import com.example.kairo.presentation.setup.ModelSetupScreen
import com.example.kairo.ui.theme.KairoTheme
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.api.DownloadEvent
import com.runanywhere.sdk.public.api.GenerationEvent
import com.runanywhere.sdk.public.api.LlmOptions
import com.runanywhere.sdk.public.api.llm
import com.runanywhere.sdk.public.api.models
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class Screen {
    object Chat : Screen()
    object ModelSetup : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KairoTheme {
                KairoRootApp()
            }
        }
    }
}

@Composable
fun KairoRootApp() {
    val scope = rememberCoroutineScope()
    val app = KairoApp.instance
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }
    var selectedModelId by remember { mutableStateOf(KairoApp.selectedModelId) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Chat sessions & active messages
    val sessions = remember { mutableStateListOf<ChatSession>() }
    var activeSessionId by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    var loadedDocName by remember { mutableStateOf(app.chunkStore.getDocumentName()) }
    var chunkCount by remember { mutableIntStateOf(app.chunkStore.getChunkCount()) }

    var isGenerating by remember { mutableStateOf(false) }
    var generationJob by remember { mutableStateOf<Job?>(null) }

    // Ingestion state
    var showIngestionSheet by remember { mutableStateOf(false) }
    var ingestionState by remember { mutableStateOf(IngestionState()) }
    var ingestionJob by remember { mutableStateOf<Job?>(null) }

    // Staged picture for OCR
    var stagedImageUri by remember { mutableStateOf<Uri?>(null) }
    var stagedImageText by remember { mutableStateOf<String?>(null) }

    fun refreshSessions() {
        sessions.clear()
        sessions.addAll(app.conversationStore.getSessions())
        val active = app.conversationStore.getActiveSession()
        activeSessionId = active.id
        messages.clear()
        messages.addAll(active.messages)
    }

    // Cancels any in-flight generation and finalizes the active placeholder, so a
    // session switch / new chat / clear never leaves a stuck "generating" bubble
    // or a coroutine writing into a replaced message list.
    fun stopGeneration() {
        generationJob?.cancel()
        isGenerating = false
        if (messages.isNotEmpty() && messages.last().isGenerating) {
            messages[messages.lastIndex] = messages.last().copy(isGenerating = false)
            app.conversationStore.updateLastAssistantMessage(
                content = messages.last().content,
                isGenerating = false
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshSessions()
    }

    // Helper: Ingest raw text into RAG engine
    fun ingestDocumentText(docName: String, textContent: String, sizeBytes: Long = textContent.length.toLong()) {
        showIngestionSheet = true
        ingestionState = IngestionState(
            stage = IngestionStage.READING,
            statusMessage = "Analyzing document content..."
        )

        ingestionJob = scope.launch {
            try {
                // Step 2: Hierarchical Outline Parsing & Contextual Breadcrumbs
                ingestionState = ingestionState.copy(
                    fileName = docName,
                    fileSize = sizeBytes,
                    stage = IngestionStage.PARSING_STRUCTURE,
                    statusMessage = "Parsing document hierarchy & generating contextual breadcrumbs...",
                    currentStep = 2,
                    totalSteps = 4
                )

                val chunks = app.chunker.chunkDocument(
                    documentName = docName,
                    text = textContent
                )
                if (chunks.isEmpty()) {
                    throw Exception("No text chunks could be extracted from document.")
                }

                // Step 3: Construct in-memory Okapi BM25 Lexical Index
                ingestionState = ingestionState.copy(
                    stage = IngestionStage.INDEXING_BM25,
                    statusMessage = "Constructing Okapi BM25 inverted index (${chunks.size} chunks)...",
                    currentStep = 3,
                    totalSteps = 4,
                    chunkCount = chunks.size
                )

                val bm25Engine = Bm25SearchEngine()
                bm25Engine.indexChunks(chunks)

                // Step 4: Compute Neural Vector Embeddings & L2 Normalization
                ingestionState = ingestionState.copy(
                    stage = IngestionStage.EMBEDDING,
                    statusMessage = "Computing on-device vector embeddings (${chunks.size} chunks)...",
                    currentStep = 4,
                    totalSteps = 4
                )

                val chunkTexts = chunks.map { it.text }
                val embeddings = app.embeddingService.generateEmbeddings(chunkTexts)

                // Save to Store with BM25 Index
                val metadata = LoadedDocumentMetadata(
                    name = docName,
                    sizeBytes = sizeBytes,
                    characterCount = textContent.length,
                    chunkCount = chunks.size
                )
                app.chunkStore.saveDocument(
                    metadata = metadata,
                    newChunks = chunks,
                    newEmbeddings = embeddings,
                    bm25Engine = bm25Engine
                )

                loadedDocName = docName
                chunkCount = chunks.size

                ingestionState = ingestionState.copy(
                    stage = IngestionStage.COMPLETED,
                    statusMessage = "Document fully indexed and ready for grounded retrieval!"
                )
            } catch (e: CancellationException) {
                // Cancelled via the sheet's Cancel button - not an ingestion failure
                throw e
            } catch (e: Exception) {
                ingestionState = ingestionState.copy(
                    stage = IngestionStage.ERROR,
                    statusMessage = "Ingestion failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            showIngestionSheet = true
            ingestionState = IngestionState(
                stage = IngestionStage.READING,
                statusMessage = "Opening document..."
            )

            ingestionJob = scope.launch {
                try {
                    val readResult = app.documentRepository.readDocument(uri)
                    val docData = readResult.getOrThrow()
                    ingestDocumentText(docData.name, docData.content, docData.sizeBytes)
                } catch (e: CancellationException) {
                    // Read/ingest cancelled by the user - not a failure
                    throw e
                } catch (e: Exception) {
                    ingestionState = ingestionState.copy(
                        stage = IngestionStage.ERROR,
                        statusMessage = "Failed to open document: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // Photo Library / Visual Media Picker launcher
    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            stagedImageUri = uri
            scope.launch {
                val ocrResult = app.imageOcrRepository.extractTextFromUri(context, uri)
                ocrResult.onSuccess { extracted ->
                    stagedImageText = extracted.fullText
                    Toast.makeText(context, "OCR Scanned: ${extracted.lineCount} lines detected", Toast.LENGTH_SHORT).show()
                    // Only index the image as a standalone document when nothing else
                    // is loaded; otherwise it would silently replace the user's active
                    // document. With a document present, the OCR text still grounds the
                    // answer directly via the prompt's additionalContext.
                    if (!app.chunkStore.isDocumentLoaded()) {
                        ingestDocumentText(extracted.imageName, extracted.fullText)
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "OCR Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Camera Capture Launcher
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    try {
                        val cacheFile = File(context.cacheDir, "captured_ocr_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(cacheFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        Uri.fromFile(cacheFile)
                    } catch (_: Exception) {
                        null
                    }
                }

                stagedImageUri = savedUri

                val ocrResult = app.imageOcrRepository.extractTextFromBitmap(bitmap)
                ocrResult.onSuccess { extracted ->
                    stagedImageText = extracted.fullText
                    Toast.makeText(context, "Photo Scanned: ${extracted.lineCount} lines detected", Toast.LENGTH_SHORT).show()
                    if (!app.chunkStore.isDocumentLoaded()) {
                        ingestDocumentText(extracted.imageName, extracted.fullText)
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "OCR Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KairoSidebarDrawer(
                sessions = sessions,
                activeSessionId = activeSessionId,
                onSelectSession = { sid ->
                    stopGeneration()
                    app.conversationStore.switchSession(sid)
                    refreshSessions()
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    stopGeneration()
                    app.conversationStore.createSession()
                    refreshSessions()
                    stagedImageUri = null
                    stagedImageText = null
                    scope.launch { drawerState.close() }
                },
                onOpenModels = {
                    currentScreen = Screen.ModelSetup
                    scope.launch { drawerState.close() }
                },
                onOpenKnowledgeBase = {
                    filePickerLauncher.launch(arrayOf("text/*", "text/plain", "application/pdf", "*/*"))
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    currentScreen = Screen.Settings
                    scope.launch { drawerState.close() }
                },
                onRenameSession = { sid, newTitle ->
                    app.conversationStore.renameSession(sid, newTitle)
                    refreshSessions()
                },
                onDeleteSession = { sid ->
                    app.conversationStore.deleteSession(sid)
                    refreshSessions()
                },
                onTogglePinSession = { sid ->
                    app.conversationStore.togglePinSession(sid)
                    refreshSessions()
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Chat -> {
                        ChatScreen(
                            messages = messages,
                            documentName = loadedDocName,
                            chunkCount = chunkCount,
                            isGenerating = isGenerating,
                            activeModelId = selectedModelId,
                            stagedImageUri = stagedImageUri,
                            onClearStagedImage = {
                                stagedImageUri = null
                                stagedImageText = null
                            },
                            onSendMessage = { query ->
                                val currentImageUriStr = stagedImageUri?.toString()
                                val currentImageText = stagedImageText
                                stagedImageUri = null
                                stagedImageText = null

                                val userMessage = ChatMessage(
                                    role = MessageRole.USER,
                                    content = query,
                                    imageUri = currentImageUriStr
                                )
                                messages.add(userMessage)
                                app.conversationStore.addMessage(userMessage)
                                app.conversationStore.autoTitleSession(query)
                                refreshSessions()

                                val assistantPlaceholder = ChatMessage(
                                    role = MessageRole.ASSISTANT,
                                    content = "",
                                    isGenerating = true
                                )
                                messages.add(assistantPlaceholder)
                                app.conversationStore.addMessage(assistantPlaceholder)

                                // Positional indices are unsafe here: switching, clearing, or
                                // starting a new session mid-stream replaces the message list.
                                // Look the bubble up by its stable id instead.
                                val assistantMessageId = assistantPlaceholder.id
                                fun updateStreamingMessage(transform: (ChatMessage) -> ChatMessage) {
                                    val idx = messages.indexOfFirst { it.id == assistantMessageId }
                                    if (idx >= 0) messages[idx] = transform(messages[idx])
                                }

                                isGenerating = true

                                // Declared outside the coroutine's try block so the
                                // cancellation handler can flush the partial response.
                                var generatedText = ""

                                generationJob = scope.launch {
                                    try {
                                        // 1. Check if model is downloaded
                                        val modelId = KairoApp.selectedModelId
                                        val modelInfo = RunAnywhere.models.get(modelId)
                                        if (modelInfo == null || modelInfo.local_path.isEmpty()) {
                                            val downloadingMsg = "Downloading model weights for $modelId... Check progress in Models Manager."
                                            updateStreamingMessage { it.copy(content = downloadingMsg) }
                                            app.conversationStore.updateLastAssistantMessage(content = downloadingMsg, isGenerating = true)

                                            RunAnywhere.models.download(modelId).collect { downloadEvent ->
                                                when (downloadEvent) {
                                                    is DownloadEvent.Progress -> {
                                                        val doneMb = downloadEvent.bytesDone / (1024 * 1024)
                                                        val totalMb = downloadEvent.bytesTotal / (1024 * 1024)
                                                        val pMsg = "Downloading model: $doneMb MB / $totalMb MB..."
                                                        updateStreamingMessage { it.copy(content = pMsg) }
                                                    }
                                                    is DownloadEvent.Completed -> {
                                                        val rMsg = "Model ready! Processing grounded query..."
                                                        updateStreamingMessage { it.copy(content = rMsg) }
                                                    }
                                                    is DownloadEvent.Failed -> {
                                                        throw downloadEvent.error
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }

                                        val isDocLoaded = !loadedDocName.isNullOrBlank() && chunkCount > 0

                                        // 2. Retrieve top-k chunks if document or image is loaded
                                        val scoredChunks = if (isDocLoaded) {
                                            app.retrievalService.retrieveTopK(
                                                query = query,
                                                k = KairoApp.topKRetrieval,
                                                minScoreThreshold = KairoApp.similarityThreshold
                                            )
                                        } else {
                                            emptyList()
                                        }

                                        val sources = scoredChunks.map {
                                            CitationSource(
                                                chunkIndex = it.chunk.chunkIndex,
                                                text = it.chunk.rawText,
                                                score = it.score,
                                                docName = loadedDocName.orEmpty(),
                                                breadcrumb = it.chunk.breadcrumb,
                                                matchType = when (it.matchType) {
                                                    MatchType.HYBRID -> "Hybrid"
                                                    MatchType.KEYWORD -> "Keyword"
                                                    MatchType.SEMANTIC -> "Semantic"
                                                }
                                            )
                                        }

                                        // 3. Assemble Grounded Prompt
                                        val ragPrompt = app.promptBuilder.buildPrompt(
                                            userQuestion = query,
                                            retrievedChunks = scoredChunks,
                                            hasLoadedDocument = isDocLoaded,
                                            persona = app.preferences.systemPersona,
                                            customPrompt = app.preferences.customSystemPrompt,
                                            additionalContext = currentImageText
                                        )

                                        // 4. Stream LLM Generation
                                        val maxTokens = if (modelId == KairoApp.MODEL_QWEN_CODER_1_5B) {
                                            app.preferences.maxOutputTokens.coerceAtLeast(1024)
                                        } else {
                                            app.preferences.maxOutputTokens
                                        }
                                        RunAnywhere.llm.generateStream(
                                            prompt = ragPrompt,
                                            options = LlmOptions(
                                                model = modelId,
                                                maxOutputTokens = maxTokens,
                                                temperature = app.preferences.temperature
                                            )
                                        ).collect { event ->
                                            when (event) {
                                                is GenerationEvent.TextDelta -> {
                                                    generatedText += event.text
                                                    updateStreamingMessage {
                                                        it.copy(
                                                            content = generatedText,
                                                            sources = sources
                                                        )
                                                    }
                                                    app.conversationStore.updateLastAssistantMessage(
                                                        content = generatedText,
                                                        isGenerating = true,
                                                        sources = sources
                                                    )
                                                }
                                                is GenerationEvent.Completed -> {
                                                    val res = event.result
                                                    val metrics = GenerationMetrics(
                                                        tokensPerSecond = res.tokensPerSecond,
                                                        inputTokens = res.inputTokens,
                                                        outputTokens = res.outputTokens
                                                    )
                                                    val isLengthLimit = (res.rawFinishReason?.contains("LENGTH", ignoreCase = true) == true) ||
                                                            (res.outputTokens >= maxTokens - 2 && res.outputTokens > 0)
                                                    val finalText = if (isLengthLimit) {
                                                        "$generatedText\n\n*(Reached token limit of $maxTokens tokens. Type 'continue' to keep going)*"
                                                    } else {
                                                        generatedText.ifBlank { "(No answer produced)" }
                                                    }
                                                    updateStreamingMessage {
                                                        it.copy(
                                                            content = finalText,
                                                            sources = sources,
                                                            metrics = metrics,
                                                            isGenerating = false
                                                        )
                                                    }
                                                    app.conversationStore.updateLastAssistantMessage(
                                                        content = finalText,
                                                        isGenerating = false,
                                                        sources = sources,
                                                        metrics = metrics
                                                    )
                                                }
                                                is GenerationEvent.Failed -> {
                                                    val errorMsg = event.error.localizedMessage ?: "Generation interrupted"
                                                    val partialText = generatedText.ifBlank { "" }
                                                    val finalText = if (partialText.isNotBlank()) {
                                                        "$partialText\n\n⚠️ *[Generation stopped: $errorMsg]*"
                                                    } else {
                                                        "⚠️ *[Generation failed: $errorMsg]*"
                                                    }
                                                    updateStreamingMessage {
                                                        it.copy(
                                                            content = finalText,
                                                            isGenerating = false
                                                        )
                                                    }
                                                    app.conversationStore.updateLastAssistantMessage(
                                                        content = finalText,
                                                        isGenerating = false
                                                    )
                                                }
                                                else -> {}
                                            }
                                        }
                                    } catch (e: CancellationException) {
                                        // User pressed Stop: keep whatever text streamed so far
                                        // instead of overwriting it with a bogus error message.
                                        val partial = generatedText.ifBlank { "(Generation stopped before any output)" }
                                        updateStreamingMessage {
                                            it.copy(
                                                content = partial,
                                                isGenerating = false
                                            )
                                        }
                                        app.conversationStore.updateLastAssistantMessage(
                                            content = partial,
                                            isGenerating = false
                                        )
                                        throw e
                                    } catch (e: Exception) {
                                        val errorMsg = "Inference error: ${e.localizedMessage ?: "Unknown error"}"
                                        updateStreamingMessage {
                                            it.copy(
                                                content = errorMsg,
                                                isGenerating = false
                                            )
                                        }
                                        app.conversationStore.updateLastAssistantMessage(
                                            content = errorMsg,
                                            isGenerating = false
                                        )
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            onStopGeneration = {
                                stopGeneration()
                            },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            },
                            onNewChat = {
                                stopGeneration()
                                app.conversationStore.createSession()
                                refreshSessions()
                                stagedImageUri = null
                                stagedImageText = null
                            },
                            onPickDocumentClick = {
                                filePickerLauncher.launch(arrayOf("text/*", "text/plain", "application/pdf", "*/*"))
                            },
                            onPickImageClick = {
                                pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onTakePhotoClick = {
                                takePicturePreviewLauncher.launch(null)
                            },
                            onOpenModelSetup = {
                                currentScreen = Screen.ModelSetup
                            },
                            onOpenSettings = {
                                currentScreen = Screen.Settings
                            },
                            onSelectModel = { mid ->
                                selectedModelId = mid
                                KairoApp.selectedModelId = mid
                                Toast.makeText(context, "Active model set to: $mid", Toast.LENGTH_SHORT).show()
                            },
                            onClearChat = {
                                stopGeneration()
                                messages.clear()
                                app.conversationStore.clearCurrentSession()
                            }
                        )
                    }

                    is Screen.ModelSetup -> {
                        ModelSetupScreen(
                            onNavigateBack = {
                                selectedModelId = KairoApp.selectedModelId
                                currentScreen = Screen.Chat
                            }
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(
                            onNavigateBack = { currentScreen = Screen.Chat },
                            onDocumentCleared = {
                                loadedDocName = null
                                chunkCount = 0
                            },
                            onChatCleared = {
                                stopGeneration()
                                messages.clear()
                                app.conversationStore.clearCurrentSession()
                            }
                        )
                    }
                }
            }

            // Ingestion Sheet Modal
            if (showIngestionSheet) {
                IngestionSheet(
                    state = ingestionState,
                    onCancel = {
                        ingestionJob?.cancel()
                        showIngestionSheet = false
                    },
                    onDismiss = {
                        showIngestionSheet = false
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
