package com.elara.app.domain.usecase

class CalibrateLanguageLevelUseCase {

    fun calibrate(userInput: String, currentLevel: Float): Float {
        val words = userInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size
        val uniqueWords = words.toSet().size
        val avgWordLen = if (words.isEmpty()) 0f else words.map { it.length }.average().toFloat()
        val hasComplexWords = words.any { it.length > 7 }
        val hasGrammarErrors = detectSimpleErrors(userInput)

        val delta = when {
            wordCount < 3 -> -0.005f
            wordCount > 12 && !hasGrammarErrors -> +0.04f
            wordCount > 8 && !hasGrammarErrors && hasComplexWords -> +0.03f
            wordCount > 5 && uniqueWords > 4 && avgWordLen > 4.5f -> +0.02f
            wordCount > 5 && !hasGrammarErrors -> +0.015f
            hasGrammarErrors -> -0.01f
            else -> +0.005f
        }

        return (currentLevel + delta).coerceIn(0f, 1f)
    }

    fun getCEFRLevel(level: Float): String = when {
        level < 0.15f -> "A1"
        level < 0.3f -> "A2"
        level < 0.5f -> "B1"
        level < 0.7f -> "B2"
        level < 0.85f -> "C1"
        else -> "C2"
    }

    private fun detectSimpleErrors(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains(Regex("\\bi go\\b")) ||
               lower.contains(Regex("\\bhe go\\b")) ||
               lower.contains(Regex("\\bshe go\\b")) ||
               lower.contains(Regex("\\bit go\\b")) ||
               lower.contains(Regex("\\bi am go\\b")) ||
               lower.contains(Regex("\\byesterday i\\s+\\w+[^ed]\\b")) ||
               lower.contains(Regex("\\bmore\\s+\\w+er\\b")) ||
               lower.contains(Regex("\\bdoes he goes\\b")) ||
               lower.contains(Regex("\\bi doesn't went\\b")) ||
               lower.contains(Regex("\\bi have \\w+ed yesterday"))
    }
}
