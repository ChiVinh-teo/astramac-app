package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AstraAiService
import com.example.data.model.DocumentSourceType
import com.example.data.model.StudyDocument
import com.example.data.model.StudyFlashcard
import com.example.data.model.UiState
import com.example.data.repository.StudyRepository
import com.example.util.DocumentIngestionHelper
import com.example.util.IngestedMaterial
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface IngestionState {
    data object Idle : IngestionState
    data class Processing(val statusLabel: String) : IngestionState
    data class Success(val message: String) : IngestionState
    data class Error(val message: String) : IngestionState
}

class LibraryViewModel(
    private val repository: StudyRepository,
    private val aiService: AstraAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "LibraryVM"
    }

    private val _ingestionState = MutableStateFlow<IngestionState>(IngestionState.Idle)
    val ingestionState: StateFlow<IngestionState> = _ingestionState.asStateFlow()

    private val _selectedChapterFilter = MutableStateFlow<Int?>(null)
    val selectedChapterFilter: StateFlow<Int?> = _selectedChapterFilter.asStateFlow()

    private val _selectedDocFilterId = MutableStateFlow<String?>(null)
    val selectedDocFilterId: StateFlow<String?> = _selectedDocFilterId.asStateFlow()

    val documentsState: StateFlow<UiState<List<StudyDocument>>> = repository.observeDocuments(currentUserId)
        .map<List<StudyDocument>, UiState<List<StudyDocument>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Không thể tải thư viện tài liệu."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val flashcardsState: StateFlow<UiState<List<StudyFlashcard>>> = repository.observeFlashcards(currentUserId)
        .map<List<StudyFlashcard>, UiState<List<StudyFlashcard>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Không thể tải bộ Flashcard."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun setChapterFilter(chapter: Int?) {
        _selectedChapterFilter.value = chapter
    }

    fun setDocumentFilter(docId: String?) {
        _selectedDocFilterId.value = docId
    }

    fun clearIngestionStatus() {
        _ingestionState.value = IngestionState.Idle
    }

    fun importNoteDocument(title: String, chapterNumber: Int, rawNote: String) {
        val cleanText = rawNote.trim()
        if (cleanText.isBlank()) return
        val cleanTitle = title.trim().ifBlank { "Ghi chú Kinh tế chính trị" }

        viewModelScope.launch {
            _ingestionState.value = IngestionState.Processing("Gemini đang tóm tắt tài liệu & tạo Flashcard...")
            try {
                val analyzed = aiService.analyzeTextDocument(
                    titleHint = cleanTitle,
                    rawText = cleanText,
                    chapterHint = chapterNumber
                )
                val finalChapter = if (chapterNumber in 1..6) chapterNumber else analyzed.detectedChapter
                val docId = repository.createDocument(
                    userId = currentUserId,
                    title = cleanTitle.ifBlank { analyzed.title },
                    chapterNumber = finalChapter,
                    sourceType = DocumentSourceType.NOTE,
                    contentText = cleanText,
                    summary = analyzed.summary,
                    keyConcepts = analyzed.keyConcepts
                ).getOrThrow()

                val cardCount = repository.addFlashcardsBatch(
                    userId = currentUserId,
                    documentId = docId,
                    chapterNumber = finalChapter,
                    cards = analyzed.flashcards
                ).getOrDefault(0)

                if (finalChapter in 1..6) {
                    repository.recordChapterActivity(currentUserId, finalChapter, sessionDelta = 1L)
                }

                _ingestionState.value = IngestionState.Success(
                    "Đã lưu tài liệu và sinh tự động $cardCount thẻ Flashcard!"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Note analysis timed out", e)
                _ingestionState.value = IngestionState.Error("Hết thời gian chờ AI phân tích tài liệu.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Note ingestion failed", e)
                _ingestionState.value = IngestionState.Error(
                    e.localizedMessage ?: "Phân tích ghi chú thất bại."
                )
            }
        }
    }

    fun importFileOrImageUri(context: Context, uri: Uri, chapterHint: Int = 0) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _ingestionState.value = IngestionState.Processing("Đang đọc tệp và phân tích bằng Gemini...")
            try {
                val material = DocumentIngestionHelper.readDocumentFromUri(appContext, uri)
                val (sourceType, analyzed) = when (material) {
                    is IngestedMaterial.TextMaterial -> {
                        DocumentSourceType.TEXT_FILE to aiService.analyzeTextDocument(
                            titleHint = material.fileName,
                            rawText = material.text,
                            chapterHint = chapterHint
                        )
                    }
                    is IngestedMaterial.PdfMaterial -> {
                        DocumentSourceType.PDF_FILE to aiService.analyzePdfDocument(
                            fileName = material.fileName,
                            pdfBytes = material.pdfBytes,
                            chapterHint = chapterHint
                        )
                    }
                    is IngestedMaterial.ImageMaterial -> {
                        DocumentSourceType.IMAGE_SCAN to aiService.analyzeImageDocument(
                            fileName = material.fileName,
                            bitmap = material.bitmap,
                            chapterHint = chapterHint
                        )
                    }
                }

                val finalChapter = if (chapterHint in 1..6) chapterHint else analyzed.detectedChapter
                val docId = repository.createDocument(
                    userId = currentUserId,
                    title = analyzed.title,
                    chapterNumber = finalChapter,
                    sourceType = sourceType,
                    contentText = analyzed.extractedText.ifBlank { analyzed.summary },
                    summary = analyzed.summary,
                    keyConcepts = analyzed.keyConcepts
                ).getOrThrow()

                val cardCount = repository.addFlashcardsBatch(
                    userId = currentUserId,
                    documentId = docId,
                    chapterNumber = finalChapter,
                    cards = analyzed.flashcards
                ).getOrDefault(0)

                if (finalChapter in 1..6) {
                    repository.recordChapterActivity(currentUserId, finalChapter, sessionDelta = 1L)
                }

                _ingestionState.value = IngestionState.Success(
                    "Đã nạp '${analyzed.title}' và tạo $cardCount Flashcard!"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "File analysis timed out", e)
                _ingestionState.value = IngestionState.Error("Hết thời gian xử lý tệp (60 giây). Hãy thử tệp ngắn hơn.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "File ingestion failed", e)
                _ingestionState.value = IngestionState.Error(
                    e.localizedMessage ?: "Không thể phân tích tài liệu đã chọn."
                )
            }
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            repository.deleteDocument(currentUserId, docId).onFailure { err ->
                _ingestionState.value = IngestionState.Error(
                    err.localizedMessage ?: "Không thể xóa tài liệu."
                )
            }
        }
    }

    fun toggleFlashcardMastered(card: StudyFlashcard, mastered: Boolean) {
        viewModelScope.launch {
            repository.updateFlashcardReview(
                userId = currentUserId,
                cardId = card.id,
                mastered = mastered,
                nextReviewCount = card.reviewCount + 1L
            )
        }
    }
}
