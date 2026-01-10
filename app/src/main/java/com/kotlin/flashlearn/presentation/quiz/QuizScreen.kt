package com.kotlin.flashlearn.presentation.quiz

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import com.kotlin.flashlearn.presentation.quiz.components.DraggableLetterTile
import com.kotlin.flashlearn.ui.theme.FlashSuccessLight
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed
import com.kotlin.flashlearn.ui.theme.FlashErrorLight
import com.kotlin.flashlearn.ui.theme.FlashErrorMed
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest
import com.kotlin.flashlearn.ui.theme.FlashSuccessDark
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

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is QuizUiEvent.NavigateToSummary -> onNavigateToSummary(event.topicId)
                is QuizUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
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
    progress: Float,
    currentIndex: Int,
    totalQuestions: Int,
    currentStreak: Int,
    onAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(progress = progress)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Question ${currentIndex + 1} of $totalQuestions", style = MaterialTheme.typography.bodySmall)
            }
            Text("🔥 $currentStreak", style = MaterialTheme.typography.titleMedium)
        }

        when (question) {
            is QuizQuestion.MultipleChoice -> MultipleChoiceView(question, showFeedback, onAnswer)
            is QuizQuestion.Scramble -> ScrambleView(question, showFeedback, onAnswer)
            is QuizQuestion.ExactTyping -> ExactTypingView(question, showFeedback, onAnswer)
            is QuizQuestion.ContextualGapFill -> ContextualGapFillView(question, showFeedback, onAnswer)
            is QuizQuestion.SentenceBuilder -> SentenceBuilderView(question, showFeedback, onAnswer)
            is QuizQuestion.Dictation -> DictationView(question, showFeedback, onAnswer)
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
   var selectedOption by remember { mutableStateOf<String?>(null) }

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
                text = question.flashcard.word,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            fun isCorrectAnswer(option: String?): Boolean {
                return option == question.flashcard.definition
            }

            val labels = listOf("A.", "B.", "C.", "D.")
            val optionsWithLabels = question.options.zip(labels)

            optionsWithLabels.forEach { (option, label) ->
                val isSelectedOption = selectedOption == option
                // Not show feedback mean anwser is submitted
                Box {
                    OutlinedCard(
                        onClick = {
                            if (selectedOption == option) {
                                selectedOption = null
                            } else {
                                selectedOption = option
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                !showFeedback && isCorrectAnswer(option) -> FlashSuccessMed // Choose correct answer
                                !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorLight // Choose not correct answer
                                isSelectedOption -> FlashSuccessLight // Selected
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            !showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(2.dp, FlashSuccessDark)
                            !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(2.dp, FlashErrorMed)
                            isSelectedOption -> androidx.compose.foundation.BorderStroke(2.dp, FlashSuccessLight)
                            else -> CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    !showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    !showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )

                        }

                    }

                    // Show checkmark icon for correct answer in feedback state
                    if (!showFeedback && option == selectedOption) {
                        if (isCorrectAnswer(selectedOption)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct Answer",
                                tint = FlashSuccessMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Wrong Answer",
                                tint = FlashErrorMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            selectedOption?.let { CheckAnswerButton { onAnswer(selectedOption!!) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrambleView(
    question: QuizQuestion.Scramble,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var letters by remember(question) { mutableStateOf(question.shuffledLetters.toList()) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }

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
                text = "Rebuild the Word",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Drag the tiles to reorder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))

            // Letter tiles area - single row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                letters.forEachIndexed { index, letter ->
                    DraggableLetterTile(
                        letter = letter,
                        index = index,
                        isDragging = draggedIndex == index,
                        enabled = showFeedback,
                        onDragStart = { draggedIndex = index },
                        onDragEnd = {
                            draggedIndex = null
                        },
                        onDrop = { targetIndex ->
                            if (targetIndex != index && targetIndex in letters.indices) {
                                letters = letters.toMutableList().apply {
                                    val item = removeAt(index)
                                    add(targetIndex, item)
                                }
                            }
                            draggedIndex = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Check Answer Button
            CheckAnswerButton { onAnswer(letters.joinToString("")) }
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

            Spacer(modifier = Modifier.weight(1f))

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
                CheckAnswerButton { onAnswer(input) }
            }
        }
    }
}

@Composable
fun ContextualGapFillView(
    question: QuizQuestion.ContextualGapFill,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

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

            fun isCorrectAnswer(option: String?): Boolean {
                return option == question.flashcard.word
            }

            val labels = listOf("A.", "B.", "C.", "D.")
            val optionsWithLabels = question.options.zip(labels)

            optionsWithLabels.forEach { (option, label) ->
                val isSelectedOption = selectedOption == option
                // Not show feedback mean anwser is submitted
                Box {
                    OutlinedCard(
                        onClick = {
                            if (selectedOption == option) {
                                selectedOption = null
                            } else {
                                selectedOption = option
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                !showFeedback && isCorrectAnswer(option) -> FlashSuccessMed // Choose correct answer
                                !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorLight // Choose not correct answer
                                isSelectedOption -> FlashSuccessLight // Selected
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            !showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(2.dp, FlashSuccessDark)
                            !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(2.dp, FlashErrorMed)
                            isSelectedOption -> androidx.compose.foundation.BorderStroke(2.dp, FlashSuccessLight)
                            else -> CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    !showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    !showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )

                        }

                    }

                    // Show checkmark icon for correct answer in feedback state
                    if (!showFeedback && option == selectedOption) {
                        if (isCorrectAnswer(selectedOption)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct Answer",
                                tint = FlashSuccessMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Wrong Answer",
                                tint = FlashErrorMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Check Answer Button
            selectedOption?.let { CheckAnswerButton { onAnswer(selectedOption!!) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderView(
    question: QuizQuestion.SentenceBuilder,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var selectedSegments by remember(question) { mutableStateOf(listOf<String>()) }
    var availableSegments by remember(question) { mutableStateOf(question.scrambledSegments) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Construct the sentence:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)) {
            FlowRow(modifier = Modifier.padding(8.dp)) {
                selectedSegments.forEach { word ->
                    AssistChip(
                        onClick = {
                            if (!showFeedback) {
                                selectedSegments = selectedSegments - word
                                availableSegments = availableSegments + word
                            }
                        },
                        label = { Text(word) },
                        enabled = !showFeedback
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableSegments.forEach { word ->
                SuggestionChip(
                    onClick = {
                        if (!showFeedback) {
                            selectedSegments = selectedSegments + word
                            availableSegments = availableSegments - word
                        }
                    },
                    label = { Text(word) },
                    enabled = !showFeedback
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onAnswer(selectedSegments.joinToString(" ")) },
            enabled = selectedSegments.isNotEmpty() && !showFeedback,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }
    }
}

@Composable
fun DictationView(
    question: QuizQuestion.Dictation,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(question.audioUrl))
                    context.startActivity(intent)
                }
            }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Play audio")
            }
            Text("Listen and type the word", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Your answer") },
            enabled = !showFeedback,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onAnswer(input) }, enabled = !showFeedback) {
            Text("Submit")
        }
    }
}
