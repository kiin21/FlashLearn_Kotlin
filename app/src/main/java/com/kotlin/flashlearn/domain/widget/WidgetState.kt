package com.kotlin.flashlearn.domain.widget

import com.kotlin.flashlearn.domain.model.Flashcard

sealed class WidgetState {

    data object SignedOut : WidgetState()

    /**
     * Card is assigned for today but not revealed yet (Reveal mode).
     */
    data class CardHidden(
        val date: String,
        val flashcard: Flashcard
    ) : WidgetState()

    /**
     * Card is revealed (show definition/example + actions).
     */
    data class CardRevealed(
        val date: String,
        val flashcard: Flashcard
    ) : WidgetState()

    /**
     * User already completed today (Got it).
     */
    data class DoneToday(
        val date: String,
        val streakCurrent: Int,
        val streakBest: Int
    ) : WidgetState()

    /**
     * No eligible word left (all excluded by MASTERED or widget history).
     */
    data class Exhausted(
        val date: String,
        val message: String = "No new word available."
    ) : WidgetState()
}