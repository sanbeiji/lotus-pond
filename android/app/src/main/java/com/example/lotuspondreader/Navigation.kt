package com.example.lotuspondreader

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    
    val userSettings by viewModel.userSettings.collectAsState(initial = UserSettings(apiKey = "loading"))
    val uiState by viewModel.uiState.collectAsState()
    
    val plot by viewModel.plot.collectAsState()
    val skillLevel by viewModel.skillLevel.collectAsState()
    val length by viewModel.length.collectAsState()
    val requiredTerms by viewModel.requiredTerms.collectAsState()
    val isGeneratingPrompt by viewModel.isGeneratingPrompt.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp > 600
    val isTwoColumnMode = configuration.screenWidthDp >= 840 && isLandscape
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
            if (!isTwoColumnMode && backStack.lastOrNull() != StoryReader) {
                backStack.add(StoryReader)
            }
        }
    }

    // Handle dynamic transitions between single-pane and two-column layouts
    LaunchedEffect(isTwoColumnMode) {
        if (!isTwoColumnMode) {
            // Transitioning to single pane
            if (uiState is StoryUiState.Success) {
                if (backStack.lastOrNull() != StoryReader) {
                    backStack.add(StoryReader)
                }
            } else {
                backStack.clear()
                when (selectedItem) {
                    0 -> backStack.add(Home)
                    1 -> backStack.add(History)
                    2 -> backStack.add(Settings)
                }
            }
        } else {
            // Transitioning to two-column layout
            if (uiState is StoryUiState.Success) {
                selectedItem = 1 // Show History on the left when there is an active story
            } else {
                when (backStack.lastOrNull()) {
                    Home -> selectedItem = 0
                    History -> selectedItem = 1
                    Settings -> selectedItem = 2
                    else -> selectedItem = 0
                }
            }
            if (backStack.contains(StoryReader)) {
                backStack.remove(StoryReader)
            }
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

    if (userSettings.apiKey.isBlank() && currentScreen != Splash) {
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

    if (currentScreen == Splash) {
        Box(modifier = Modifier.fillMaxSize()) {
            SplashScreen(
                onSplashFinished = {
                    backStack.clear()
                    backStack.add(Home)
                }
            )
        }
    } else if (isTwoColumnMode) {
        TwoColumnLayout(
            viewModel = viewModel,
            userSettings = userSettings,
            uiState = uiState,
            plot = plot,
            onPlotChange = { viewModel.plot.value = it },
            skillLevel = skillLevel,
            onSkillLevelChange = { viewModel.skillLevel.value = it },
            length = length,
            onLengthChange = { viewModel.length.value = it },
            requiredTerms = requiredTerms,
            onRequiredTermsChange = { viewModel.requiredTerms.value = it },
            isGeneratingPrompt = isGeneratingPrompt,
            tts = tts,
            selectedItem = selectedItem,
            onSelectedItemChange = { selectedItem = it },
            backStack = backStack
        )
    } else {
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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                ),
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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                ),
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
                        selectedModel = userSettings.selectedModel,
                        onClearForm = {
                            viewModel.plot.value = ""
                            viewModel.skillLevel.value = "A1 (Entry)"
                            viewModel.length.value = "400"
                            viewModel.requiredTerms.value = ""
                            viewModel.resetUiState()
                        },
                        onResetError = { viewModel.resetUiState() },
                        isGeneratingPrompt = isGeneratingPrompt,
                        onSelectGenre = { genre -> viewModel.fetchGenrePrompt(genre) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                entry<History> {
                    val historyList by viewModel.history.collectAsState(initial = emptyList())
                    HistoryScreen(
                        history = historyList,
                        onStoryClick = { storyEntity ->
                            viewModel.loadStoryFromHistory(storyEntity)
                        },
                        onClearHistory = { viewModel.clearHistory() },
                        onDeleteStory = { storyEntity ->
                            viewModel.deleteStory(storyEntity)
                        },
                        onUndoDelete = { storyId ->
                            viewModel.undoDeleteStory(storyId)
                        },
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
                    var lastStory by remember { mutableStateOf<com.example.lotuspondreader.models.StoryResponse?>(null) }
                    if (uiState is StoryUiState.Success) {
                        lastStory = (uiState as StoryUiState.Success).story
                    }
                    val story = lastStory
                    val scope = rememberCoroutineScope()
                    val pcmPlayer = remember { com.example.lotuspondreader.api.TaiwaneseMandarinPcmPlayer() }
                    
                    if (story != null) {
                        val termsList = story.requiredTerms.split("[,，]".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
                        var showBottomSheet by remember { mutableStateOf(false) }
                        val activeFetches by viewModel.activeFetches.collectAsState()
                        val errorEvent by viewModel.errorEvent.collectAsState()
                        val snackbarHostState = remember { SnackbarHostState() }

                        val hasPinyin = remember(story) { story.sentences.any { !it.pinyin.isNullOrEmpty() } }
                        val hasZhuyin = remember(story) { story.sentences.any { !it.zhuyin.isNullOrEmpty() } }
                        val hasEnglish = remember(story) { story.sentences.any { !it.english.isNullOrEmpty() } }

                        val pinyinLoading = activeFetches.contains("pinyin")
                        val zhuyinLoading = activeFetches.contains("zhuyin")
                        val englishLoading = activeFetches.contains("english")

                        val showPinyinSession = userSettings.showPinyin && (hasPinyin || pinyinLoading)
                        val showZhuyinSession = userSettings.showZhuyin && (hasZhuyin || zhuyinLoading)
                        val showTranslationSession = userSettings.showTranslation && (hasEnglish || englishLoading)

                        LaunchedEffect(errorEvent) {
                            errorEvent?.let {
                                snackbarHostState.showSnackbar(
                                    message = it,
                                    actionLabel = "OK",
                                    duration = SnackbarDuration.Long
                                )
                                viewModel.clearErrorEvent()
                            }
                        }
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        if (showBottomSheet) {
                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            val sheetScrollState = androidx.compose.foundation.rememberScrollState()
                            ModalBottomSheet(
                                onDismissRequest = { showBottomSheet = false },
                                sheetState = sheetState
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .navigationBarsPadding()
                                        .verticalScroll(sheetScrollState),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Show", style = MaterialTheme.typography.titleMedium)
                                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = showPinyinSession,
                                                onClick = {
                                                    val newValue = !showPinyinSession
                                                    viewModel.updateSettings(userSettings.copy(showPinyin = newValue))
                                                    if (newValue) viewModel.checkAndFetchMissing("pinyin")
                                                },
                                                label = { Text("Pinyin") },
                                                enabled = !pinyinLoading,
                                                leadingIcon = if (showPinyinSession) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Done,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null
                                            )

                                            FilterChip(
                                                selected = showZhuyinSession,
                                                onClick = {
                                                    val newValue = !showZhuyinSession
                                                    viewModel.updateSettings(userSettings.copy(showZhuyin = newValue))
                                                    if (newValue) viewModel.checkAndFetchMissing("zhuyin")
                                                },
                                                label = { Text("Zhuyin") },
                                                enabled = !zhuyinLoading,
                                                leadingIcon = if (showZhuyinSession) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Done,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null
                                            )

                                            FilterChip(
                                                selected = showTranslationSession,
                                                onClick = {
                                                    val newValue = !showTranslationSession
                                                    viewModel.updateSettings(userSettings.copy(showTranslation = newValue))
                                                    if (newValue) viewModel.checkAndFetchMissing("english")
                                                },
                                                label = { Text("English") },
                                                enabled = !englishLoading,
                                                leadingIcon = if (showTranslationSession) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Done,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null
                                            )

                                            FilterChip(
                                                selected = userSettings.studyMode,
                                                onClick = {
                                                    viewModel.updateSettings(userSettings.copy(studyMode = !userSettings.studyMode))
                                                },
                                                label = { Text("Highlight Vocab") },
                                                leadingIcon = if (userSettings.studyMode) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Done,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    }

                                    HorizontalDivider()

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                        Text("Speech speed", style = MaterialTheme.typography.titleMedium)
                                        val rates = listOf(1.0f, 0.9f, 0.75f, 0.5f)
                                        val rateLabels = listOf("100%", "90%", "75%", "50%")
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rates.forEachIndexed { index, rate ->
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = rates.size),
                                                    onClick = { viewModel.updateSettings(userSettings.copy(speechRatePreference = rate)) },
                                                    selected = userSettings.speechRatePreference == rate,
                                                    icon = { SegmentedButtonDefaults.Icon(active = userSettings.speechRatePreference == rate) }
                                                ) {
                                                    Text(rateLabels[index])
                                                }
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Voice gender", style = MaterialTheme.typography.titleMedium)
                                        val genderOptions = listOf("female", "male")
                                        val genderLabels = listOf("Female", "Male")
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            genderOptions.forEachIndexed { index, option ->
                                                val isSelected = userSettings.voiceGender == option
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = genderOptions.size),
                                                    onClick = { viewModel.updateSettings(userSettings.copy(voiceGender = option)) },
                                                    selected = isSelected,
                                                    icon = { SegmentedButtonDefaults.Icon(active = isSelected) }
                                                ) {
                                                    Text(genderLabels[index])
                                                }
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Voice engine", style = MaterialTheme.typography.titleMedium)
                                        val engineOptions = listOf("Android", "Gemini")
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            engineOptions.forEachIndexed { index, option ->
                                                val isSelected = if (option == "Gemini") userSettings.useGeminiTts else !userSettings.useGeminiTts
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = engineOptions.size),
                                                    onClick = {
                                                        val isGemini = option == "Gemini"
                                                        viewModel.updateSettings(userSettings.copy(useGeminiTts = isGemini))
                                                        if (isGemini) {
                                                            scope.launch {
                                                                kotlinx.coroutines.delay(100)
                                                                sheetScrollState.animateScrollTo(sheetScrollState.maxValue)
                                                            }
                                                        }
                                                    },
                                                    selected = isSelected,
                                                    icon = { SegmentedButtonDefaults.Icon(active = isSelected) }
                                                ) {
                                                    Text(option)
                                                }
                                            }
                                        }

                                        val subtext = if (userSettings.useGeminiTts) {
                                            "Uses Gemini AI (Experimental). High-quality voices and regional accents; requires internet and has minor initial latency/token costs."
                                        } else {
                                            "Uses native text-to-speech. Fast, free, and works offline."
                                        }
                                        Text(
                                            text = subtext,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        )
                                    }

                                    if (userSettings.useGeminiTts) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("Voice style", style = MaterialTheme.typography.bodyMedium)

                                            val voiceStyles = listOf(
                                                "standard" to "Standard Taiwanese",
                                                "southern" to "Southern Taiwan (台南高雄腔)",
                                                "heavy_southern" to "Southern + Minnan (偏鄉本土腔)",
                                                "beijing" to "Beijing (北京腔)"
                                            )

                                            voiceStyles.forEach { (value, label) ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = userSettings.geminiTtsVoiceStyle == value,
                                                        onClick = { viewModel.updateSettings(userSettings.copy(geminiTtsVoiceStyle = value)) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                @OptIn(ExperimentalMaterial3Api::class)
                                CenterAlignedTopAppBar(
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
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                                showPinyin = showPinyinSession,
                                showZhuyin = showZhuyinSession,
                                showTranslation = showTranslationSession,
                                studyMode = userSettings.studyMode,
                                fontSizePreference = userSettings.fontSizePreference,
                                requiredTerms = termsList,
                                onPlayAudio = { textToSpeak -> 
                                    if (userSettings.useGeminiTts) {
                                        tts?.stop()
                                        scope.launch {
                                            val audioData = viewModel.generateSpeech(textToSpeak, userSettings.geminiTtsVoiceStyle)
                                            if (audioData != null) {
                                                pcmPlayer.playRawPcm(audioData, userSettings.speechRatePreference)
                                            }
                                        }
                                    } else {
                                        val currentTts = tts
                                        if (currentTts != null) {
                                            try {
                                                val voices = currentTts.voices
                                                if (voices != null) {
                                                    val genderTarget = userSettings.voiceGender
                                                    val targetVoice = if (genderTarget == "male") {
                                                        voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired && (it.name.contains("male", ignoreCase = true) || it.name.contains("-ctd-") || it.name.contains("-ccd-")) }
                                                            ?: voices.firstOrNull { it.locale.language == "zh" && (it.name.contains("male", ignoreCase = true) || it.name.contains("-ctd-") || it.name.contains("-ccd-")) }
                                                    } else {
                                                        voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired && (it.name.contains("female", ignoreCase = true) || it.name.contains("-ctc-") || it.name.contains("-cte-") || it.name.contains("-ccc-") || it.name.contains("-ssa-")) }
                                                            ?: voices.firstOrNull { it.locale.language == "zh" && (it.name.contains("female", ignoreCase = true) || it.name.contains("-ctc-") || it.name.contains("-cte-") || it.name.contains("-ccc-") || it.name.contains("-ssa-")) }
                                                    }
                                                    val finalVoice = targetVoice 
                                                        ?: voices.firstOrNull { it.locale.language == "zh" && it.locale.country == "TW" && !it.isNetworkConnectionRequired }
                                                        ?: voices.firstOrNull { it.locale.language == "zh" && it.locale.country == "TW" }
                                                        ?: voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired }
                                                        ?: voices.firstOrNull { it.locale.language == "zh" }
                                                    if (finalVoice != null) {
                                                        currentTts.voice = finalVoice
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("TTS_DEBUG", "Error setting voice", e)
                                            }
                                        }
                                        tts?.setSpeechRate(userSettings.speechRatePreference)
                                        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                },
                                onLookupWord = { word -> viewModel.lookupWord(word) },
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
  } // End if-else isTwoColumnMode
}

@Composable
fun TwoColumnLayout(
    viewModel: StoryViewModel,
    userSettings: UserSettings,
    uiState: StoryUiState,
    plot: String,
    onPlotChange: (String) -> Unit,
    skillLevel: String,
    onSkillLevelChange: (String) -> Unit,
    length: String,
    onLengthChange: (String) -> Unit,
    requiredTerms: String,
    onRequiredTermsChange: (String) -> Unit,
    isGeneratingPrompt: Boolean,
    tts: TextToSpeech?,
    selectedItem: Int,
    onSelectedItemChange: (Int) -> Unit,
    backStack: androidx.navigation3.runtime.NavBackStack<androidx.navigation3.runtime.NavKey>
) {
    val historyList by viewModel.history.collectAsState(initial = emptyList())
    val activeStory = if (uiState is StoryUiState.Success) uiState.story else null
    val scope = rememberCoroutineScope()
    val pcmPlayer = remember { com.example.lotuspondreader.api.TaiwaneseMandarinPcmPlayer() }
    
    // Manage local UI settings dropdown
    var showSettingsMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left column: Accordion (1/3 width)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stylized Sidebar Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Lotus Pond Reader",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("🪷", style = MaterialTheme.typography.headlineSmall)
            }

            // Accordion Sections (exactly one open)
            AccordionSection(
                title = "Create",
                isOpen = selectedItem == 0,
                onClick = { onSelectedItemChange(0) },
                modifier = if (selectedItem == 0) Modifier.weight(1f) else Modifier
            ) {
                HomeScreen(
                    plot = plot,
                    onPlotChange = onPlotChange,
                    skillLevel = skillLevel,
                    onSkillLevelChange = onSkillLevelChange,
                    length = length,
                    onLengthChange = onLengthChange,
                    requiredTerms = requiredTerms,
                    onRequiredTermsChange = onRequiredTermsChange,
                    uiState = uiState,
                    onGenerate = {
                        viewModel.generateStory(
                            plot = plot,
                            skillLevel = skillLevel,
                            length = length.toIntOrNull() ?: 300,
                            requiredTerms = requiredTerms
                        )
                    },
                    selectedModel = userSettings.selectedModel,
                    onClearForm = {
                        viewModel.plot.value = ""
                        viewModel.skillLevel.value = "A1 (Entry)"
                        viewModel.length.value = "400"
                        viewModel.requiredTerms.value = ""
                        viewModel.resetUiState()
                    },
                    onResetError = { viewModel.resetUiState() },
                    isGeneratingPrompt = isGeneratingPrompt,
                    onSelectGenre = { genre -> viewModel.fetchGenrePrompt(genre) }
                )
            }

            AccordionSection(
                title = "History",
                isOpen = selectedItem == 1,
                onClick = { onSelectedItemChange(1) },
                modifier = if (selectedItem == 1) Modifier.weight(1f) else Modifier
            ) {
                HistoryScreen(
                    history = historyList,
                    onStoryClick = { storyEntity ->
                        viewModel.loadStoryFromHistory(storyEntity)
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    onDeleteStory = { storyEntity ->
                        viewModel.deleteStory(storyEntity)
                    },
                    onUndoDelete = { storyId ->
                        viewModel.undoDeleteStory(storyId)
                    }
                )
            }

            AccordionSection(
                title = "Settings",
                isOpen = selectedItem == 2,
                onClick = { onSelectedItemChange(2) },
                modifier = if (selectedItem == 2) Modifier.weight(1f) else Modifier
            ) {
                SettingsScreen(
                    settings = userSettings,
                    onSettingsChanged = { viewModel.updateSettings(it) }
                )
            }
        }

        // Divider
        VerticalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        // Right column: Content Reader (2/3 width)
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState is StoryUiState.Loading -> {
                    // Loading State with spinner
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Generating your story...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState is StoryUiState.Error -> {
                    // Error state directly in reader pane
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Generation Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetUiState() }) {
                            Text("Try Again / Clear")
                        }
                    }
                }
                activeStory != null -> {
                    // Story Reader Content
                    val story = activeStory
                    val termsList = remember(story) {
                        story.requiredTerms.split("[,，]".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val activeFetches by viewModel.activeFetches.collectAsState()
                    
                    val hasPinyin = remember(story) { story.sentences.any { !it.pinyin.isNullOrEmpty() } }
                    val hasZhuyin = remember(story) { story.sentences.any { !it.zhuyin.isNullOrEmpty() } }
                    val hasEnglish = remember(story) { story.sentences.any { !it.english.isNullOrEmpty() } }

                    val pinyinLoading = activeFetches.contains("pinyin")
                    val zhuyinLoading = activeFetches.contains("zhuyin")
                    val englishLoading = activeFetches.contains("english")

                    val showPinyinSession = userSettings.showPinyin && (hasPinyin || pinyinLoading)
                    val showZhuyinSession = userSettings.showZhuyin && (hasZhuyin || zhuyinLoading)
                    val showTranslationSession = userSettings.showTranslation && (hasEnglish || englishLoading)

                    Scaffold(
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            CenterAlignedTopAppBar(
                                title = { 
                                    Text(
                                        text = story.title,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                actions = {
                                    Box {
                                        IconButton(onClick = { showSettingsMenu = true }) {
                                            Icon(Icons.Filled.Settings, contentDescription = "Display Settings")
                                        }
                                        ReaderSettingsDropdown(
                                            expanded = showSettingsMenu,
                                            onDismissRequest = { showSettingsMenu = false },
                                            userSettings = userSettings,
                                            viewModel = viewModel,
                                            showPinyinSession = showPinyinSession,
                                            showZhuyinSession = showZhuyinSession,
                                            showTranslationSession = showTranslationSession,
                                            pinyinLoading = pinyinLoading,
                                            zhuyinLoading = zhuyinLoading,
                                            englishLoading = englishLoading
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerReaderPadding ->
                        StoryView(
                            story = story,
                            showPinyin = showPinyinSession,
                            showZhuyin = showZhuyinSession,
                            showTranslation = showTranslationSession,
                            studyMode = userSettings.studyMode,
                            fontSizePreference = userSettings.fontSizePreference,
                            requiredTerms = termsList,
                            onPlayAudio = { textToSpeak -> 
                                if (userSettings.useGeminiTts) {
                                    tts?.stop()
                                    scope.launch {
                                        val audioData = viewModel.generateSpeech(textToSpeak, userSettings.geminiTtsVoiceStyle)
                                        if (audioData != null) {
                                            pcmPlayer.playRawPcm(audioData, userSettings.speechRatePreference)
                                        }
                                    }
                                } else {
                                    val currentTts = tts
                                    if (currentTts != null) {
                                        try {
                                            val voices = currentTts.voices
                                            if (voices != null) {
                                                val genderTarget = userSettings.voiceGender
                                                val targetVoice = if (genderTarget == "male") {
                                                    voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired && (it.name.contains("male", ignoreCase = true) || it.name.contains("-ctd-") || it.name.contains("-ccd-")) }
                                                        ?: voices.firstOrNull { it.locale.language == "zh" && (it.name.contains("male", ignoreCase = true) || it.name.contains("-ctd-") || it.name.contains("-ccd-")) }
                                                } else {
                                                    voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired && (it.name.contains("female", ignoreCase = true) || it.name.contains("-ctc-") || it.name.contains("-cte-") || it.name.contains("-ccc-") || it.name.contains("-ssa-")) }
                                                        ?: voices.firstOrNull { it.locale.language == "zh" && (it.name.contains("female", ignoreCase = true) || it.name.contains("-ctc-") || it.name.contains("-cte-") || it.name.contains("-ccc-") || it.name.contains("-ssa-")) }
                                                }
                                                val finalVoice = targetVoice 
                                                    ?: voices.firstOrNull { it.locale.language == "zh" && it.locale.country == "TW" && !it.isNetworkConnectionRequired }
                                                    ?: voices.firstOrNull { it.locale.language == "zh" && it.locale.country == "TW" }
                                                    ?: voices.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired }
                                                    ?: voices.firstOrNull { it.locale.language == "zh" }
                                                if (finalVoice != null) {
                                                    currentTts.voice = finalVoice
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("TTS_DEBUG", "Error setting voice", e)
                                        }
                                    }
                                    tts?.setSpeechRate(userSettings.speechRatePreference)
                                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            },
                            onLookupWord = { word -> viewModel.lookupWord(word) },
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = innerReaderPadding.calculateTopPadding() + 16.dp,
                                bottom = innerReaderPadding.calculateBottomPadding() + 16.dp
                            )
                        )
                    }
                }
                else -> {
                    // Welcome screen state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🪷",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Welcome to Lotus Pond Reader!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Fill out the form on the left to generate a new story, or select a story from your History.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 480.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccordionSection(
    title: String,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(if (isOpen) 180f else 0f)

    if (isOpen) {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
                HorizontalDivider()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = com.example.lotuspondreader.theme.LobsterFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    userSettings: UserSettings,
    viewModel: StoryViewModel,
    showPinyinSession: Boolean,
    showZhuyinSession: Boolean,
    showTranslationSession: Boolean,
    pinyinLoading: Boolean,
    zhuyinLoading: Boolean,
    englishLoading: Boolean
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(360.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Show", style = MaterialTheme.typography.titleMedium)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showPinyinSession,
                        onClick = {
                            val newValue = !showPinyinSession
                            viewModel.updateSettings(userSettings.copy(showPinyin = newValue))
                            if (newValue) viewModel.checkAndFetchMissing("pinyin")
                        },
                        label = { Text("Pinyin") },
                        enabled = !pinyinLoading,
                        leadingIcon = if (showPinyinSession) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = showZhuyinSession,
                        onClick = {
                            val newValue = !showZhuyinSession
                            viewModel.updateSettings(userSettings.copy(showZhuyin = newValue))
                            if (newValue) viewModel.checkAndFetchMissing("zhuyin")
                        },
                        label = { Text("Zhuyin") },
                        enabled = !zhuyinLoading,
                        leadingIcon = if (showZhuyinSession) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = showTranslationSession,
                        onClick = {
                            val newValue = !showTranslationSession
                            viewModel.updateSettings(userSettings.copy(showTranslation = newValue))
                            if (newValue) viewModel.checkAndFetchMissing("english")
                        },
                        label = { Text("English") },
                        enabled = !englishLoading,
                        leadingIcon = if (showTranslationSession) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = userSettings.studyMode,
                        onClick = {
                            viewModel.updateSettings(userSettings.copy(studyMode = !userSettings.studyMode))
                        },
                        label = { Text("Highlight Vocab") },
                        leadingIcon = if (userSettings.studyMode) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Speech speed", style = MaterialTheme.typography.titleMedium)
                val rates = listOf(1.0f, 0.9f, 0.75f, 0.5f)
                val rateLabels = listOf("100%", "90%", "75%", "50%")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rates.forEachIndexed { index, rate ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = rates.size),
                            onClick = { viewModel.updateSettings(userSettings.copy(speechRatePreference = rate)) },
                            selected = userSettings.speechRatePreference == rate,
                            icon = { SegmentedButtonDefaults.Icon(active = userSettings.speechRatePreference == rate) }
                        ) {
                            Text(rateLabels[index])
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Voice gender", style = MaterialTheme.typography.titleMedium)
                val genderOptions = listOf("female", "male")
                val genderLabels = listOf("Female", "Male")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    genderOptions.forEachIndexed { index, option ->
                        val isSelected = userSettings.voiceGender == option
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = genderOptions.size),
                            onClick = { viewModel.updateSettings(userSettings.copy(voiceGender = option)) },
                            selected = isSelected,
                            icon = { SegmentedButtonDefaults.Icon(active = isSelected) }
                        ) {
                            Text(genderLabels[index])
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Voice engine", style = MaterialTheme.typography.titleMedium)
                val engineOptions = listOf("Android", "Gemini")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    engineOptions.forEachIndexed { index, option ->
                        val isSelected = if (option == "Gemini") userSettings.useGeminiTts else !userSettings.useGeminiTts
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = engineOptions.size),
                            onClick = {
                                val isGemini = option == "Gemini"
                                viewModel.updateSettings(userSettings.copy(useGeminiTts = isGemini))
                            },
                            selected = isSelected,
                            icon = { SegmentedButtonDefaults.Icon(active = isSelected) }
                        ) {
                            Text(option)
                        }
                    }
                }
            }

            if (userSettings.useGeminiTts) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Voice style", style = MaterialTheme.typography.bodyMedium)

                    val voiceStyles = listOf(
                        "standard" to "Standard Taiwanese",
                        "southern" to "Southern Taiwan (台南高雄腔)",
                        "heavy_southern" to "Southern + Minnan (偏鄉本土腔)",
                        "beijing" to "Beijing (北京腔)"
                    )

                    voiceStyles.forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userSettings.geminiTtsVoiceStyle == value,
                                onClick = { viewModel.updateSettings(userSettings.copy(geminiTtsVoiceStyle = value)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
