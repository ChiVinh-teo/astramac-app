package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AstraAiService
import com.example.data.model.MessageRole
import com.example.data.model.ModelTier
import com.example.data.model.StudyDocument
import com.example.data.model.TutorMessage
import com.example.data.model.TutorMode
import com.example.data.model.TutorSession
import com.example.data.model.UiState
import com.example.data.repository.StudyRepository
import com.example.domain.PoliticalEconomyCurriculum
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiTurnState {
    data object Idle : AiTurnState
    data object Thinking : AiTurnState
    data class Streaming(val partialText: String) : AiTurnState
    data class Error(val message: String) : AiTurnState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TutorViewModel(
    private val repository: StudyRepository,
    private val aiService: AstraAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "TutorViewModel"
    }

    private val _selectedMode = MutableStateFlow(TutorMode.SOCRATIC)
    val selectedMode: StateFlow<TutorMode> = _selectedMode.asStateFlow()

    private val _selectedModelTier = MutableStateFlow(ModelTier.FLASH)
    val selectedModelTier: StateFlow<ModelTier> = _selectedModelTier.asStateFlow()

    private val _selectedChapter = MutableStateFlow(3) // Mặc định Chương 3 - Giá trị thặng dư
    val selectedChapter: StateFlow<Int> = _selectedChapter.asStateFlow()

    private val _linkedDocumentId = MutableStateFlow("")
    val linkedDocumentId: StateFlow<String> = _linkedDocumentId.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _pendingImage = MutableStateFlow<Pair<String, Bitmap>?>(null)
    val pendingImage: StateFlow<Pair<String, Bitmap>?> = _pendingImage.asStateFlow()

    private val _aiTurnState = MutableStateFlow<AiTurnState>(AiTurnState.Idle)
    val aiTurnState: StateFlow<AiTurnState> = _aiTurnState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    val documentsState: StateFlow<UiState<List<StudyDocument>>> = repository.observeDocuments(currentUserId)
        .map<List<StudyDocument>, UiState<List<StudyDocument>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Lỗi tải danh sách tài liệu."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val sessionsState: StateFlow<UiState<List<TutorSession>>> = repository.observeSessions(currentUserId)
        .map<List<TutorSession>, UiState<List<TutorSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Lỗi tải lịch sử phiên học."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<TutorMessage>>> = _activeSessionId
        .flatMapLatest { sid ->
            if (sid.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, sid)
                    .map<List<TutorMessage>, UiState<List<TutorMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Lỗi tải hội thoại."))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    fun setMode(mode: TutorMode) {
        _selectedMode.value = mode
        val sid = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.updateSessionMetadata(currentUserId, sid, mode = mode)
        }
    }

    fun setModelTier(tier: ModelTier) {
        _selectedModelTier.value = tier
        val sid = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.updateSessionMetadata(currentUserId, sid, modelTier = tier)
        }
    }

    fun setChapter(chapterNumber: Int) {
        _selectedChapter.value = chapterNumber.coerceIn(0, 6)
    }

    fun linkDocument(document: StudyDocument?) {
        _linkedDocumentId.value = document?.id.orEmpty()
        if (document != null && document.chapterNumber in 1..6) {
            _selectedChapter.value = document.chapterNumber.toInt()
        }
        val sid = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.updateSessionMetadata(
                currentUserId,
                sid,
                linkedDocumentId = document?.id.orEmpty(),
                linkedDocumentTitle = document?.title.orEmpty()
            )
        }
    }

    fun attachImage(label: String, bitmap: Bitmap) {
        _pendingImage.value = label to bitmap
    }

    fun clearAttachedImage() {
        _pendingImage.value = null
    }

    fun startNewSession(
        chapterNumber: Int = _selectedChapter.value,
        mode: TutorMode = _selectedMode.value,
        document: StudyDocument? = null
    ) {
        _activeSessionId.value = null
        _selectedChapter.value = chapterNumber.coerceIn(0, 6)
        _selectedMode.value = mode
        _linkedDocumentId.value = document?.id.orEmpty()
        _aiTurnState.value = AiTurnState.Idle
        _pendingImage.value = null
    }

    fun openSession(session: TutorSession) {
        _activeSessionId.value = session.id
        _selectedMode.value = TutorMode.fromRaw(session.mode)
        _selectedModelTier.value = ModelTier.fromRaw(session.modelTier)
        _selectedChapter.value = session.chapterNumber.toInt().coerceIn(0, 6)
        _linkedDocumentId.value = session.linkedDocumentId
        _aiTurnState.value = AiTurnState.Idle
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val result = repository.deleteSession(currentUserId, sessionId)
            result.onFailure { _actionError.value = it.localizedMessage ?: "Không thể xóa phiên học." }
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun dismissAiError() {
        if (_aiTurnState.value is AiTurnState.Error) {
            _aiTurnState.value = AiTurnState.Idle
        }
    }

    fun sendMessage(rawPrompt: String) {
        val imagePair = _pendingImage.value
        val prompt = rawPrompt.trim().ifBlank {
            if (imagePair != null) "Hãy phân tích và giải bài tập/nội dung trong ảnh này." else return
        }
        if (_aiTurnState.value is AiTurnState.Thinking || _aiTurnState.value is AiTurnState.Streaming) {
            return
        }

        val currentDocs = (documentsState.value as? UiState.Success)?.data.orEmpty()
        val linkedDoc = currentDocs.firstOrNull { it.id == _linkedDocumentId.value }
        val currentHistory = (messagesState.value as? UiState.Success)?.data.orEmpty()
        val mode = _selectedMode.value
        val tier = _selectedModelTier.value
        val chapter = _selectedChapter.value

        _pendingImage.value = null

        viewModelScope.launch {
            _aiTurnState.value = AiTurnState.Thinking
            try {
                // Ensure session exists
                var sid = _activeSessionId.value
                var nextMsgCount = currentHistory.size.toLong()
                if (sid.isNullOrBlank()) {
                    val chShort = PoliticalEconomyCurriculum.byNumber(chapter)?.shortTitle ?: "Tổng hợp"
                    val autoTitle = prompt.take(48).ifBlank { "${mode.label} • $chShort" }
                    val created = repository.createSession(
                        userId = currentUserId,
                        title = autoTitle,
                        mode = mode,
                        modelTier = tier,
                        chapterNumber = chapter,
                        linkedDocumentId = linkedDoc?.id.orEmpty(),
                        linkedDocumentTitle = linkedDoc?.title.orEmpty()
                    )
                    sid = created.getOrElse { err ->
                        _aiTurnState.value = AiTurnState.Error(err.localizedMessage ?: "Không thể tạo phiên học trên đám mây.")
                        return@launch
                    }
                    _activeSessionId.value = sid
                    nextMsgCount = 0L
                }

                // Append user message
                nextMsgCount += 1L
                val userAppendResult = repository.appendMessage(
                    userId = currentUserId,
                    sessionId = sid,
                    role = MessageRole.USER,
                    content = prompt,
                    mode = mode,
                    modelUsed = tier.modelId,
                    hasAttachment = imagePair != null,
                    attachmentLabel = imagePair?.first.orEmpty(),
                    hintChips = emptyList(),
                    newMessageCount = nextMsgCount
                )
                userAppendResult.onFailure { err ->
                    _aiTurnState.value = AiTurnState.Error(err.localizedMessage ?: "Không thể lưu câu hỏi.")
                    return@launch
                }

                // Stream Gemini response
                var finalAnswer = ""
                aiService.streamTutorResponse(
                    mode = mode,
                    modelTier = tier,
                    chapterNumber = chapter,
                    linkedDocument = linkedDoc,
                    history = currentHistory,
                    userPrompt = prompt,
                    attachedImage = imagePair?.second
                ).collect { partial ->
                    finalAnswer = partial
                    _aiTurnState.value = AiTurnState.Streaming(partial)
                }

                if (finalAnswer.isBlank()) {
                    _aiTurnState.value = AiTurnState.Error("Mô hình Gemini trả về nội dung trống. Vui lòng thử lại.")
                    return@launch
                }

                // Generate fast follow-up chips via gemini-3.5-flash-lite
                val hints = aiService.generateFollowUpHints(mode, finalAnswer)

                nextMsgCount += 1L
                repository.appendMessage(
                    userId = currentUserId,
                    sessionId = sid,
                    role = MessageRole.ASSISTANT,
                    content = finalAnswer,
                    mode = mode,
                    modelUsed = tier.modelId,
                    hasAttachment = false,
                    attachmentLabel = "",
                    hintChips = hints,
                    newMessageCount = nextMsgCount
                ).onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Không thể lưu câu trả lời."
                }

                if (chapter in 1..6) {
                    repository.recordChapterActivity(currentUserId, chapter, sessionDelta = 1L)
                }
                _aiTurnState.value = AiTurnState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tutor generation timed out", e)
                _aiTurnState.value = AiTurnState.Error("Hết thời gian chờ 60 giây từ Gemini. Vui lòng thử lại.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tutor AI generation failed", e)
                _aiTurnState.value = AiTurnState.Error(
                    e.localizedMessage ?: "Kết nối gia sư AI gặp sự cố. Vui lòng kiểm tra mạng và thử lại."
                )
            }
        }
    }
}
