package com.kotlin.flashlearn.presentation.quiz.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.ui.theme.FlashErrorLight
import com.kotlin.flashlearn.ui.theme.FlashErrorMed
import com.kotlin.flashlearn.ui.theme.FlashSuccessDark
import com.kotlin.flashlearn.ui.theme.FlashSuccessLight
import com.kotlin.flashlearn.ui.theme.FlashSuccessMed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerNotificationSheet(
    isCorrect: Boolean,
    correctAnswer: String,
    onContinue: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { /* Prevent dismissal by swipe/tap outside - force user to click Continue */ },
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    tint = if (isCorrect) FlashSuccessMed else FlashErrorMed,
                    modifier = Modifier.size(48.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCorrect) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) FlashSuccessDark else FlashErrorMed
                    )
                    if (!isCorrect) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Correct answer: $correctAnswer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FlashErrorMed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCorrect) FlashSuccessMed else FlashErrorMed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
