package com.kotlin.flashlearn.presentation.quiz.questionview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.CheckAnswerButton
import com.kotlin.flashlearn.presentation.quiz.components.MultipleChoiceOptions

@Composable
fun ContextualGapFillView(
    question: QuizQuestion.ContextualGapFill,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var selectedOption by remember(question) { mutableStateOf<String?>(null) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Question header
            Text(
                text = "QUESTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question.sentenceWithBlank,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Which word have this Definition",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            MultipleChoiceOptions(
                options = question.options,
                correctAnswer = question.flashcard.word,
                showFeedback = showFeedback,
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Check Answer Button
            selectedOption?.let {
                CheckAnswerButton(
                    onClick = { onAnswer(selectedOption!!) }
                )
            }
        }
    }
}

