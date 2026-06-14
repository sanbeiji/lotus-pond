package com.example.lotuspondreader.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinConverterTest {

    @Test
    fun testSingleSyllableConversions() {
        assertEquals("hǎo", PinyinConverter.convertToToneMarks("hao3"))
        assertEquals("diàn", PinyinConverter.convertToToneMarks("dian4"))
        assertEquals("lǜ", PinyinConverter.convertToToneMarks("lu:4"))
        assertEquals("lǜ", PinyinConverter.convertToToneMarks("lv4"))
        assertEquals("nǚ", PinyinConverter.convertToToneMarks("nu:3"))
        assertEquals("nǚ", PinyinConverter.convertToToneMarks("nv3"))
    }

    @Test
    fun testMultiSyllableConversions() {
        assertEquals("shūdiàn", PinyinConverter.convertToToneMarks("shu1 dian4"))
        assertEquals("tiānqì", PinyinConverter.convertToToneMarks("tian1 qi4"))
        assertEquals("Běijīng", PinyinConverter.convertToToneMarks("Bei3 jing1"))
        assertEquals("shànghǎi", PinyinConverter.convertToToneMarks("shang4 hai3"))
    }

    @Test
    fun testNeutralTone() {
        assertEquals("ma", PinyinConverter.convertToToneMarks("ma5"))
        assertEquals("de", PinyinConverter.convertToToneMarks("de5"))
        assertEquals("le", PinyinConverter.convertToToneMarks("le"))
        assertEquals("ba", PinyinConverter.convertToToneMarks("ba5"))
    }

    @Test
    fun testTonePlacementRules() {
        // priority a, e, o
        assertEquals("hǎo", PinyinConverter.convertToToneMarks("hao3"))
        assertEquals("shéi", PinyinConverter.convertToToneMarks("shei2"))
        assertEquals("duō", PinyinConverter.convertToToneMarks("duo1"))
        
        // ou gets tone on o
        assertEquals("kǒu", PinyinConverter.convertToToneMarks("kou3"))
        
        // ui / iu gets tone on second vowel
        assertEquals("huí", PinyinConverter.convertToToneMarks("hui2"))
        assertEquals("shuǐ", PinyinConverter.convertToToneMarks("shui3"))
        assertEquals("liǔ", PinyinConverter.convertToToneMarks("liu3"))
        assertEquals("jiǔ", PinyinConverter.convertToToneMarks("jiu3"))
    }

    @Test
    fun testCaseSensitivityAndCapitalization() {
        assertEquals("Ā", PinyinConverter.convertToToneMarks("A1"))
        assertEquals("Běijīng", PinyinConverter.convertToToneMarks("Bei3 jing1"))
        assertEquals("Án", PinyinConverter.convertToToneMarks("An2"))
    }

    @Test
    fun testEmptyAndWhitespaceInput() {
        assertEquals("", PinyinConverter.convertToToneMarks(""))
        assertEquals("", PinyinConverter.convertToToneMarks("   "))
        assertEquals("hǎoma", PinyinConverter.convertToToneMarks("hao3   ma5"))
    }
}
