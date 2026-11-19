package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MessageRole
import com.example.data.model.ModelTier
import com.example.data.model.StudyDocument
import com.example.data.model.TutorMessage
import com.example.data.model.TutorMode
import com.example.data.model.TutorSession
import com.example.data.model.UiState
import com.example.domain.PoliticalEconomyCurriculum
import com.example.ui.components.AstraBreathingOrb
import com.example.ui.components.AstraOrbState
import com.example.ui.components.FormattedAiText
import com.example.ui.viewmodel.AiTurnState
import com.example.ui.viewmodel.TutorViewModel
import com.example.util.DocumentIngestionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TutorScreen(
    viewModel: TutorViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedTier by viewModel.selectedModelTier.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val linkedDocId by viewModel.linkedDocumentId.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
    val aiTurnState by viewModel.aiTurnState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()

    val docs = (documentsState as? UiState.Success)?.data.orEmpty()
    val linkedDoc = docs.firstOrNull { it.id == linkedDocId }
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    var inputText by remember { mutableStateOf("") }
    var showSessionsSheet by remember { mutableStateOf(false) }
    var showDocPickerSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bmp = DocumentIngestionHelper.loadBitmapFromUri(context, uri)
                    val label = DocumentIngestionHelper.resolveFileName(context, uri)
                    viewModel.attachImage(label, bmp)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        e.localizedMessage ?: "Không thể đọc ảnh đã chọn.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(messages.size, aiTurnState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    LaunchedEffect(actionError) {
        if (actionError != null) {
            Toast.makeText(context, actionError, Toast.LENGTH_LONG).show()
            viewModel.clearActionError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // Top Controls Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                            TutorMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = selectedMode == mode,
                                    onClick = { viewModel.setMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = TutorMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("tutor_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = if (mode == TutorMode.SOCRATIC) "Socratic" else "Giải nhanh",
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showSessionsSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tutor_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Lịch sử phiên học"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.startNewSession() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tutor_new_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Phiên học mới"
                            )
                        }
                    }

                    // Context Pills: Model Tier + Chapter + Linked Document
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTier == ModelTier.ARGON,
                            onClick = {
                                val next = if (selectedTier == ModelTier.FLASH) ModelTier.ARGON else ModelTier.FLASH
                                viewModel.setModelTier(next)
                            },
                            label = {
                                Text(
                                    if (selectedTier == ModelTier.ARGON) "Gemini 4 Argon • Suy luận sâu"
                                    else "Gemini 3.8 Flash • Tiêu chuẩn"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedTier == ModelTier.ARGON) Icons.Rounded.Psychology else Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("model_tier_toggle")
                        )

                        FilterChip(
                            selected = linkedDoc != null,
                            onClick = { showDocPickerSheet = true },
                            label = {
                                Text(
                                    text = linkedDoc?.title?.take(22) ?: "Gắn tài liệu",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("link_document_chip")
                        )

                        (1..6).forEach { ch ->
                            FilterChip(
                                selected = selectedChapter == ch,
                                onClick = { viewModel.setChapter(ch) },
                                label = { Text("Ch.$ch") }
                            )
                        }
                    }
                }
            }

            // Message Feed or Empty Astra Hero
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && aiTurnState is AiTurnState.Idle) {
                    item {
                        TutorEmptyHero(
                            mode = selectedMode,
                            chapterNumber = selectedChapter,
                            linkedDoc = linkedDoc,
                            onPromptPick = { prompt -> viewModel.sendMessage(prompt) }
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        TutorMessageBubble(
                            message = message,
                            onChipClick = { chip -> viewModel.sendMessage(chip) }
                        )
                    }
                }

                // Live streaming / thinking indicator
                when (val turn = aiTurnState) {
                    is AiTurnState.Thinking -> {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                AstraBreathingOrb(state = AstraOrbState.THINKING, size = 44.dp)
                                Text(
                                    text = "${selectedTier.displayName} đang phân tích biện chứng...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    is AiTurnState.Streaming -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "AstraMác • Đang trả lời...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FormattedAiText(text = turn.partialText)
                                }
                            }
                        }
                    }
                    is AiTurnState.Error -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = turn.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.dismissAiError() }) {
                                        Text("Đóng")
                                    }
                                }
                            }
                        }
                    }
                    AiTurnState.Idle -> Unit
                }
            }

            // Pending Image Preview Strip
            AnimatedVisibility(
                visible = pendingImage != null,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                pendingImage?.let { (label, bmp) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Ảnh bài tập đính kèm",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearAttachedImage() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Gỡ ảnh"
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Composer
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("snap_solve_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Chọn ảnh bài tập (Snap & Solve)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (selectedMode == TutorMode.SOCRATIC) {
                                    "Nhập khái niệm hoặc câu trả lời của bạn..."
                                } else {
                                    "Nhập đề bài tính m', M, c/v hoặc câu hỏi..."
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tutor_prompt_input")
                    )

                    val isBusy = aiTurnState is AiTurnState.Thinking || aiTurnState is AiTurnState.Streaming
                    Surface(
                        shape = CircleShape,
                        color = if (!isBusy && (inputText.isNotBlank() || pendingImage != null)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val textToSend = inputText
                                inputText = ""
                                viewModel.sendMessage(textToSend)
                            },
                            enabled = !isBusy && (inputText.isNotBlank() || pendingImage != null),
                            modifier = Modifier.testTag("tutor_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Gửi câu hỏi",
                                tint = if (!isBusy && (inputText.isNotBlank() || pendingImage != null)) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sessions History Bottom Sheet
    if (showSessionsSheet) {
        ModalBottomSheet(onDismissRequest = { showSessionsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Lịch sử phiên học đám mây",
                    style = MaterialTheme.typography.titleLarge
                )
                val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                if (sessions.isEmpty()) {
                    Text(
                        text = "Chưa có phiên học nào được lưu. Hãy gửi câu hỏi đầu tiên!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    sessions.take(15).forEach { session ->
                        SessionRowItem(
                            session = session,
                            onSelect = {
                                viewModel.openSession(session)
                                showSessionsSheet = false
                            },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Document Picker Bottom Sheet
    if (showDocPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showDocPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chọn tài liệu làm ngữ cảnh cho AI",
                    style = MaterialTheme.typography.titleLarge
                )
                AssistChip(
                    onClick = {
                        viewModel.linkDocument(null)
                        showDocPickerSheet = false
                    },
                    label = { Text("Không gắn tài liệu (chỉ dùng giáo trình chuẩn)") }
                )
                if (docs.isEmpty()) {
                    Text(
                        text = "Thư viện chưa có tài liệu cá nhân. Hãy vào tab Tài liệu để tải PDF, ảnh hoặc ghi chú.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    docs.forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (doc.id == linkedDocId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.linkDocument(doc)
                                    showDocPickerSheet = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = doc.summary.take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorEmptyHero(
    mode: TutorMode,
    chapterNumber: Int,
    linkedDoc: StudyDocument?,
    onPromptPick: (String) -> Unit
) {
    val chapter = PoliticalEconomyCurriculum.byNumber(chapterNumber)
    val suggestions = if (chapter != null) {
        listOf(chapter.sampleSocraticPrompt, chapter.sampleSolverPrompt)
    } else {
        listOf(
            "Giải thích công thức tỷ suất giá trị thặng dư m' = (m/v)×100% qua ví dụ dễ hiểu",
            "Phân biệt 5 đặc điểm kinh tế của độc quyền theo V.I. Lênin"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AstraBreathingOrb(state = AstraOrbState.IDLE, size = 112.dp)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = linkedDoc?.let { "Đang bám sát: ${it.title}" }
                    ?: chapter?.title
                    ?: "Kinh tế chính trị Mác - Lênin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPromptPick(sample) }
                ) {
                    Text(
                        text = sample,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorMessageBubble(
    message: TutorMessage,
    onChipClick: (String) -> Unit
) {
    val isUser = message.role == MessageRole.USER.name
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${TutorMode.fromRaw(message.mode).label} • ${message.modelUsed}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FormattedAiText(text = message.content)
                } else {
                    if (message.hasAttachment && message.attachmentLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Ảnh đính kèm: ${message.attachmentLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (!isUser && message.hintChips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.hintChips.forEach { hint ->
                    AssistChip(
                        onClick = { onChipClick(hint) },
                        label = {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRowItem(
    session: TutorSession,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${TutorMode.fromRaw(session.mode).label} • ${session.lastMessagePreview}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Xóa phiên học",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
