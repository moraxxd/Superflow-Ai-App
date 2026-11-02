package com.example.ai.core

data class LanguageDetectionResult(
    val languageCode: String,
    val languageName: String,
    val nativeScript: String,
    val confidence: Float,
    val isIndianLanguage: Boolean,
    val isTribalLanguage: Boolean
)

object LocalLanguageIdentifier {

    fun identify(text: String): LanguageDetectionResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return LanguageDetectionResult(
                languageCode = "und",
                languageName = "Undetermined",
                nativeScript = "Unknown",
                confidence = 0f,
                isIndianLanguage = false,
                isTribalLanguage = false
            )
        }

        // Script analysis via Unicode code points
        var devanagariCount = 0
        var bengaliCount = 0
        var tamilCount = 0
        var teluguCount = 0
        var gujaratiCount = 0
        var gurmukhiCount = 0
        var kannadaCount = 0
        var malayalamCount = 0
        var odiaCount = 0
        var olChikiCount = 0
        var arabicCount = 0
        var latinCount = 0

        for (char in trimmed) {
            val code = char.code
            when (code) {
                in 0x0900..0x097F -> devanagariCount++
                in 0x0980..0x09FF -> bengaliCount++
                in 0x0A00..0x0A7F -> gurmukhiCount++
                in 0x0A80..0x0AFF -> gujaratiCount++
                in 0x0B00..0x0B7F -> odiaCount++
                in 0x0B80..0x0BFF -> tamilCount++
                in 0x0C00..0x0C7F -> teluguCount++
                in 0x0C80..0x0CFF -> kannadaCount++
                in 0x0D00..0x0D7F -> malayalamCount++
                in 0x1C50..0x1C7F -> olChikiCount++
                in 0x0600..0x06FF -> arabicCount++
                in 0x0041..0x005A, in 0x0061..0x007A -> latinCount++
            }
        }

        val totalIdentified = devanagariCount + bengaliCount + tamilCount + teluguCount +
                gujaratiCount + gurmukhiCount + kannadaCount + malayalamCount + odiaCount +
                olChikiCount + arabicCount + latinCount

        if (totalIdentified == 0) {
            return LanguageDetectionResult(
                languageCode = "en",
                languageName = "English",
                nativeScript = "Latin",
                confidence = 0.50f,
                isIndianLanguage = false,
                isTribalLanguage = false
            )
        }

        // Ol Chiki -> Santali (Tribal Language)
        if (olChikiCount > 0 && olChikiCount >= devanagariCount) {
            return LanguageDetectionResult(
                languageCode = "sat",
                languageName = "Santali",
                nativeScript = "Ol Chiki (ᱚᱞ ᱪᱤᱠᱤ)",
                confidence = 0.96f,
                isIndianLanguage = true,
                isTribalLanguage = true
            )
        }

        // Odia
        if (odiaCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "or",
                languageName = "Odia",
                nativeScript = "Odia (ଓଡ଼ିଆ)",
                confidence = 0.94f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Tamil
        if (tamilCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "ta",
                languageName = "Tamil",
                nativeScript = "Tamil (தமிழ்)",
                confidence = 0.95f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Telugu
        if (teluguCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "te",
                languageName = "Telugu",
                nativeScript = "Telugu (తెలుగు)",
                confidence = 0.95f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Kannada
        if (kannadaCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "kn",
                languageName = "Kannada",
                nativeScript = "Kannada (ಕನ್ನಡ)",
                confidence = 0.93f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Malayalam
        if (malayalamCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "ml",
                languageName = "Malayalam",
                nativeScript = "Malayalam (മലയാളം)",
                confidence = 0.95f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Gujarati
        if (gujaratiCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "gu",
                languageName = "Gujarati",
                nativeScript = "Gujarati (ગુજરાતી)",
                confidence = 0.94f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Gurmukhi -> Punjabi
        if (gurmukhiCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "pa",
                languageName = "Punjabi",
                nativeScript = "Gurmukhi (ਪੰਜਾਬੀ)",
                confidence = 0.96f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Bengali or Assamese script
        if (bengaliCount > totalIdentified * 0.3) {
            val isAssamese = trimmed.contains("ৰ") || trimmed.contains("ৱ")
            return if (isAssamese) {
                LanguageDetectionResult(
                    languageCode = "as",
                    languageName = "Assamese",
                    nativeScript = "Bengali-Assamese (অসমীয়া)",
                    confidence = 0.92f,
                    isIndianLanguage = true,
                    isTribalLanguage = false
                )
            } else {
                LanguageDetectionResult(
                    languageCode = "bn",
                    languageName = "Bengali",
                    nativeScript = "Bengali (বাংলা)",
                    confidence = 0.95f,
                    isIndianLanguage = true,
                    isTribalLanguage = false
                )
            }
        }

        // Arabic -> Urdu
        if (arabicCount > totalIdentified * 0.3) {
            return LanguageDetectionResult(
                languageCode = "ur",
                languageName = "Urdu",
                nativeScript = "Perso-Arabic (اردو)",
                confidence = 0.93f,
                isIndianLanguage = true,
                isTribalLanguage = false
            )
        }

        // Devanagari script: Disambiguate between Hindi and Marathi (and check for Marathi-specific markers)
        if (devanagariCount > totalIdentified * 0.3) {
            val marathiMarkers = listOf("आहे", "नाही", "करतो", "करते", "आहोत", "कसा", "कशी", "झाले", "होते", "ळ")
            val isMarathi = marathiMarkers.any { trimmed.contains(it) }

            return if (isMarathi) {
                LanguageDetectionResult(
                    languageCode = "mr",
                    languageName = "Marathi",
                    nativeScript = "Devanagari (मराठी)",
                    confidence = 0.94f,
                    isIndianLanguage = true,
                    isTribalLanguage = false
                )
            } else {
                LanguageDetectionResult(
                    languageCode = "hi",
                    languageName = "Hindi",
                    nativeScript = "Devanagari (हिन्दी)",
                    confidence = 0.96f,
                    isIndianLanguage = true,
                    isTribalLanguage = false
                )
            }
        }

        // Latin script: Check for Khasi / Garo / Romanized Hindi / English
        if (latinCount > totalIdentified * 0.4) {
            val lower = trimmed.lowercase()

            // Khasi markers
            val khasiMarkers = listOf("kumno", "phi", "nga", "don", "bha", "khublei", "kane", "ka ktien")
            if (khasiMarkers.any { lower.contains(it) }) {
                return LanguageDetectionResult(
                    languageCode = "kha",
                    languageName = "Khasi",
                    nativeScript = "Latin (Khasi)",
                    confidence = 0.91f,
                    isIndianLanguage = true,
                    isTribalLanguage = true
                )
            }

            // Garo markers
            val garoMarkers = listOf("mittam", "nama", "an·seng", "dong·a", "ia", "chong·mot")
            if (garoMarkers.any { lower.contains(it) }) {
                return LanguageDetectionResult(
                    languageCode = "grt",
                    languageName = "Garo",
                    nativeScript = "Latin (A·chik)",
                    confidence = 0.88f,
                    isIndianLanguage = true,
                    isTribalLanguage = true
                )
            }

            // Hinglish / Romanized Hindi markers
            val hinglishMarkers = listOf("kya", "hai", "kaise", "aap", "mera", "naam", "accha", "bahut", "theek", "shukriya", "dhanyawad")
            val matchesHinglish = hinglishMarkers.count { lower.split("\\s+".toRegex()).contains(it) }
            if (matchesHinglish >= 2) {
                return LanguageDetectionResult(
                    languageCode = "hi-Latn",
                    languageName = "Hindi (Romanized)",
                    nativeScript = "Latin (Hinglish)",
                    confidence = 0.89f,
                    isIndianLanguage = true,
                    isTribalLanguage = false
                )
            }

            return LanguageDetectionResult(
                languageCode = "en",
                languageName = "English",
                nativeScript = "Latin",
                confidence = 0.94f,
                isIndianLanguage = false,
                isTribalLanguage = false
            )
        }

        return LanguageDetectionResult(
            languageCode = "und",
            languageName = "Unknown",
            nativeScript = "Mixed",
            confidence = 0.40f,
            isIndianLanguage = false,
            isTribalLanguage = false
        )
    }
}
