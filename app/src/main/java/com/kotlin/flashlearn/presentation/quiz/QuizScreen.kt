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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerNotificationSheet(
    isCorrect: Boolean,
    correctAnswer: String,
    onContinue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { /* Prevent dismissal by swipe/tap outside - force user to click Continue */ },
        containerColor = if (isCorrect) FlashSuccessLight else FlashErrorLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    tint = if (isCorrect) FlashSuccessMed else FlashErrorMed,
                    modifier = Modifier.size(48.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCorrect) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) FlashSuccessDark else FlashErrorMed
                    )
                    if (!isCorrect) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Correct answer: $correctAnswer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FlashErrorMed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCorrect) FlashSuccessMed else FlashErrorMed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MultipleChoiceView(
    question: QuizQuestion.MultipleChoice,
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
                text = question.flashcard.definition,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

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
                                showFeedback && isSelectedOption && isCorrectAnswer(option) -> FlashSuccessMed // Choose correct answer
                                showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorLight // Choose not correct answer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessDark
                            )

                            showFeedback && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashErrorMed
                            )

                            !showFeedback && isSelectedOption -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessLight
                            )

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
                                    showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )

                        }

                    }

                    // Show checkmark icon for correct answer in feedback state
                    if (showFeedback && option == selectedOption) {
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

            selectedOption?.let {
                CheckAnswerButton(
                    onClick = { onAnswer(selectedOption!!) }
                )
            }
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
    // Create unique IDs for each letter to track them properly during reordering
    data class LetterItem(val id: String, val char: Char)

    // State: The list of letter items with stable IDs
    val letterItems by remember(question) {
        mutableStateOf(
            question.shuffledLetters.mapIndexed { index, char ->
                LetterItem(id = "${question.flashcard.id}_$index", char = char)
            }
        )
    }
    var letters by remember(question) { mutableStateOf(letterItems) }

    // State: Dragging tracking
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var itemInitialOffset by remember { mutableStateOf(Offset.Zero) }

    val gridState = rememberLazyGridState()

    // Layout constants for ghost positioning
    val headerPadding = 24.dp
    val titleHeight = 32.dp
    val spacerHeight = 32.dp

    // Helper to find valid index from offset
    fun findIndexByOffset(offset: Offset): Int? {
        return gridState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                        offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
            }?.index
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(headerPadding)) {
                Text(
                    text = "Rebuild the Word",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacerHeight))

                // The draggable grid - Adaptive with 52dp tiles + 12dp spacing allows 5-7 tiles per row
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 52.dp),
                    state = gridState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .pointerInput(showFeedback) {
                            // Disable dragging when feedback is shown
                            if (showFeedback) return@pointerInput

                            detectDragGestures(
                                onDragStart = { offset ->
                                    val index = findIndexByOffset(offset)
                                    if (index != null) {
                                        draggingItemIndex = index
                                        val itemInfo = gridState.layoutInfo.visibleItemsInfo
                                            .find { it.index == index }
                                        itemInfo?.let {
                                            itemInitialOffset = Offset(
                                                it.offset.x.toFloat(),
                                                it.offset.y.toFloat()
                                            )
                                            dragOffset = itemInitialOffset
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount

                                    // Swap logic: Check if hovering over a new slot
                                    val targetIndex = findIndexByOffset(change.position)
                                    if (targetIndex != null &&
                                        draggingItemIndex != null &&
                                        targetIndex != draggingItemIndex
                                    ) {
                                        val currentList = letters.toMutableList()
                                        Collections.swap(
                                            currentList,
                                            draggingItemIndex!!,
                                            targetIndex
                                        )
                                        letters = currentList
                                        draggingItemIndex = targetIndex
                                    }
                                },
                                onDragEnd = { draggingItemIndex = null },
                                onDragCancel = { draggingItemIndex = null }
                            )
                        }
                ) {
                    itemsIndexed(
                        items = letters,
                        key = { _, item -> item.id } // Use stable ID for proper tracking
                    ) { index, letterItem ->
                        Box(
                            modifier = Modifier
                                .animateItem() // Smooth reorder animations
                                .graphicsLayer {
                                    // Hide the item being dragged (ghost replaces it)
                                    alpha = if (index == draggingItemIndex) 0f else 1f
                                }
                        ) {
                            LetterTile(letter = letterItem.char)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Submit button
                CheckAnswerButton(
                    enabled = !showFeedback,
                    onClick = { onAnswer(letters.map { it.char }.joinToString("")) }
                )
            }

            // Ghost tile that follows the finger during drag
            if (draggingItemIndex != null && draggingItemIndex!! < letters.size) {
                val letter = letters[draggingItemIndex!!]
                LetterTile(
                    letter = letter.char,
                    isGhost = true,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                dragOffset.x.roundToInt(),
                                dragOffset.y.roundToInt()
                            )
                        }
                        .offset(
                            x = headerPadding,
                            y = headerPadding + titleHeight + spacerHeight
                        )
                        .zIndex(10f) // Ensure ghost is always on top
                )
            }
        }
    }
}

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
                            !showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessDark
                            )

                            !showFeedback && isSelectedOption && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashErrorMed
                            )

                            isSelectedOption -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessLight
                            )

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
            selectedOption?.let {
                CheckAnswerButton(
                    onClick = { onAnswer(selectedOption!!) }
                )
            }
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
        ) {
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
    var input by remember(question) { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
