package com.example.lotuspondreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lotuspondreader.models.Sentence
import com.example.lotuspondreader.models.StoryResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryView(
    story: StoryResponse,
    showPronunciation: Boolean,
    pronunciationType: String,
    showTranslation: Boolean,
    studyMode: Boolean,
    fontSizePreference: String,
    requiredTerms: List<String>,
    onPlayAudio: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(story.sentences) { sentence ->
            SentenceBlock(
                sentence = sentence,
                showPronunciation = showPronunciation,
                pronunciationType = pronunciationType,
                showTranslation = showTranslation,
                studyMode = studyMode,
                fontSizePreference = fontSizePreference,
                requiredTerms = requiredTerms,
                onPlayAudio = onPlayAudio
            )
        }

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
                            if (showPronunciation) {
                                val pron = if (pronunciationType == "zhuyin") s.zhuyin else s.pinyin
                                if (!pron.isNullOrEmpty()) {
                                    sb.append(pron).append("\n")
                                }
                            }
                            if (showTranslation) {
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
    }
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
            showPronunciation = true,
            pronunciationType = "pinyin",
            showTranslation = true,
            studyMode = true,
            fontSizePreference = "small",
            requiredTerms = listOf("夜市", "朋友"),
            onPlayAudio = {}
        )
    }
}

