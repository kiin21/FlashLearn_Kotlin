package com.kotlin.flashlearn.presentation.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.AnswerNotificationSheet
import com.kotlin.flashlearn.presentation.quiz.questionview.ContextualGapFillView
import com.kotlin.flashlearn.presentation.quiz.questionview.DictationView
import com.kotlin.flashlearn.presentation.quiz.questionview.ExactTypingView
import com.kotlin.flashlearn.presentation.quiz.questionview.MultipleChoiceView
import com.kotlin.flashlearn.presentation.quiz.questionview.ScrambleView
import com.kotlin.flashlearn.presentation.quiz.questionview.SentenceBuilderView
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (String) -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAnswerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is QuizUiEvent.NavigateToSummary -> onNavigateToSummary(event.topicId)
                is QuizUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Show answer sheet when feedback is available
    LaunchedEffect(state.showFeedback) {
        showAnswerSheet = state.showFeedback
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.isCompleted) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Session complete", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preparing summary...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                state.currentQuestion?.let { question ->
                    val progress = if (state.totalQuestions > 0) {
                        (state.currentIndex + 1f) / state.totalQuestions
                    } else 0f

                    QuestionContent(
                        question = question,
                        isAnswerCorrect = state.isAnswerCorrect,
                        showFeedback = state.showFeedback,
                        progress = progress,
                        currentIndex = state.currentIndex,
                        totalQuestions = state.totalQuestions,
                        currentStreak = state.currentStreak,
                        onAnswer = viewModel::submitAnswer,
                        onExit = onNavigateBack
                    )
                }
            }
        }

        // Show answer notification sheet when feedback is ready
        if (showAnswerSheet && state.showFeedback && state.isAnswerCorrect != null) {
            state.currentQuestion?.let { question ->
                AnswerNotificationSheet(
                    isCorrect = state.isAnswerCorrect ?: false,
                    correctAnswer = question.flashcard.word,
                    onContinue = {
                        showAnswerSheet = false
                        viewModel.continueToNext()
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionContent(
    question: QuizQuestion,
    isAnswerCorrect: Boolean?,
    showFeedback: Boolean,
    progress: Float,
    currentIndex: Int,
    totalQuestions: Int,
    currentStreak: Int,
    onAnswer: (String) -> Unit,
    onExit: () -> Unit
) {
    // Timer state - 60 seconds per question
    var timeRemaining by remember(question) { mutableStateOf(60) }
    var isTimeUp by remember(question) { mutableStateOf(false) }

    // Timer countdown
    LaunchedEffect(question, showFeedback) {
        if (!showFeedback && !isTimeUp) {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining--
            }
            // Time's up - auto-submit wrong answer
            if (!showFeedback) {
                isTimeUp = true
                onAnswer("") // Empty answer to trigger wrong feedback
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar at the top
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = FlashRedDarkest,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom row with 3 elements: Exit button | Timer | Question count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exit button (leftmost)
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Quiz",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Timer badge (center)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Timer",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${timeRemaining}s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Question count (rightmost)
                Text(
                    text = "${currentIndex + 1}/$totalQuestions",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when (question) {
            is QuizQuestion.MultipleChoice -> MultipleChoiceView(question, showFeedback, onAnswer)
            is QuizQuestion.Scramble -> ScrambleView(question, showFeedback, onAnswer)
            is QuizQuestion.ExactTyping -> ExactTypingView(question, showFeedback, onAnswer)
            is QuizQuestion.ContextualGapFill -> ContextualGapFillView(
                question,
                showFeedback,
                onAnswer
            )

            is QuizQuestion.SentenceBuilder -> SentenceBuilderView(question, showFeedback, onAnswer)
            is QuizQuestion.Dictation -> DictationView(question, showFeedback, onAnswer)
        }
    }
}
