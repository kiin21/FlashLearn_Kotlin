package com.kotlin.flashlearn.presentation.quiz.questionview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.presentation.quiz.components.CheckAnswerButton
import com.kotlin.flashlearn.ui.theme.FlashErrorLight
import com.kotlin.flashlearn.ui.theme.FlashErrorMed
import com.kotlin.flashlearn.ui.theme.FlashSuccessDark
import com.kotlin.flashlearn.ui.theme.FlashSuccessLight
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed

@Composable
fun MultipleChoiceView(
    question: QuizQuestion.MultipleChoice,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    var selectedOption by remember(question) { mutableStateOf<String?>(null) }

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
                text = question.flashcard.definition,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            fun isCorrectAnswer(option: String?): Boolean {
                return option == question.flashcard.word
            }

            val labels = listOf("A.", "B.", "C.", "D.")
            val optionsWithLabels = question.options.zip(labels)

            optionsWithLabels.forEach { (option, label) ->
                val isSelectedOption = selectedOption == option
                // Not show feedback mean anwser is submitted
                Box {
                    OutlinedCard(
                        onClick = {
                            if (selectedOption == option) {
                                selectedOption = null
                            } else {
                                selectedOption = option
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                showFeedback && isSelectedOption && isCorrectAnswer(option) -> FlashSuccessMed // Choose correct answer
                                showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorLight // Choose not correct answer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessDark
                            )

                            showFeedback && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashErrorMed
                            )

                            !showFeedback && isSelectedOption -> androidx.compose.foundation.BorderStroke(
                                2.dp,
                                FlashSuccessLight
                            )

                            else -> CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    showFeedback && isCorrectAnswer(option) -> FlashSuccessDark
                                    showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )

                        }

                    }

                    // Show checkmark icon for correct answer in feedback state
                    if (showFeedback && option == selectedOption) {
                        if (isCorrectAnswer(selectedOption)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct Answer",
                                tint = FlashSuccessMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Wrong Answer",
                                tint = FlashErrorMed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            selectedOption?.let {
                CheckAnswerButton(
                    onClick = { onAnswer(selectedOption!!) }
                )
            }
        }
    }
}

