package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.AstraAiService
import com.example.data.model.TutorMode
import com.example.data.repository.StudyRepository
import com.example.firebase.authStateFlow
import com.example.firebase.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OralExamScreen
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.TutorScreen
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.OralExamViewModel
import com.example.ui.viewmodel.StudyHubViewModel
import com.example.ui.viewmodel.TutorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AstraTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HUB("Trang chủ", Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_hub"),
    TUTOR("Gia sư AI", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_tutor"),
    ORAL("Vấn đáp", Icons.Rounded.RecordVoiceOver, Icons.Outlined.RecordVoiceOver, "nav_tab_oral"),
    LIBRARY("Tài liệu", Icons.Rounded.LibraryBooks, Icons.Outlined.LibraryBooks, "nav_tab_library")
}

@Composable
fun AstraMacApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedAstraShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Học viên"
            )
        }
    }
}

@Composable
private fun AuthenticatedAstraShell(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val aiService = remember { AstraAiService() }

    val hubViewModel: StudyHubViewModel = viewModel(
        key = "hub_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val repo = StudyRepository(FirebaseFirestore.getInstance(dbId))
                StudyHubViewModel(repo, currentUserId)
            }
        }
    )

    val tutorViewModel: TutorViewModel = viewModel(
        key = "tutor_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val repo = StudyRepository(FirebaseFirestore.getInstance(dbId))
                TutorViewModel(repo, aiService, currentUserId)
            }
        }
    )

    val oralViewModel: OralExamViewModel = viewModel(
        key = "oral_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val repo = StudyRepository(FirebaseFirestore.getInstance(dbId))
                OralExamViewModel(repo, aiService, currentUserId)
            }
        }
    )

    val libraryViewModel: LibraryViewModel = viewModel(
        key = "library_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val repo = StudyRepository(FirebaseFirestore.getInstance(dbId))
                LibraryViewModel(repo, aiService, currentUserId)
            }
        }
    )

    var selectedTab by remember { mutableStateOf(AstraTab.HUB) }
    var openFlashcardsOnLibrary by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != AstraTab.HUB) {
        selectedTab = AstraTab.HUB
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        if (useNavRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    AstraTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (tab == AstraTab.LIBRARY) openFlashcardsOnLibrary = false
                                selectedTab = tab
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    TabContentHost(
                        selectedTab = selectedTab,
                        openFlashcardsOnLibrary = openFlashcardsOnLibrary,
                        hubViewModel = hubViewModel,
                        tutorViewModel = tutorViewModel,
                        oralViewModel = oralViewModel,
                        libraryViewModel = libraryViewModel,
                        userDisplayName = userDisplayName,
                        onSelectTab = { selectedTab = it },
                        onSetOpenFlashcards = { openFlashcardsOnLibrary = it },
                        onRequestSignOut = { showSignOutConfirm = true }
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AstraTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab == AstraTab.LIBRARY) openFlashcardsOnLibrary = false
                                    selectedTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabContentHost(
                        selectedTab = selectedTab,
                        openFlashcardsOnLibrary = openFlashcardsOnLibrary,
                        hubViewModel = hubViewModel,
                        tutorViewModel = tutorViewModel,
                        oralViewModel = oralViewModel,
                        libraryViewModel = libraryViewModel,
                        userDisplayName = userDisplayName,
                        onSelectTab = { selectedTab = it },
                        onSetOpenFlashcards = { openFlashcardsOnLibrary = it },
                        onRequestSignOut = { showSignOutConfirm = true }
                    )
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Đăng xuất tài khoản") },
            text = { Text("Dữ liệu tài liệu, Flashcard và lịch sử học của bạn đã được đồng bộ an toàn trên đám mây.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        signOut(credentialManager, scope)
                    }
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Ở lại")
                }
            }
        )
    }
}

@Composable
private fun TabContentHost(
    selectedTab: AstraTab,
    openFlashcardsOnLibrary: Boolean,
    hubViewModel: StudyHubViewModel,
    tutorViewModel: TutorViewModel,
    oralViewModel: OralExamViewModel,
    libraryViewModel: LibraryViewModel,
    userDisplayName: String,
    onSelectTab: (AstraTab) -> Unit,
    onSetOpenFlashcards: (Boolean) -> Unit,
    onRequestSignOut: () -> Unit
) {
    Crossfade(targetState = selectedTab, label = "astra_tab_crossfade") { tab ->
        when (tab) {
            AstraTab.HUB -> {
                StudyHubScreen(
                    viewModel = hubViewModel,
                    userDisplayName = userDisplayName,
                    onLaunchTutor = { chapter, mode, prefill ->
                        tutorViewModel.startNewSession(chapterNumber = chapter, mode = mode)
                        onSelectTab(AstraTab.TUTOR)
                        if (!prefill.isNullOrBlank()) {
                            tutorViewModel.sendMessage(prefill)
                        }
                    },
                    onLaunchOral = { chapter ->
                        oralViewModel.setChapter(chapter)
                        onSelectTab(AstraTab.ORAL)
                    },
                    onOpenLibrary = { flashcards ->
                        onSetOpenFlashcards(flashcards)
                        onSelectTab(AstraTab.LIBRARY)
                    },
                    onSignOutClick = onRequestSignOut
                )
            }
            AstraTab.TUTOR -> {
                TutorScreen(viewModel = tutorViewModel)
            }
            AstraTab.ORAL -> {
                OralExamScreen(viewModel = oralViewModel)
            }
            AstraTab.LIBRARY -> {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    initialShowFlashcards = openFlashcardsOnLibrary,
                    onStudyWithDocument = { doc ->
                        tutorViewModel.startNewSession(
                            chapterNumber = doc.chapterNumber.toInt(),
                            mode = TutorMode.SOCRATIC,
                            document = doc
                        )
                        onSelectTab(AstraTab.TUTOR)
                    }
                )
            }
        }
    }
}
