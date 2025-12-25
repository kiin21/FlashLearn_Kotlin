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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.VolumeUp
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
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    state: CardDetailState,
    onFlip: () -> Unit,
    onBack: () -> Unit,
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
            CardFront(flashcard)
        } else {
            Box(
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            ) {
                CardBack(flashcard)
            }
        }
    }
}

@Composable
private fun CardFront(flashcard: Flashcard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = flashcard.word,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = flashcard.partOfSpeech,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FlashRed,
                modifier = Modifier
                    .background(
                        color = Color(0xFFFFCDD2),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFFFCDD2),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // speak
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Pronunciation",
                    tint = FlashRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = flashcard.pronunciation,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CardBack(flashcard: Flashcard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = flashcard.word,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = FlashRed,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = flashcard.definition,
            fontSize = 18.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "\"${flashcard.exampleSentence}\"",
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
