package com.kotlin.flashlearn.presentation.quiz

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.CheckAnswerButton
import com.kotlin.flashlearn.presentation.quiz.components.LetterTile
import com.kotlin.flashlearn.ui.theme.FlashSuccessLight
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed
import com.kotlin.flashlearn.ui.theme.FlashErrorLight
import com.kotlin.flashlearn.ui.theme.FlashErrorMed
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest
import com.kotlin.flashlearn.ui.theme.FlashSuccessDark
import com.kotlin.flashlearn.ui.theme.BrandRed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import java.util.Collections
import androidx.core.net.toUri
import com.kotlin.flashlearn.presentation.quiz.components.AnswerNotificationSheet
import com.kotlin.flashlearn.presentation.quiz.questionview.ContextualGapFillView
import com.kotlin.flashlearn.presentation.quiz.questionview.DictationView
import com.kotlin.flashlearn.presentation.quiz.questionview.ExactTypingView
import com.kotlin.flashlearn.presentation.quiz.questionview.MultipleChoiceView
import com.kotlin.flashlearn.presentation.quiz.questionview.ScrambleView
import com.kotlin.flashlearn.presentation.quiz.questionview.SentenceBuilderView

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
