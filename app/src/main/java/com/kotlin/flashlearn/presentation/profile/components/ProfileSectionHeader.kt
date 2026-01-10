package com.kotlin.flashlearn.presentation.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
    )
}
