package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.ui.theme.FlashRed
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
                1 -> {
                    // Unified Manual Entry Step (with auto-fill)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Add Words", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            ManualEntryForm(
                                uiState = uiState,
                                onWordChange = viewModel::onManualWordChange,
                                onDefinitionChange = viewModel::onManualDefinitionChange,
                                onExampleChange = viewModel::onManualExampleChange,
                                onIpaChange = viewModel::onManualIpaChange,
                                onPosChange = viewModel::onManualPartOfSpeechChange,
                                onLevelChange = viewModel::updateManualLevel,
                                onImageUriChange = viewModel::onManualImageUriChange,
                                onAdd = viewModel::addManualCard
                            )
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
