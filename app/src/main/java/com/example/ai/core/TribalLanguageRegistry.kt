package com.example.ai.core

enum class TribalSupportStatus(val label: String, val badgeColorHex: Long) {
    SUPPORTED("Supported", 0xFF00C853),
    EXPERIMENTAL("Experimental (Beta)", 0xFFFFAB00),
    UNSUPPORTED("Not currently supported", 0xFFFF5252)
}

data class TribalLanguage(
    val id: String,
    val name: String,
    val nativeName: String,
    val isoCode: String,
    val languageFamily: String,
    val primaryScript: String,
    val estimatedSpeakers: String,
    val primaryRegions: List<String>,
    val supportStatus: TribalSupportStatus,
    val modelRequirement: String,
    val datasetNotes: String,
    val statusExplanation: String,
    val nllbCode: String? = null
)

object TribalLanguageRegistry {

    val languages: List<TribalLanguage> = listOf(
        TribalLanguage(
            id = "santali",
            name = "Santali",
            nativeName = "ᱥᱟᱱᱛᱟᱲᱤ",
            isoCode = "sat",
            languageFamily = "Austroasiatic (Munda)",
            primaryScript = "Ol Chiki (ᱚᱞ ᱪᱤᱠᱤ)",
            estimatedSpeakers = "~7.6 Million",
            primaryRegions = listOf("Jharkhand", "West Bengal", "Odisha", "Bihar"),
            supportStatus = TribalSupportStatus.SUPPORTED,
            modelRequirement = "NLLB-200 (sat_Olck) / IndicTrans2 Local GGUF (380 MB)",
            datasetNotes = "Covered in AI4Bharat IndicTrans2 and Meta NLLB-200 benchmarks with Ol Chiki tokenization.",
            statusExplanation = "Local translation pipeline verified for Ol Chiki script. High accuracy on common conversation phrases.",
            nllbCode = "sat_Olck"
        ),
        TribalLanguage(
            id = "khasi",
            name = "Khasi",
            nativeName = "Ka Ktien Khasi",
            isoCode = "kha",
            languageFamily = "Austroasiatic (Khasic)",
            primaryScript = "Latin script",
            estimatedSpeakers = "~1.4 Million",
            primaryRegions = listOf("Meghalaya", "Assam"),
            supportStatus = TribalSupportStatus.SUPPORTED,
            modelRequirement = "NLLB-200 (kha_Latn) Multilingual Matrix (380 MB)",
            datasetNotes = "Native Latin orthography included in NLLB-200 training corpus.",
            statusExplanation = "Local translation verified for Latin-script Khasi into English. Basic sentences and idiomatic forms recognized.",
            nllbCode = "kha_Latn"
        ),
        TribalLanguage(
            id = "garo",
            name = "Garo",
            nativeName = "A·chik Ku·sik",
            isoCode = "grt",
            languageFamily = "Sino-Tibetan (Bodo-Garo)",
            primaryScript = "Latin script (A·chik)",
            estimatedSpeakers = "~1.1 Million",
            primaryRegions = listOf("Meghalaya", "Assam", "Tripura"),
            supportStatus = TribalSupportStatus.EXPERIMENTAL,
            modelRequirement = "IndicTrans2 Specialized Checkpoint (95 MB)",
            datasetNotes = "Partial coverage in community IndicCorp v2 datasets; limited parallel alignment pairs.",
            statusExplanation = "Experimental vocabulary matcher loaded. Complex syntax or regional dialects may produce partial translations.",
            nllbCode = null
        ),
        TribalLanguage(
            id = "bhili",
            name = "Bhili",
            nativeName = "भीली",
            isoCode = "bhi",
            languageFamily = "Indo-Aryan (Bhil group)",
            primaryScript = "Devanagari",
            estimatedSpeakers = "~3.2 Million",
            primaryRegions = listOf("Madhya Pradesh", "Gujarat", "Rajasthan", "Maharashtra"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Dedicated Bhili Fine-Tuned Checkpoint required",
            datasetNotes = "High dialectal variation (Wagdi, Bareli). Lacks standardized parallel sentence corpora for offline neural models.",
            statusExplanation = "Not currently supported. To avoid producing incorrect translations or conflating with Rajasthani/Gujarati, VoiceFlow will not hallucinate outputs until a verified local checkpoint is installed."
        ),
        TribalLanguage(
            id = "gondi",
            name = "Gondi",
            nativeName = "गोंडी / 𑴎𑴽𑴟𑴳",
            isoCode = "gon",
            languageFamily = "Dravidian (South-Central)",
            primaryScript = "Masaram Gondi, Gunjala, Devanagari",
            estimatedSpeakers = "~3.0 Million",
            primaryRegions = listOf("Madhya Pradesh", "Chhattisgarh", "Maharashtra", "Telangana"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Open-source Gondi Field Dataset (Ongoing by AI4Bharat / CGNet Swara)",
            datasetNotes = "Extremely low-resource. Currently undergoing field audio collection.",
            statusExplanation = "Not currently supported. No reliable open-source local checkpoint exists for mobile inference. VoiceFlow strictly rejects fake translation generation."
        ),
        TribalLanguage(
            id = "mundari",
            name = "Mundari",
            nativeName = "मुंडारी / ᱢᱩᱱᱰᱟᱨᱤ",
            isoCode = "unr",
            languageFamily = "Austroasiatic (Munda)",
            primaryScript = "Mundari Bani / Devanagari",
            estimatedSpeakers = "~1.1 Million",
            primaryRegions = listOf("Jharkhand", "Odisha", "West Bengal"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Mundari Bani Tokenizer & Local Quantized Checkpoint",
            datasetNotes = "Limited academic corpora at IIT Patna. No mobile quantized checkpoint available.",
            statusExplanation = "Not currently supported. Awaiting open-source tokenized models."
        ),
        TribalLanguage(
            id = "ho",
            name = "Ho",
            nativeName = "𑢹𑣉𑣉 𑣎𑣋𑣜",
            isoCode = "hoc",
            languageFamily = "Austroasiatic (Munda)",
            primaryScript = "Warang Citi (𑢹𑣉𑣉 𑣏𑣂𑣑𑣂)",
            estimatedSpeakers = "~1.0 Million",
            primaryRegions = listOf("Jharkhand", "Odisha"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Warang Citi Unicode support & Munda local checkpoint",
            datasetNotes = "Low-resource status in standard NLP benchmarks.",
            statusExplanation = "Not currently supported. Lacks verified local offline weights."
        ),
        TribalLanguage(
            id = "kokborok",
            name = "Kokborok",
            nativeName = "Kokborok",
            isoCode = "trp",
            languageFamily = "Sino-Tibetan (Bodo-Garo)",
            primaryScript = "Latin (Koloma) / Eastern Nagari",
            estimatedSpeakers = "~1.0 Million",
            primaryRegions = listOf("Tripura", "Assam"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "IndicBERT / NLLB community finetune",
            datasetNotes = "Script debate (Roman vs Bengali) limits unified parallel training set availability.",
            statusExplanation = "Not currently supported. VoiceFlow displays this notice honestly until a verified checkpoint is released."
        ),
        TribalLanguage(
            id = "kui",
            name = "Kui",
            nativeName = "କୁଇ",
            isoCode = "kxu",
            languageFamily = "Dravidian (South-Central)",
            primaryScript = "Odia script",
            estimatedSpeakers = "~900,000",
            primaryRegions = listOf("Odisha (Kandhamal)"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Dravidian Low-Resource Acoustic/Text Checkpoint",
            datasetNotes = "Classified as vulnerable language by UNESCO. No public neural weights available.",
            statusExplanation = "Not currently supported. Honest reporting enforced."
        ),
        TribalLanguage(
            id = "kurukh",
            name = "Kurukh (Oraon)",
            nativeName = "कुड़ुख़ / 𑱲𑱵𑱳𑱰𑱲",
            isoCode = "kru",
            languageFamily = "Dravidian (North Dravidian)",
            primaryScript = "Tolong Siki / Devanagari",
            estimatedSpeakers = "~2.0 Million",
            primaryRegions = listOf("Jharkhand", "Bihar", "Chhattisgarh", "West Bengal"),
            supportStatus = TribalSupportStatus.UNSUPPORTED,
            modelRequirement = "Tolong Siki quantized model checkpoint",
            datasetNotes = "Oral tradition dominant; written digital parallel corpora scarce.",
            statusExplanation = "Not currently supported. Awaiting standardized offline weights."
        )
    )

    fun getLanguageById(id: String): TribalLanguage? = languages.find { it.id == id }
    fun getLanguageByIso(iso: String): TribalLanguage? = languages.find { it.isoCode.equals(iso, ignoreCase = true) }
}
