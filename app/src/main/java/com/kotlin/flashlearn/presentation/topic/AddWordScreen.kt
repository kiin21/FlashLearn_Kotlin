package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    topicId: String?,
    onBack: () -> Unit,
    onWordAdded: () -> Unit = {},
    viewModel: AddWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Add Words",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            tint = FlashRed,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Section
            item {
                Text(
                    text = "Search for a word",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Type to search...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    )
                )
            }
            
            // Search Suggestions
            if (uiState.isSearching) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FlashRed
                        )
                    }
                }
            } else if (uiState.searchSuggestions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FlashLightGrey)
                    ) {
                        Column {
                            uiState.searchSuggestions.take(8).forEach { suggestion ->
                                Text(
                                    text = suggestion.word,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onSuggestionSelected(suggestion.word) }
                                        .padding(16.dp),
                                    fontWeight = FontWeight.Medium
                                )
                                if (suggestion != uiState.searchSuggestions.take(8).last()) {
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
            
            // Selected Word Details
            if (uiState.selectedWord != null) {
                item {
                    SelectedWordCard(
                        word = uiState.selectedWord!!,
                        onAdd = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Word added! (Save to DB coming soon)")
                            }
                            viewModel.clearSelectedWord()
                        },
                        onClear = { viewModel.clearSelectedWord() }
                    )
                }
            }
            
            // Divider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  OR  ",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
            
            // Topic Suggestion Section
            item {
                Text(
                    text = "Get suggestions by topic",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.topicQuery,
                        onValueChange = { viewModel.onTopicQueryChange(it) },
                        placeholder = { Text("e.g. environment, technology...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { 
                            focusManager.clearFocus()
                            viewModel.loadWordsByTopic() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Get")
                    }
                }
            }
            
            // Topic Word Suggestions
            if (uiState.isLoadingTopicWords) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlashRed)
                    }
                }
            } else if (uiState.topicSuggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggested Words (${uiState.topicSuggestions.size})",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                items(uiState.topicSuggestions.filter { it.definition.isNotBlank() }) { word ->
                    TopicWordCard(
                        word = word,
                        isSelected = uiState.selectedWords.contains(word),
                        onToggle = { viewModel.toggleWordSelection(word) }
                    )
                }
                
                // Add Selected Button
                if (uiState.selectedWords.isNotEmpty()) {
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "${uiState.selectedWords.size} words added! (Save to DB coming soon)"
                                    )
                                }
                                viewModel.clearAll()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add ${uiState.selectedWords.size} Selected Words")
                        }
                    }
                }
            }
            
            // Bottom spacing
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SelectedWordCard(
    word: VocabularyWord,
    onAdd: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = word.word,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (word.partOfSpeech.isNotBlank()) {
                        Text(
                            text = word.partOfSpeech.lowercase(),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                TextButton(onClick = onClear) {
                    Text("Clear", color = Color.Gray)
                }
            }
            
            if (word.definition.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = word.definition,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Collection")
            }
        }
    }
}

@Composable
fun TopicWordCard(
    word: VocabularyWord,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    FlashRed,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FlashRed.copy(alpha = 0.1f) else FlashLightGrey
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        fontWeight = FontWeight.Bold
                    )
                    if (word.partOfSpeech.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = word.partOfSpeech.lowercase(),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .background(
                                    Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (word.definition.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = word.definition,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        maxLines = 2
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = FlashRed
                )
            }
        }
    }
}
