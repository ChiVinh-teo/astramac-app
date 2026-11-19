package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class DocumentSourceType {
    NOTE,
    TEXT_FILE,
    PDF_FILE,
    IMAGE_SCAN;

    companion object {
        fun fromRaw(raw: String): DocumentSourceType =
            entries.firstOrNull { it.name == raw } ?: NOTE
    }
}

enum class TutorMode(val label: String, val subtitle: String) {
    SOCRATIC("Gia sư Socratic", "Gợi mở tư duy từng bước"),
    SOLVER("Giải nhanh AI", "Phân tích công thức & lời giải");

    companion object {
        fun fromRaw(raw: String): TutorMode =
            entries.firstOrNull { it.name == raw } ?: SOCRATIC
    }
}

enum class ModelTier(val modelId: String, val displayName: String, val badge: String) {
    FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Nhanh & Đa năng"),
    ARGON("gemini-4-argon", "Gemini 4 Argon", "Suy luận sâu");

    companion object {
        fun fromRaw(raw: String): ModelTier =
            entries.firstOrNull { it.name == raw } ?: FLASH
    }
}

enum class MessageRole {
    USER,
    ASSISTANT;

    companion object {
        fun fromRaw(raw: String): MessageRole =
            entries.firstOrNull { it.name == raw } ?: ASSISTANT
    }
}

@Keep
@IgnoreExtraProperties
data class StudyDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val chapterNumber: Long = 0L,
    val sourceType: String = DocumentSourceType.NOTE.name,
    val contentText: String = "",
    val summary: String = "",
    val keyConcepts: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TutorSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mode: String = TutorMode.SOCRATIC.name,
    val modelTier: String = ModelTier.FLASH.name,
    val chapterNumber: Long = 0L,
    val linkedDocumentId: String = "",
    val linkedDocumentTitle: String = "",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TutorMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = MessageRole.ASSISTANT.name,
    val content: String = "",
    val mode: String = TutorMode.SOCRATIC.name,
    val modelUsed: String = ModelTier.FLASH.modelId,
    val hasAttachment: Boolean = false,
    val attachmentLabel: String = "",
    val hintChips: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyFlashcard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val documentId: String = "",
    val chapterNumber: Long = 0L,
    val frontTerm: String = "",
    val backDefinition: String = "",
    val formula: String = "",
    val example: String = "",
    val mastered: Boolean = false,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OralExamAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chapterNumber: Long = 0L,
    val documentId: String = "",
    val question: String = "",
    val userAnswer: String = "",
    val score: Double = 0.0,
    val strengths: List<String> = emptyList(),
    val gaps: List<String> = emptyList(),
    val modelFeedback: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChapterProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chapterNumber: Long = 1L,
    val sessionsCompleted: Long = 0L,
    val oralBestScore: Double = 0.0,
    val masteryPercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
