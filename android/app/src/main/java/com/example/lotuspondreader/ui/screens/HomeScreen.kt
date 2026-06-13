package com.example.lotuspondreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lotuspondreader.viewmodel.StoryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    plot: String,
    onPlotChange: (String) -> Unit,
    skillLevel: String,
    onSkillLevelChange: (String) -> Unit,
    length: String,
    onLengthChange: (String) -> Unit,
    requiredTerms: String,
    onRequiredTermsChange: (String) -> Unit,
    uiState: StoryUiState,
    onGenerate: () -> Unit,
    selectedModel: String = "gemini-flash-lite-latest",
    onClearForm: () -> Unit = {},
    onResetError: () -> Unit = {},
    isGeneratingPrompt: Boolean = false,
    onSelectGenre: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skillLevels = listOf(
        "Novice 1", "Novice 2", "A1 (Entry)", "A2 (Foundation)",
        "B3 (Intermediate)", "B4 (Upper Intermediate)",
        "C5 (Fluent)", "C6 (Advanced)"
    )
    val lengthOptions = listOf("200", "400", "600", "800", "1000")

    val genres = listOf(
        "Adventure", "Daily Life", "Fantasy", "Mystery",
        "Sci-Fi", "Historical", "Romance", "Pirates"
    )

    var skillExpanded by remember { mutableStateOf(false) }
    var lengthExpanded by remember { mutableStateOf(false) }

    var selectedGenreToFetch by remember { mutableStateOf<String?>(null) }
    var showOverwriteConfirmation by remember { mutableStateOf(false) }

    if (showOverwriteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showOverwriteConfirmation = false
                selectedGenreToFetch = null
            },
            title = { Text("Overwrite Plot?") },
            text = { Text("Are you sure you want to overwrite your current plot text with a new AI-generated premise?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverwriteConfirmation = false
                        selectedGenreToFetch?.let { genre ->
                            onSelectGenre(genre)
                        }
                        selectedGenreToFetch = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverwriteConfirmation = false
                        selectedGenreToFetch = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plot *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    
                    var genreExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = genreExpanded,
                        onExpandedChange = { if (!isGeneratingPrompt) genreExpanded = it }
                    ) {
                        TextButton(
                            onClick = { if (!isGeneratingPrompt) genreExpanded = true },
                            enabled = !isGeneratingPrompt,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        ) {
                            Text(
                                text = "💡 Inspire me",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded)
                        }
                        
                        ExposedDropdownMenu(
                            expanded = genreExpanded,
                            onDismissRequest = { genreExpanded = false }
                        ) {
                            genres.forEach { genreOption ->
                                DropdownMenuItem(
                                    text = { Text(genreOption) },
                                    onClick = {
                                        genreExpanded = false
                                        if (plot.isNotBlank()) {
                                            selectedGenreToFetch = genreOption
                                            showOverwriteConfirmation = true
                                        } else {
                                            onSelectGenre(genreOption)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = if (isGeneratingPrompt) "Generating premise..." else plot,
                    onValueChange = onPlotChange,
                    enabled = !isGeneratingPrompt,
                    placeholder = { Text("e.g. A college student looking for a job…") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    trailingIcon = {
                        if (isGeneratingPrompt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Level (TOCFL band) *", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            ExposedDropdownMenuBox(
                expanded = skillExpanded,
                onExpandedChange = { skillExpanded = it },
            ) {
                OutlinedTextField(
                    value = skillLevel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = skillExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = skillExpanded,
                    onDismissRequest = { skillExpanded = false }
                ) {
                    skillLevels.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onSkillLevelChange(selectionOption)
                                skillExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Number of Mandarin characters *", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            ExposedDropdownMenuBox(
                expanded = lengthExpanded,
                onExpandedChange = { lengthExpanded = it },
            ) {
                OutlinedTextField(
                    value = length,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = lengthExpanded,
                    onDismissRequest = { lengthExpanded = false }
                ) {
                    lengthOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onLengthChange(selectionOption)
                                lengthExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Required Mandarin vocabulary (optional)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            OutlinedTextField(
                value = requiredTerms,
                onValueChange = onRequiredTermsChange,
                placeholder = { Text("e.g. 電腦, 學習, 朋友") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onClearForm,
                modifier = Modifier.weight(1f),
                enabled = uiState !is StoryUiState.Loading && !isGeneratingPrompt
            ) {
                Text("Clear")
            }
            Button(
                onClick = onGenerate,
                modifier = Modifier.weight(2f),
                enabled = plot.isNotBlank() && uiState !is StoryUiState.Loading && !isGeneratingPrompt
            ) {
                if (uiState is StoryUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✨ Creating...")
                } else {
                    Text("✨ Create story")
                }
            }
        }
        
        if (uiState is StoryUiState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onResetError) {
                        Text("Clear", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        } // Close the scrolling Column
    }
}
