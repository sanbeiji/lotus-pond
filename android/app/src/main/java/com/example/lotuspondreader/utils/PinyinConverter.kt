package com.example.lotuspondreader.utils

object PinyinConverter {

    private val toneMap = mapOf(
        'a' to charArrayOf('a', 'ā', 'á', 'ǎ', 'à'),
        'A' to charArrayOf('A', 'Ā', 'Á', 'Ǎ', 'À'),
        'e' to charArrayOf('e', 'ē', 'é', 'ě', 'è'),
        'E' to charArrayOf('E', 'Ē', 'É', 'Ě', 'È'),
        'o' to charArrayOf('o', 'ō', 'ó', 'ǒ', 'ò'),
        'O' to charArrayOf('O', 'Ō', 'Ó', 'Ǒ', 'Ò'),
        'i' to charArrayOf('i', 'ī', 'í', 'ǐ', 'ì'),
        'I' to charArrayOf('I', 'Ī', 'Í', 'Ǐ', 'Ì'),
        'u' to charArrayOf('u', 'ū', 'ú', 'ǔ', 'ù'),
        'U' to charArrayOf('U', 'Ū', 'Ú', 'Ǔ', 'Ù'),
        'ü' to charArrayOf('ü', 'ǖ', 'ǘ', 'ǚ', 'ǜ'),
        'Ü' to charArrayOf('Ü', 'Ǖ', 'Ǘ', 'Ǚ', 'Ǜ')
    )

    fun convertToToneMarks(pinyin: String): String {
        if (pinyin.isBlank()) return ""
        
        // Split by whitespace
        val syllables = pinyin.trim().split(Regex("\\s+"))
        val result = StringBuilder()
        
        for (syllable in syllables) {
            result.append(convertSyllable(syllable))
        }
        
        return result.toString()
    }

    private fun convertSyllable(syllable: String): String {
        if (syllable.isEmpty()) return ""
        
        // 1. Extract tone number at the end
        val lastChar = syllable.last()
        val tone = if (lastChar in '1'..'5') {
            lastChar.digitToInt()
        } else {
            5
        }
        
        // Strip tone number if present
        var base = if (lastChar in '1'..'5') {
            syllable.substring(0, syllable.length - 1)
        } else {
            syllable
        }
        
        // 2. Replace u: / U: / v / V with ü / Ü
        base = base.replace("u:", "ü").replace("U:", "Ü")
                   .replace("v", "ü").replace("V", "Ü")
        
        // 3. Locate target vowel index to place tone mark
        val vowelIndex = findVowelIndexForTone(base)
        if (vowelIndex == -1 || tone == 5 || tone < 1 || tone > 4) {
            // Neutral tone or no vowel found: return base as is
            return base
        }
        
        // 4. Map the vowel character to its tone equivalent
        val targetChar = base[vowelIndex]
        val tonedArray = toneMap[targetChar]
        if (tonedArray != null && tone in 1..4) {
            val tonedChar = tonedArray[tone]
            val sb = java.lang.StringBuilder(base)
            sb.setCharAt(vowelIndex, tonedChar)
            return sb.toString()
        }
        
        return base
    }

    private fun findVowelIndexForTone(base: String): Int {
        // Priority 1: a/A
        val idxA = indexOfCaseInsensitive(base, 'a')
        if (idxA != -1) return idxA
        
        // Priority 2: e/E
        val idxE = indexOfCaseInsensitive(base, 'e')
        if (idxE != -1) return idxE
        
        // Priority 3: o/O
        val idxO = indexOfCaseInsensitive(base, 'o')
        if (idxO != -1) return idxO
        
        // Priority 4: ui/UI/Ui/uI or iu/IU/Iu/iU
        // For 'ui' -> tone on 'i'; for 'iu' -> tone on 'u'
        val idxUi = base.indexOf("ui", ignoreCase = true)
        if (idxUi != -1) return idxUi + 1
        
        val idxIu = base.indexOf("iu", ignoreCase = true)
        if (idxIu != -1) return idxIu + 1
        
        // Priority 5: otherwise, first vowel from list: i, u, ü
        for (i in base.indices) {
            val char = base[i]
            if (char == 'i' || char == 'I' ||
                char == 'u' || char == 'U' ||
                char == 'ü' || char == 'Ü') {
                return i
            }
        }
        
        return -1
    }

    private fun indexOfCaseInsensitive(str: String, char: Char): Int {
        val lowerChar = char.lowercaseChar()
        for (i in str.indices) {
            if (str[i].lowercaseChar() == lowerChar) {
                return i
            }
        }
        return -1
    }
}
