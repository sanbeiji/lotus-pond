package com.example.lotuspondreader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.example.lotuspondreader.models.UserSettings
import com.example.lotuspondreader.ui.components.StoryView
import com.example.lotuspondreader.ui.screens.HistoryScreen
import com.example.lotuspondreader.ui.screens.HomeScreen
import com.example.lotuspondreader.ui.screens.SettingsScreen
import com.example.lotuspondreader.ui.screens.SplashScreen
import com.example.lotuspondreader.viewmodel.StoryUiState
import com.example.lotuspondreader.viewmodel.StoryViewModel
import com.example.lotuspondreader.viewmodel.StoryViewModelFactory
// import com.example.lotuspondreader.ui.screens.HistoryScreen

@Composable
fun MainNavigation(
    viewModel: StoryViewModel = viewModel(factory = StoryViewModelFactory(LocalContext.current))
) {
    val backStack = rememberNavBackStack(Splash)
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Create", "History", "Settings")
    
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings())
    val uiState by viewModel.uiState.collectAsState()
    
    val plot by viewModel.plot.collectAsState()
    val skillLevel by viewModel.skillLevel.collectAsState()
    val length by viewModel.length.collectAsState()
    val requiredTerms by viewModel.requiredTerms.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    val context = LocalContext.current
    
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.TRADITIONAL_CHINESE
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

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

    val currentScreen = backStack.lastOrNull()
    var showQuitDialog by remember { mutableStateOf(false) }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Quit App") },
            text = { Text("Do you really want to quit the app?") },
            confirmButton = {
                val context = LocalContext.current
                TextButton(onClick = { 
                    var activityContext = context
                    while (activityContext is android.content.ContextWrapper) {
                        if (activityContext is ComponentActivity) break
                        activityContext = activityContext.baseContext
                    }
                    (activityContext as? ComponentActivity)?.finish()
                }) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (userSettings.apiKey.isBlank()) {
        var tempApiKey by remember { mutableStateOf("") }
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { /* Force user to enter key or quit */ },
            title = { Text("API Key Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lotus Pond Reader requires that you supply your own Gemini API Key.")
                    Text(
                        text = "Get your Gemini API key here.",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://aistudio.google.com/app/apikey")
                        }
                    )
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("Gemini API key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempApiKey.isNotBlank()) {
                            viewModel.updateSettings(userSettings.copy(apiKey = tempApiKey))
                        }
                    },
                    enabled = tempApiKey.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        )
    }

    BackHandler(enabled = currentScreen == Home || currentScreen == History || currentScreen == Settings) {
        if (currentScreen == Home) {
            showQuitDialog = true
        } else {
            selectedItem = 0
            backStack.clear()
            backStack.add(Home)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to MaterialTheme.colorScheme.primary,
                    0.2f to androidx.compose.ui.graphics.Color.Transparent,
                    1.0f to androidx.compose.ui.graphics.Color.Transparent
                )
            )
    ) {
        if (isWideScreen && (backStack.lastOrNull() == Home || backStack.lastOrNull() == History || backStack.lastOrNull() == Settings)) {
            NavigationRail(
                modifier = Modifier.safeDrawingPadding(),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Spacer(Modifier.weight(1f))
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Filled.Create, contentDescription = item)
                                1 -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = item)
                                2 -> Icon(Icons.Filled.Settings, contentDescription = item)
                            }
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.onPrimary
                        ),
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
                Spacer(Modifier.weight(1f))
            }
        }
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                val currentScreen = backStack.lastOrNull()
                if (currentScreen == Home || currentScreen == History || currentScreen == Settings) {
                    val viewName = when (currentScreen) {
                        Home -> "Create"
                        History -> "History"
                        Settings -> "Settings"
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Lotus Pond Reader",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = "🪷", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = viewName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Only show bottom bar on root screens
                if (!isWideScreen && (backStack.lastOrNull() == Home || backStack.lastOrNull() == History || backStack.lastOrNull() == Settings)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                indicatorColor = MaterialTheme.colorScheme.onPrimary
                            ),
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
            entryProvider = entryProvider {
                entry<Splash> {
                    SplashScreen(
                        onSplashFinished = {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    )
                }
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
                        modifier = Modifier.padding(innerPadding)
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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        settings = userSettings,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        modifier = Modifier.padding(innerPadding)
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
                                    
                                    Spacer(Modifier.height(8.dp))
                                    Text("Font size", style = MaterialTheme.typography.titleMedium)
                                    val fontOptions = listOf("small", "medium", "large")
                                    val fontLabels = listOf("Small", "Medium", "Large")
                                    SingleChoiceSegmentedButtonRow(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        fontOptions.forEachIndexed { index, option ->
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = index, count = fontOptions.size),
                                                onClick = { viewModel.updateSettings(userSettings.copy(fontSizePreference = option)) },
                                                selected = userSettings.fontSizePreference == option,
                                                icon = { SegmentedButtonDefaults.Icon(active = userSettings.fontSizePreference == option) }
                                            ) {
                                                Text(fontLabels[index])
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Text("Speech speed", style = MaterialTheme.typography.titleMedium)
                                    var expanded by remember { mutableStateOf(false) }
                                    val rates = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
                                    val rateLabels = listOf("100%", "90%", "80%", "70%", "60%", "50%")
                                    Box {
                                        OutlinedButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val currentLabel = rateLabels[rates.indexOf(userSettings.speechRatePreference).coerceAtLeast(0)]
                                            Text("Speed: $currentLabel")
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            rates.forEachIndexed { index, rate ->
                                                DropdownMenuItem(
                                                    text = { Text(rateLabels[index]) },
                                                    onClick = {
                                                        viewModel.updateSettings(userSettings.copy(speechRatePreference = rate))
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
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
                                                fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
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
                                fontSizePreference = userSettings.fontSizePreference,
                                requiredTerms = termsList,
                                onPlayAudio = { textToSpeak -> 
                                    tts?.setSpeechRate(userSettings.speechRatePreference)
                                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = innerReaderPadding.calculateTopPadding() + 16.dp,
                                    bottom = innerReaderPadding.calculateBottomPadding() + 16.dp
                                )
                            )
                        }
                    } else {
                        // Fallback during exit transitions
                        Text("", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        )
      } // End Scaffold
    } // End Row
}
