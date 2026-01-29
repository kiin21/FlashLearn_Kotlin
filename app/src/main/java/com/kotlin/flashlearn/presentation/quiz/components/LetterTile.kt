package com.kotlin.flashlearn.presentation.quiz.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.ui.theme.FlashRedDarkest

@Composable
fun LetterTile(
    letter: String,
    modifier: Modifier = Modifier,
    isGhost: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        enabled = true,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isGhost) 10.dp else 0.dp
        )
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Preview
@Composable
fun LetterTilePreview(
) {
    OutlinedCard(
        onClick = {
        },
        modifier = Modifier,
        border = BorderStroke(2.dp, FlashRedDarkest),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        enabled = true
    ) {
        Text(
            text = "Word",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

}
