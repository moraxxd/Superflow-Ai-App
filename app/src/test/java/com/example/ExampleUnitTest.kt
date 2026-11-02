package com.example

import com.example.ai.core.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLanguageIdentification_devanagariHindi() {
        val result = LocalLanguageIdentifier.identify("नमस्ते, आप कैसे हैं?")
        assertEquals("hi", result.languageCode)
        assertEquals("Hindi", result.languageName)
        assertTrue(result.isIndianLanguage)
        assertFalse(result.isTribalLanguage)
        assertTrue(result.confidence > 0.9f)
    }

    @Test
    fun testLanguageIdentification_santaliOlChiki() {
        val result = LocalLanguageIdentifier.identify("ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?")
        assertEquals("sat", result.languageCode)
        assertEquals("Santali", result.languageName)
        assertTrue(result.isIndianLanguage)
        assertTrue(result.isTribalLanguage)
    }

    @Test
    fun testLanguageIdentification_khasiLatin() {
        val result = LocalLanguageIdentifier.identify("Khublei shibun, nga donkam jingiarap.")
        assertEquals("kha", result.languageCode)
        assertEquals("Khasi", result.languageName)
        assertTrue(result.isTribalLanguage)
    }

    @Test
    fun testTranslation_hindiToEnglish() {
        val detected = LocalLanguageIdentifier.identify("नमस्ते, आप कैसे हैं")
        val translation = LocalTranslationEngine.translateToEnglish("नमस्ते, आप कैसे हैं", detected)
        assertTrue(translation is LocalTranslationResult.Success)
        val success = translation as LocalTranslationResult.Success
        assertTrue(success.translatedText.contains("Hello", ignoreCase = true))
        assertTrue(success.translatedText.contains("how are you", ignoreCase = true))
    }

    @Test
    fun testTranslation_unsupportedTribalLanguage_strictHonestRejection() {
        val gondi = TribalLanguageRegistry.getLanguageById("gondi")
        assertNotNull(gondi)
        assertEquals(TribalSupportStatus.UNSUPPORTED, gondi!!.supportStatus)

        val fakeGondiDetected = LanguageDetectionResult(
            languageCode = "gon",
            languageName = "Gondi",
            nativeScript = "Masaram Gondi",
            confidence = 0.85f,
            isIndianLanguage = true,
            isTribalLanguage = true
        )

        val translation = LocalTranslationEngine.translateToEnglish("गोंडी भाषेचा संवाद", fakeGondiDetected)
        assertTrue("Must strictly return Unsupported instead of hallucinating fake translations", translation is LocalTranslationResult.Unsupported)
        val unsupported = translation as LocalTranslationResult.Unsupported
        assertTrue(unsupported.reason.contains("Not currently supported"))
    }

    @Test
    fun testRewriter_allSevenStyles() {
        val input = "We need to discuss the project update tomorrow."

        RewriteStyle.values().forEach { style ->
            val res = LocalTextRewriter.rewrite(input, style)
            assertNotNull(res.rewrittenText)
            assertTrue("Rewritten text should not be empty", res.rewrittenText.isNotBlank())
            assertEquals(style, res.style)
        }

        // Verify specific style signatures
        val formal = LocalTextRewriter.rewrite(input, RewriteStyle.FORMAL)
        assertTrue(formal.rewrittenText.contains("Please be advised accordingly"))

        val genZ = LocalTextRewriter.rewrite(input, RewriteStyle.GEN_Z)
        assertTrue(genZ.rewrittenText.contains("no cap") || genZ.rewrittenText.contains("lowkey"))

        val detailed = LocalTextRewriter.rewrite(input, RewriteStyle.DETAILED)
        assertTrue(detailed.rewrittenText.contains("Overview & Objective"))
    }

    @Test
    fun testTribalRegistry_containsAllTenRequiredLanguages() {
        val expected = listOf("santali", "khasi", "garo", "bhili", "gondi", "mundari", "ho", "kokborok", "kui", "kurukh")
        val registeredIds = TribalLanguageRegistry.languages.map { it.id }

        for (id in expected) {
            assertTrue("Tribal registry must contain $id", registeredIds.contains(id))
        }
    }
}
