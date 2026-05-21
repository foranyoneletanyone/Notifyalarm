package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keyword_rules")
data class KeywordRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isEnabled: Boolean = true,
    // Comma-separated or line-separated list of keywords
    val keywordsString: String,
    val isAndLogic: Boolean = false, // true = AND logic, false = OR logic
    val isExactWord: Boolean = false, // true = Match exact words, false = Substring
    val isCaseSensitive: Boolean = false, // true = Case sensitive, false = Ignore case
    val soundType: String = "Classic Sirens", // Classic Sirens, Zen Waves, Retro Beeps, Digital Alert
    val snoozeDurationMinutes: Int = 1, // Default 1 min for quick testing, users can change
    val createdAt: Long = System.currentTimeMillis()
) {
    // Utility to parse individual keywords
    fun getKeywordsList(): List<String> {
        return keywordsString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Check if a given notification (title or text) triggers this rule.
     */
    fun matches(title: String, text: String): Boolean {
        if (!isEnabled) return false

        val keywords = getKeywordsList()
        if (keywords.isEmpty()) return false

        val targetContent = "$title \n $text"

        val matchResults = keywords.map { keyword ->
            checkKeywordMatch(targetContent, keyword)
        }

        return if (isAndLogic) {
            matchResults.all { it }
        } else {
            matchResults.any { it }
        }
    }

    private fun checkKeywordMatch(content: String, keyword: String): Boolean {
        return if (isExactWord) {
            // Find exact word boundary match
            val regexOption = if (isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            // Use regex with word boundaries \b keyword \b.
            // Note: handle non-word boundaries carefully for symbols if needed, but word boundaries is standard for exactly.
            val regex = Regex("\\b${Regex.escape(keyword)}\\b", regexOption)
            regex.containsMatchIn(content)
        } else {
            // Substring or phrase match
            content.contains(keyword, ignoreCase = !isCaseSensitive)
        }
    }
}
