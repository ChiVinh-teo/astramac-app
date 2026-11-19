package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AstraAiService
import com.example.data.ai.OralGradingResult
import com.example.data.model.OralExamAttempt
import com.example.data.model.StudyDocument
import com.example.data.model.UiState
import com.example.data.repository.StudyRepository
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

sealed interface OralExamPhase {
    data object Idle : OralExamPhase
    data object GeneratingQuestion : OralExamPhase
    data class QuestionReady(val question: String) : OralExamPhase
    data class Grading(val question: String, val userAnswer: String) : OralExamPhase
    data class Graded(
        val question: String,
        val userAnswer: String,
        val result: OralGradingResult
    ) : OralExamPhase
    data class Error(val message: String) : OralExamPhase
}

class OralExamViewModel(
    private val repository: StudyRepository,
    private val aiService: AstraAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "OralExamVM"
    }

    private val _selectedChapter = MutableStateFlow(3)
    val selectedChapter: StateFlow<Int> = _selectedChapter.asStateFlow()

    private val _linkedDocumentId = MutableStateFlow("")
    val linkedDocumentId: StateFlow<String> = _linkedDocumentId.asStateFlow()

    private val _phase = MutableStateFlow<OralExamPhase>(OralExamPhase.Idle)
    val phase: StateFlow<OralExamPhase> = _phase.asStateFlow()

    val documentsState: StateFlow<UiState<List<StudyDocument>>> = repository.observeDocuments(currentUserId)
        .map<List<StudyDocument>, UiState<List<StudyDocument>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Lỗi tải tài liệu."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val attemptsState: StateFlow<UiState<List<OralExamAttempt>>> = repository.observeOralAttempts(currentUserId)
        .map<List<OralExamAttempt>, UiState<List<OralExamAttempt>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Lỗi tải lịch sử vấn đáp."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun setChapter(chapterNumber: Int) {
        _selectedChapter.value = chapterNumber.coerceIn(0, 6)
    }

    fun linkDocument(docId: String) {
        _linkedDocumentId.value = docId
    }

    fun generateNewQuestion() {
        val docs = (documentsState.value as? UiState.Success)?.data.orEmpty()
        val linkedDoc = docs.firstOrNull { it.id == _linkedDocumentId.value }
        val prevQuestions = (attemptsState.value as? UiState.Success)?.data
            ?.map { it.question }.orEmpty()

        viewModelScope.launch {
            _phase.value = OralExamPhase.GeneratingQuestion
            try {
                val question = aiService.generateOralQuestion(
                    chapterNumber = _selectedChapter.value,
                    linkedDocument = linkedDoc,
                    previousQuestions = prevQuestions
                )
                _phase.value = OralExamPhase.QuestionReady(question)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Question generation timed out", e)
                _phase.value = OralExamPhase.Error("Quá thời gian tạo câu hỏi vấn đáp. Vui lòng thử lại.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate oral question", e)
                _phase.value = OralExamPhase.Error(
                    e.localizedMessage ?: "Không thể tạo câu hỏi vấn đáp lúc này."
                )
            }
        }
    }

    fun submitAnswer(rawAnswer: String) {
        val currentQuestion = when (val p = _phase.value) {
            is OralExamPhase.QuestionReady -> p.question
            is OralExamPhase.Graded -> p.question
            else -> return
        }
        val answer = rawAnswer.trim()
        if (answer.isBlank()) return

        val docs = (documentsState.value as? UiState.Success)?.data.orEmpty()
        val linkedDoc = docs.firstOrNull { it.id == _linkedDocumentId.value }
        val chapter = _selectedChapter.value

        viewModelScope.launch {
            _phase.value = OralExamPhase.Grading(currentQuestion, answer)
            try {
                val result = aiService.gradeOralAnswer(
                    chapterNumber = chapter,
                    linkedDocument = linkedDoc,
                    question = currentQuestion,
                    userAnswer = answer
                )
                repository.saveOralAttempt(
                    userId = currentUserId,
                    chapterNumber = chapter,
                    documentId = linkedDoc?.id.orEmpty(),
                    question = currentQuestion,
                    userAnswer = answer,
                    score = result.score,
                    strengths = result.strengths,
                    gaps = result.gaps,
                    modelFeedback = result.feedback
                )
                _phase.value = OralExamPhase.Graded(
                    question = currentQuestion,
                    userAnswer = answer,
                    result = result
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Oral grading timed out", e)
                _phase.value = OralExamPhase.Error("Quá thời gian chấm điểm vấn đáp. Vui lòng thử lại.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Oral grading failed", e)
                _phase.value = OralExamPhase.Error(
                    e.localizedMessage ?: "Chấm điểm vấn đáp thất bại."
                )
            }
        }
    }

    fun resetPhase() {
        _phase.value = OralExamPhase.Idle
    }
}
