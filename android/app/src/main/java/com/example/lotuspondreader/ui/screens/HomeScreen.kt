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
    modifier: Modifier = Modifier
) {
    val skillLevels = listOf(
        "Novice 1", "Novice 2", "A1 (Entry)", "A2 (Foundation)",
        "B3 (Intermediate)", "B4 (Upper Intermediate)",
        "C5 (Fluent)", "C6 (Advanced)"
    )
    val lengthOptions = listOf("100", "200", "300", "400", "500", "600", "700")
    var skillExpanded by remember { mutableStateOf(false) }
    var lengthExpanded by remember { mutableStateOf(false) }

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
                Text("Plot / theme *", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                OutlinedTextField(
                    value = plot,
                    onValueChange = onPlotChange,
                    placeholder = { Text("e.g. A college student looking for a job…") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
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

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = plot.isNotBlank() && uiState !is StoryUiState.Loading
        ) {
            if (uiState is StoryUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨ Creating...")
            } else {
                Text("✨ Create story")
            }
        }
        
        if (uiState is StoryUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        } // Close the scrolling Column
    }
}
