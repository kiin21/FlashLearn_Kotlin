package com.kotlin.flashlearn.presentation.quiz.questionview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

@Composable
fun ExactTypingView(
    question: QuizQuestion.ExactTyping,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var input by remember(question) { mutableStateOf("") }

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
                text = question.flashcard.definition,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Which word have this Definition",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Letter tiles area - single row
            Column {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Type Answer") }
                )
                if (question.hint != null && !showFeedback) {
                    Text("Hint: Starts with ${question.hint}")
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // Check Answer Button
            if (input != "") {
                CheckAnswerButton(
                    onClick = { onAnswer(input) }
                )
            }
        }
    }
}

