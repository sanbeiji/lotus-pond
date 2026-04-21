package com.example.lotuspondreader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.lotuspondreader.models.UserSettings
import com.example.lotuspondreader.ui.components.StoryView
import com.example.lotuspondreader.ui.screens.HistoryScreen
import com.example.lotuspondreader.ui.screens.HomeScreen
import com.example.lotuspondreader.ui.screens.SettingsScreen
import com.example.lotuspondreader.viewmodel.StoryUiState
import com.example.lotuspondreader.viewmodel.StoryViewModel
import com.example.lotuspondreader.viewmodel.StoryViewModelFactory
// import com.example.lotuspondreader.ui.screens.HistoryScreen

@Composable
fun MainNavigation(
    viewModel: StoryViewModel = viewModel(factory = StoryViewModelFactory(LocalContext.current))
) {
    val backStack = rememberNavBackStack(Home)
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Generate", "History", "Settings")
    
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings())
    val uiState by viewModel.uiState.collectAsState()
    
    val plot by viewModel.plot.collectAsState()
    val skillLevel by viewModel.skillLevel.collectAsState()
    val length by viewModel.length.collectAsState()
    val requiredTerms by viewModel.requiredTerms.collectAsState()

    // Handle navigation when generation succeeds
    LaunchedEffect(uiState) {
        if (uiState is StoryUiState.Success) {
            // Push the reader onto the backstack. Since we are using standard Compose navigation3,
            // we use the enum or data class key.
            backStack.add(StoryReader)
            // We do NOT reset the UI state here immediately because the StoryReader will display it.
            // If we reset it, the story disappears from the screen!
        }
    }

    Scaffold(
        bottomBar = {
            // Only show bottom bar on root screens
            if (backStack.lastOrNull() == Home || backStack.lastOrNull() == History || backStack.lastOrNull() == Settings) {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Filled.Create, contentDescription = item)
                                    1 -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = item)
                                    2 -> Icon(Icons.Filled.Settings, contentDescription = item)
                                }
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = {
                                selectedItem = index
                                when (index) {
                                    0 -> {
                                        backStack.clear()
                                        backStack.add(Home)
                                    }
                                    1 -> {
                                        backStack.clear()
                                        backStack.add(History)
                                    }
                                    2 -> {
                                        backStack.clear()
                                        backStack.add(Settings)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                val current = backStack.lastOrNull()
                backStack.removeLastOrNull()
                if (current == StoryReader) {
                    viewModel.resetUiState()
                }
            },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        plot = plot,
                        onPlotChange = { viewModel.plot.value = it },
                        skillLevel = skillLevel,
                        onSkillLevelChange = { viewModel.skillLevel.value = it },
                        length = length,
                        onLengthChange = { viewModel.length.value = it },
                        requiredTerms = requiredTerms,
                        onRequiredTermsChange = { viewModel.requiredTerms.value = it },
                        uiState = uiState,
                        onGenerate = {
                            viewModel.generateStory(
                                plot = plot,
                                skillLevel = skillLevel,
                                length = length.toIntOrNull() ?: 300,
                                requiredTerms = requiredTerms
                            )
                        },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                entry<History> {
                    val historyList by viewModel.history.collectAsState(initial = emptyList())
                    HistoryScreen(
                        history = historyList,
                        onStoryClick = { storyEntity ->
                            viewModel.loadStoryFromHistory(storyEntity.storyData)
                        },
                        onClearHistory = { viewModel.clearHistory() },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        settings = userSettings,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                entry<StoryReader> {
                    if (uiState is StoryUiState.Success) {
                        val story = (uiState as StoryUiState.Success).story
                        val termsList = requiredTerms.split("[,，]".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
                        var showBottomSheet by remember { mutableStateOf(false) }
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        if (showBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showBottomSheet = false }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Display Settings", style = MaterialTheme.typography.titleLarge)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Text("Show pronunciation")
                                        Switch(
                                            checked = userSettings.showPronunciation,
                                            onCheckedChange = { viewModel.updateSettings(userSettings.copy(showPronunciation = it)) }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Text("Show English translation")
                                        Switch(
                                            checked = userSettings.showTranslation,
                                            onCheckedChange = { viewModel.updateSettings(userSettings.copy(showTranslation = it)) }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Text("Highlight required vocabulary")
                                        Switch(
                                            checked = userSettings.studyMode,
                                            onCheckedChange = { viewModel.updateSettings(userSettings.copy(studyMode = it)) }
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }

                        Scaffold(
                            topBar = {
                                @OptIn(ExperimentalMaterial3Api::class)
                                TopAppBar(
                                    title = { 
                                        Text(
                                            text = story.title,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        ) 
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            backStack.removeLastOrNull()
                                            viewModel.resetUiState()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { showBottomSheet = true }) {
                                            Icon(Icons.Filled.Settings, contentDescription = "Display Settings")
                                        }
                                    }
                                )
                            }
                        ) { innerReaderPadding ->
                            StoryView(
                                story = story,
                                showPronunciation = userSettings.showPronunciation,
                                pronunciationType = userSettings.pronunciation,
                                showTranslation = userSettings.showTranslation,
                                studyMode = userSettings.studyMode,
                                requiredTerms = termsList,
                                onPlayAudio = { /* TODO hook to TTS */ },
                                modifier = Modifier.padding(innerReaderPadding).safeDrawingPadding()
                            )
                        }
                    } else {
                        // Fallback during exit transitions
                        Text("", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        )
    }
}
