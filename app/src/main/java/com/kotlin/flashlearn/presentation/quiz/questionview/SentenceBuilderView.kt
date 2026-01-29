package com.kotlin.flashlearn.presentation.quiz.questionview

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.CheckAnswerButton
import com.kotlin.flashlearn.presentation.quiz.components.LetterTile
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderView(
    question: QuizQuestion.SentenceBuilder,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var selectedSegments by remember(question) { mutableStateOf(listOf<String>()) }
    var availableSegments by remember(question) { mutableStateOf(question.scrambledSegments) }

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
                text = "Build the sentence",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap words to build sentences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Paper lines area with selected words
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Three horizontal lines in the background
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    repeat(3) {
                        Column {
                            Spacer(modifier = Modifier.height(48.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }

                // Words flowing across all lines
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    selectedSegments.forEach { word ->
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(word) {
                            isVisible = true
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isVisible,
                            enter = androidx.compose.animation.fadeIn(
                                animationSpec = tween(300)
                            ) + androidx.compose.animation.scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(300)
                            ),
                            exit = androidx.compose.animation.fadeOut(
                                animationSpec = tween(200)
                            ) + androidx.compose.animation.scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(200)
                            )
                        ) {
                            OutlinedCard(
                                onClick = {
                                    if (!showFeedback) {
                                        isVisible = false
                                        selectedSegments = selectedSegments - word
                                        availableSegments = availableSegments + word
                                    }
                                },
                                modifier = Modifier.padding(vertical = 4.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                enabled = !showFeedback
                            ) {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Available word tiles
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSegments.forEach { word ->
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(word) {
                        delay(50)
                        isVisible = true
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = androidx.compose.animation.fadeIn(
                            animationSpec = tween(300)
                        ) + androidx.compose.animation.scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(300)
                        ),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = tween(200)
                        ) + androidx.compose.animation.scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(200)
                        )
                    ) {
                        LetterTile(letter = word, onClick = {
                            if (!showFeedback) {
                                isVisible = false
                                selectedSegments = selectedSegments + word
                                availableSegments = availableSegments - word
                            }
                        }, enabled = !showFeedback)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Check Answer Button
            if (selectedSegments.isNotEmpty()) {
                CheckAnswerButton(
                    enabled = !showFeedback,
                    onClick = { onAnswer(selectedSegments.joinToString(" ")) }
                )
            }
        }
    }
}

