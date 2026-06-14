package com.example.lotuspondreader.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLookupServiceTest {

    // A fake DictDao that holds a simple in-memory map for lookups
    private class FakeDictDao(private val db: Map<String, List<DictEntry>>) : DictDao {
        override suspend fun insertAll(entries: List<DictEntry>) {
            // Not needed for testing lookup
        }

        override suspend fun lookupWord(word: String): List<DictEntry> {
            return db[word] ?: emptyList()
        }

        override suspend fun count(): Int {
            return db.size
        }

        override suspend fun clearAll() {
            // Not needed for testing lookup
        }
    }

    @Test
    fun testLookupDirectMatch() = runBlocking {
        val db = mapOf(
            "珍珠奶茶" to listOf(DictEntry(traditional = "珍珠奶茶", simplified = "珍珠奶茶", pinyin = "zhen1 zhu1 nai3 cha2", english = "bubble tea"))
        )
        val fakeDao = FakeDictDao(db)
        
        val results = fakeDao.lookupWord("珍珠奶茶")
        assertEquals(1, results.size)
        assertEquals("bubble tea", results[0].english)
    }

    @Test
    fun testFallbackSubWordSegmentation() = runBlocking {
        // Mock a dictionary that has "珍珠奶茶" and "店", but NOT the combined "珍珠奶茶店"
        val db = mapOf(
            "珍珠奶茶" to listOf(DictEntry(traditional = "珍珠奶茶", simplified = "珍珠奶茶", pinyin = "zhen1 zhu1 nai3 cha2", english = "bubble tea")),
            "店" to listOf(DictEntry(traditional = "店", simplified = "店", pinyin = "dian4", english = "shop"))
        )
        
        val fakeDao = FakeDictDao(db)
        val results = WordLookupService.lookupWord("珍珠奶茶店", fakeDao)
        
        // The service should segment "珍珠奶茶店" -> "珍珠奶茶" + "店" and return entries for both
        assertEquals(2, results.size)
        assertEquals("珍珠奶茶", results[0].traditional)
        assertEquals("bubble tea", results[0].english)
        assertEquals("店", results[1].traditional)
        assertEquals("shop", results[1].english)
    }

    @Test
    fun testFallbackWithUnknownCharacters() = runBlocking {
        // Mock dictionary with "珍珠" and "茶", but not "奶"
        val db = mapOf(
            "珍珠" to listOf(DictEntry(traditional = "珍珠", simplified = "珍珠", pinyin = "zhen1 zhu1", english = "pearl")),
            "茶" to listOf(DictEntry(traditional = "茶", simplified = "茶", pinyin = "cha2", english = "tea"))
        )
        
        val fakeDao = FakeDictDao(db)
        val results = WordLookupService.lookupWord("珍珠奶茶", fakeDao)
        
        // It should match "珍珠", skip "奶" (not in dict), match "茶", and return entries for "珍珠" and "茶"
        assertEquals(2, results.size)
        assertEquals("珍珠", results[0].traditional)
        assertEquals("茶", results[1].traditional)
    }

    @Test
    fun testLookupWithPunctuationAndWhitespace() = runBlocking {
        val db = mapOf(
            "天氣" to listOf(DictEntry(traditional = "天氣", simplified = "天氣", pinyin = "tian1 qi4", english = "weather"))
        )
        val fakeDao = FakeDictDao(db)
        
        // Lookup with punctuation and carriage returns
        val results1 = WordLookupService.lookupWord("天氣。", fakeDao)
        assertEquals(1, results1.size)
        assertEquals("天氣", results1[0].traditional)
        
        val results2 = WordLookupService.lookupWord(" 天氣\n", fakeDao)
        assertEquals(1, results2.size)
        assertEquals("天氣", results2[0].traditional)
    }
}
