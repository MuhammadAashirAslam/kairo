package com.example.kairo.presentation.chat

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.kairo.R
import com.example.kairo.KairoApp
import com.example.kairo.data.ChatMessage
import com.example.kairo.presentation.components.AttachmentBottomSheet
import com.example.kairo.presentation.components.ChatBubble
import com.example.kairo.presentation.components.DocumentStatusChip
import com.example.kairo.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    documentName: String?,
    chunkCount: Int,
    isGenerating: Boolean,
    activeModelId: String,
    stagedImageUri: Uri?,
    onClearStagedImage: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onPickDocumentClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onOpenModelSetup: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectModel: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isDocLoaded = !documentName.isNullOrBlank() && chunkCount > 0

    // State to track if the user has actively scrolled away from the bottom
    var userScrolledUp by remember { mutableStateOf(false) }

    // Check whether the trailing edge of the last message is actually visible.
    // A streaming bubble can be taller than the screen, so checking only the last
    // item's index wrongly reports "at bottom" while new tokens render off-screen.
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastVisible.index >= totalItems - 1 &&
                lastVisible.offset + lastVisible.size <= layoutInfo.viewportEndOffset + 48
        }
    }

    // Detect user manual scroll interaction - pause sticky auto-scroll when leaving the bottom
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtBottom) {
            userScrolledUp = true
        }
    }

    // When the list reaches the bottom again, re-enable sticky auto-scroll
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            userScrolledUp = false
        }
    }

    // Auto-scroll on new message or streaming delta, ONLY if user hasn't scrolled up.
    // Int.MAX_VALUE offset clamps to the max scroll position, pinning the view to the
    // true end of the stream instead of the top of a screen-tall bubble.
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty() && !userScrolledUp) {
            // Instant scroll keeps up with token streaming without sluggish animations or locking gestures
            listState.scrollToItem(messages.size - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // Floating scroll-to-bottom arrow visibility (visible whenever user is scrolled up)
    val isScrolledUp by remember {
        derivedStateOf {
            (userScrolledUp || !isAtBottom) && messages.size > 1
        }
    }

    // Attachment sheet modal
    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            sheetState = sheetState,
            onDismiss = { showAttachmentSheet = false },
            onPickDocument = onPickDocumentClick,
            onPickImage = onPickImageClick,
            onTakePhoto = onTakePhotoClick
        )
    }

    // Staged image thumbnail bitmap
    val stagedBitmap = remember(stagedImageUri) {
        stagedImageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val modelDisplayName = when (activeModelId) {
        KairoApp.MODEL_QWEN_CODER_1_5B -> "Qwen 2.5 Coder 1.5B"
        KairoApp.MODEL_SMOLLM_360M_Q4 -> "SmolLM2 360M Q4"
        KairoApp.MODEL_SMOLLM_360M -> "SmolLM2 360M Q8"
        KairoApp.MODEL_LLAMA_1B -> "Llama 3.2 1B"
        else -> "Kairo"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceElevated)
                            .clickable { showModelDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = modelDisplayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Model",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("SmolLM2 360M Q4_K_M", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Ultra Fast • ~12-16 tok/s", color = OpenAIGreen, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    showModelDropdown = false
                                    onSelectModel(KairoApp.MODEL_SMOLLM_360M_Q4)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Qwen 2.5 Coder 1.5B Q4", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Code & Technical Reasoning • 1.1GB", color = TextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    showModelDropdown = false
                                    onSelectModel(KairoApp.MODEL_QWEN_CODER_1_5B)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Llama 3.2 1B Q4_K_M", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("General Reasoning • 747MB", color = TextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    showModelDropdown = false
                                    onSelectModel(KairoApp.MODEL_LLAMA_1B)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Open Models Manager...", color = OpenAIGreen, fontSize = 12.sp)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = OpenAIGreen)
                                },
                                onClick = {
                                    showModelDropdown = false
                                    onOpenModelSetup()
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Sidebar",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Quick New Chat button
                    IconButton(onClick = onNewChat) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "New Chat",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Overflow Menu
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Attach Document / Picture", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = Cyan400) },
                            onClick = {
                                showOverflowMenu = false
                                showAttachmentSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Model Setup", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Indigo400) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenModelSetup()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("RAG Settings", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOverflowMenu = false
                                onClearChat()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Active Document Status Strip (if document is loaded)
                if (isDocLoaded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Emerald400, modifier = Modifier.size(14.dp))
                            Text(
                                text = documentName.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "($chunkCount chunks)",
                                fontSize = 11.sp,
                                color = Emerald400
                            )
                        }
                    }
                }

                // Chat Messages List
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        EmptyChatState(
                            documentName = documentName,
                            chunkCount = chunkCount,
                            onAttachClick = { showAttachmentSheet = true },
                            onSuggestionClick = { onSendMessage(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                ChatBubble(message = message)
                            }
                        }
                    }

                    // Floating Scroll-to-Bottom Arrow Button (ChatGPT style)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isScrolledUp && messages.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                userScrolledUp = false
                                scope.launch {
                                    listState.animateScrollToItem(messages.size - 1, scrollOffset = Int.MAX_VALUE)
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Staged Image Preview (if image selected before sending)
                AnimatedVisibility(visible = stagedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            stagedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Staged Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Picture Staged for OCR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Text will be decoded on-device with your prompt",
                                    fontSize = 11.sp,
                                    color = OpenAIGreen
                                )
                            }
                            IconButton(onClick = onClearStagedImage) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 3. Modern Floating Pill-Shaped Input Bar (ChatGPT Style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = DarkBg
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(26.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Circular "+" Button (Subtle Neutral)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceHighlight)
                                    .clickable { showAttachmentSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Attach Document or Picture",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Text Input Field
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = if (isDocLoaded) {
                                            "Ask about ${documentName.orEmpty().take(18)}..."
                                        } else {
                                            "Message Kairo..."
                                        },
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                },
                                enabled = !isGenerating,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank() && !isGenerating) {
                                            val query = inputText.trim()
                                            inputText = ""
                                            userScrolledUp = false
                                            focusManager.clearFocus()
                                            onSendMessage(query)
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Right Action: Stop Generating OR Send Button (ChatGPT Stark Contrast)
                            if (isGenerating) {
                                IconButton(
                                    onClick = onStopGeneration,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Generating",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                val canSend = inputText.isNotBlank() || stagedImageUri != null
                                IconButton(
                                    onClick = {
                                        if (canSend) {
                                            val query = inputText.trim()
                                            inputText = ""
                                            userScrolledUp = false
                                            focusManager.clearFocus()
                                            onSendMessage(query)
                                        }
                                    },
                                    enabled = canSend,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (canSend) Color.White else DarkSurfaceHighlight
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (canSend) DarkBg else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    documentName: String?,
    chunkCount: Int,
    onAttachClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDocLoaded = !documentName.isNullOrBlank() && chunkCount > 0

    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo_round),
            contentDescription = "Kairo Logo",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.dp, DarkBorder, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What can I help with?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "100% On-Device Offline RAG & Document Intelligence",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isDocLoaded) {
            Button(
                onClick = onAttachClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = OpenAIGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Document or Picture", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Try asking directly:",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                "What can Kairo do offline?",
                "How does on-device hybrid RAG work?"
            ).forEach { question ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSuggestionClick(question) }
                ) {
                    Text(
                        text = question,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = documentName.orEmpty(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$chunkCount chunks indexed and ready",
                        fontSize = 12.sp,
                        color = Emerald400
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Suggested Questions:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "Summarize the key points in this document",
                        "What are the main findings or takeaways?"
                    ).forEach { question ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onSuggestionClick(question) }
                        ) {
                            Text(
                                text = "💬  $question",
                                fontSize = 12.sp,
                                color = Cyan400,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
