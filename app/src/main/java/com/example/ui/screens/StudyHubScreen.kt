package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TutorMode
import com.example.data.model.UiState
import com.example.domain.ChapterCurriculum
import com.example.domain.PoliticalEconomyCurriculum
import com.example.ui.components.AnimatedMasteryRing
import com.example.ui.components.AstraBreathingOrb
import com.example.ui.components.AstraOrbState
import com.example.ui.theme.FormulaTextStyle
import com.example.ui.viewmodel.StudyHubSummary
import com.example.ui.viewmodel.StudyHubViewModel

@Composable
fun StudyHubScreen(
    viewModel: StudyHubViewModel,
    userDisplayName: String,
    onLaunchTutor: (chapterNumber: Int, mode: TutorMode, prefillPrompt: String?) -> Unit,
    onLaunchOral: (chapterNumber: Int) -> Unit,
    onOpenLibrary: (openFlashcards: Boolean) -> Unit,
    onSignOutClick: () -> Unit
) {
    val hubState by viewModel.hubState.collectAsStateWithLifecycle()
    var listEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        listEntered = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Xin chào, ${userDisplayName.ifBlank { "Học viên" }}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kinh tế chính trị Mác - Lênin • Astra AI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSignOutClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hero Mastery Card
            item {
                when (val state = hubState) {
                    is UiState.Loading -> {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        HeroMasteryCard(summary = state.data)
                    }
                }
            }

            // Astra AI 4 Mode Launchpad
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mô hình học Astra AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AstraModeCard(
                            title = "Gia sư Socratic",
                            subtitle = "Gợi mở từng bước",
                            icon = Icons.Rounded.Psychology,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_socratic_card"),
                            onClick = { onLaunchTutor(3, TutorMode.SOCRATIC, null) }
                        )
                        AstraModeCard(
                            title = "Giải nhanh AI",
                            subtitle = "Ảnh & Công thức m'",
                            icon = Icons.Rounded.Bolt,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_solver_card"),
                            onClick = { onLaunchTutor(3, TutorMode.SOLVER, null) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AstraModeCard(
                            title = "Luyện Vấn đáp",
                            subtitle = "Chấm điểm thang 10",
                            icon = Icons.Rounded.RecordVoiceOver,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_oral_card"),
                            onClick = { onLaunchOral(3) }
                        )
                        AstraModeCard(
                            title = "Tài liệu & Thẻ",
                            subtitle = "PDF & Flashcard 3D",
                            icon = Icons.Rounded.Style,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_library_card"),
                            onClick = { onOpenLibrary(false) }
                        )
                    }
                }
            }

            // 6-Chapter Roadmap
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lộ trình 6 chương chuẩn",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Giáo trình ĐH",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            itemsIndexed(
                items = PoliticalEconomyCurriculum.chapters,
                key = { _, chapter -> chapter.number }
            ) { index, chapter ->
                val summary = (hubState as? UiState.Success)?.data
                val progress = summary?.chapterProgress?.get(chapter.number)
                AnimatedVisibility(
                    visible = listEntered,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { (index + 1) * 28 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    ChapterRoadmapCard(
                        chapter = chapter,
                        masteryPercent = progress?.masteryPercent?.toInt() ?: 0,
                        sessionsCompleted = progress?.sessionsCompleted?.toInt() ?: 0,
                        bestOralScore = progress?.oralBestScore ?: 0.0,
                        onAskSocratic = {
                            onLaunchTutor(chapter.number, TutorMode.SOCRATIC, chapter.sampleSocraticPrompt)
                        },
                        onQuickSolve = {
                            onLaunchTutor(chapter.number, TutorMode.SOLVER, chapter.sampleSolverPrompt)
                        },
                        onStartOral = {
                            onLaunchOral(chapter.number)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HeroMasteryCard(summary: StudyHubSummary) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AstraBreathingOrb(state = AstraOrbState.IDLE, size = 86.dp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tiến độ làm chủ học phần",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.documentCount} tài liệu • ${summary.masteredCards}/${summary.totalCards} thẻ thuộc • ${summary.oralCount} lượt vấn đáp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summary.oralCount > 0) {
                        Text(
                            text = "Điểm vấn đáp TB: ${"%.1f".format(summary.averageOralScore)}/10",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedMasteryRing(
                    percent = summary.overallMasteryPercent,
                    size = 72.dp,
                    strokeWidth = 7.dp
                )
            }
        }
    }
}

@Composable
private fun AstraModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChapterRoadmapCard(
    chapter: ChapterCurriculum,
    masteryPercent: Int,
    sessionsCompleted: Int,
    bestOralScore: Double,
    onAskSocratic: () -> Unit,
    onQuickSolve: () -> Unit,
    onStartOral: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (masteryPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chapter_progress_${chapter.number}"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.number}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "CHƯƠNG ${chapter.number}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = if (bestOralScore > 0.0) {
                        "Đã học $sessionsCompleted phiên • Vấn đáp ${"%.1f".format(bestOralScore)}đ"
                    } else {
                        "Đã học $sessionsCompleted phiên"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = chapter.shortTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = chapter.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Key formulas / concepts strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chapter.keyFormulas.take(3).forEach { formula ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = formula,
                            style = FormulaTextStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onAskSocratic,
                    modifier = Modifier.testTag("chapter_${chapter.number}_socratic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Socratic")
                }
                TextButton(
                    onClick = onQuickSolve,
                    modifier = Modifier.testTag("chapter_${chapter.number}_solver_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bài tập")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onStartOral,
                    modifier = Modifier.testTag("chapter_${chapter.number}_oral_btn")
                ) {
                    Text("Vấn đáp")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
