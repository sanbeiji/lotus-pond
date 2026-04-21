package com.example.lotuspondreader.api

import com.example.lotuspondreader.models.StoryResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max

class StoryRepository {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    suspend fun generateStory(
        apiKey: String,
        model: String,
        plot: String,
        skillLevel: String,
        length: Int,
        requiredTerms: String,
        pronunciation: String
    ): StoryResponse {
        val prompt = buildPrompt(plot, skillLevel, length, requiredTerms, pronunciation)
        
        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig()
        )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}"
        
        val response: GeminiResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        if (response.error != null) {
            throw Exception(response.error.message)
        }

        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No content returned from Gemini API")

        return parseResponse(responseText, pronunciation)
    }

    private fun parseResponse(text: String, pronunciationField: String): StoryResponse {
        try {
            // 1. Try clean parse
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                val cleanJson = text.substring(start, end + 1)
                try {
                    return jsonConfig.decodeFromString(cleanJson)
                } catch (e: Exception) {
                    // Fallthrough to recovery
                }
            }

            // 2. Recovery mode using robust Regex for incomplete JSON
            val sentenceRegex = Regex("""\{\s*"mandarin"\s*:\s*"(.*?)"\s*,\s*"$pronunciationField"\s*:\s*"(.*?)"\s*,\s*"english"\s*:\s*"(.*?)"\s*\}""", RegexOption.DOT_MATCHES_ALL)
            val sentences = mutableListOf<com.example.lotuspondreader.models.Sentence>()
            
            sentenceRegex.findAll(text).forEach { matchResult ->
                val mandarin = matchResult.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
                val pron = matchResult.groupValues[2].replace("\\\"", "\"").replace("\\n", "\n")
                val english = matchResult.groupValues[3].replace("\\\"", "\"").replace("\\n", "\n")
                
                val sentence = if (pronunciationField == "zhuyin") {
                    com.example.lotuspondreader.models.Sentence(mandarin = mandarin, zhuyin = pron, english = english)
                } else {
                    com.example.lotuspondreader.models.Sentence(mandarin = mandarin, pinyin = pron, english = english)
                }
                sentences.add(sentence)
            }

            if (sentences.isNotEmpty()) {
                val titleMatch = Regex(""""title"\s*:\s*"([^"]*)"""").find(text)
                val title = titleMatch?.groupValues?.get(1) ?: "Recovered Story (Incomplete)"
                return StoryResponse(title = "$title [Truncated]", sentences = sentences)
            }
            throw Exception("Could not find any valid story sentences in the response.")
        } catch (e: Exception) {
            throw Exception("Failed to parse AI response. The story may be too long for the AI to finish. Try a shorter length. Error: ${e.message}")
        }
    }

    private fun buildPrompt(
        plot: String,
        skillLevel: String,
        length: Int,
        requiredTerms: String,
        pronunciation: String
    ): String {
        val levelGuide = """
            SKILL LEVEL DEFINITIONS (TOCFL BANDS):
            - Novice 1/2: Extremely simple S-V-O sentences. Use only the most basic daily vocabulary (numbers, greetings, colors, family). Avoid all complex grammar.
            - A1 (Entry): Basic social interactions. Simple daily topics (shopping, weather). Clear, short sentences.
            - A2 (Foundation): Common life situations. Basic connectors (because, but). Simple descriptions of past/future events.
            - B3 (Intermediate): Fluent daily communication. Use of more varied conjunctions and descriptive adverbs. Discussion of work/travel.
            - B4 (Upper Intermediate): Can discuss abstract topics. Uses passive voice and complex relative clauses. Varied vocabulary.
            - C5 (Fluent): Professional and academic topics. High-level idioms and nuanced cultural expressions.
            - C6 (Advanced): Academic, technical, and literary proficiency. Use of sophisticated Chengyu (idioms), classical structures, and nuanced stylistic variances.
        """.trimIndent()

        val sentenceTarget = max(3, ceil(length / 22.0).toInt())

        val novellaInstruction = if (length > 1000) {
            """
            The requested story is a NOVELLA (at least $length characters). 
            You MUST structure it as a 5-chapter story with distinct scenes for each chapter. 
            Expand on the world-building, sensory details, internal character thoughts, and extensive dialogue. 
            DO NOT SUMMARIZE. Write as if you are a professional author.
            """.trimIndent()
        } else ""

        val lengthPriority = if (length > 1000) "ABSOLUTE HIGHEST priority" else "important target"
        val lengthAdjective = if (length > 1000) "AT LEAST" else "approximately"

        val pronLabel = if (pronunciation == "zhuyin") "zhuyin" else "pinyin"
        val pronInstruction = if (pronunciation == "zhuyin") "Zhuyin/Bopomofo pronunciation here" else "Pinyin pronunciation here (Taiwanese style, e.g. 'hàn')"

        return """
            You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used for the purposes of learning to read, write, and speak Mandarin. 

            $levelGuide

            CRITICAL LINGUISTIC REQUIREMENTS:
            1. TRADITIONAL CHARACTERS: Use traditional Mandarin characters only.
            2. TAIWANESE STYLE: Use grammar, slang, and idioms common to Taiwan (e.g., use 影片 instead of 視頻, 捷運 instead of 地鐵, 腳踏車 instead of 自行車).
            3. TAIWANESE PRONUNCIATION: The ${pronunciation.uppercase()} MUST reflect local Taiwanese pronunciation. 
               - CRUCIAL: '和' must be pronounced 'hàn' (not 'hé').
               - Use other Taiwanese variations where applicable (e.g., 垃圾 as 'lèsè').
            4. CULTURAL & GEOGRAPHICAL BREADTH: Explore the full diversity of Taiwan. Do not over-rely on Taipei or common tropes. 
               - GEOGRAPHY: Vary the settings across different cities (e.g., Taichung, Tainan, Hualien, Keelung), counties (e.g., Yilan, Pingtung, Nantou), and landscapes (high mountain tea farms, coastal fishing villages, bustling night markets, quiet rural towns).
               - CULTURE: Incorporate a wide range of Taiwanese life, such as temple festivals, traditional arts (like glove puppetry), tea ceremonies, hiking culture, family dynamics, local snacks (小吃), and historical landmarks.
               - SOCIAL NORMS: Reflect authentic Taiwanese social etiquette and daily interactions.
            5. SKILL LEVEL: Adhere strictly to the $skillLevel level requirements defined above.
            6. VOCABULARY INTEGRATION: If specific vocabulary terms are provided ("$requiredTerms"), you MUST include EVERY term at least TWICE in the story. Ensure they are used naturally but frequently enough for the reader to practice them. Integrate them into both narrative and dialogue where appropriate.
            7. STRUCTURE: Break the story into logical sentences. Each sentence must be its own object in the response.

            OUTPUT FORMAT:
            You must return a valid JSON object with NO OTHER TEXT before or after the JSON. DO NOT include markdown code blocks.
            The JSON must follow this exact structure:
            {
              "title": "Story Title in Traditional Mandarin",
              "sentences": [
                {
                  "mandarin": "Mandarin sentence here",
                  "$pronLabel": "$pronInstruction",
                  "english": "Natural English translation here"
                }
              ]
            }

            CRITICAL LENGTH REQUIREMENT:
            $novellaInstruction
            The user has requested a story of $lengthAdjective $length Mandarin characters.
            To achieve this, you MUST:
            - Generate approximately $sentenceTarget sentences.
            - Do not summarize. 
            - This length requirement is an $lengthPriority.

            Plot for the story: $plot
        """.trimIndent()
    }
}
