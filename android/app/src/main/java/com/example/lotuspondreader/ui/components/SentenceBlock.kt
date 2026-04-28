package com.example.lotuspondreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.lotuspondreader.models.Sentence

@Composable
fun SentenceBlock(
    sentence: Sentence,
    showPronunciation: Boolean,
    pronunciationType: String, // "pinyin" or "zhuyin"
    showTranslation: Boolean,
    studyMode: Boolean,
    fontSizePreference: String,
    requiredTerms: List<String>,
    onPlayAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mandarinFontSize = when (fontSizePreference) {
        "larger" -> 28.sp
        "largest" -> 32.sp
        else -> 24.sp
    }
    
    val mandarinLineHeight = when (fontSizePreference) {
        "larger" -> 42.sp
        "largest" -> 48.sp
        else -> 36.sp
    }
    
    val pinyinFontSize = when (fontSizePreference) {
        "larger" -> 18.sp
        "largest" -> 20.sp
        else -> 16.sp
    }
    
    val englishStyle = when (fontSizePreference) {
        "larger" -> MaterialTheme.typography.titleMedium
        "largest" -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.bodyLarge
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
                verticalAlignment = Alignment.Top
            ) {
                // Highlight vocab if in study mode
                val annotatedString = buildAnnotatedString {
                    if (studyMode && requiredTerms.isNotEmpty()) {
                        // Very basic substring highlighting
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
                        
                        // Sort by start index and avoid overlapping (simplified for brevity)
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
                
                if (showPronunciation) {
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
            }
            
            // Pronunciation
            if (showPronunciation) {
                val pronText = if (pronunciationType == "zhuyin") sentence.zhuyin else sentence.pinyin
                if (!pronText.isNullOrEmpty()) {
                    Text(
                        text = pronText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                            fontSize = pinyinFontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            
            // English Translation
            if (showTranslation) {
                Text(
                    text = sentence.english,
                    style = englishStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
