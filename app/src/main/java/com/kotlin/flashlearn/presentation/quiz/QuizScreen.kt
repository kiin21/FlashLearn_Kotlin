package com.kotlin.flashlearn.presentation.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.model.QuizQuestion

@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                state.currentQuestion?.let { question ->
                    QuestionContent(
                        question = question,
                        isAnswerCorrect = state.isAnswerCorrect,
                        showFeedback = state.showFeedback,
                        onAnswer = viewModel::submitAnswer
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionContent(
    question: QuizQuestion,
    isAnswerCorrect: Boolean?,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = question.flashcard.definition, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        when (question) {
            is QuizQuestion.MultipleChoice -> MultipleChoiceView(question, showFeedback, onAnswer)
            is QuizQuestion.Scramble -> ScrambleView(question, showFeedback, onAnswer)
            is QuizQuestion.ExactTyping -> ExactTypingView(question, showFeedback, onAnswer)
        }

        if (showFeedback) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isAnswerCorrect == true) "Correct!" else "Wrong! Answer: ${question.flashcard.word}",
                color = if (isAnswerCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MultipleChoiceView(
    question: QuizQuestion.MultipleChoice,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    Column {
        question.options.forEach { option ->
            Button(
                onClick = { onAnswer(option) },
                enabled = !showFeedback,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(text = option)
            }
        }
    }
}

@Composable
fun ScrambleView(
    question: QuizQuestion.Scramble,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    // Simplified implementation
    var input by remember { mutableStateOf("") }
    
    Column {
        Text("Letters: ${question.shuffledLetters.joinToString(" ")}")
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Type Answer") }
        )
        Button(onClick = { onAnswer(input) }, enabled = !showFeedback) {
            Text("Submit")
        }
    }
}

@Composable
fun ExactTypingView(
    question: QuizQuestion.ExactTyping,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    
    Column {
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Type Answer") }
        )
        if (question.hint != null && !showFeedback) {
            Text("Hint: Starts with ${question.hint}")
        }
        Button(onClick = { onAnswer(input) }, enabled = !showFeedback) {
            Text("Submit")
        }
    }
}
