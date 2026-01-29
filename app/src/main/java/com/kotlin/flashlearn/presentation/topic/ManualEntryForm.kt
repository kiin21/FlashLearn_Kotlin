package com.kotlin.flashlearn.presentation.topic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kotlin.flashlearn.domain.model.VSTEPLevel
import com.kotlin.flashlearn.ui.theme.FlashRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryForm(
    uiState: AddWordUiState,
    onWordChange: (String) -> Unit,
    onDefinitionChange: (String) -> Unit,
    onExampleChange: (String) -> Unit,
    onIpaChange: (String) -> Unit,
    onPosChange: (String) -> Unit,
    onLevelChange: (VSTEPLevel) -> Unit,
    onImageUriChange: (String?) -> Unit,
    onAdd: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImageUriChange(uri.toString())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                if (uiState.manualImageUri != null) {
                    AsyncImage(
                        model = uiState.manualImageUri,
                        contentDescription = "Card Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Image",
                            tint = Color.White
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Add Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // VSTEP Level Dropdown
            var levelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = !levelExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedLevel.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("VSTEP Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    ),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false }
                ) {
                    VSTEPLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.displayName) },
                            onClick = {
                                onLevelChange(level)
                                levelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Word input with autocomplete suggestions
            var wordDropdownExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = wordDropdownExpanded && uiState.wordSuggestions.isNotEmpty(),
                onExpandedChange = { wordDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.manualWord,
                    onValueChange = {
                        onWordChange(it)
                        wordDropdownExpanded = true
                    },
                    label = { Text("Word/Term") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.isFetchingDetails) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = FlashRed
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                ExposedDropdownMenu(
                    expanded = wordDropdownExpanded && uiState.wordSuggestions.isNotEmpty(),
                    onDismissRequest = { wordDropdownExpanded = false }
                ) {
                    uiState.wordSuggestions.take(8).forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = suggestion.word,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                onWordChange(suggestion.word)
                                wordDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.manualIpa,
                    onValueChange = onIpaChange,
                    label = { Text("IPA") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                var posExpanded by remember { mutableStateOf(false) }
                val posOptions = listOf(
                    "noun",
                    "verb",
                    "adjective",
                    "adverb",
                    "pronoun",
                    "preposition",
                    "conjunction",
                    "interjection"
                )

                ExposedDropdownMenuBox(
                    expanded = posExpanded,
                    onExpandedChange = { posExpanded = !posExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.manualPartOfSpeech,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = FlashRed,
                            focusedLabelColor = FlashRed,
                            cursorColor = FlashRed
                        ),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = posExpanded,
                        onDismissRequest = { posExpanded = false }
                    ) {
                        posOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    onPosChange(selectionOption)
                                    posExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.manualDefinition,
                onValueChange = onDefinitionChange,
                label = { Text("Definition") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlashRed,
                    focusedLabelColor = FlashRed,
                    cursorColor = FlashRed
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.manualExample,
                onValueChange = onExampleChange,
                label = { Text("Example Sentence") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlashRed,
                    focusedLabelColor = FlashRed,
                    cursorColor = FlashRed
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState.manualWord.isNotBlank() && uiState.manualDefinition.isNotBlank()
            ) {
                Text("Add to List")
            }
        }
    }
}
