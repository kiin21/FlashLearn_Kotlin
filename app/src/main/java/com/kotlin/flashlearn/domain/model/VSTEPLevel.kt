package com.kotlin.flashlearn.domain.model

/**
 * Represents VSTEP English proficiency levels.
 * Used for filtering and categorizing topics/flashcards.
 */
enum class VSTEPLevel(val displayName: String) {
    B1("B1"),
    B2("B2"),
    C1("C1"),
    C2("C2");

    companion object {
        fun fromString(value: String?): VSTEPLevel? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }

        fun fromStringOrDefault(value: String?, default: VSTEPLevel = B1): VSTEPLevel {
            return fromString(value) ?: default
        }
    }
}
