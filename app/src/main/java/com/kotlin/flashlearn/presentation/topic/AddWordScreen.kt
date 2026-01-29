package com.kotlin.flashlearn.presentation.topic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import com.kotlin.flashlearn.ui.theme.FlashResultText
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

    val prefs = remember {
        context.getSharedPreferences("flashlearn_onboarding", android.content.Context.MODE_PRIVATE)
    }
    var showTopicTutorial by remember {
        mutableStateOf(
            !prefs.getBoolean("topic_tutorial_shown", false) && !uiState.isEditMode
        )
    }
    var topicTutorialStep by rememberSaveable { mutableStateOf(0) }

    fun closeTopicTutorial(markShown: Boolean = true) {
        showTopicTutorial = false
        if (markShown) prefs.edit().putBoolean("topic_tutorial_shown", true).apply()
    }

    var wordListExpanded by rememberSaveable { mutableStateOf(true) }

    fun handleBack() {
        if (uiState.currentStep > 0) {
            viewModel.prevStep()
        } else {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.add_words) else stringResource(R.string.create_new_topic),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
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
            Column {
                // Fixed bottom sheet for word list - always visible when words exist
                if (uiState.selectedWords.isNotEmpty() && uiState.currentStep > 0) {
                    PersistentWordListBottomSheet(
                        selectedWords = uiState.selectedWords,
                        isExpanded = wordListExpanded,
                        onToggleExpand = { wordListExpanded = !wordListExpanded },
                        onRemove = { word -> viewModel.removeSelectedWord(word) }
                    )
                }
                
                // Create Topic button
                if (uiState.selectedWords.isNotEmpty() && (uiState.currentStep > 0 || uiState.isEditMode)) {
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
                            enabled = !uiState.isCreatingTopic
                        ) {
                            if (uiState.isCreatingTopic) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (uiState.isEditMode) "Save Changes" else stringResource(
                                        R.string.create_topic_words,
                                        uiState.selectedWords.size
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState.currentStep) {
                0 -> TopicInfoStep(
                    name = uiState.newTopicName,
                    onNameChange = viewModel::onNewTopicNameChange,
                    description = uiState.newTopicDescription,
                    onDescriptionChange = viewModel::onNewTopicDescriptionChange,
                    onNext = { viewModel.nextStep() }
                )
                1 -> MethodSelectionStep(
                    onManualSelect = { viewModel.setStep(2) },
                    onSearchSelect = { viewModel.setStep(3) }
                )
                2 -> {
                    // Manual Entry Step
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Manual Entry", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            ManualEntryForm(
                                uiState = uiState,
                                onWordChange = viewModel::onManualWordChange,
                                onDefinitionChange = viewModel::onManualDefinitionChange,
                                onExampleChange = viewModel::onManualExampleChange,
                                onIpaChange = viewModel::onManualIpaChange,
                                onPosChange = viewModel::onManualPartOfSpeechChange,
                                onImageUriChange = viewModel::onManualImageUriChange,
                                onAdd = viewModel::addManualCard,
                                isExpanded = true,
                                onToggleExpand = {}
                            )
                        }
                    }
                }
                3 -> {
                    // Search Step
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Search Dictionary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text(stringResource(R.string.type_to_search)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        }
                        if (uiState.isSearching) {
                            item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                        } else if (uiState.searchSuggestions.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    uiState.searchSuggestions.forEach { suggestion ->
                                        Text(
                                            text = suggestion.word,
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.onSuggestionSelected(suggestion.word) }.padding(16.dp),
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
fun ManualEntryForm(
    uiState: AddWordUiState,
    onWordChange: (String) -> Unit,
    onDefinitionChange: (String) -> Unit,
    onExampleChange: (String) -> Unit,
    onIpaChange: (String) -> Unit,
    onPosChange: (String) -> Unit,
    onImageUriChange: (String?) -> Unit,
    onAdd: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Manually",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.manualWord,
                    onValueChange = onWordChange,
                    label = { Text("Word/Term") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlashRed,
                        focusedLabelColor = FlashRed,
                        cursorColor = FlashRed
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.manualIpa,
                        onValueChange = onIpaChange,
                        label = { Text("IPA") },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlashRed,
                            focusedLabelColor = FlashRed,
                            cursorColor = FlashRed
                        )
                    )
                    OutlinedTextField(
                        value = uiState.manualPartOfSpeech,
                        onValueChange = onPosChange,
                        label = { Text("Type") },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlashRed,
                            focusedLabelColor = FlashRed,
                            cursorColor = FlashRed
                        )
                    )
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
}

@Composable
fun PersistentWordListBottomSheet(
    selectedWords: Set<VocabularyWord>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRemove: (VocabularyWord) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Collapse/Expand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FlashRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selected Words (${selectedWords.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FlashRed
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = FlashRed
                )
            }
            
            // Expandable word list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedWords.toList()) { word ->
                        SelectedWordChip(word = word, onRemove = { onRemove(word) })
                    }
                }
            }
            
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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

@Composable
fun TopicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.create_new_topic),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.topic_name)) },
            placeholder = { Text(stringResource(R.string.enter_topic_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.description_optional)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = FlashRed)
        ) {
            Text("Next: Add Words", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MethodSelectionStep(
    onManualSelect: () -> Unit,
    onSearchSelect: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Choose method",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) { page ->
            val (title, icon, color, desc) = when (page) {
                0 -> Quad(
                    "Manual Entry",
                    Icons.Default.Edit,
                    Color(0xFFE53935), // Red
                    "Create your own cards with custom definitions and images."
                )
                else -> Quad(
                    "Search Dictionary",
                    Icons.Default.Search,
                    Color(0xFF1E88E5), // Blue
                    "Search our database for words and definitions."
                )
            }

            MethodSlide(
                title = title,
                icon = icon,
                color = color,
                description = desc,
                onClick = {
                    when (page) {
                        0 -> onManualSelect()
                        1 -> onSearchSelect()
                    }
                },
                pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MethodSlide(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit,
    pageOffset: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val pageOffsetAbs = Math.abs(pageOffset)
                // Scale down slightly when not focused
                val scale = androidx.compose.ui.util.lerp(1f, 0.85f, pageOffsetAbs)
                scaleX = scale
                scaleY = scale
                alpha = androidx.compose.ui.util.lerp(1f, 0.5f, pageOffsetAbs)
                
                // Rotate based on position
                rotationZ = pageOffset * -10f
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper data class with unique name
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
