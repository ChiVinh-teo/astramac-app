package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyDocument
import com.example.data.model.StudyFlashcard
import com.example.data.model.UiState
import com.example.ui.components.FlippingFlashcard
import com.example.ui.components.FormattedAiText
import com.example.ui.theme.EmeraldMastery
import com.example.ui.viewmodel.IngestionState
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    initialShowFlashcards: Boolean,
    onStudyWithDocument: (StudyDocument) -> Unit
) {
    val context = LocalContext.current
    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val flashcardsState by viewModel.flashcardsState.collectAsStateWithLifecycle()
    val ingestionState by viewModel.ingestionState.collectAsStateWithLifecycle()
    val selectedChapterFilter by viewModel.selectedChapterFilter.collectAsStateWithLifecycle()
    val selectedDocFilterId by viewModel.selectedDocFilterId.collectAsStateWithLifecycle()

    var showFlashcardsTab by remember(initialShowFlashcards) { mutableStateOf(initialShowFlashcards) }
    var showNoteDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileOrImageUri(
                context = context,
                uri = uri,
                chapterHint = selectedChapterFilter ?: 0
            )
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileOrImageUri(
                context = context,
                uri = uri,
                chapterHint = selectedChapterFilter ?: 0
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // Header & Tab Switcher
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tài liệu & Flashcard 3D",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !showFlashcardsTab,
                        onClick = { showFlashcardsTab = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("library_tab_docs")
                    ) {
                        Text("Tài liệu học")
                    }
                    SegmentedButton(
                        selected = showFlashcardsTab,
                        onClick = { showFlashcardsTab = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("library_tab_flashcards")
                    ) {
                        Text("Flashcard 3D")
                    }
                }

                // Chapter Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedChapterFilter == null,
                        onClick = { viewModel.setChapterFilter(null) },
                        label = { Text("Tất cả") }
                    )
                    (1..6).forEach { ch ->
                        FilterChip(
                            selected = selectedChapterFilter == ch,
                            onClick = { viewModel.setChapterFilter(ch) },
                            label = { Text("Chương $ch") }
                        )
                    }
                }
            }

            // Ingestion Status Banner
            AnimatedVisibility(
                visible = ingestionState !is IngestionState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    when (val ing = ingestionState) {
                        is IngestionState.Processing -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                    Text(
                                        text = ing.statusLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        is IngestionState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = EmeraldMastery.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ing.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EmeraldMastery,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearIngestionStatus() }) {
                                        Text("OK")
                                    }
                                }
                            }
                        }
                        is IngestionState.Error -> {
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
                                        text = ing.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearIngestionStatus() }) {
                                        Text("Đóng")
                                    }
                                }
                            }
                        }
                        IngestionState.Idle -> Unit
                    }
                }
            }

            if (!showFlashcardsTab) {
                DocumentsTabContent(
                    documentsState = documentsState,
                    chapterFilter = selectedChapterFilter,
                    onUploadFile = {
                        filePicker.launch(arrayOf("application/pdf", "text/plain", "text/markdown", "text/*"))
                    },
                    onPickImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onWriteNote = { showNoteDialog = true },
                    onStudyWithDoc = onStudyWithDocument,
                    onViewFlashcardsForDoc = { doc ->
                        viewModel.setDocumentFilter(doc.id)
                        showFlashcardsTab = true
                    },
                    onDeleteDoc = { viewModel.deleteDocument(it.id) }
                )
            } else {
                FlashcardsTabContent(
                    flashcardsState = flashcardsState,
                    chapterFilter = selectedChapterFilter,
                    docFilterId = selectedDocFilterId,
                    onClearDocFilter = { viewModel.setDocumentFilter(null) },
                    onReviewCard = { card, mastered ->
                        viewModel.toggleFlashcardMastered(card, mastered)
                    }
                )
            }
        }
    }

    if (showNoteDialog) {
        AddNoteDocumentDialog(
            initialChapter = selectedChapterFilter ?: 3,
            onDismiss = { showNoteDialog = false },
            onSubmit = { title, chapter, content ->
                showNoteDialog = false
                viewModel.importNoteDocument(title, chapter, content)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentsTabContent(
    documentsState: UiState<List<StudyDocument>>,
    chapterFilter: Int?,
    onUploadFile: () -> Unit,
    onPickImage: () -> Unit,
    onWriteNote: () -> Unit,
    onStudyWithDoc: (StudyDocument) -> Unit,
    onViewFlashcardsForDoc: (StudyDocument) -> Unit,
    onDeleteDoc: (StudyDocument) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upload Action Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUploadFile,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_pdf_txt_button")
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF / TXT", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onPickImage,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_image_button")
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ảnh trang", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onWriteNote,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_note_button")
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ghi chú", maxLines = 1)
                }
            }
        }

        when (documentsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = documentsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = documentsState.data.filter { doc ->
                    chapterFilter == null || doc.chapterNumber.toInt() == chapterFilter
                }
                if (filtered.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Chưa có tài liệu nào",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Hãy tải giáo trình PDF, ảnh chụp slide hoặc dán đề cương ôn thi. Gemini sẽ tự động tóm tắt và tạo bộ Flashcard 3D đồng bộ đám mây.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onStudyWithDoc = { onStudyWithDoc(doc) },
                            onViewFlashcards = { onViewFlashcardsForDoc(doc) },
                            onDelete = { onDeleteDoc(doc) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentCard(
    doc: StudyDocument,
    onStudyWithDoc: () -> Unit,
    onViewFlashcards: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (doc.chapterNumber in 1..6) "Ch.${doc.chapterNumber}" else "Tổng hợp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xóa tài liệu",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (expanded) {
                FormattedAiText(text = doc.summary)
            } else {
                Text(
                    text = doc.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (doc.keyConcepts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    doc.keyConcepts.take(6).forEach { concept ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = concept,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Thu gọn" else "Xem tóm tắt")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onViewFlashcards,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flashcard")
                }
                Button(
                    onClick = onStudyWithDoc,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Học AI")
                }
            }
        }
    }
}

@Composable
private fun FlashcardsTabContent(
    flashcardsState: UiState<List<StudyFlashcard>>,
    chapterFilter: Int?,
    docFilterId: String?,
    onClearDocFilter: () -> Unit,
    onReviewCard: (StudyFlashcard, Boolean) -> Unit
) {
    when (flashcardsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(text = flashcardsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val filtered = flashcardsState.data.filter { card ->
                val chMatch = chapterFilter == null || card.chapterNumber.toInt() == chapterFilter
                val docMatch = docFilterId == null || card.documentId == docFilterId
                chMatch && docMatch
            }
            var currentIndex by remember(filtered.size, chapterFilter, docFilterId) { mutableIntStateOf(0) }
            var flipped by remember(currentIndex, filtered.size) { mutableStateOf(false) }

            LaunchedEffect(filtered.size) {
                if (currentIndex >= filtered.size && filtered.isNotEmpty()) {
                    currentIndex = 0
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (docFilterId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang lọc Flashcard theo tài liệu đã chọn",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onClearDocFilter) {
                            Text("Hiện tất cả")
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("Chưa có Flashcard nào", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Hãy nạp một tài liệu học hoặc ghi chú ở tab bên cạnh, Gemini sẽ tự động sinh thẻ ôn thi 3D cho bạn.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val safeIdx = currentIndex.coerceIn(0, filtered.lastIndex)
                    val card = filtered[safeIdx]
                    val masteredCount = filtered.count { it.mastered }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Thẻ ${safeIdx + 1}/${filtered.size}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Đã thuộc: $masteredCount/${filtered.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldMastery
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (safeIdx + 1).toFloat() / filtered.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    FlippingFlashcard(
                        card = card,
                        flipped = flipped,
                        onToggleFlip = { flipped = !flipped },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("flashcard_3d_card")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (safeIdx > 0) {
                                    currentIndex = safeIdx - 1
                                    flipped = false
                                }
                            },
                            enabled = safeIdx > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Thẻ trước")
                        }

                        OutlinedButton(
                            onClick = {
                                onReviewCard(card, false)
                                if (safeIdx < filtered.lastIndex) {
                                    currentIndex = safeIdx + 1
                                    flipped = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flashcard_review_again_btn")
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ôn lại")
                        }

                        Button(
                            onClick = {
                                onReviewCard(card, true)
                                if (safeIdx < filtered.lastIndex) {
                                    currentIndex = safeIdx + 1
                                    flipped = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flashcard_mastered_btn")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đã thuộc")
                        }

                        IconButton(
                            onClick = {
                                if (safeIdx < filtered.lastIndex) {
                                    currentIndex = safeIdx + 1
                                    flipped = false
                                }
                            },
                            enabled = safeIdx < filtered.lastIndex,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Thẻ tiếp theo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNoteDocumentDialog(
    initialChapter: Int,
    onDismiss: () -> Unit,
    onSubmit: (title: String, chapter: Int, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var chapter by remember { mutableIntStateOf(initialChapter.coerceIn(0, 6)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ghi chú / Đề cương ôn thi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề tài liệu") },
                    placeholder = { Text("Ví dụ: Đề cương Chương 3 - Giá trị thặng dư") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = chapter == 0,
                        onClick = { chapter = 0 },
                        label = { Text("Tự nhận diện") }
                    )
                    (1..6).forEach { ch ->
                        FilterChip(
                            selected = chapter == ch,
                            onClick = { chapter = ch },
                            label = { Text("Ch.$ch") }
                        )
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung tài liệu / bài giảng") },
                    placeholder = { Text("Dán văn bản bài giảng, câu hỏi ôn tập hoặc tóm tắt của bạn vào đây...") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, chapter, content) },
                enabled = content.isNotBlank()
            ) {
                Text("Phân tích & Tạo Flashcard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
