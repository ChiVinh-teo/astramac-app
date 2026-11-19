package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChapterProgress
import com.example.data.model.DocumentSourceType
import com.example.data.model.MessageRole
import com.example.data.model.ModelTier
import com.example.data.model.OralExamAttempt
import com.example.data.model.StudyDocument
import com.example.data.model.StudyFlashcard
import com.example.data.model.TutorMessage
import com.example.data.model.TutorMode
import com.example.data.model.TutorSession
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class StudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Người dùng cần đăng nhập Google trước khi truy cập dữ liệu.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Phiên đăng nhập đã thay đổi; từ chối ghi dữ liệu chéo tài khoản.")
        }
        return uid
    }

    private fun <T> Flow<T>.handleStreamError(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // ---- Study Documents ----

    fun observeDocuments(userId: String): Flow<List<StudyDocument>> {
        val path = "users/$userId/documents"
        return db.collection("users").document(userId).collection("documents")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StudyDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    suspend fun createDocument(
        userId: String,
        title: String,
        chapterNumber: Int,
        sourceType: DocumentSourceType,
        contentText: String,
        summary: String,
        keyConcepts: List<String>
    ): Result<String> {
        var docPath = "users/$userId/documents"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("documents").document()
            docPath = docRef.path
            val safeTitle = title.trim().ifBlank { "Tài liệu học tập" }.take(140)
            val safeChapter = chapterNumber.coerceIn(0, 6).toLong()
            val safeConcepts = keyConcepts.map { it.trim().take(120) }.filter { it.isNotBlank() }.take(15)
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "chapterNumber" to safeChapter,
                "sourceType" to sourceType.name,
                "contentText" to contentText.take(45_000),
                "summary" to summary.take(8_000),
                "keyConcepts" to safeConcepts,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(userId: String, docId: String): Result<Unit> {
        val path = "users/$userId/documents/$docId"
        return try {
            val uid = requireUserId(userId)
            require(docId.isNotBlank()) { "Document ID cannot be blank." }
            db.collection("users").document(uid).collection("documents").document(docId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Tutor Sessions & Messages ----

    fun observeSessions(userId: String): Flow<List<TutorSession>> {
        val path = "users/$userId/sessions"
        return db.collection("users").document(userId).collection("sessions")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TutorSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeMessages(userId: String, sessionId: String): Flow<List<TutorMessage>> {
        val path = "users/$userId/sessions/$sessionId/messages"
        return db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TutorMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    suspend fun createSession(
        userId: String,
        title: String,
        mode: TutorMode,
        modelTier: ModelTier,
        chapterNumber: Int,
        linkedDocumentId: String,
        linkedDocumentTitle: String
    ): Result<String> {
        var docPath = "users/$userId/sessions"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("sessions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().ifBlank { "Phiên học AI mới" }.take(140),
                "mode" to mode.name,
                "modelTier" to modelTier.name,
                "chapterNumber" to chapterNumber.coerceIn(0, 6).toLong(),
                "linkedDocumentId" to linkedDocumentId.take(128),
                "linkedDocumentTitle" to linkedDocumentTitle.take(140),
                "lastMessagePreview" to "Bắt đầu đối thoại học tập...",
                "messageCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSessionMetadata(
        userId: String,
        sessionId: String,
        title: String? = null,
        mode: TutorMode? = null,
        modelTier: ModelTier? = null,
        linkedDocumentId: String? = null,
        linkedDocumentTitle: String? = null,
        lastMessagePreview: String? = null,
        messageCount: Long? = null
    ): Result<Unit> {
        val docPath = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID must not be blank." }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.let { updates["title"] = it.trim().ifBlank { "Phiên học AI" }.take(140) }
            mode?.let { updates["mode"] = it.name }
            modelTier?.let { updates["modelTier"] = it.name }
            linkedDocumentId?.let { updates["linkedDocumentId"] = it.take(128) }
            linkedDocumentTitle?.let { updates["linkedDocumentTitle"] = it.take(140) }
            lastMessagePreview?.let { updates["lastMessagePreview"] = it.take(300) }
            messageCount?.let { updates["messageCount"] = it.coerceIn(0L, 5000L) }

            db.collection("users").document(uid).collection("sessions").document(sessionId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun appendMessage(
        userId: String,
        sessionId: String,
        role: MessageRole,
        content: String,
        mode: TutorMode,
        modelUsed: String,
        hasAttachment: Boolean,
        attachmentLabel: String,
        hintChips: List<String>,
        newMessageCount: Long
    ): Result<String> {
        var msgPath = "users/$userId/sessions/$sessionId/messages"
        return try {
            val uid = requireUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID must not be blank." }
            val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
            val msgRef = sessionRef.collection("messages").document()
            msgPath = msgRef.path
            val safeContent = content.trim().ifBlank { "..." }.take(25_000)
            val safeChips = hintChips.map { it.trim().take(120) }.filter { it.isNotBlank() }.take(6)

            val msgPayload = mapOf(
                "userId" to uid,
                "role" to role.name,
                "content" to safeContent,
                "mode" to mode.name,
                "modelUsed" to modelUsed.take(64),
                "hasAttachment" to hasAttachment,
                "attachmentLabel" to attachmentLabel.take(140),
                "hintChips" to safeChips,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(msgPayload).await()

            sessionRef.update(
                mapOf(
                    "lastMessagePreview" to safeContent.replace("\n", " ").take(280),
                    "messageCount" to newMessageCount.coerceIn(0L, 5000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
        val path = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID must not be blank." }
            db.collection("users").document(uid).collection("sessions").document(sessionId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Flashcards ----

    fun observeFlashcards(userId: String): Flow<List<StudyFlashcard>> {
        val path = "users/$userId/flashcards"
        return db.collection("users").document(userId).collection("flashcards")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StudyFlashcard::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    suspend fun addFlashcardsBatch(
        userId: String,
        documentId: String,
        chapterNumber: Int,
        cards: List<FlashcardDraft>
    ): Result<Int> {
        val basePath = "users/$userId/flashcards"
        return try {
            val uid = requireUserId(userId)
            if (cards.isEmpty()) return Result.success(0)
            val batch = db.batch()
            val col = db.collection("users").document(uid).collection("flashcards")
            var count = 0
            for (draft in cards.take(20)) {
                val term = draft.frontTerm.trim().take(300)
                val def = draft.backDefinition.trim().take(2000)
                if (term.isBlank() || def.isBlank()) continue
                val docRef = col.document()
                val payload = mapOf(
                    "userId" to uid,
                    "documentId" to documentId.take(128),
                    "chapterNumber" to chapterNumber.coerceIn(0, 6).toLong(),
                    "frontTerm" to term,
                    "backDefinition" to def,
                    "formula" to draft.formula.trim().take(400),
                    "example" to draft.example.trim().take(1000),
                    "mastered" to false,
                    "reviewCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
                count++
            }
            if (count > 0) {
                batch.commit().await()
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    suspend fun updateFlashcardReview(
        userId: String,
        cardId: String,
        mastered: Boolean,
        nextReviewCount: Long
    ): Result<Unit> {
        val path = "users/$userId/flashcards/$cardId"
        return try {
            val uid = requireUserId(userId)
            require(cardId.isNotBlank()) { "Card ID cannot be blank." }
            db.collection("users").document(uid).collection("flashcards").document(cardId)
                .update(
                    mapOf(
                        "mastered" to mastered,
                        "reviewCount" to nextReviewCount.coerceIn(0L, 100_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---- Oral Exam Attempts ----

    fun observeOralAttempts(userId: String): Flow<List<OralExamAttempt>> {
        val path = "users/$userId/oralAttempts"
        return db.collection("users").document(userId).collection("oralAttempts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    OralExamAttempt::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    suspend fun saveOralAttempt(
        userId: String,
        chapterNumber: Int,
        documentId: String,
        question: String,
        userAnswer: String,
        score: Double,
        strengths: List<String>,
        gaps: List<String>,
        modelFeedback: String
    ): Result<String> {
        var path = "users/$userId/oralAttempts"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("oralAttempts").document()
            path = docRef.path
            val safeScore = score.coerceIn(0.0, 10.0)
            val payload = mapOf(
                "userId" to uid,
                "chapterNumber" to chapterNumber.coerceIn(0, 6).toLong(),
                "documentId" to documentId.take(128),
                "question" to question.trim().ifBlank { "Câu hỏi vấn đáp" }.take(2000),
                "userAnswer" to userAnswer.trim().ifBlank { "(Không có nội dung)" }.take(10_000),
                "score" to safeScore,
                "strengths" to strengths.map { it.trim().take(300) }.filter { it.isNotBlank() }.take(8),
                "gaps" to gaps.map { it.trim().take(300) }.filter { it.isNotBlank() }.take(8),
                "modelFeedback" to modelFeedback.trim().ifBlank { "Đã hoàn thành lượt vấn đáp." }.take(4000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            if (chapterNumber in 1..6) {
                recordChapterActivity(uid, chapterNumber, sessionDelta = 1L, oralScore = safeScore)
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ---- Chapter Mastery Progress ----

    fun observeChapterProgress(userId: String): Flow<List<ChapterProgress>> {
        val path = "users/$userId/chapterProgress"
        return db.collection("users").document(userId).collection("chapterProgress")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChapterProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    suspend fun recordChapterActivity(
        userId: String,
        chapterNumber: Int,
        sessionDelta: Long = 1L,
        oralScore: Double? = null
    ): Result<Unit> {
        if (chapterNumber !in 1..6) return Result.success(Unit)
        val docId = "chapter_$chapterNumber"
        val path = "users/$userId/chapterProgress/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("chapterProgress").document(docId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val prevSessions = existingSnap.getLong("sessionsCompleted") ?: 0L
                val prevBest = existingSnap.getDouble("oralBestScore") ?: 0.0
                val prevMastery = existingSnap.getLong("masteryPercent") ?: 0L
                val nextSessions = (prevSessions + sessionDelta).coerceIn(0L, 100_000L)
                val nextBest = if (oralScore != null) max(prevBest, oralScore.coerceIn(0.0, 10.0)) else prevBest
                val calculatedMastery = max(
                    prevMastery,
                    ((nextSessions * 12L) + (nextBest * 6.0).toLong()).coerceIn(0L, 100L)
                )
                docRef.update(
                    mapOf(
                        "sessionsCompleted" to nextSessions,
                        "oralBestScore" to nextBest,
                        "masteryPercent" to calculatedMastery,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val initialBest = oralScore?.coerceIn(0.0, 10.0) ?: 0.0
                val initialSessions = sessionDelta.coerceIn(0L, 100_000L)
                val initialMastery = ((initialSessions * 12L) + (initialBest * 6.0).toLong()).coerceIn(0L, 100L)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "chapterNumber" to chapterNumber.toLong(),
                        "sessionsCompleted" to initialSessions,
                        "oralBestScore" to initialBest,
                        "masteryPercent" to initialMastery,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

data class FlashcardDraft(
    val frontTerm: String,
    val backDefinition: String,
    val formula: String = "",
    val example: String = ""
)
