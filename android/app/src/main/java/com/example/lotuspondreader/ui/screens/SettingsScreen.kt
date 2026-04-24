package com.example.lotuspondreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lotuspondreader.models.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSettingsChanged: (UserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = settings.apiKey,
            onValueChange = { onSettingsChanged(settings.copy(apiKey = it)) },
            label = { Text("Gemini API key *") },
            placeholder = { Text("Enter your Google AI Studio API key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Your key is stored locally on your device.") }
        )

        var modelExpanded by remember { mutableStateOf(false) }
        val models = listOf(
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-3.1-flash-lite-preview"
        )
        
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = it },
        ) {
            OutlinedTextField(
                value = settings.selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gemini model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false }
            ) {
                models.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onSettingsChanged(settings.copy(selectedModel = selectionOption))
                            modelExpanded = false
                        }
                    )
                }
            }
        }

        Text("Pronunciation style", style = MaterialTheme.typography.titleMedium)
        
        val pronOptions = listOf("pinyin", "zhuyin")
        val pronLabels = listOf("Pinyin", "Zhuyin")
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            pronOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = pronOptions.size),
                    onClick = { onSettingsChanged(settings.copy(pronunciation = option)) },
                    selected = settings.pronunciation == option,
                    icon = { SegmentedButtonDefaults.Icon(active = settings.pronunciation == option) }
                ) {
                    Text(pronLabels[index])
                }
            }
        }

        HorizontalDivider()

        Text("App theme", style = MaterialTheme.typography.titleMedium)
        
        val themeOptions = listOf("system", "light", "dark")
        val themeLabels = listOf("System", "Light", "Dark")
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            themeOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                    onClick = { onSettingsChanged(settings.copy(themePreference = option)) },
                    selected = settings.themePreference == option,
                    icon = { SegmentedButtonDefaults.Icon(active = settings.themePreference == option) }
                ) {
                    Text(themeLabels[index])
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dynamic Color", style = MaterialTheme.typography.titleMedium)
                Text("Use wallpaper colors (Android 12+)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.useDynamicColor,
                onCheckedChange = { onSettingsChanged(settings.copy(useDynamicColor = it)) }
            )
        }

        HorizontalDivider()
        
        var showAboutDialog by remember { mutableStateOf(false) }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Lotus Pond Reader") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Lotus Pond Reader is an interactive tool designed to help Mandarin learners master Traditional Chinese characters, with a specific focus on Taiwanese language patterns and cultural context.")
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Key Features", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("• Adaptive Difficulty: Choose from 8 TOCFL levels.\n• Interlinear Assistance: Toggle Pinyin/Zhuyin.\n• English Translations: Read natural English translations.\n• Vocabulary Practice: AI integrates custom words.")
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How to Use", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("1. Get a Gemini API key and enter it in Settings.\n2. Enter a plot or theme.\n3. Select your proficiency level.\n4. (Optional) Enter vocabulary to study.\n5. Generate and wait for content.\n6. Toggle options on the story screen.\n7. Practice reading aloud.")
                        }
                        
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { uriHandler.openUri("https://en.wikipedia.org/wiki/Test_of_Chinese_as_a_Foreign_Language") }, modifier = Modifier.weight(1f)) {
                                Text("What is TOCFL?", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }, modifier = Modifier.weight(1f)) {
                                Text("Get API Key", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        OutlinedButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ℹ️ About Lotus Pond Reader")
        }
    }
}
