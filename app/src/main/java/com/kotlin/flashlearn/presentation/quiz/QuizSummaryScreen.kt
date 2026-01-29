package com.kotlin.flashlearn.presentation.quiz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.QuizResult
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed

@Composable
fun QuizSummaryScreen(
    results: List<QuizResult>,
    onBackToTopic: () -> Unit
) {
    val mastered = results.filter { it.isCorrect }
    val needsReview = results.filterNot { it.isCorrect }
    val accuracy = if (results.isNotEmpty()) mastered.size.toFloat() / results.size else 0f
    
    // Animation state for progress
    val progressAnim = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = accuracy,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Button(
                    onClick = onBackToTopic, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlashRed)
                ) {
                    Text(stringResource(R.string.continue_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Score Section
            Box(contentAlignment = Alignment.Center) {
                // Background circle
                CircularProgressRing(
                    progress = 1f,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    size = 180.dp
                )
                
                // Progress circle
                CircularProgressRing(
                    progress = progressAnim.value,
                    color = if (accuracy >= 0.8f) FlashSuccessMed else if (accuracy >= 0.5f) Color(0xFFFFB020) else FlashRed,
                    strokeWidth = 12.dp,
                    size = 180.dp
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progressAnim.value * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.accuracy),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feedback Message
            Text(
                text = when {
                    accuracy >= 0.9f -> stringResource(R.string.excellent_work)
                    accuracy >= 0.7f -> stringResource(R.string.great_job)
                    accuracy >= 0.5f -> stringResource(R.string.good_effort)
                    else -> stringResource(R.string.keep_practicing)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Statistics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    count = mastered.size,
                    label = stringResource(R.string.correct_count),
                    color = FlashSuccessMed,
                    icon = Icons.Default.CheckCircle
                )
                StatItem(
                    count = needsReview.size,
                    label = stringResource(R.string.incorrect_count),
                    color = FlashRed,
                    icon = Icons.Default.Close
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Lists
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (needsReview.isNotEmpty()) {
                    Text(
                        stringResource(R.string.needs_review),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlashRed
                    )
                    needsReview.forEach { result ->
                        ResultCard(result = result, isCorrect = false)
                    }
                }
                
                if (mastered.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.mastered_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlashSuccessMed
                    )
                    mastered.forEach { result ->
                        ResultCard(result = result, isCorrect = true)
                    }
                }
                
                 Spacer(modifier = Modifier.height(80.dp)) // Space for bottom button
            }
        }
    }
}

@Composable
fun CircularProgressRing(
    progress: Float,
    color: Color,
    strokeWidth: Dp,
    size: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360 * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ResultCard(result: QuizResult, isCorrect: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCorrect) FlashSuccessMed else FlashRed)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = result.flashcard.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (result.flashcard.definition.isNotBlank()) {
                     Text(
                        text = result.flashcard.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

