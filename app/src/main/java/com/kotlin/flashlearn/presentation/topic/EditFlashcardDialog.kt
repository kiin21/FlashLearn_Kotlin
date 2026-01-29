package com.kotlin.flashlearn.presentation.topic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.ui.theme.FlashRed

@Composable
fun EditFlashcardDialog(
    flashcard: Flashcard,
    onDismiss: () -> Unit,
    onConfirm: (Flashcard) -> Unit
) {
    var word by remember { mutableStateOf(flashcard.word) }
    var definition by remember { mutableStateOf(flashcard.definition) }
    var example by remember { mutableStateOf(flashcard.exampleSentence) }
    var ipa by remember { mutableStateOf(flashcard.ipa) }
    var partOfSpeech by remember { mutableStateOf(flashcard.partOfSpeech) }
    var imageUrl by remember { mutableStateOf(flashcard.imageUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    // Simple validation
    val isValid = word.isNotBlank() && definition.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Card",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Flashcard Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay for edit hint
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Image",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Add Image",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fields
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word/Term") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ipa,
                        onValueChange = { ipa = it },
                        label = { Text("IPA") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlashRed,
                            focusedLabelColor = FlashRed,
                            cursorColor = FlashRed
                        )
                    )

                    OutlinedTextField(
                        value = partOfSpeech,
                        onValueChange = { partOfSpeech = it },
                        label = { Text("Type (e.g. noun)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlashRed,
                            focusedLabelColor = FlashRed,
                            cursorColor = FlashRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example Sentence") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                flashcard.copy(
                                    word = word.trim(),
                                    definition = definition.trim(),
                                    exampleSentence = example.trim(),
                                    ipa = ipa.trim(),
                                    partOfSpeech = partOfSpeech.trim(),
                                    imageUrl = imageUrl.trim()
                                )
                            )
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlashRed,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
