package com.kotlin.flashlearn.presentation.quiz.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest

@Composable
fun CheckAnswerButton(
    onClick: (String) -> Unit
) {
    Button(
        onClick = { onClick },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = FlashRedDarkest,
            contentColor = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Text(
            text = "Check Answer",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

