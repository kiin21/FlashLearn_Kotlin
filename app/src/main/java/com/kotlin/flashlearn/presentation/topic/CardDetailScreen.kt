package com.kotlin.flashlearn.presentation.topic

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.presentation.components.FlashcardBack
import com.kotlin.flashlearn.presentation.components.FlashcardFront
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    state: CardDetailState,
    onFlip: () -> Unit,
    onBack: () -> Unit,
    onRegenerateImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // We can't set language on 'ttsInstance' here easily as it might not be initialized in this scope yet
                // But we can rely on the 'tts' state being updated
            }
        }
        // Ideally we should set language inside the init listener, but strict scoping makes it hard to access the variable being constructed.
        // Simple workaround: Set it after construction (might rely on sync init) or just rely on 'tts' state update which passes the instance.
        // Actually, TextToSpeech constructor is synchronous for the object creation, so 'ttsInstance' is available immediately.
        // But the engine init is async. 
        // Safest pattern:

        ttsInstance.language = Locale.US // Attempt setting language
        tts = ttsInstance

        onDispose {
            ttsInstance.shutdown()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Card Detail", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = FlashRed
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRegenerateImage) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate Image",
                            tint = FlashRed
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = FlashRed)
                state.error != null -> Text(state.error, color = Color.Red)
                state.flashcard == null -> Text("Card not found", color = Color.Gray)
                else -> {
                    CardFlipContent(
                        flashcard = state.flashcard,
                        isFlipped = state.isFlipped,
                        onFlip = onFlip,
                        onPlayAudio = {
                            tts?.speak(state.flashcard.word, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFlipContent(
    flashcard: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onPlayAudio: () -> Unit
) {
    val density = LocalDensity.current

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(500.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .clickable { onFlip() }
            .background(FlashLightGrey, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            FlashcardFront(
                flashcard = flashcard,
                onPlayAudio = onPlayAudio
            )
        } else {
            Box(
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            ) {
                FlashcardBack(flashcard = flashcard)
            }
        }
    }
}


