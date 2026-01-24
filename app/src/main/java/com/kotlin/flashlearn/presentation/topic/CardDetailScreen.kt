package com.kotlin.flashlearn.presentation.topic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.presentation.components.FlashcardBack
import com.kotlin.flashlearn.presentation.components.FlashcardFront
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    state: CardDetailState,
    onFlip: () -> Unit,
    onBack: () -> Unit,
    onRegenerateImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Card Detail", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = FlashRed)
                    }
                },
                actions = {
                    IconButton(onClick = onRegenerateImage) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate Image", tint = FlashRed)
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
                state.error != null -> Text(state.error ?: "Error", color = Color.Red)
                state.flashcard == null -> Text("Card not found", color = Color.Gray)
                else -> {
                    CardFlipContent(
                        flashcard = state.flashcard,
                        isFlipped = state.isFlipped,
                        onFlip = onFlip
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
    onFlip: () -> Unit
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
            FlashcardFront(flashcard = flashcard)
        } else {
            Box(
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            ) {
                FlashcardBack(flashcard = flashcard)
            }
        }
    }
}


