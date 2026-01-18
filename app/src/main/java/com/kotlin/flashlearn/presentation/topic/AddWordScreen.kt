package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.kotlin.flashlearn.ui.theme.FlashResultText
import androidx.compose.ui.res.stringResource
import com.kotlin.flashlearn.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    topicId: String? = null,
    onBack: () -> Unit,
    onWordAdded: () -> Unit = {},
    viewModel: AddWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.create_new_topic),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            tint = FlashRed,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Sticky Create Topic Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        viewModel.createTopic(
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.topic_created_success))
                                }
                                onWordAdded()
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.newTopicName.isNotBlank() && 
                              uiState.selectedWords.isNotEmpty() && 
                              !uiState.isCreatingTopic
                ) {
                    if (uiState.isCreatingTopic) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.create_topic_words, uiState.selectedWords.size))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Topic Name Section
            item {
                Text(
                    text = stringResource(R.string.topic_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.newTopicName,
                    onValueChange = { viewModel.onNewTopicNameChange(it) },
                    placeholder = { Text(stringResource(R.string.enter_topic_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            
            // Topic Description (Optional)
            item {
                Text(
                    text = stringResource(R.string.description_optional),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.newTopicDescription,
                    onValueChange = { viewModel.onNewTopicDescriptionChange(it) },
                    placeholder = { Text(stringResource(R.string.add_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )
            }
            
            // Selected Words Preview
            if (uiState.selectedWords.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.selected_words, uiState.selectedWords.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.selectedWords.toList()) { word ->
                            SelectedWordChip(
                                word = word,
                                onRemove = { viewModel.removeSelectedWord(word) }
                            )
                        }
                    }
                }
            }
            
            // Divider
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            // Search Section
            item {
                Text(
                    text = stringResource(R.string.search_for_word),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text(stringResource(R.string.type_to_search)) },
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
                                    fontWeight = FontWeight.Medium,
                                    color = FlashResultText
                                )
                                if (suggestion != uiState.searchSuggestions.take(8).last()) {
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
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
                        text = "  ${stringResource(R.string.or)}  ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
            
            // Topic Selection Section
            item {
                Text(
                    text = stringResource(R.string.get_words_by_topic),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Topic Dropdown
                if (uiState.availableTopics.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = uiState.showTopicDropdown,
                        onExpandedChange = { viewModel.toggleTopicDropdown() }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedTopic?.name ?: stringResource(R.string.choose_topic),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.showTopicDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = if (uiState.selectedTopic != null) FlashRed.copy(alpha = 0.1f) else Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.showTopicDropdown,
                            onDismissRequest = { viewModel.toggleTopicDropdown() }
                        ) {
                            uiState.availableTopics.forEach { topic ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(topic.name, fontWeight = FontWeight.Medium)
                                            if (topic.description.isNotBlank()) {
                                                Text(
                                                    topic.description,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    },
                                    onClick = { viewModel.onTopicSelected(topic) }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = stringResource(R.string.or_enter_manually),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.topicQuery,
                        onValueChange = { viewModel.onTopicQueryChange(it) },
                        placeholder = { Text(stringResource(R.string.topic_query_placeholder)) },
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
                        Text(stringResource(R.string.get_button))
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
                        text = stringResource(R.string.available_words, uiState.topicSuggestions.size),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
            
            // Bottom spacing for list items above the sticky button
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SelectedWordChip(
    word: VocabularyWord,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FlashRed.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word.word,
                fontWeight = FontWeight.Medium,
                color = FlashRed
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove),
                    tint = FlashRed,
                    modifier = Modifier.size(14.dp)
                )
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
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) FlashRed else FlashResultText
                    )
                    if (word.partOfSpeech.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = word.partOfSpeech.lowercase(),
                            fontSize = 12.sp,
                            color = if (isSelected) FlashRed else FlashResultText.copy(alpha = 0.7f),
                            modifier = Modifier
                                .background(
                                    if (isSelected) FlashRed.copy(alpha = 0.1f) else Color.White,
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
                        color = if (isSelected) FlashRed else FlashResultText.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected_txt),
                    tint = FlashRed
                )
            }
        }
    }
}
