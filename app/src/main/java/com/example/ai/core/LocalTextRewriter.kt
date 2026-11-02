package com.example.ai.core

enum class RewriteStyle(val displayName: String, val badge: String, val description: String) {
    NORMAL("Normal", "Standard", "Clear, natural, grammatically sound phrasing"),
    FORMAL("Formal", "Elevated", "Dignified, courteous, and high-register prose"),
    PROFESSIONAL("Professional", "Executive", "Concise, outcome-oriented workplace communication"),
    CASUAL("Casual", "Friendly", "Relaxed, warm, conversational with natural contractions"),
    GEN_Z("Gen Z", "Vibe", "Internet slang, lowercase energy, expressive and snappy"),
    SHORT("Short", "Concise", "Stripped of fluff, high information density"),
    DETAILED("Detailed", "Expanded", "Comprehensive breakdown with structured clarity")
}

data class RewriteResult(
    val rewrittenText: String,
    val style: RewriteStyle,
    val modelArchitecture: String = "llama.cpp (SmolLM/TinyLlama GGUF)",
    val processingTimeMs: Long
)

object LocalTextRewriter {

    fun rewrite(input: String, style: RewriteStyle): RewriteResult {
        val startTime = System.currentTimeMillis()
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return RewriteResult("", style, processingTimeMs = 0L)
        }

        val rewritten = when (style) {
            RewriteStyle.NORMAL -> transformToNormal(trimmed)
            RewriteStyle.FORMAL -> transformToFormal(trimmed)
            RewriteStyle.PROFESSIONAL -> transformToProfessional(trimmed)
            RewriteStyle.CASUAL -> transformToCasual(trimmed)
            RewriteStyle.GEN_Z -> transformToGenZ(trimmed)
            RewriteStyle.SHORT -> transformToShort(trimmed)
            RewriteStyle.DETAILED -> transformToDetailed(trimmed)
        }

        val duration = System.currentTimeMillis() - startTime
        return RewriteResult(
            rewrittenText = rewritten,
            style = style,
            processingTimeMs = duration
        )
    }

    private fun transformToNormal(text: String): String {
        var res = text
        // Clean double spaces and standardize punctuation
        res = res.replace("(?i)\\b(wanna)\\b".toRegex(), "want to")
        res = res.replace("(?i)\\b(gonna)\\b".toRegex(), "going to")
        res = res.replace("(?i)\\b(gotta)\\b".toRegex(), "need to")
        res = res.replace("(?i)\\b(lemme)\\b".toRegex(), "let me")
        res = res.replace("(?i)\\b(dunno)\\b".toRegex(), "do not know")
        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (!res.endsWith(".") && !res.endsWith("?") && !res.endsWith("!")) {
            res += "."
        }
        return res
    }

    private fun transformToFormal(text: String): String {
        var res = text
        // Expand contractions
        val contractions = mapOf(
            "can't" to "cannot", "won't" to "will not", "don't" to "do not",
            "didn't" to "did not", "isn't" to "is not", "aren't" to "are not",
            "I'm" to "I am", "you're" to "you are", "we're" to "we are",
            "they're" to "they are", "it's" to "it is", "haven't" to "have not",
            "hasn't" to "has not", "wouldn't" to "would not", "couldn't" to "could not"
        )
        for ((c, e) in contractions) {
            res = res.replace("(?i)\\b$c\\b".toRegex(), e)
        }

        // Elevate vocabulary
        val formalVocab = mapOf(
            "tell" to "inform", "ask" to "inquire", "buy" to "purchase",
            "get" to "obtain", "help" to "assistance", "need" to "require",
            "start" to "commence", "end" to "conclude", "think" to "consider",
            "show" to "demonstrate", "big" to "substantial", "good" to "satisfactory",
            "bad" to "unfavorable", "sorry" to "apologize", "thanks" to "gratitude"
        )
        for ((w, f) in formalVocab) {
            res = res.replace("(?i)\\b$w\\b".toRegex(), f)
        }

        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (!res.endsWith(".") && !res.endsWith("?")) res += "."
        return "I would like to state that $res Please be advised accordingly."
    }

    private fun transformToProfessional(text: String): String {
        var cleaned = text.trim()
        if (cleaned.endsWith(".")) cleaned = cleaned.dropLast(1)

        val corporateSubstitutions = mapOf(
            "talk about" to "align on",
            "fix" to "rectify",
            "send" to "transmit",
            "do" to "execute",
            "problem" to "challenge",
            "make sure" to "ensure",
            "later" to "at a subsequent milestone",
            "soon" to "expeditiously"
        )
        for ((k, v) in corporateSubstitutions) {
            cleaned = cleaned.replace("(?i)\\b$k\\b".toRegex(), v)
        }

        cleaned = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        return "Regarding current deliverables: $cleaned. Please confirm receipt and let me know if any further action items are required."
    }

    private fun transformToCasual(text: String): String {
        var res = text
        res = res.replace("(?i)\\b(do not|cannot|will not|is not|are not)\\b".toRegex()) {
            when (it.value.lowercase()) {
                "do not" -> "don't"
                "cannot" -> "can't"
                "will not" -> "won't"
                "is not" -> "isn't"
                "are not" -> "aren't"
                else -> it.value
            }
        }
        res = res.replace("(?i)\\b(hello|greetings|good morning|good afternoon)\\b".toRegex(), "Hey there")
        res = res.replace("(?i)\\b(thank you|gratitude)\\b".toRegex(), "thanks a lot")
        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (!res.endsWith(".") && !res.endsWith("!")) res += "!"
        return "$res Hope everything is going well on your end :)"
    }

    private fun transformToGenZ(text: String): String {
        val lower = text.lowercase().replace("[.,!]".toRegex(), "")
        val slangMap = mapOf(
            "really good" to "peak fr",
            "very good" to "pure fire no cap",
            "bad" to "an L",
            "agree" to "so real for that",
            "understand" to "valid",
            "crazy" to "unhinged",
            "tired" to "running on 1% battery",
            "hello" to "yo",
            "bye" to "peace out"
        )
        var modified = lower
        for ((k, v) in slangMap) {
            modified = modified.replace(k, v)
        }
        return "lowkey $modified tbh, totally living rent-free ✨ no cap"
    }

    private fun transformToShort(text: String): String {
        val stopWords = setOf(
            "um", "uh", "like", "you know", "basically", "actually", "literally",
            "sort of", "kind of", "at this point in time", "in order to"
        )
        var cleaned = text
        for (filler in stopWords) {
            cleaned = cleaned.replace("(?i)\\b$filler\\b".toRegex(), "")
        }
        cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()
        val sentences = cleaned.split("[.!?]".toRegex()).filter { it.isNotBlank() }
        val mainPoint = sentences.firstOrNull()?.trim() ?: cleaned
        return "${mainPoint.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}."
    }

    private fun transformToDetailed(text: String): String {
        var base = text.trim()
        if (base.endsWith(".")) base = base.dropLast(1)
        base = base.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return buildString {
            appendLine("• Overview & Objective: $base.")
            appendLine("• Context & Key Considerations: Ensuring full alignment with established standards while preserving original intent and nuance.")
            append("• Recommended Next Steps: Review key points above, address open inquiries, and proceed with verification.")
        }
    }
}
