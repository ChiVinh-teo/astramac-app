package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ModelTier
import com.example.data.model.StudyDocument
import com.example.data.model.TutorMessage
import com.example.data.model.TutorMode
import com.example.data.repository.FlashcardDraft
import com.example.domain.PoliticalEconomyCurriculum
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AnalyzedDocumentResult(
    val title: String,
    val detectedChapter: Int,
    val extractedText: String,
    val summary: String,
    val keyConcepts: List<String>,
    val flashcards: List<FlashcardDraft>
)

data class OralGradingResult(
    val score: Double,
    val strengths: List<String>,
    val gaps: List<String>,
    val feedback: String
)

class AstraAiService {
    companion object {
        private const val TAG = "AstraAiService"
        private const val MODEL_FLASH = "gemini-3.8-flash"
        private const val MODEL_ARGON = "gemini-4-argon"
        private const val MODEL_LITE = "gemini-3.5-flash-lite"
        private const val AI_TIMEOUT_MS = 60_000L
    }

    // Structured Schemas
    private val hintChipsSchema = Schema.obj(
        properties = mapOf(
            "hints" to Schema.array(
                items = Schema.string(description = "Câu hỏi hoặc câu trả lời gợi ý ngắn gọn bằng tiếng Việt (tối đa 10 từ)"),
                description = "Danh sách 3 gợi ý tiếp theo cho sinh viên"
            )
        )
    )

