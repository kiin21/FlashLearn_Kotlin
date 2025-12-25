package com.kotlin.flashlearn.presentation.learning_session

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import kotlin.math.roundToInt

/**
 * Learning Session screen composable.
 * Features:
 * - Flip animation for flashcards
 * - Swipe gesture to move to next card
 * - Progress tracking
 * - "Got It" / "Study Again" actions
 */
@Composable
fun LearningSessionScreen(
    state: LearningSessionState,
    onFlipCard: () -> Unit,
    onGotIt: () -> Unit,
    onStudyAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = FlashRed
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                // Top bar with close button and progress
                TopBar(
                    progress = state.progress,
                    progressText = state.progressText,
                    onClose = onExit
                )

                // Flashcard content
                state.currentCard?.let { flashcard ->
                    FlashcardContent(
                        flashcard = flashcard,
                        isFlipped = state.isCardFlipped,
                        onFlip = onFlipCard,
                        onSwipeNext = onStudyAgain,
                        modifier = Modifier
                            .weight(1f)

                    )
                } ?: run {
                    // No cards available
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flashcards available for this topic",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }

                // Bottom action buttons
                if (state.currentCard != null) {
                    BottomActionButtons(
                        onStudyAgain = onStudyAgain,
                        onGotIt = onGotIt,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    progress: Float,
    progressText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(4.dp,60.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .height(6.dp),
            color = FlashRed,
            trackColor = FlashRedLight
        )

        // Close button and progress text
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Close button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
            }

            // Center: Progress text
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            // Right: Empty space to balance the layout
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FlashcardContent(
    flashcard: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSwipeNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipingAway by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Animate the swipe away to the left
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isSwipingAway) -with(density) { 1000.dp.toPx() } else offsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "swipe_away",
        finishedListener = {
            if (isSwipingAway) {
                isSwipingAway = false
                offsetX = 0f
                onSwipeNext()
            }
        }
    )

    // Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -with(density) { 100.dp.toPx() }) {
                            // Trigger swipe away animation
                            isSwipingAway = true
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!isSwipingAway) {
                            offsetX = (offsetX + dragAmount).coerceAtMost(0f)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FlashLightGrey)
                .height(500.dp)
                .clickable { onFlip() }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density.density
                }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front side - Word
                FlashcardFront(flashcard = flashcard)
            } else {
                // Back side - Definition
                FlashcardBack(
                    flashcard = flashcard,
                    modifier = Modifier.graphicsLayer {
                        rotationY = 180f
                    }
                )
            }
        }
    }
}

@Composable
private fun FlashcardFront(
    flashcard: Flashcard,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Word
        Text(
            text = flashcard.word,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Part of speech and pronunciation
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Part of speech badge
            Text(
                text = flashcard.partOfSpeech,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FlashRedLight)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = FlashRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(12.dp))

            // Pronunciation with speaker icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FlashRedLight)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Pronunciation",
                    modifier = Modifier.size(20.dp),
                    tint = FlashRed
                )
            }
        }
    }
}

@Composable
private fun FlashcardBack(
    flashcard: Flashcard,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Word (smaller)
        Text(
            text = flashcard.word,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = FlashRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pronunciation
        Text(
            text = flashcard.pronunciation,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Definition
        Text(
            text = flashcard.definition,
            fontSize = 18.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Example sentence
        Text(
            text = "\"${flashcard.exampleSentence}\"",
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun BottomActionButtons(
    onStudyAgain: () -> Unit,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Study Again button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onStudyAgain() }
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Study Again",
                    modifier = Modifier.size(32.dp),
                    tint = FlashRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "STUDY AGAIN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }

        // Got It button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onGotIt() }
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GOT IT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}
