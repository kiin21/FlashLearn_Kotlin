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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import com.kotlin.flashlearn.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


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

    val prefs = remember {
        context.getSharedPreferences("flashlearn_onboarding", android.content.Context.MODE_PRIVATE)
    }
    var showTopicTutorial by remember { mutableStateOf(!prefs.getBoolean("topic_tutorial_shown", false)) }
    var topicTutorialStep by rememberSaveable { mutableStateOf(0) }

    fun closeTopicTutorial(markShown: Boolean = true) {
        showTopicTutorial = false
        if (markShown) prefs.edit().putBoolean("topic_tutorial_shown", true).apply()
    }


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
    if (showTopicTutorial) {
        TopicTutorialModal(
            step = topicTutorialStep,
            onCancel = { closeTopicTutorial(true) },
            onBack = { topicTutorialStep = (topicTutorialStep - 1).coerceAtLeast(0) },
            onNext = {
                if (topicTutorialStep >= 4) closeTopicTutorial(true)
                else topicTutorialStep += 1
            }
        )
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

@Composable
private fun TopicTutorialModal(
    step: Int,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val total = 5
    val s = step.coerceIn(0, total - 1)

    data class StepData(
        val title: String,
        val desc: String,
        val content: @Composable () -> Unit
    )

    val steps = listOf(
        StepData(
            title = "Topic name",
            desc = "Name your topic. Enter a title to organize your vocabulary set.",
            content = {
                MockLabel("Topic Name *")
                MockOutlinedField(placeholder = "Enter topic name...")
            }
        ),
        StepData(
            title = "Description",
            desc = "Add a description to help you remember what this topic is about.",
            content = {
                Text(
                    "Description (Optional)",
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
                MockTextArea(placeholder = "Add description...")
            }
        ),
        StepData(
            title = "Search for a word",
            desc = "Search for a word to quickly find and add vocabulary.",
            content = {
                MockLabel("Search for a word")
                MockOutlinedField(
                    placeholder = "Type to search...",
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(6.dp))
                        )
                    }
                )
            }
        ),
        StepData(
            title = "Get words by topic",
            desc = "Pick a suggested topic to get words instantly from curated lists.",
            content = {
                MockLabel("Get words by topic")
                MockOutlinedField(
                    placeholder = "Choose a topic...",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(4.dp))
                        )
                    }
                )
            }
        ),
        StepData(
            title = "Enter manually",
            desc = "Or enter a keyword and tap Get to fetch words by topic.",
            content = {
                Text(
                    "Or enter manually:",
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MockOutlinedField(
                            placeholder = "e.g. environment, technology...",
                            height = 52.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(86.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FlashRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Get", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    )

    val data = steps[s]

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF3F4F6))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Create New Topic",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Step ${s + 1} / $total — ${data.title}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = data.desc,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.content()
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(total) { i ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == s) 20.dp else 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (i == s) FlashRed else Color(0xFFE5E7EB))
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Cancel",
                        color = FlashRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCancel() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF3F4F6))
                                .then(
                                    if (s == 0) Modifier else Modifier.clickable { onBack() }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "Back",
                                color = if (s == 0) Color(0xFF9CA3AF) else Color(0xFF111827),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val isLast = s == total - 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(FlashRed)
                                .clickable { onNext() }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (isLast) "Done" else "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF111827)
    )
}

@Composable
private fun MockOutlinedField(
    placeholder: String,
    height: Dp = 52.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = placeholder,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun MockTextArea(
    placeholder: String,
    height: Dp = 86.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(text = placeholder, color = Color(0xFF9CA3AF))
    }
}
