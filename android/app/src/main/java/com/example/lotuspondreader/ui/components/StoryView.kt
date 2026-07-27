package com.example.lotuspondreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Popup
import com.example.lotuspondreader.models.Sentence
import com.example.lotuspondreader.models.StoryResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryView(
    story: StoryResponse,
    showPinyin: Boolean,
    showZhuyin: Boolean,
    showTranslation: Boolean,
    studyMode: Boolean,
    fontSizePreference: String,
    requiredTerms: List<String>,
    onPlayAudio: (String) -> Unit,
    onLookupWord: suspend (String) -> List<com.example.lotuspondreader.data.DictEntry>,
    onPlayWordTts: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    readerStyle: String = "sentence",
    activePlayingSentenceIndex: Int? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (readerStyle == "paragraph") {
            // Group sentences into paragraphs of 3 sentences each
            val sentenceParagraphs = story.sentences.chunked(3)

            // 1. Mandarin Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Mandarin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        sentenceParagraphs.forEachIndexed { pIndex, pSentences ->
                            if (pIndex > 0) {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                            MandarinParagraph(
                                pSentences = pSentences,
                                startSentenceIndex = pIndex * 3,
                                activePlayingSentenceIndex = activePlayingSentenceIndex,
                                studyMode = studyMode,
                                requiredTerms = requiredTerms,
                                mandarinFontSize = mandarinFontSize,
                                mandarinLineHeight = mandarinLineHeight,
                                dictTitleFontSize = dictTitleFontSize,
                                dictTitleLineHeight = dictTitleLineHeight,
                                dictTraditionalFontSize = dictTraditionalFontSize,
                                dictTraditionalLineHeight = dictTraditionalLineHeight,
                                dictContentFontSize = dictContentFontSize,
                                dictContentLineHeight = dictContentLineHeight,
                                dictPlecoButtonHeight = dictPlecoButtonHeight,
                                dictPlecoFontSize = dictPlecoFontSize,
                                dictPopupWidth = dictPopupWidth,
                                dictPopupMaxHeight = dictPopupMaxHeight,
                                onLookupWord = onLookupWord,
                                onPlayWordTts = onPlayWordTts
                            )
                        }
                    }
                }
            }

            // 2. Pinyin Section (if enabled)
            if (showPinyin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Pinyin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            sentenceParagraphs.forEachIndexed { pIndex, pSentences ->
                                if (pIndex > 0) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                                val pinyinText = pSentences.mapNotNull { it.pinyin }.joinToString(" ")
                                Text(
                                    text = pinyinText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                        fontSize = pinyinFontSize,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Zhuyin Section (if enabled)
            if (showZhuyin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Zhuyin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            sentenceParagraphs.forEachIndexed { pIndex, pSentences ->
                                if (pIndex > 0) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                                val zhuyinText = pSentences.mapNotNull { it.zhuyin }.joinToString(" ")
                                Text(
                                    text = zhuyinText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = com.example.lotuspondreader.theme.IansuiFontFamily,
                                        fontSize = pinyinFontSize
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. English Section (if enabled)
            if (showTranslation) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "English Translation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            sentenceParagraphs.forEachIndexed { pIndex, pSentences ->
                                if (pIndex > 0) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                                val englishText = pSentences.mapNotNull { it.english }.joinToString(" ")
                                Text(
                                    text = englishText,
                                    style = englishStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // Default: Individual sentences view
            items(story.sentences) { sentence ->
                SentenceBlock(
                    sentence = sentence,
                    showPinyin = showPinyin,
                    showZhuyin = showZhuyin,
                    showTranslation = showTranslation,
                    studyMode = studyMode,
                    fontSizePreference = fontSizePreference,
                    requiredTerms = requiredTerms,
                    onPlayAudio = onPlayAudio,
                    onLookupWord = onLookupWord,
                    onPlayWordTts = onPlayWordTts
                )
            }
        }

        // Copy Story Button
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = {
                        val sb = StringBuilder()
                        sb.append(story.title).append("\n\n")
                        story.sentences.forEach { s ->
                            sb.append(s.mandarin).append("\n")
                            if (showPinyin && s.pinyin != null) {
                                sb.append(s.pinyin).append("\n")
                            }
                            if (showZhuyin && s.zhuyin != null) {
                                sb.append(s.zhuyin).append("\n")
                            }
                            if (showTranslation && s.english != null) {
                                sb.append(s.english).append("\n")
                            }
                            sb.append("\n")
                        }
                        clipboardManager.setText(AnnotatedString(sb.toString().trimEnd()))
                        copied = true
                        coroutineScope.launch {
                            delay(2000)
                            copied = false
                        }
                    }
                ) {
                    Text(if (copied) "✅ Copied!" else "Copy story")
                }
            }
        }

        story.modelUsed?.takeIf { it.isNotBlank() }?.let { model ->
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Model used: $model",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MandarinParagraph(
    pSentences: List<Sentence>,
    startSentenceIndex: Int,
    activePlayingSentenceIndex: Int?,
    studyMode: Boolean,
    requiredTerms: List<String>,
    mandarinFontSize: androidx.compose.ui.unit.TextUnit,
    mandarinLineHeight: androidx.compose.ui.unit.TextUnit,
    dictTitleFontSize: androidx.compose.ui.unit.TextUnit,
    dictTitleLineHeight: androidx.compose.ui.unit.TextUnit,
    dictTraditionalFontSize: androidx.compose.ui.unit.TextUnit,
    dictTraditionalLineHeight: androidx.compose.ui.unit.TextUnit,
    dictContentFontSize: androidx.compose.ui.unit.TextUnit,
    dictContentLineHeight: androidx.compose.ui.unit.TextUnit,
    dictPlecoButtonHeight: androidx.compose.ui.unit.Dp,
    dictPlecoFontSize: androidx.compose.ui.unit.TextUnit,
    dictPopupWidth: androidx.compose.ui.unit.Dp,
    dictPopupMaxHeight: androidx.compose.ui.unit.Dp,
    onLookupWord: suspend (String) -> List<com.example.lotuspondreader.data.DictEntry>,
    onPlayWordTts: (String) -> Unit
) {
    var selectedWordIndex by remember { mutableStateOf<Int?>(null) }
    var lookupResult by remember { mutableStateOf<List<com.example.lotuspondreader.data.DictEntry>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Flatten words with global sentence indices
    val paragraphWords = remember(pSentences) {
        val list = mutableListOf<ParagraphWord>()
        pSentences.forEachIndexed { sIndexInParagraph, sentence ->
            val globalSentenceIndex = startSentenceIndex + sIndexInParagraph
            val wordsList = if (sentence.words.isNotEmpty()) sentence.words else listOf(sentence.mandarin)
            wordsList.forEachIndexed { wIndex, word ->
                list.add(ParagraphWord(word, globalSentenceIndex, wIndex))
            }
        }
        list
    }
    val renderWords = remember(paragraphWords) { prepareParagraphRenderWords(paragraphWords) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        renderWords.forEachIndexed { index, rWord ->
            val word = rWord.word
            val isChinese = rWord.isChinese
            val isHighlighted = studyMode && requiredTerms.isNotEmpty() && requiredTerms.any { term -> word.contains(term) }
            val isSelected = selectedWordIndex == index
            val isSentenceActive = rWord.globalSentenceIndex == activePlayingSentenceIndex

            val backgroundColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                isSentenceActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                isHighlighted -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }

            val textColor = when {
                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                isSentenceActive -> MaterialTheme.colorScheme.onPrimaryContainer
                isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            val fontWeight = when {
                isSelected || isSentenceActive || isHighlighted -> FontWeight.Bold
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
                            object : androidx.compose.ui.window.PopupPositionProvider {
                                override fun calculatePosition(
                                    anchorBounds: androidx.compose.ui.unit.IntRect,
                                    windowSize: androidx.compose.ui.unit.IntSize,
                                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                    popupContentSize: androidx.compose.ui.unit.IntSize
                                ): androidx.compose.ui.unit.IntOffset {
                                    val margin = 24
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
                                    return androidx.compose.ui.unit.IntOffset(clampedX, y)
                                }
                            }
                        }

                        Popup(
                            popupPositionProvider = popupPositionProvider,
                            onDismissRequest = {
                                selectedWordIndex = null
                                lookupResult = null
                            },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
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
                                                        text = com.example.lotuspondreader.utils.PinyinConverter.convertToToneMarks(entry.pinyin),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = dictContentFontSize,
                                                            lineHeight = dictContentLineHeight
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = com.example.lotuspondreader.utils.PinyinConverter.convertPinyinInDefinition(entry.english),
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
}

private data class ParagraphWord(
    val word: String,
    val globalSentenceIndex: Int,
    val wordIndexInSentence: Int
)

private data class ParagraphRenderWord(
    val word: String,
    val isChinese: Boolean,
    val globalSentenceIndex: Int,
    val wordIndexInSentence: Int,
    val punctuation: String = ""
)

private fun isPunctuation(text: String): Boolean {
    if (text.isEmpty()) return false
    val c = text[0]
    return c == '。' || c == '，' || c == '、' || c == '！' || c == '？' || 
           c == '：' || c == '；' || c == '“' || c == '”' || c == '（' || c == '）' ||
           c == '「' || c == '」' || c == '—' || c == '.' || c == ',' || c == '!' || c == '?'
}

private fun prepareParagraphRenderWords(pWords: List<ParagraphWord>): List<ParagraphRenderWord> {
    val renderWords = mutableListOf<ParagraphRenderWord>()
    var currentWord: ParagraphRenderWord? = null

    pWords.forEach { pWord ->
        if (isPunctuation(pWord.word)) {
            if (currentWord != null && currentWord!!.word.trim().isNotEmpty()) {
                currentWord = currentWord!!.copy(punctuation = currentWord!!.punctuation + pWord.word)
            } else {
                val lastWordIdx = renderWords.indexOfLast { it.word.trim().isNotEmpty() }
                if (lastWordIdx != -1) {
                    renderWords[lastWordIdx] = renderWords[lastWordIdx].copy(punctuation = renderWords[lastWordIdx].punctuation + pWord.word)
                } else if (currentWord != null) {
                    currentWord = currentWord!!.copy(punctuation = currentWord!!.punctuation + pWord.word)
                } else {
                    renderWords.add(ParagraphRenderWord(pWord.word, false, pWord.globalSentenceIndex, pWord.wordIndexInSentence))
                }
            }
        } else {
            if (currentWord != null) {
                renderWords.add(currentWord!!)
            }
            val isChinese = pWord.word.any { it.code in 0x4e00..0x9fff }
            currentWord = ParagraphRenderWord(pWord.word, isChinese, pWord.globalSentenceIndex, pWord.wordIndexInSentence)
        }
    }
    if (currentWord != null) {
        renderWords.add(currentWord!!)
    }
    return renderWords
}

@Preview(showBackground = true)
@Composable
fun StoryViewPreview() {
    MaterialTheme {
        val dummyStory = StoryResponse(
            title = "神奇的夜市",
            sentences = listOf(
                Sentence(
                    mandarin = "今天晚上，我和朋友去台北的夜市。",
                    pinyin = "Jīntiān wǎnshang, wǒ hàn péngyou qù Táiběi de yèshì.",
                    english = "Tonight, my friend and I went to the night market in Taipei."
                ),
                Sentence(
                    mandarin = "夜市裡有很多人，也有很多好吃的食物。",
                    pinyin = "Yèshì lǐ yǒu hěn duō rén, yě yǒu hěn duō hǎochī de shíwù.",
                    english = "There are many people in the night market, and also a lot of delicious food."
                )
            )
        )
        StoryView(
            story = dummyStory,
            showPinyin = true,
            showZhuyin = false,
            showTranslation = true,
            studyMode = true,
            fontSizePreference = "small",
            requiredTerms = listOf("夜市", "朋友"),
            onPlayAudio = {},
            onLookupWord = { emptyList() }
        )
    }
}
