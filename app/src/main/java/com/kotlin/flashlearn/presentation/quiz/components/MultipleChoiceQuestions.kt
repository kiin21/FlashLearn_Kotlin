package com.kotlin.flashlearn.presentation.quiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.ui.theme.FlashErrorLight
import com.kotlin.flashlearn.ui.theme.FlashErrorMed
import com.kotlin.flashlearn.ui.theme.FlashSuccessDark
import com.kotlin.flashlearn.ui.theme.FlashSuccessLight
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed
import com.kotlin.flashlearn.ui.theme.FlashInfoMed
import com.kotlin.flashlearn.ui.theme.FlashInfoLight
import com.kotlin.flashlearn.ui.theme.FlashInfoDark

/**
 * Reusable multiple choice options component that manages selection state.
 * Can be used in MultipleChoiceQuestion, ContextualGapFill, or any quiz view
 * that requires multiple choice selection.
 *
 * @param options List of option strings to display
 * @param correctAnswer The correct answer string to compare against
 * @param showFeedback Whether to display feedback (correct/incorrect styling)
 * @param selectedOption Currently selected option (controlled state)
 * @param onOptionSelected Callback when an option is selected/deselected
 * @param modifier Optional modifier for the component
 */
@Composable
fun MultipleChoiceOptions(
    options: List<String>,
    correctAnswer: String,
    showFeedback: Boolean,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("A.", "B.", "C.", "D.")
    val optionsWithLabels = options.zip(labels)

    fun isCorrectAnswer(option: String?): Boolean {
        return option == correctAnswer
    }

    Column(modifier = modifier) {
        optionsWithLabels.forEach { (option, label) ->
            val isSelectedOption = selectedOption == option

            Box {
                OutlinedCard(
                    onClick = {
                        if (!showFeedback) { // Only allow selection when not showing feedback
                            if (selectedOption == option) {
                                onOptionSelected(null)
                            } else {
                                onOptionSelected(option)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = when {
                            showFeedback && isSelectedOption && isCorrectAnswer(option) -> FlashSuccessLight
                            showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorLight
                            showFeedback && isCorrectAnswer(option) -> FlashSuccessLight
                            !showFeedback && isSelectedOption -> FlashInfoLight
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = when {
                        // In feedback mode: only highlight selected option and correct option
                        showFeedback && isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                            2.dp,
                            FlashSuccessMed
                        )

                        showFeedback && isSelectedOption && !isCorrectAnswer(option) -> androidx.compose.foundation.BorderStroke(
                            2.dp,
                            FlashErrorMed
                        )

                        // In selection mode: highlight selected option with info blue
                        !showFeedback && isSelectedOption -> androidx.compose.foundation.BorderStroke(
                            2.dp,
                            FlashInfoMed
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
                                // In feedback mode: only color selected option and correct option
                                showFeedback && isCorrectAnswer(option) -> FlashSuccessMed
                                showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = when {
                                // In feedback mode: only color selected option and correct option
                                showFeedback && isCorrectAnswer(option) -> FlashSuccessMed
                                showFeedback && isSelectedOption && !isCorrectAnswer(option) -> FlashErrorMed
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Show checkmark icon for correct answer in feedback state
                // Show crossmark icon for incorrect selected answer
                if (showFeedback) {
                    // Show checkmark on correct answer (always when in feedback mode)
                    if (isCorrectAnswer(option)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct Answer",
                                tint = FlashSuccessMed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    // Show crossmark on selected wrong answer
                    else if (isSelectedOption && !isCorrectAnswer(option)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Wrong Answer",
                                tint = FlashErrorMed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
