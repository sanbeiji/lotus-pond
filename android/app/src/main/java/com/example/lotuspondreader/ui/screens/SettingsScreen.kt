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
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = settings.pronunciation == "pinyin",
                onClick = { onSettingsChanged(settings.copy(pronunciation = "pinyin")) }
            )
            Text("Pinyin", modifier = Modifier.padding(end = 16.dp))
            
            RadioButton(
                selected = settings.pronunciation == "zhuyin",
                onClick = { onSettingsChanged(settings.copy(pronunciation = "zhuyin")) }
            )
            Text("Zhuyin")
        }
        
        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show English translation")
            Switch(
                checked = settings.showTranslation,
                onCheckedChange = { onSettingsChanged(settings.copy(showTranslation = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Highlight required vocabulary")
            Switch(
                checked = settings.studyMode,
                onCheckedChange = { onSettingsChanged(settings.copy(studyMode = it)) }
            )
        }
    }
}
