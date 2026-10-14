package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.repository.FokusRepository
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.localization.getFokusStrings
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BlockerScreen
import com.example.ui.screens.BulkChapterEntryScreen
import com.example.ui.screens.ChapterDetailScreen
import com.example.ui.screens.DailyStudyJournalScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.admission.AdmissionCommandCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import com.example.ui.theme.FokusTheme
import kotlinx.coroutines.launch

enum class MainNavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SYLLABUS("Syllabus", Icons.Default.MenuBook),
    FOCUS("Focus", Icons.Default.Timer),
    BLOCKER("Blocker", Icons.Default.Shield),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FokusApplication
        val repository = app.repository
        val preferencesManager = app.preferencesManager

        setContent {
            val userPrefs by preferencesManager.preferencesFlow.collectAsState(initial = UserPreferences())
            val scope = rememberCoroutineScope()

            val isDark = when (userPrefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val currentStrings = getFokusStrings(userPrefs.language)

            CompositionLocalProvider(LocalFokusStrings provides currentStrings) {
                FokusTheme(darkTheme = isDark) {
                    if (!userPrefs.onboardingCompleted) {
                        OnboardingScreen(
                            onComplete = { goal, targetMinutes ->
                                scope.launch {
                                    preferencesManager.setDailyTargetMinutes(targetMinutes)
                                    preferencesManager.setOnboardingCompleted(true)
                                }
                            }
                        )
                    } else {
                        MainAppContent(
                            repository = repository,
                            preferencesManager = preferencesManager,
                            initialNavigateToAdmission = intent?.getStringExtra("navigate_to") == "admission"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    repository: FokusRepository,
    preferencesManager: PreferencesManager,
    initialNavigateToAdmission: Boolean = false
) {
    val strings = LocalFokusStrings.current

    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }

    // Secondary screen states
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var bulkAddSubjectId by remember { mutableStateOf<String?>(null) }
    var isBulkAddActive by remember { mutableStateOf(false) }
    var isPlannerActive by remember { mutableStateOf(false) }
    var isRevisionQueueActive by remember { mutableStateOf(false) }
    var isDailyJournalActive by remember { mutableStateOf(false) }
    var isAdmissionActive by remember { mutableStateOf(initialNavigateToAdmission) }
    var focusTargetChapterId by remember { mutableStateOf<String?>(null) }

    // Android System Back handling
    BackHandler(enabled = isBulkAddActive || selectedChapterId != null || isPlannerActive || isRevisionQueueActive || isDailyJournalActive || isAdmissionActive || currentTab != MainNavTab.HOME) {
        when {
            isAdmissionActive -> isAdmissionActive = false
            isBulkAddActive -> isBulkAddActive = false
            selectedChapterId != null -> selectedChapterId = null
            isPlannerActive -> isPlannerActive = false
            isRevisionQueueActive -> isRevisionQueueActive = false
            isDailyJournalActive -> isDailyJournalActive = false
            currentTab != MainNavTab.HOME -> currentTab = MainNavTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isBulkAddActive && selectedChapterId == null && !isPlannerActive && !isRevisionQueueActive && !isDailyJournalActive && !isAdmissionActive) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    listOf(
                        MainNavTab.HOME,
                        MainNavTab.SYLLABUS,
                        MainNavTab.FOCUS,
                        MainNavTab.BLOCKER,
                        MainNavTab.ANALYTICS,
                        MainNavTab.SETTINGS
                    ).forEach { tab ->
                        val label = when (tab) {
                            MainNavTab.HOME -> strings.navHome
                            MainNavTab.SYLLABUS -> strings.navSyllabus
                            MainNavTab.FOCUS -> strings.navFocus
                            MainNavTab.BLOCKER -> "Blocker"
                            MainNavTab.ANALYTICS -> strings.navAnalytics
                            MainNavTab.SETTINGS -> strings.navSettings
                        }

                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = { Icon(imageVector = tab.icon, contentDescription = label) },
                            label = { Text(text = label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FokusOnLime,
                                selectedTextColor = FokusElectricLime,
                                indicatorColor = FokusElectricLime,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isAdmissionActive -> {
                    AdmissionCommandCenterScreen(
                        repository = repository,
                        preferencesManager = preferencesManager,
                        onBack = { isAdmissionActive = false }
                    )
                }
                isDailyJournalActive -> {
                    DailyStudyJournalScreen(
                        repository = repository,
                        onBack = { isDailyJournalActive = false }
                    )
                }
                isBulkAddActive -> {
                    BulkChapterEntryScreen(
                        repository = repository,
                        preselectedSubjectId = bulkAddSubjectId,
                        onBack = { isBulkAddActive = false }
                    )
                }
                selectedChapterId != null -> {
                    ChapterDetailScreen(
                        chapterId = selectedChapterId!!,
                        repository = repository,
                        onBack = { selectedChapterId = null },
                        onStartFocus = { chId ->
                            focusTargetChapterId = chId
                            selectedChapterId = null
                            currentTab = MainNavTab.FOCUS
                        }
                    )
                }
                isPlannerActive -> {
                    PlannerScreen(
                        repository = repository,
                        preferencesManager = preferencesManager,
                        onStartFocus = { chId ->
                            focusTargetChapterId = chId
                            isPlannerActive = false
                            currentTab = MainNavTab.FOCUS
                        },
                        onNavigateToJournal = { isDailyJournalActive = true }
                    )
                }
                isRevisionQueueActive -> {
                    RevisionScreen(
                        repository = repository,
                        preferencesManager = preferencesManager,
                        onStartFocus = { chId ->
                            focusTargetChapterId = chId
                            isRevisionQueueActive = false
                            currentTab = MainNavTab.FOCUS
                        }
                    )
                }
                else -> {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "main_tabs"
                    ) { tab ->
                        when (tab) {
                            MainNavTab.HOME -> HomeScreen(
                                repository = repository,
                                onNavigateToSyllabus = { currentTab = MainNavTab.SYLLABUS },
                                onNavigateToSubject = { subId ->
                                    currentTab = MainNavTab.SYLLABUS
                                },
                                onNavigateToChapter = { chId ->
                                    selectedChapterId = chId
                                },
                                onStartFocus = { chId ->
                                    focusTargetChapterId = chId
                                    currentTab = MainNavTab.FOCUS
                                },
                                onNavigateToRevisions = { isRevisionQueueActive = true },
                                onNavigateToPlanner = { isPlannerActive = true },
                                onNavigateToJournal = { isDailyJournalActive = true },
                                onNavigateToAdmission = { isAdmissionActive = true }
                            )
                            MainNavTab.SYLLABUS -> SyllabusScreen(
                                repository = repository,
                                onNavigateToChapter = { chId ->
                                    selectedChapterId = chId
                                },
                                onNavigateToBulkAdd = { subId ->
                                    bulkAddSubjectId = subId
                                    isBulkAddActive = true
                                }
                            )
                            MainNavTab.FOCUS -> FocusScreen(
                                repository = repository,
                                initialChapterId = focusTargetChapterId
                            )
                            MainNavTab.BLOCKER -> BlockerScreen(
                                repository = repository
                            )
                            MainNavTab.ANALYTICS -> AnalyticsScreen(
                                repository = repository
                            )
                            MainNavTab.SETTINGS -> SettingsScreen(
                                preferencesManager = preferencesManager,
                                repository = repository,
                                onNavigateToBlockerDiagnostics = { currentTab = MainNavTab.BLOCKER }
                            )
                        }
                    }
                }
            }
        }
    }
}
