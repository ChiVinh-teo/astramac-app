package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChapterProgress
import com.example.data.model.OralExamAttempt
import com.example.data.model.StudyDocument
import com.example.data.model.StudyFlashcard
import com.example.data.model.TutorSession
import com.example.data.model.UiState
import com.example.data.repository.StudyRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StudyHubSummary(
    val chapterProgress: Map<Int, ChapterProgress> = emptyMap(),
    val documentCount: Int = 0,
    val sessionCount: Int = 0,
    val masteredCards: Int = 0,
    val totalCards: Int = 0,
    val oralCount: Int = 0,
    val averageOralScore: Double = 0.0,
    val overallMasteryPercent: Int = 0,
    val recentSessions: List<TutorSession> = emptyList()
)

class StudyHubViewModel(
    repository: StudyRepository,
    currentUserId: String
) : ViewModel() {

    val hubState: StateFlow<UiState<StudyHubSummary>> = combine(
        repository.observeChapterProgress(currentUserId),
        repository.observeDocuments(currentUserId),
        repository.observeSessions(currentUserId),
        repository.observeFlashcards(currentUserId),
        repository.observeOralAttempts(currentUserId)
    ) { progressList, docs, sessions, cards, orals ->
        val progressMap = progressList.associateBy { it.chapterNumber.toInt() }
        val mastered = cards.count { it.mastered }
        val avgOral = if (orals.isNotEmpty()) orals.map { it.score }.average() else 0.0
        val avgChapterMastery = if (progressList.isNotEmpty()) {
            (progressList.sumOf { it.masteryPercent } / 6.0).toInt().coerceIn(0, 100)
        } else 0
        val cardBonus = if (cards.isNotEmpty()) ((mastered.toDouble() / cards.size) * 20).toInt() else 0
        val overall = (avgChapterMastery + cardBonus).coerceIn(0, 100)

        StudyHubSummary(
            chapterProgress = progressMap,
            documentCount = docs.size,
            sessionCount = sessions.size,
            masteredCards = mastered,
            totalCards = cards.size,
            oralCount = orals.size,
            averageOralScore = avgOral,
            overallMasteryPercent = overall,
            recentSessions = sessions.take(4)
        )
    }.toUiStateFlow("StudyHubVM", "Không thể tải tổng quan học tập")

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(tag, "Stream error", root)
                emit(UiState.Error(root.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
}
