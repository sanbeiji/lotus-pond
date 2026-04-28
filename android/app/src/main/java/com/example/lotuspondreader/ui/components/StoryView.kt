package com.example.lotuspondreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lotuspondreader.models.Sentence
import com.example.lotuspondreader.models.StoryResponse

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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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

