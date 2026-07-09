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
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.example.lotuspondreader.models.Sentence
import com.example.lotuspondreader.utils.PinyinConverter
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
    onPlayWordTts: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mandarinFontSize = when (fontSizePreference) {
        "medium" -> 30.sp
        "large" -> 40.sp
        else -> 20.sp
    }
    
    val mandarinLineHeight = when (fontSizePreference) {
        "medium" -> 45.sp
        "large" -> 60.sp
        else -> 30.sp
    }
    
    val pinyinFontSize = when (fontSizePreference) {
        "medium" -> 19.sp
        "large" -> 24.sp
        else -> 14.sp
    }
    
    val englishStyle = when (fontSizePreference) {
        "medium" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
        "large" -> MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp)
        else -> MaterialTheme.typography.bodyMedium
    }

    val dictTitleFontSize = when (fontSizePreference) {
        "medium" -> 22.sp
        "large" -> 26.sp
        else -> 18.sp
    }
    val dictTitleLineHeight = when (fontSizePreference) {
        "medium" -> 28.sp
        "large" -> 34.sp
        else -> 24.sp
    }
    
    val dictTraditionalFontSize = when (fontSizePreference) {
        "medium" -> 18.sp
        "large" -> 22.sp
        else -> 16.sp
    }
    val dictTraditionalLineHeight = when (fontSizePreference) {
        "medium" -> 24.sp
        "large" -> 30.sp
        else -> 22.sp
    }
    
    val dictContentFontSize = when (fontSizePreference) {
        "medium" -> 14.sp
        "large" -> 17.sp
        else -> 13.sp
    }
    val dictContentLineHeight = when (fontSizePreference) {
        "medium" -> 20.sp
        "large" -> 24.sp
        else -> 18.sp
    }
    
    val dictPlecoButtonHeight = when (fontSizePreference) {
        "medium" -> 30.dp
        "large" -> 34.dp
        else -> 28.dp
    }
    
    val dictPlecoFontSize = when (fontSizePreference) {
        "medium" -> 12.sp
        "large" -> 14.sp
        else -> 11.sp
    }
    
    val dictPopupWidth = when (fontSizePreference) {
        "medium" -> 320.dp
        "large" -> 350.dp
        else -> 280.dp
    }
    
    val dictPopupMaxHeight = when (fontSizePreference) {
        "medium" -> 300.dp
        "large" -> 360.dp
        else -> 240.dp
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
                    var selectedWordIndex by remember { mutableStateOf<Int?>(null) }
                    var lookupResult by remember { mutableStateOf<List<com.example.lotuspondreader.data.DictEntry>?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current

                    val renderWords = remember(sentence.words) { prepareRenderWords(sentence.words) }
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        renderWords.forEachIndexed { index, rWord ->
                            val word = rWord.word
                            val isChinese = rWord.isChinese
                            val isHighlighted = studyMode && requiredTerms.isNotEmpty() && requiredTerms.any { term -> word.contains(term) }
                            val isSelected = selectedWordIndex == index

                            val backgroundColor = when {
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                isHighlighted -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }

                            val textColor = when {
                                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val fontWeight = when {
                                isSelected || isHighlighted -> FontWeight.Bold
                                else -> FontWeight.Normal
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                            fontSize = mandarinFontSize,
                                            lineHeight = mandarinLineHeight,
                                            fontWeight = fontWeight,
                                            lineBreak = LineBreak.Paragraph
                                        ),
                                        color = textColor,
                                        modifier = Modifier
                                            .background(
                                                color = backgroundColor,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .run {
                                                if (isChinese) {
                                                    clickable {
                                                        selectedWordIndex = index
                                                        onPlayWordTts(word)
                                                        coroutineScope.launch {
                                                            isLoading = true
                                                            lookupResult = onLookupWord(word)
                                                            isLoading = false
                                                        }
                                                    }
                                                } else this
                                            }
                                            .padding(horizontal = 1.dp)
                                    )

                                    if (isSelected) {
                                        val popupPositionProvider = remember {
                                            object : PopupPositionProvider {
                                                override fun calculatePosition(
                                                    anchorBounds: IntRect,
                                                    windowSize: IntSize,
                                                    layoutDirection: LayoutDirection,
                                                    popupContentSize: IntSize
                                                ): IntOffset {
                                                    val margin = 24 // pixels
                                                    val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                                                    
                                                    val spaceAbove = anchorBounds.top
                                                    val spaceBelow = windowSize.height - anchorBounds.bottom
                                                    
                                                    val fitsAbove = spaceAbove >= popupContentSize.height + margin
                                                    val fitsBelow = spaceBelow >= popupContentSize.height + margin
                                                    
                                                    val y = if (fitsAbove) {
                                                        anchorBounds.top - popupContentSize.height - margin
                                                    } else if (fitsBelow) {
                                                        anchorBounds.bottom + margin
                                                    } else {
                                                        if (spaceAbove > spaceBelow) {
                                                            margin
                                                        } else {
                                                            (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
                                                        }
                                                    }
                                                    val clampedX = x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                                                    return IntOffset(clampedX, y)
                                                }
                                            }
                                        }

                                        Popup(
                                            popupPositionProvider = popupPositionProvider,
                                            onDismissRequest = {
                                                selectedWordIndex = null
                                                lookupResult = null
                                            },
                                            properties = PopupProperties(focusable = true)
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .width(dictPopupWidth)
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
                                                        .heightIn(max = dictPopupMaxHeight)
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
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = dictTitleFontSize,
                                                                lineHeight = dictTitleLineHeight
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
                                                            modifier = Modifier.height(dictPlecoButtonHeight),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        ) {
                                                            Text("Pleco", fontSize = dictPlecoFontSize, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Button(
                                                            onClick = {
                                                                onPlayWordTts(word)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(dictPlecoButtonHeight),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        ) {
                                                            Text("🔊", fontSize = dictPlecoFontSize)
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
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontSize = dictContentFontSize,
                                                                    lineHeight = dictContentLineHeight
                                                                ),
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
                                                                                fontWeight = FontWeight.Bold,
                                                                                fontSize = dictTraditionalFontSize,
                                                                                lineHeight = dictTraditionalLineHeight
                                                                            ),
                                                                            color = MaterialTheme.colorScheme.secondary
                                                                        )
                                                                    }
                                                                    Text(
                                                                        text = PinyinConverter.convertToToneMarks(entry.pinyin),
                                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                                            fontWeight = FontWeight.Medium,
                                                                            fontSize = dictContentFontSize,
                                                                            lineHeight = dictContentLineHeight
                                                                        ),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                    Text(
                                                                        text = PinyinConverter.convertPinyinInDefinition(entry.english),
                                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                                            fontSize = dictContentFontSize,
                                                                            lineHeight = dictContentLineHeight
                                                                        ),
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
                                if (rWord.punctuation.isNotEmpty()) {
                                    Text(
                                        text = rWord.punctuation,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                            fontSize = mandarinFontSize,
                                            lineHeight = mandarinLineHeight,
                                            lineBreak = LineBreak.Paragraph
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 1.dp)
                                    )
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
                            lineHeight = mandarinLineHeight,
                            lineBreak = LineBreak.Paragraph
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

private data class RenderWord(
    val word: String,
    val isChinese: Boolean,
    val punctuation: String = ""
)

private fun isPunctuation(text: String): Boolean {
    if (text.isEmpty()) return false
    val c = text[0]
    return c == '。' || c == '，' || c == '、' || c == '！' || c == '？' || 
           c == '：' || c == '；' || c == '“' || c == '”' || c == '（' || c == '）' ||
           c == '「' || c == '」' || c == '—' || c == '.' || c == ',' || c == '!' || c == '?'
}

private fun prepareRenderWords(words: List<String>): List<RenderWord> {
    val renderWords = mutableListOf<RenderWord>()
    var currentWord: RenderWord? = null

    words.forEach { token ->
        if (isPunctuation(token)) {
            if (currentWord != null && currentWord!!.word.trim().isNotEmpty()) {
                currentWord = currentWord!!.copy(punctuation = currentWord!!.punctuation + token)
            } else {
                val lastWordIdx = renderWords.indexOfLast { it.word.trim().isNotEmpty() }
                if (lastWordIdx != -1) {
                    renderWords[lastWordIdx] = renderWords[lastWordIdx].copy(punctuation = renderWords[lastWordIdx].punctuation + token)
                } else if (currentWord != null) {
                    currentWord = currentWord!!.copy(punctuation = currentWord!!.punctuation + token)
                } else {
                    renderWords.add(RenderWord(token, false))
                }
            }
        } else {
            if (currentWord != null) {
                renderWords.add(currentWord!!)
            }
            val isChinese = token.any { it.code in 0x4e00..0x9fff }
            currentWord = RenderWord(token, isChinese)
        }
    }
    if (currentWord != null) {
        renderWords.add(currentWord!!)
    }
    return renderWords
}
