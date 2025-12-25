package com.kotlin.flashlearn.presentation.learning_session

/**
 * UI Events for Learning Session screen.
 * Using sealed class for type-safety and exhaustive when statements.
 */
sealed class LearningSessionUiEvent {
    data object NavigateToHome : LearningSessionUiEvent()
    data object SessionComplete : LearningSessionUiEvent()
    data class ShowError(val message: String) : LearningSessionUiEvent()
}
