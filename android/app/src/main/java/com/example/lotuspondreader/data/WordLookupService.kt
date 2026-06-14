package com.example.lotuspondreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

object WordLookupService {

    suspend fun lookupWord(word: String, dictDao: DictDao): List<DictEntry> = withContext(Dispatchers.IO) {
        val cleaned = word.filter { it.code in 0x4e00..0x9fff }
        if (cleaned.isEmpty()) {
            return@withContext emptyList()
        }
        val directResult = dictDao.lookupWord(cleaned)
        if (directResult.isNotEmpty()) {
            return@withContext directResult
        }

        // Fallback: Segment and lookup sub-words using Left-to-Right Maximum Matching
        return@withContext segmentAndLookup(cleaned, dictDao)
    }

    private suspend fun segmentAndLookup(word: String, dictDao: DictDao): List<DictEntry> {
        val result = mutableListOf<DictEntry>()
        var i = 0
        val n = word.length
        
        while (i < n) {
            var found = false
            // Check substrings starting at i, from max length 4 down to 1
            val maxLen = min(4, n - i)
            for (len in maxLen downTo 1) {
                val substring = word.substring(i, i + len)
                val entries = dictDao.lookupWord(substring)
                if (entries.isNotEmpty()) {
                    result.addAll(entries)
                    i += len
                    found = true
                    break
                }
            }
            if (!found) {
                // If no dictionary entry exists for any substring starting at i,
                // skip or advance by 1 character to avoid infinite loop.
                i += 1
            }
        }
        return result
    }
}
