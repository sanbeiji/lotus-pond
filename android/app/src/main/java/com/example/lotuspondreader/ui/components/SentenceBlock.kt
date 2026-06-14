package com.example.lotuspondreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.lotuspondreader.models.Sentence
import kotlinx.coroutines.launch

@Composable
fun SentenceBlock(
    sentence: Sentence,
    showPinyin: Boolean,
    showZhuyin: Boolean,
    showTranslation: Boolean,
    studyMode: Boolean,
    fontSizePreference: String,
    requiredTerms: List<String>,
    onPlayAudio: (String) -> Unit,
    onLookupWord: suspend (String) -> List<com.example.lotuspondreader.data.DictEntry>,
    modifier: Modifier = Modifier
) {
    val mandarinFontSize = when (fontSizePreference) {
        "medium" -> 28.sp
        "large" -> 36.sp
        else -> 20.sp
    }
    
    val mandarinLineHeight = when (fontSizePreference) {
        "medium" -> 42.sp
        "large" -> 54.sp
        else -> 30.sp
    }
    
    val pinyinFontSize = when (fontSizePreference) {
        "medium" -> 18.sp
        "large" -> 22.sp
        else -> 14.sp
    }
    
    val englishStyle = when (fontSizePreference) {
        "medium" -> MaterialTheme.typography.titleMedium
        "large" -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.bodyMedium
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mandarin and Play Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sentence.words.isNotEmpty()) {
                    // Render as FlowRow of word tokens
                    var selectedWord by remember { mutableStateOf<String?>(null) }
                    var lookupResult by remember { mutableStateOf<List<com.example.lotuspondreader.data.DictEntry>?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current

                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        sentence.words.forEach { word ->
                            val isChinese = word.any { it.code in 0x4e00..0x9fff }
                            val isHighlighted = studyMode && requiredTerms.isNotEmpty() && requiredTerms.any { term -> word.contains(term) }
                            
                            Box {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                        fontSize = mandarinFontSize,
                                        lineHeight = mandarinLineHeight,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .run {
                                            if (isChinese) {
                                                combinedClickable(
                                                    onLongClick = {
                                                        selectedWord = word
                                                        coroutineScope.launch {
                                                            isLoading = true
                                                            lookupResult = onLookupWord(word)
                                                            isLoading = false
                                                        }
                                                    },
                                                    onClick = {}
                                                )
                                            } else this
                                        }
                                        .padding(horizontal = 1.dp)
                                )

                                if (selectedWord == word) {
                                    Popup(
                                        onDismissRequest = {
                                            selectedWord = null
                                            lookupResult = null
                                        },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .width(280.dp)
                                                .shadow(8.dp, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .heightIn(max = 240.dp)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = word,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            com.example.lotuspondreader.utils.PlecoDeepLinkHelper.openPleco(context, word)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    ) {
                                                        Text("Pleco", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                if (isLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .align(Alignment.CenterHorizontally)
                                                    )
                                                } else {
                                                    val entries = lookupResult
                                                    if (entries.isNullOrEmpty()) {
                                                        Text(
                                                            text = "No definition found in local dictionary.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    } else {
                                                        entries.forEach { entry ->
                                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                if (entry.traditional != word) {
                                                                    Text(
                                                                        text = entry.traditional,
                                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                                                            fontWeight = FontWeight.Bold
                                                                        ),
                                                                        color = MaterialTheme.colorScheme.secondary
                                                                    )
                                                                }
                                                                Text(
                                                                    text = entry.pinyin,
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = entry.english,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Highlight vocab if in study mode (Fallback for old stories)
                    val annotatedString = buildAnnotatedString {
                        if (studyMode && requiredTerms.isNotEmpty()) {
                            var currentIndex = 0
                            val text = sentence.mandarin
                            val matches = mutableListOf<Pair<Int, Int>>()
                            
                            requiredTerms.forEach { term ->
                                var startIndex = text.indexOf(term)
                                while (startIndex >= 0) {
                                    matches.add(startIndex to startIndex + term.length)
                                    startIndex = text.indexOf(term, startIndex + term.length)
                                }
                            }
                            
                            matches.sortBy { it.first }
                            
                            var lastEnd = 0
                            for (match in matches) {
                                if (match.first >= lastEnd) {
                                    append(text.substring(lastEnd, match.first))
                                    withStyle(style = SpanStyle(
                                        background = MaterialTheme.colorScheme.primaryContainer,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )) {
                                        append(text.substring(match.first, match.second))
                                    }
                                    lastEnd = match.second
                                }
                            }
                            if (lastEnd < text.length) {
                                append(text.substring(lastEnd))
                            }
                            
                        } else {
                            append(sentence.mandarin)
                        }
                    }
                    
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = mandarinFontSize,
                            lineHeight = mandarinLineHeight
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { onPlayAudio(sentence.mandarin) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "🔊",
                        fontSize = 24.sp
                    )
                }
            }
            
            // Pinyin
            if (showPinyin) {
                val pinyinText = sentence.pinyin
                if (pinyinText != null) {
                    Text(
                        text = pinyinText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = pinyinFontSize,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = pinyinFontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Zhuyin
            if (showZhuyin) {
                val zhuyinText = sentence.zhuyin
                if (zhuyinText != null) {
                    Text(
                        text = zhuyinText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = pinyinFontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = pinyinFontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // English Translation
            if (showTranslation) {
                val englishText = sentence.english
                if (englishText != null) {
                    Text(
                        text = englishText,
                        style = englishStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Loading...",
                        style = englishStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