    private val documentAnalysisSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Tiêu đề học thuật ngắn gọn cho tài liệu"),
            "detectedChapter" to Schema.integer(description = "Số chương phù hợp nhất từ 1 đến 6 theo giáo trình Kinh tế chính trị Mác-Lênin, hoặc 0 nếu tổng hợp nhiều chương"),
            "extractedText" to Schema.string(description = "Nội dung cốt lõi trích xuất từ tài liệu (tối đa 12000 ký tự)"),
            "summary" to Schema.string(description = "Tóm tắt có cấu trúc rõ ràng bằng tiếng Việt các luận điểm chính và công thức"),
            "keyConcepts" to Schema.array(
                items = Schema.string(description = "Khái niệm / phạm trù kinh tế chính trị trọng tâm"),
                description = "Tối đa 10 khái niệm cốt lõi"
            ),
            "flashcards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "frontTerm" to Schema.string(description = "Thuật ngữ hoặc câu hỏi ngắn ở mặt trước"),
                        "backDefinition" to Schema.string(description = "Định nghĩa, bản chất kinh tế chính trị ở mặt sau"),
                        "formula" to Schema.string(description = "Công thức toán học/kinh tế nếu có, hoặc chuỗi rỗng"),
                        "example" to Schema.string(description = "Ví dụ minh họa ngắn gọn sát thực tiễn Việt Nam")
                    )
                ),
                description = "Từ 4 đến 8 thẻ Flashcard chất lượng cao rút ra từ tài liệu"
            )
        )
    )

    private val oralGradingSchema = Schema.obj(
        properties = mapOf(
            "score" to Schema.double(description = "Điểm số trên thang 10.0 (ví dụ 8.5)"),
            "strengths" to Schema.array(
                items = Schema.string(description = "Luận điểm sinh viên đã trả lời đúng"),
                description = "Từ 1 đến 4 điểm mạnh"
            ),
            "gaps" to Schema.array(
                items = Schema.string(description = "Ý còn thiếu, nhầm lẫn khái niệm hoặc thiếu liên hệ thực tiễn"),
                description = "Từ 0 đến 4 điểm cần bổ sung"
            ),
            "feedback" to Schema.string(description = "Nhận xét tổng hợp mang tính sư phạm như giảng viên đại học, chỉ rõ cách nâng điểm")
        )
    )

    private val liteHintsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_LITE,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = hintChipsSchema
            }
        )
    }

    private val documentAnalyzerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = documentAnalysisSchema
            },
            systemInstruction = content {
                text(
                    "Bạn là trợ lý học thuật chuyên ngành Kinh tế chính trị Mác - Lênin theo giáo trình chuẩn đại học Việt Nam (6 chương). " +
                        "Hãy phân tích chính xác nội dung tài liệu được cung cấp, giữ nguyên các công thức kinh tế (c, v, m, m', M, p', W = c + v + m), " +
                        "trích xuất khái niệm cốt lõi và tạo bộ Flashcard ôn thi súc tích bằng tiếng Việt."
                )
            }
        )
    }

    private val oralQuestionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            systemInstruction = content {
                text(
                    "Bạn là giảng viên đại học môn Kinh tế chính trị Mác - Lênin đang hỏi thi vấn đáp sinh viên (phong cách Astra AI Oral Exam). " +
                        "Chỉ đưa ra DUY NHẤT 1 câu hỏi vấn đáp rõ ràng, có chiều sâu tư duy (kết hợp lý luận và liên hệ thực tiễn hoặc bài tập tính toán ngắn), " +
                        "không trả lời hộ sinh viên, không thêm lời chào dài dòng. Độ dài 1-3 câu."
                )
            }
        )
    }

    private val oralGraderModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = oralGradingSchema
            },
            systemInstruction = content {
                text(
                    "Bạn là giảng viên chấm thi vấn đáp môn Kinh tế chính trị Mác - Lênin. " +
                        "Đánh giá câu trả lời của sinh viên công tâm trên thang 10.0 dựa trên: " +
                        "(1) Độ chính xác khái niệm/quy luật Mác-Lênin, (2) Lập luận biện chứng & công thức (nếu có), (3) Liên hệ thực tiễn Việt Nam."
                )
            }
        )
    }

    private fun buildTutorSystemInstruction(
        mode: TutorMode,
        chapterNumber: Int,
        linkedDocument: StudyDocument?
    ): Content {
        val chapterContext = PoliticalEconomyCurriculum.byNumber(chapterNumber)?.let { ch ->
            "Ngữ cảnh chương hiện tại: ${ch.title}. Chủ đề trọng tâm: ${ch.coreTopics.joinToString("; ")}. Công thức chính: ${ch.keyFormulas.joinToString(", ")}."
        } ?: "Ngữ cảnh: Toàn bộ 6 chương giáo trình Kinh tế chính trị Mác - Lênin."

        val docContext = linkedDocument?.let { doc ->
            "\n\nTài liệu sinh viên đang mở (${doc.title}):\nTóm tắt: ${doc.summary}\nNội dung tham chiếu:\n${doc.contentText.take(6000)}"
        }.orEmpty()

        val roleInstruction = when (mode) {
            TutorMode.SOCRATIC ->
                "Bạn là 'Bậc thầy AI AstraMác' dạy theo phương pháp Gia sư Socratic (giống Astra AI Socratic Mode) cho môn Kinh tế chính trị Mác - Lênin. " +
                    "QUY TẮC CỐT LÕI: KHÔNG bao giờ đưa ngay toàn bộ đáp án cuối cùng ở lượt đầu nếu sinh viên đang giải bài hoặc hỏi khái niệm. " +
                    "Thay vào đó: (1) Ghi nhận đúng/sai trong lập luận hiện tại của sinh viên một cách khích lệ, " +
                    "(2) Giải thích ngắn gọn 1 mắt xích lý luận hoặc 1 công thức gợi ý, " +
                    "(3) Kết thúc bằng 1-2 câu hỏi dẫn dắt sắc bén để sinh viên tự rút ra kết luận tiếp theo. " +
                    "Nếu sinh viên đã suy luận đúng hết hoặc yêu cầu chốt lại, hãy tổng kết chuẩn xác."
            TutorMode.SOLVER ->
                "Bạn là 'Bậc thầy AI AstraMác' ở chế độ Giải nhanh (giống Astra AI Solver Mode) cho môn Kinh tế chính trị Mác - Lênin. " +
                    "Hãy trình bày lời giải mạch lạc, chuẩn học thuật: " +
                    "(1) Xác định phạm trù/quy luật hoặc tóm tắt dữ kiện (c, v, m, m', c/v, thời gian lao động...), " +
                    "(2) Nêu công thức rõ ràng, " +
                    "(3) Tính toán hoặc lập luận từng bước minh bạch, " +
                    "(4) Chốt kết quả & ý nghĩa kinh tế chính trị."
        }

        return content {
            text("$roleInstruction\n\n$chapterContext$docContext\n\nLuôn trả lời bằng tiếng Việt chuẩn mực, dễ hiểu, trình bày đẹp với gạch đầu dòng rõ ràng.")
        }
    }

    fun streamTutorResponse(
        mode: TutorMode,
        modelTier: ModelTier,
        chapterNumber: Int,
        linkedDocument: StudyDocument?,
        history: List<TutorMessage>,
        userPrompt: String,
        attachedImage: Bitmap? = null
    ): Flow<String> = flow {
        val model = Firebase.ai.generativeModel(
            modelName = modelTier.modelId,
            systemInstruction = buildTutorSystemInstruction(mode, chapterNumber, linkedDocument)
        )

        val chatHistory = history.takeLast(12).map { msg ->
            val roleStr = if (msg.role == "USER") "user" else "model"
            content(roleStr) { text(msg.content) }
        }

        val userContent = content("user") {
            if (attachedImage != null) {
                image(attachedImage)
            }
            text(userPrompt)
        }

        val chat = model.startChat(history = chatHistory)
        val accumulated = StringBuilder()

        withTimeout(AI_TIMEOUT_MS) {
            chat.sendMessageStream(userContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
    }

    suspend fun generateFollowUpHints(
        mode: TutorMode,
        lastAssistantMessage: String
    ): List<String> {
        return try {
            val prompt = if (mode == TutorMode.SOCRATIC) {
                "Dựa trên câu hỏi gợi mở sau của gia sư Kinh tế chính trị Mác-Lênin, hãy tạo 3 lựa chọn trả lời ngắn gọn mà sinh viên có thể bấm để đáp lại gia sư:\n\n$lastAssistantMessage"
            } else {
                "Dựa trên lời giải sau, hãy tạo 3 câu hỏi đào sâu ngắn gọn mà sinh viên nên hỏi tiếp:\n\n$lastAssistantMessage"
            }
            val response = withTimeout(30_000L) {
                liteHintsModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: return emptyList())
            val arr = json.optJSONArray("hints") ?: return emptyList()
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).trim().takeIf { it.isNotBlank() }?.take(90)
            }.take(3)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Hint chip generation skipped: ${e.message}")
            emptyList()
        }
    }

    suspend fun analyzeTextDocument(
        titleHint: String,
        rawText: String,
        chapterHint: Int
    ): AnalyzedDocumentResult {
        val response = withTimeout(AI_TIMEOUT_MS) {
            documentAnalyzerModel.generateContent(
                "Tên gợi ý: $titleHint\nChương gợi ý (0 nếu chưa rõ): $chapterHint\n\nNội dung tài liệu:\n${rawText.take(35_000)}"
            )
        }
        return parseAnalyzedDocument(response.text, fallbackTitle = titleHint, fallbackText = rawText)
    }

    suspend fun analyzePdfDocument(
        fileName: String,
        pdfBytes: ByteArray,
        chapterHint: Int
    ): AnalyzedDocumentResult {
        val response = withTimeout(AI_TIMEOUT_MS) {
            documentAnalyzerModel.generateContent(
                content {
                    inlineData(pdfBytes, "application/pdf")
                    text(
                        "Hãy đọc toàn bộ tài liệu PDF '$fileName' (chương gợi ý: $chapterHint). " +
                            "Trích xuất nội dung chính, tóm tắt trọng tâm ôn thi môn Kinh tế chính trị Mác - Lênin, liệt kê khái niệm và tạo 5-8 Flashcard."
                    )
                }
            )
        }
        return parseAnalyzedDocument(response.text, fallbackTitle = fileName, fallbackText = "")
    }

    suspend fun analyzeImageDocument(
        fileName: String,
        bitmap: Bitmap,
        chapterHint: Int
    ): AnalyzedDocumentResult {
        val response = withTimeout(AI_TIMEOUT_MS) {
            documentAnalyzerModel.generateContent(
                content {
                    image(bitmap)
                    text(
                        "Hãy đọc trang sách/slide/vở ghi '$fileName' (chương gợi ý: $chapterHint). " +
                            "Chép lại nội dung quan trọng, tóm tắt logic Kinh tế chính trị Mác - Lênin, trích xuất khái niệm và tạo 4-6 Flashcard."
                    )
                }
            )
        }
        return parseAnalyzedDocument(response.text, fallbackTitle = fileName, fallbackText = "")
    }

    private fun parseAnalyzedDocument(
        jsonString: String?,
        fallbackTitle: String,
        fallbackText: String
    ): AnalyzedDocumentResult {
        if (jsonString.isNullOrBlank()) {
            throw IllegalStateException("Gemini trả về kết quả phân tích tài liệu rỗng.")
        }
        val json = JSONObject(jsonString)
        val title = json.optString("title").ifBlank { fallbackTitle }.take(140)
        val chapter = json.optInt("detectedChapter", 0).coerceIn(0, 6)
        val extracted = json.optString("extractedText").ifBlank { fallbackText }.take(40_000)
        val summary = json.optString("summary").ifBlank { "Đã phân tích tài liệu." }.take(8000)

        val conceptsArr = json.optJSONArray("keyConcepts")
        val concepts = if (conceptsArr != null) {
            (0 until conceptsArr.length()).mapNotNull {
                conceptsArr.optString(it).trim().takeIf { s -> s.isNotBlank() }
            }.take(15)
        } else emptyList()

        val cardsArr = json.optJSONArray("flashcards")
        val flashcards = if (cardsArr != null) {
            (0 until cardsArr.length()).mapNotNull { idx ->
                val obj = cardsArr.optJSONObject(idx) ?: return@mapNotNull null
                val front = obj.optString("frontTerm").trim()
                val back = obj.optString("backDefinition").trim()
                if (front.isBlank() || back.isBlank()) null
                else FlashcardDraft(
                    frontTerm = front,
                    backDefinition = back,
                    formula = obj.optString("formula").trim(),
                    example = obj.optString("example").trim()
                )
            }
        } else emptyList()

        return AnalyzedDocumentResult(
            title = title,
            detectedChapter = chapter,
            extractedText = extracted,
            summary = summary,
            keyConcepts = concepts,
            flashcards = flashcards
        )
    }

    suspend fun generateOralQuestion(
        chapterNumber: Int,
        linkedDocument: StudyDocument?,
        previousQuestions: List<String>
    ): String {
        val chInfo = PoliticalEconomyCurriculum.byNumber(chapterNumber)?.let {
            "Chủ đề: ${it.title}. Các ý trọng tâm: ${it.coreTopics.joinToString("; ")}"
        } ?: "Tổng hợp toàn bộ giáo trình Kinh tế chính trị Mác - Lênin (Chương 1–6)."

        val docInfo = linkedDocument?.let {
            "\nBám sát thêm tài liệu cá nhân '${it.title}':\n${it.summary}\n${it.contentText.take(3000)}"
        }.orEmpty()

        val avoid = if (previousQuestions.isNotEmpty()) {
            "\nTránh trùng lặp với các câu hỏi trước đó: ${previousQuestions.take(5).joinToString(" | ")}"
        } else ""

        val response = withTimeout(AI_TIMEOUT_MS) {
            oralQuestionModel.generateContent(
                "Hãy đặt 1 câu hỏi thi vấn đáp.\n$chInfo$docInfo$avoid"
            )
        }
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Không nhận được câu hỏi vấn đáp từ Gemini.")
    }

    suspend fun gradeOralAnswer(
        chapterNumber: Int,
        linkedDocument: StudyDocument?,
        question: String,
        userAnswer: String
    ): OralGradingResult {
        val docRef = linkedDocument?.let { "\nNgữ cảnh tài liệu gốc (${it.title}): ${it.summary}" }.orEmpty()
        val chTitle = PoliticalEconomyCurriculum.byNumber(chapterNumber)?.title ?: "Kinh tế chính trị Mác - Lênin tổng hợp"

        val response = withTimeout(AI_TIMEOUT_MS) {
            oralGraderModel.generateContent(
                "Học phần: $chTitle$docRef\n\nCâu hỏi vấn đáp: $question\n\nCâu trả lời của sinh viên: $userAnswer"
            )
        }
        val jsonString = response.text ?: throw IllegalStateException("Kết quả chấm điểm rỗng.")
        val json = JSONObject(jsonString)
        val score = json.optDouble("score", 5.0).coerceIn(0.0, 10.0)
        val strengthsArr = json.optJSONArray("strengths")
        val strengths = if (strengthsArr != null) {
            (0 until strengthsArr.length()).mapNotNull {
                strengthsArr.optString(it).trim().takeIf { s -> s.isNotBlank() }
            }
        } else emptyList()
        val gapsArr = json.optJSONArray("gaps")
        val gaps = if (gapsArr != null) {
            (0 until gapsArr.length()).mapNotNull {
                gapsArr.optString(it).trim().takeIf { s -> s.isNotBlank() }
            }
        } else emptyList()
        val feedback = json.optString("feedback").ifBlank { "Đã chấm điểm lượt thi vấn đáp." }

        return OralGradingResult(
            score = score,
            strengths = strengths,
            gaps = gaps,
            feedback = feedback
        )
    }
}
