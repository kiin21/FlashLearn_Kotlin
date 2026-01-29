package com.kotlin.flashlearn.presentation.learning_session

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.presentation.components.FlashcardBack
import com.kotlin.flashlearn.presentation.components.FlashcardFront
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashRedLight
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Learning Session screen composable.
 * Features:
 * - Flip animation for flashcards
 * - Swipe gesture (Left/Right) for review
 * - Progress tracking
 * - Undo functionality
 */
@Composable
fun LearningSessionScreen(
    state: LearningSessionState,
    onFlipCard: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onUndo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("flashlearn_onboarding", android.content.Context.MODE_PRIVATE)
    }
    var showTutorial by remember {
        mutableStateOf(
            !prefs.getBoolean(
                "learning_tutorial_shown",
                false
            )
        )
    }
    var tutorialStep by rememberSaveable { mutableStateOf(0) }

    fun dismissTutorial(markShown: Boolean = true) {
        showTutorial = false
        if (markShown) {
            prefs.edit().putBoolean("learning_tutorial_shown", true).apply()
        }
    }

    LaunchedEffect(key1 = state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showTutorial) {
            LearningSessionTutorialDialog(
                step = tutorialStep,
                onCancel = { dismissTutorial(markShown = true) },
                onNext = {
                    if (tutorialStep >= 2) {
                        dismissTutorial(markShown = true)
                    } else {
                        tutorialStep += 1
                    }
                }
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = FlashRed
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top bar with close button and progress
                TopBar(
                    progress = state.progress,
                    progressText = state.progressText,
                    onClose = onExit
                )

                // Flashcard content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.currentCard != null) {
                        // Show next card below (if any) for stack effect
                        if (state.sessionQueue.size > 1) {
                            val nextCard = state.sessionQueue[1]
                            FlashcardItem(
                                flashcard = nextCard,
                                isFlipped = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                                    .graphicsLayer {
                                        scaleX = 0.95f
                                        scaleY = 0.95f
                                        translationY = 20f
                                    }
                                    .alpha(0.5f)
                            )
                        }

                        // Current active card
                        SwipeableFlashcard(
                            flashcard = state.currentCard!!,
                            isFlipped = state.isCardFlipped,
                            onFlip = onFlipCard,
                            onSwipeRight = onSwipeRight,
                            onSwipeLeft = onSwipeLeft
                        )
                    } else {
                        // No cards available
                        Text(
                            text = stringResource(R.string.no_flashcards_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bottom action buttons (Undo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.previousState != null) {
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp, 60.dp)
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
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Empty space to balance the layout
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SwipeableFlashcard(
    flashcard: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels.toFloat()
    val threshold = screenWidth * 0.3f
    val scope = rememberCoroutineScope()

    // Use Animatable for responsive gesture tracking
    // Key it to flashcard.id so it resets for new cards
    val offsetX = remember(flashcard.id) { Animatable(0f) }

    // TextToSpeech setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { ttsInstance ->
                    val usLocale = Locale.US
                    val result = ttsInstance.setLanguage(usLocale)

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        android.util.Log.e("TTS", "US English not supported")
                    } else {
                        // Explicitly choose an en-US voice
                        val usVoice = ttsInstance.voices?.firstOrNull { voice ->
                            voice.locale == usLocale && !voice.isNetworkConnectionRequired
                        }

                        if (usVoice != null) {
                            ttsInstance.voice = usVoice
                        } else {
                            android.util.Log.w("TTS", "No offline US English voice found")
                        }
                    }
                }
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    // Auto-play audio on flip
    LaunchedEffect(isFlipped) {
        if (isFlipped) {
            tts?.speak(flashcard.word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Rotation based on drag
    val dragRotation = (offsetX.value / screenWidth) * 15f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = dragRotation
            }
            .pointerInput(flashcard.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val targetValue = if (offsetX.value > threshold) {
                            screenWidth * 1.5f
                        } else if (offsetX.value < -threshold) {
                            -screenWidth * 1.5f
                        } else {
                            0f
                        }

                        scope.launch {
                            offsetX.animateTo(
                                targetValue = targetValue,
                                animationSpec = tween(durationMillis = 300)
                            )
                            if (targetValue > 0 && targetValue >= screenWidth) {
                                onSwipeRight()
                            } else if (targetValue < 0 && targetValue <= -screenWidth) {
                                onSwipeLeft()
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        FlashcardItem(
            flashcard = flashcard,
            isFlipped = isFlipped,
            onFlip = onFlip
        )

        // Visual Feedback Overlay
        if (offsetX.value.absoluteValue > 20f) {
            val alpha = (offsetX.value.absoluteValue / threshold).coerceIn(0f, 1f)
            val color = if (offsetX.value > 0) Color.Green else Color.Red
            val icon = if (offsetX.value > 0) "✓" else "✕"

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = alpha * 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = alpha)
                )
            }
        }
    }
}

@Composable
private fun FlashcardItem(
    flashcard: Flashcard,
    isFlipped: Boolean,
    onFlip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .height(500.dp)
            .then(if (onFlip != null) Modifier.clickable { onFlip() } else Modifier)
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
            Box(
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            ) {
                FlashcardBack(flashcard = flashcard)
            }
        }
    }
}

@Composable
private fun LearningSessionTutorialDialog(
    step: Int,
    onCancel: () -> Unit,
    onNext: () -> Unit
) {
    val (title, hint, accent, badgeText) = when (step.coerceIn(0, 2)) {
        0 -> Quad(
            "Swipe left if you don’t remember",
            "This word will come back later in the session.",
            Color(0xFFE53935),
            "LEFT"
        )

        1 -> Quad(
            "Swipe right if you mastered the word",
            "We’ll count it as completed and move on.",
            Color(0xFF43A047),
            "RIGHT"
        )

        else -> Quad(
            "Click card to show definition",
            "Tap to flip between word and meaning.",
            Color(0xFF616161),
            "TAP"
        )
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 170.dp, height = 190.dp)
                            .graphicsLayer {
                                rotationZ = when (step.coerceIn(0, 2)) {
                                    0 -> -12f
                                    1 -> 12f
                                    else -> 0f
                                }
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE5E7EB))
                            )
                            Text(
                                text = "Business\norganization",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .height(12.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFE5E7EB))
                                )
                                Box(
                                    modifier = Modifier
                                        .height(12.dp)
                                        .width(28.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFE5E7EB))
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(accent.copy(alpha = 0.10f))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.20f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val isActive = i == step.coerceIn(0, 2)
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isActive) 26.dp else 10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isActive) Color(0xFF9B1C1C) else Color(0xFFE5E7EB)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCancel() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        color = Color(0xFF9B1C1C),
                        fontWeight = FontWeight.SemiBold
                    )

                    val isLast = step.coerceIn(0, 2) == 2
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF9B1C1C))
                            .clickable { onNext() }
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isLast) "Done" else "Next",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Quad(
    val a: String,
    val b: String,
    val c: Color,
    val d: String
)
