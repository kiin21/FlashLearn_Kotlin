package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kotlin.flashlearn.ui.theme.FlashRed

@Composable
fun EditTopicDialog(
    currentName: String,
    currentDescription: String,
    currentImageUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onRegenerateImage: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription) }
    var imageUrl by remember { mutableStateOf(currentImageUrl) }

    // Update local state if currentImageUrl changes (e.g. after regeneration)
    // But wait, onRegenerateImage triggers viewModel update which updates state which updates this dialog? 
    // Yes, but we need to observe it.
    // Actually, local state `imageUrl` won't update automatically if we use `remember { mutableStateOf }` on primitives.
    // We should use `LaunchedEffect` to sync if external prop changes?
    // Or just let parent control it? 
    // Let's use `LaunchedEffect` to update local state when `currentImageUrl` changes.
    androidx.compose.runtime.LaunchedEffect(currentImageUrl) {
        imageUrl = currentImageUrl
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Cover Image",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true
                    )

                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRegenerateImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-Fetch from Pixabay")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, imageUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = FlashRed)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
