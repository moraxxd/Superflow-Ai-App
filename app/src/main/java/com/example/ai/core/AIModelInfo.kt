package com.example.ai.core

enum class ModelCategory(val displayName: String) {
    SPEECH_TO_TEXT("Speech Recognition (STT)"),
    LANGUAGE_IDENTIFICATION("Language Identification (LID)"),
    TRANSLATION("Neural Translation (MT)"),
    TEXT_REWRITING("Instruction Rewriting (LLM)")
}

enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    INSTALLED,
    LOADED_IN_RAM
}

data class AIModelInfo(
    val id: String,
    val name: String,
    val category: ModelCategory,
    val framework: String,
    val filename: String,
    val downloadUrl: String,
    val sizeMb: Double,
    val ramRequiredMb: Int,
    val quantization: String,
    val license: String,
    val licenseUrl: String,
    val description: String,
    val supportedLanguages: List<String>,
    val isLightweightRecommended: Boolean,
    val sha256Expected: String,
    var status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    var downloadProgress: Float = 0f,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long = (sizeMb * 1024 * 1024).toLong()
)
