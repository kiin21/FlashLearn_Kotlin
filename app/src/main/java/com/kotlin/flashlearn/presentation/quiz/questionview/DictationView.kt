package com.kotlin.flashlearn.presentation.quiz.questionview

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kotlin.flashlearn.domain.model.QuizQuestion

@Composable
fun DictationView(
    question: QuizQuestion.Dictation,
    showFeedback: Boolean,
    onAnswer: (String) -> Unit
) {
    val context = LocalContext.current
    var input by remember(question) { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, question.audioUrl.toUri())
                    context.startActivity(intent)
                }
            }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Play audio")
            }
            Text("Listen and type the word", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Your answer") },
            enabled = !showFeedback,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onAnswer(input) }, enabled = !showFeedback) {
            Text("Submit")
        }
    }
}
