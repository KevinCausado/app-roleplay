package com.elara.app.domain.usecase

class CalibrateLanguageLevelUseCase {

    fun calibrate(userInput: String, currentLevel: Float): Float {
        val words = userInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size
        val hasComplexWords = words.any { it.length > 7 }
        val hasGrammarErrors = detectSimpleErrors(userInput)

        val delta = when {
            wordCount < 3 -> -0.01f
            wordCount > 10 && !hasGrammarErrors -> +0.03f
            wordCount > 5 && !hasGrammarErrors && hasComplexWords -> +0.02f
            hasGrammarErrors -> -0.01f
            else -> 0f
        }

        return (currentLevel + delta).coerceIn(0f, 1f)
    }

    private fun detectSimpleErrors(input: String): Boolean {
        val lower = input.lowercase()
        // Common beginner errors
        return lower.contains(Regex("\\bi go\\b")) ||
               lower.contains(Regex("\\bhe go\\b")) ||
               lower.contains(Regex("\\bshe go\\b")) ||
               lower.contains(Regex("\\bi am go\\b")) ||
               lower.contains(Regex("\\byesterday i\\s+\\w+[^ed]\\b")) ||
               lower.contains(Regex("\\bmore\\s+\\w+er\\b"))
    }
}
