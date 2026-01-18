package com.kotlin.flashlearn.presentation.quiz.questionview

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.CheckAnswerButton
import com.kotlin.flashlearn.presentation.quiz.components.LetterTile
import java.util.Collections
import kotlin.math.roundToInt

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

