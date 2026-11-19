package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OralExamAttempt
import com.example.data.model.UiState
import com.example.ui.components.AstraBreathingOrb
import com.example.ui.components.AstraOrbState
import com.example.ui.components.AstraVoiceWaveform
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ScholarAmber
import com.example.ui.viewmodel.OralExamPhase
import com.example.ui.viewmodel.OralExamViewModel
import java.util.Locale

@Composable
fun OralExamScreen(
    viewModel: OralExamViewModel
) {
    val context = LocalContext.current
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val linkedDocId by viewModel.linkedDocumentId.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val attemptsState by viewModel.attemptsState.collectAsStateWithLifecycle()

    val docs = (documentsState as? UiState.Success)?.data.orEmpty()
    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()

    var answerDraft by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val viLocale = Locale.forLanguageTag("vi-VN")
                val res = instance?.setLanguage(viLocale)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        instance
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
            speechRecognizer?.destroy()
        }
    }

    fun startVoiceCapture() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            Toast.makeText(
                context,
                "Thiết bị chưa hỗ trợ nhận diện giọng nói. Bạn hãy nhập câu trả lời bằng văn bản bên dưới.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(
                    context,
                    "Không nghe rõ giọng nói (mã $error). Bạn có thể thử lại hoặc nhập chữ.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    answerDraft = if (answerDraft.isBlank()) spoken else "$answerDraft $spoken"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (partial.isNotBlank() && answerDraft.isBlank()) {
                    answerDraft = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            Toast.makeText(
                context,
                "Cần quyền Micro để trả lời vấn đáp bằng giọng nói. Bạn vẫn có thể gõ câu trả lời.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(phase) {
        if (phase is OralExamPhase.QuestionReady) {
            answerDraft = ""
        }
    }

    val orbState = when {
        isListening -> AstraOrbState.LISTENING
        isSpeaking -> AstraOrbState.SPEAKING
        phase is OralExamPhase.GeneratingQuestion || phase is OralExamPhase.Grading -> AstraOrbState.THINKING
        else -> AstraOrbState.IDLE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Phòng Luyện thi Vấn đáp AI",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Trả lời bằng giọng nói hoặc văn bản, giảng viên Gemini chấm theo thang 10 và chỉ rõ lỗ hổng kiến thức.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Context Filter Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedChapter == 0,
                            onClick = { viewModel.setChapter(0) },
                            label = { Text("Tổng hợp 6 chương") }
                        )
                        (1..6).forEach { ch ->
                            FilterChip(
                                selected = selectedChapter == ch,
                                onClick = { viewModel.setChapter(ch) },
                                label = { Text("Chương $ch") }
                            )
                        }
                    }

                    if (docs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = linkedDocId.isBlank(),
                                onClick = { viewModel.linkDocument("") },
                                label = { Text("Không gắn tài liệu") }
                            )
                            docs.forEach { doc ->
                                FilterChip(
                                    selected = linkedDocId == doc.id,
                                    onClick = { viewModel.linkDocument(doc.id) },
                                    label = {
                                        Text(
                                            text = doc.title.take(24),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Center Astra AI Oral Orb Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AstraBreathingOrb(state = orbState, size = 124.dp)
                        AstraVoiceWaveform(
                            isActive = isListening || isSpeaking || phase is OralExamPhase.Grading,
                            color = if (isListening) ScholarAmber else MaterialTheme.colorScheme.primary
                        )

                        when (val current = phase) {
                            OralExamPhase.Idle -> {
                                Text(
                                    text = "Sẵn sàng kiểm tra vấn đáp",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Button(
                                    onClick = { viewModel.generateNewQuestion() },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("oral_start_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bắt đầu câu hỏi vấn đáp")
                                }
                            }
                            OralExamPhase.GeneratingQuestion -> {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Giảng viên AI đang bốc đề phù hợp...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            is OralExamPhase.QuestionReady -> {
                                OralQuestionBox(
                                    question = current.question,
                                    ttsReady = ttsReady,
                                    onSpeakClick = {
                                        isSpeaking = true
                                        tts.speak(current.question, TextToSpeech.QUEUE_FLUSH, null, "oral_q")
                                    }
                                )

                                OutlinedTextField(
                                    value = answerDraft,
                                    onValueChange = { answerDraft = it },
                                    label = { Text("Câu trả lời của bạn") },
                                    placeholder = { Text("Bấm Micro để nói hoặc gõ luận điểm trả lời...") },
                                    minLines = 3,
                                    maxLines = 6,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("oral_answer_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (isListening) {
                                                speechRecognizer?.stopListening()
                                                isListening = false
                                            } else {
                                                val granted = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (granted) {
                                                    startVoiceCapture()
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("oral_mic_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                            contentDescription = if (isListening) "Dừng thu âm" else "Trả lời bằng giọng nói"
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isListening) "Dừng nói" else "Nói")
                                    }

                                    Button(
                                        onClick = { viewModel.submitAnswer(answerDraft) },
                                        enabled = answerDraft.isNotBlank(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("oral_submit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nộp & Chấm điểm")
                                    }
                                }
                            }
                            is OralExamPhase.Grading -> {
                                Text(
                                    text = current.question,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Đang chấm điểm theo thang tiêu chí Mác - Lênin...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            is OralExamPhase.Graded -> {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                    )
                                ) {
                                    OralResultCard(
                                        phase = current,
                                        onNextQuestion = { viewModel.generateNewQuestion() }
                                    )
                                }
                            }
                            is OralExamPhase.Error -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = current.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(14.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.generateNewQuestion() },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Thử lại")
                                }
                            }
                        }
                    }
                }
            }

            // Recent Oral Exam History
            if (attempts.isNotEmpty()) {
                item {
                    Text(
                        text = "Lịch sử chấm thi gần đây",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(attempts.take(8), key = { it.id }) { attempt ->
                    OralAttemptHistoryRow(attempt = attempt)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OralQuestionBox(
    question: String,
    ttsReady: Boolean,
    onSpeakClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CÂU HỎI VẤN ĐÁP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (ttsReady) {
                IconButton(
                    onClick = onSpeakClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Nghe đọc câu hỏi",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OralResultCard(
    phase: OralExamPhase.Graded,
    onNextQuestion: () -> Unit
) {
    val score = phase.result.score
    val scoreColor = when {
        score >= 8.0 -> EmeraldMastery
        score >= 6.0 -> ScholarAmber
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = scoreColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "KẾT QUẢ CHẤM THI AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor
                    )
                    Text(
                        text = when {
                            score >= 8.5 -> "Xuất sắc • Nắm vững bản chất"
                            score >= 7.0 -> "Khá tốt • Lập luận mạch lạc"
                            score >= 5.0 -> "Đạt yêu cầu • Cần đào sâu thêm"
                            else -> "Cần ôn tập lại trọng tâm"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${"%.1f".format(score)}/10",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = scoreColor
                )
            }
        }

        if (phase.result.strengths.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Điểm mạnh",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldMastery
                )
                phase.result.strengths.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldMastery,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Text(text = item, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (phase.result.gaps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ý cần bổ sung",
                    style = MaterialTheme.typography.titleSmall,
                    color = ScholarAmber
                )
                phase.result.gaps.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = ScholarAmber,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Text(text = item, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text(
            text = phase.result.feedback,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Button(
            onClick = onNextQuestion,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("oral_next_question_button")
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Câu hỏi vấn đáp tiếp theo")
        }
    }
}

@Composable
private fun OralAttemptHistoryRow(attempt: OralExamAttempt) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${"%.1f".format(attempt.score)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.question,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attempt.modelFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
