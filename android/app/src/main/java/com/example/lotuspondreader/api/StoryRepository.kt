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

@kotlinx.serialization.Serializable
private data class DynamicFetchResult(val result: List<String>)

class StoryRepository {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createClient(timeoutSeconds: Long) = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    private val standardClient = createClient(60)
    private val extendedClient = createClient(180)

    suspend fun generateStory(
        apiKey: String,
        model: String,
        plot: String,
        skillLevel: String,
        length: Int,
        requiredTerms: String
    ): StoryResponse {
        try {
            val prompt = buildPrompt(plot, skillLevel, length, requiredTerms)
            
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 8192,
                    responseMimeType = "application/json"
                )
            )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}"
            
            val activeClient = if (model == "gemini-flash-latest") extendedClient else standardClient
            val response: GeminiResponse = activeClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            if (response.error != null) {
                throw Exception(response.error.message)
            }

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned from Gemini API")

            val parsedStory = parseResponse(responseText)
            return parsedStory.copy(requiredTerms = requiredTerms)
        } catch (e: Exception) {
            val msg = e.message ?: "Network request failed"
            val sanitizedMsg = if (apiKey.isNotBlank()) msg.replace(apiKey, "[REDACTED]") else msg
            throw Exception(sanitizedMsg)
        }
    }

    suspend fun fetchDynamicContent(
        apiKey: String,
        model: String,
        type: String,
        sentences: List<String>
    ): List<String> {
        try {
            val prompt = when (type) {
                "pinyin" -> "Generate Pinyin pronunciation (Taiwanese style, e.g. '和' as 'hàn') for the following Traditional Mandarin sentences. You MUST adhere to these strict Pinyin guidelines: 1. Capitalization: Capitalize first letter of each sentence, proper nouns (Běijīng, Zhōngguó), and personal names (e.g. Wáng Xiǎoyún). 2. Word Grouping: Group multi-syllable words continuously (fánguǎn, not fán guǎn), separate distinct words with spaces (Wǒ qù fánguǎn), keep particles (de, le, ma) as separate words, and use apostrophes before a, e, or o for ambiguous boundaries (píng'ān). Return ONLY a valid JSON object with a \"result\" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${sentences.joinToString(";")}"
                "zhuyin" -> "Generate Zhuyin/Bopomofo pronunciation for the following Traditional Mandarin sentences. Return ONLY a valid JSON object with a \"result\" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${sentences.joinToString(";")}"
                else -> "Generate natural English translations for the following Traditional Mandarin sentences. Return ONLY a valid JSON object with a \"result\" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${sentences.joinToString(";")}"
            }

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 8192,
                    responseMimeType = "application/json"
                )
            )

            val fetchModel = "gemini-flash-lite-latest"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${fetchModel}:generateContent?key=${apiKey}"
            
            val response: GeminiResponse = standardClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            if (response.error != null) {
                throw Exception(response.error.message)
            }

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned from Gemini API")

            val cleanedText = responseText.replace("```json", "").replace("```", "").trim()
            val start = cleanedText.indexOf('{')
            val end = cleanedText.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                val cleanJson = cleanedText.substring(start, end + 1)
                try {
                    val res: DynamicFetchResult = jsonConfig.decodeFromString(cleanJson)
                    if (res.result.size != sentences.size) {
                        throw Exception("Length mismatch in dynamic content fetch")
                    }
                    return res.result
                } catch (e: Exception) {
                    // Fall through to fallback array parsing
                }
            }

            val arrStart = cleanedText.indexOf('[')
            val arrEnd = cleanedText.lastIndexOf(']')
            if (arrStart != -1 && arrEnd != -1 && arrEnd > arrStart) {
                val cleanArr = cleanedText.substring(arrStart, arrEnd + 1)
                try {
                    val list: List<String> = jsonConfig.decodeFromString(cleanArr)
                    if (list.size != sentences.size) {
                        throw Exception("Length mismatch in dynamic content fetch")
                    }
                    return list
                } catch (e: Exception) {
                    throw Exception("Failed to parse dynamic content response: ${e.message}")
                }
            }
            throw Exception("Invalid response structure from Gemini API")
        } catch (e: Exception) {
            val msg = e.message ?: "Dynamic content fetch failed"
            val sanitizedMsg = if (apiKey.isNotBlank()) msg.replace(apiKey, "[REDACTED]") else msg
            throw Exception(sanitizedMsg)
        }
    }

    suspend fun generateGenrePrompt(
        apiKey: String,
        genre: String
    ): String {
        try {
            val prompt = "Generate a creative, engaging story premise in English suitable for a Mandarin learning story in the \"$genre\" genre. The premise must be between 1 and 4 sentences long. It should set up an interesting plot, setting, or character dilemma, preferably reflecting Taiwanese culture, geography, or context. Return ONLY the raw story premise text. Do not include titles, quotes, markdown, JSON, or explanation."
            
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 8192,
                    responseMimeType = "application/json"
                )
            )

            val fetchModel = "gemini-flash-lite-latest"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${fetchModel}:generateContent?key=${apiKey}"
            
            val response: GeminiResponse = standardClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            if (response.error != null) {
                throw Exception(response.error.message)
            }

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned from Gemini API")

            return responseText.replace("[\"'“”‘’]".toRegex(), "").trim()
        } catch (e: Exception) {
            val msg = e.message ?: "Genre prompt generation failed"
            val sanitizedMsg = if (apiKey.isNotBlank()) msg.replace(apiKey, "[REDACTED]") else msg
            throw Exception(sanitizedMsg)
        }
    }

    suspend fun generateSpeech(apiKey: String, text: String, voiceStyle: String): String {
        try {
            val voicePrompt = when (voiceStyle) {
                "southern" -> "[Voice Style: Speak gently, softly, and reassuringly, at a relaxed pace with extreme warmth.]\nRead in a warm, relaxed, authentic Southern Taiwanese Mandarin (台灣國語) regional accent (popular in Tainan, Kaohsiung, and Pingtung).\n- Speak with a friendly, local Taiwanese cadence and relaxed mouth positioning.\n- Strictly avoid Beijing-style speech: absolutely no curl-tongue \"er\" (no 兒化音) and do not retroflex sounds like zh, ch, sh (pronounce them shifted toward z, c, s, e.g. 知道 sounds like zīdào, 是 sounds like sì).\n- Do not suppress tones into neutral short tones (輕聲), pronounce grammatically light words with their full traditional Taiwanese Mandarin tones (e.g. 舒服 is shūfú, 先生 is xiānshēng).\n- Keep any natural sentence-final particles from Taiwan (like '啦', '齁', '喔', '欸') represented with authentic, comfortable, musical southern cadence."
                "heavy_southern" -> "[Voice Style: Speak extremely casually, off-the-cuff, like chatting with a close family member or childhood friend.]\nRead in a local, very down-to-earth, thick Southern Taiwanese Mandarin colloquial style (重度南部腔台灣國語) with strong Taiwanese (Minnan/Hokkien) substrate.\n- Sound like a friendly neighbor from Tainan or Kaohsiung speaking casual Mandarin.\n- Strongly dentalize retroflexes (zh, ch, sh -> z, c, s, e.g., 船 sounds like cuán, 睡 sounds like suì).\n- Use Minnan speech substrate pitch and rhythm. Syllable-final nasals \"eng\" and \"en\" can merge with \"ing\" and \"in\", or open slightly (e.g. 朋友 sounds like píngyǒu or péng-ǐou).\n- Do not use neutral/light tones (輕聲); give everything comfortable, rich Taiwanese tones.\n- If natural, blend f and h sounds gently (e.g., 飯 fàn sounds like huàn, 發生 fāshēng sounds like huāshēng).\n- Keep the cadence relaxed, friendly, and expressive, showing regional southern warmth."
                else -> "[Voice Style: Speak in a natural, standard, clear reading style.]\nRead in standard, clear Taiwanese Mandarin (都會風格台北/台灣腔) as heard in public announcements (like the Taipei MRT) or urban professional settings.\n- Speak in a natural, clean, moderately fast, modern Taiwanese tempo.\n- Retroflex sounds (zh, ch, sh) are relaxed and naturally simplified, avoiding any dry retroflex friction or thick northern Beijing acoustics. No \"er\" (no 兒化音).\n- Render neutral tones (輕聲) in accordance with general urban Taiwanese Mandarin usage (typically pronounced as lighter full tones rather than clipped neutral vowels).\n- Deliver with a clean, melodic, polite, and professional Taiwanese tone."
            }

            val fullText = "$voicePrompt\n\nPlease recite the following text exactly as requested: \"$text\""

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = fullText)))),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = SpeechConfig(
                        voiceConfig = VoiceConfig(
                            prebuiltVoiceConfig = PrebuiltVoiceConfig(
                                voiceName = "Kore"
                            )
                        )
                    )
                )
            )

            val fetchModel = "gemini-3.1-flash-tts-preview"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${fetchModel}:generateContent?key=$apiKey"
            
            val response: GeminiResponse = standardClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            if (response.error != null) {
                throw Exception(response.error.message)
            }

            val candidate = response.candidates?.firstOrNull()
            val part = candidate?.content?.parts?.find { it.inlineData != null && it.inlineData.mimeType.startsWith("audio/") }
            
            if (part?.inlineData?.data == null) {
                throw Exception("No audio returned from Gemini API")
            }

            return part.inlineData.data
        } catch (e: Exception) {
            val msg = e.message ?: "Audio generation failed"
            val sanitizedMsg = if (apiKey.isNotBlank()) msg.replace(apiKey, "[REDACTED]") else msg
            throw Exception(sanitizedMsg)
        }
    }

    private fun parseResponse(
        text: String
    ): StoryResponse {
        try {
            // 1. Try clean parse
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                val cleanJson = text.substring(start, end + 1)
                try {
                    return jsonConfig.decodeFromString(cleanJson)
                } catch (e: Exception) {
                }
            }

            // 2. Recovery mode
            val sentenceRegex = Regex("""\{\s*"mandarin"\s*:\s*"(.*?)"\s*\}""", RegexOption.DOT_MATCHES_ALL)
            val sentences = mutableListOf<com.example.lotuspondreader.models.Sentence>()
            
            sentenceRegex.findAll(text).forEach { matchResult ->
                val mandarin = matchResult.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
                sentences.add(com.example.lotuspondreader.models.Sentence(mandarin = mandarin))
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
        requiredTerms: String
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

        return """
            You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used for the purposes of learning to read, write, and speak Mandarin. 

            $levelGuide

            CRITICAL LINGUISTIC REQUIREMENTS:
            1. TRADITIONAL CHARACTERS: Use traditional Mandarin characters only.
            2. TAIWANESE STYLE: Use grammar, slang, and idioms common to Taiwan (e.g., use 影片 instead of 視頻, 捷運 instead of 地鐵, 腳踏車 instead of 自行車).
            3. CULTURAL & GEOGRAPHICAL BREADTH: Explore the full diversity of Taiwan. Do not over-rely on Taipei or common tropes. 
               - GEOGRAPHY: Vary the settings across different cities (e.g., Taichung, Tainan, Hualien, Keelung), counties (e.g., Yilan, Pingtung, Nantou), and landscapes (high mountain tea farms, coastal fishing villages, bustling night markets, quiet rural towns).
               - CULTURE: Incorporate a wide range of Taiwanese life, such as temple festivals, traditional arts (like glove puppetry), tea ceremonies, hiking culture, family dynamics, local snacks (小吃), and historical landmarks.
               - SOCIAL NORMS: Reflect authentic Taiwanese social etiquette and daily interactions.
            4. SKILL LEVEL: Adhere strictly to the $skillLevel level requirements defined above.
            5. VOCABULARY INTEGRATION: If specific vocabulary terms are provided ("$requiredTerms"), you MUST include EVERY term at least TWICE in the story. Ensure they are used naturally but frequently enough for the reader to practice them. Integrate them into both narrative and dialogue where appropriate.
            6. STRUCTURE: Break the story into logical sentences. Each sentence must be its own object in the response.

            OUTPUT FORMAT:
            You must return a valid JSON object with NO OTHER TEXT before or after the JSON. DO NOT include markdown code blocks.
            The JSON must follow this exact structure:
            {
              "title": "Story Title in Traditional Mandarin",
              "sentences": [
                {
                  "mandarin": "Mandarin sentence here"
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
