package com.kotlin.flashlearn.presentation.topic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.ui.theme.FlashRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GeneratingTopicScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "generating_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val messages = listOf(
        "Asking Gemini...",
        "Brainstorming vocabulary...",
        "Finding definitions...",
        "Creating your topic...",
        "Almost there..."
    )

    var currentMessageIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Run progress animation in parallel
        launch {
            // Stage 1: Fast start (Searching/Connecting) - 0% -> 30%
            animate(
                initialValue = 0f,
                targetValue = 0.3f,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            ) { value, _ -> progress = value }

            // Stage 2: Thinking/Processing - 30% -> 60%
            animate(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = tween(2000, easing = LinearEasing)
            ) { value, _ -> progress = value }

            // Stage 3: Small stall/pause to simulate work
            delay(300)

            // Stage 4: Generating content - 60% -> 85%
            animate(
                initialValue = 0.6f,
                targetValue = 0.85f,
                animationSpec = tween(3000, easing = FastOutSlowInEasing)
            ) { value, _ -> progress = value }

            // Stage 5: Finalizing (The "99%" slow crawl) - 85% -> 98%
            // Takes a long time to handle worst-case scenarios without reaching 100% too early
            animate(
                initialValue = 0.85f,
                targetValue = 0.90f,
                animationSpec = tween(15000, easing = LinearEasing)
            ) { value, _ -> progress = value }
        }

        while (true) {
            delay(2000)
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .background(FlashRed.copy(alpha = 0.2f * alpha), androidx.compose.foundation.shape.CircleShape)
                )
                
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    tint = FlashRed
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Building Topic",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Message
            Text(
                text = messages[currentMessageIndex],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(24.dp) // Fixed height to prevent jumping
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = FlashRed,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )
        }
    }
}
