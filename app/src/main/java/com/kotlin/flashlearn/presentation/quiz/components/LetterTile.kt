package com.kotlin.flashlearn.presentation.quiz.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun DraggableLetterTile(
    letter: Char,
    index: Int,
    isDragging: Boolean,
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrop: (Int) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by animateOffsetAsState(
        targetValue = if (isDragging) offset else Offset.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .size(64.dp)
            .offset { IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDragEnd = {
                        offset = Offset.Zero
                        onDragEnd()
                    },
                    onDragCancel = {
                        offset = Offset.Zero
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.toString().uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
