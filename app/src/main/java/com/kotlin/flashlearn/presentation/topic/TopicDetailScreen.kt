package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kotlin.flashlearn.domain.model.QuizConfig
import com.kotlin.flashlearn.domain.model.QuizMode
import com.kotlin.flashlearn.presentation.components.SearchBar
import com.kotlin.flashlearn.ui.theme.FlashRed
import androidx.compose.ui.res.stringResource
import com.kotlin.flashlearn.R
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    topicId: String,
    state: TopicDetailState,
    onBack: () -> Unit,
    onNavigateToCardDetail: (String) -> Unit,
    onStudyNow: () -> Unit,
    onTakeQuiz: (QuizConfig) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleCardSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteTopic: () -> Unit,
    onUpdateTopic: (String, String, String) -> Unit,
    onRegenerateImage: () -> Unit,
    onTogglePublic: () -> Unit,
    onSaveToMyTopics: () -> Unit,
    onClearMessages: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showNonOwnerMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showQuizConfig by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { ttsInstance ->
                    val result = ttsInstance.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Handle error if needed
                    }
                }
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.shutdown()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar for success/error messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    if (showEditDialog) {
        EditTopicDialog(
            currentName = state.topicTitle,
            currentDescription = state.topicDescription,
            currentImageUrl = state.imageUrl,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, desc, imgUrl -> 
                onUpdateTopic(name, desc, imgUrl)
                showEditDialog = false
            },
            onRegenerateImage = onRegenerateImage
        )
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            stringResource(R.string.selected_count, state.selectedCardIds.size), 
                            style = MaterialTheme.typography.titleMedium,
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onToggleSelectionMode) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = onSelectAll) { 
                             Icon(
                                 imageVector = Icons.Default.CheckCircle, 
                                 contentDescription = stringResource(R.string.select_all), 
                                 tint = FlashRed
                             )
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected), tint = FlashRed)
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(state.topicTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                tint = FlashRed,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (state.isOwner) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more), tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_topic)) },
                                        onClick = {
                                            showMenu = false
                                            showEditDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                if (state.isPublic) stringResource(R.string.make_private) else stringResource(R.string.make_public)
                                            ) 
                                        },
                                        onClick = {
                                            showMenu = false
                                            onTogglePublic()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (state.isPublic) Icons.Default.Lock else Icons.Default.Public, 
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_topic), color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            onDeleteTopic()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    )
                                }
                            }
                        } else {
                            // Non-owner: 3-dot menu with Save/Share options
                            Box {
                                IconButton(onClick = { showNonOwnerMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more), tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = showNonOwnerMenu,
                                    onDismissRequest = { showNonOwnerMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.save_to_my_topics)) },
                                        onClick = {
                                            showNonOwnerMenu = false
                                            onSaveToMyTopics()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = FlashRed)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showNonOwnerMenu = false
                                            // TODO: Implement share when app is released
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        enabled = false // Disabled until app release
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add new card */ },
                containerColor = FlashRed,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            // Header
            if (!state.imageUrl.isBlank()) {
                AsyncImage(
                    model = state.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         painter = painterResource(id = android.R.drawable.ic_menu_agenda), // Placeholder if we don't have nice icon res
                         contentDescription = null,
                         tint = MaterialTheme.colorScheme.onPrimaryContainer
                     )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(state.topicTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                state.topicDescription.ifBlank { stringResource(R.string.vocabulary_collection) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = onStudyNow,
                    colors = ButtonDefaults.buttonColors(containerColor = FlashRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.study_now), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { showQuizConfig = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FlashRed),
                    border = BorderStroke(1.dp, FlashRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF9E0000))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.take_quiz), style = MaterialTheme.typography.labelLarge, color = FlashRed)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Header with count
            Text(
                text = stringResource(R.string.cards_in_topic, state.cards.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search Bar for flashcards
            if (state.cards.isNotEmpty()) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.search_cards)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlashRed)
                    }
                }
                state.error != null -> {
                    Text(state.error, color = Color.Red)
                }
                state.cards.isEmpty() -> {
                    Text(stringResource(R.string.no_cards_in_topic), color = Color.Gray)
                }
                state.displayedCards.isEmpty() && state.searchQuery.isNotBlank() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_cards_found, state.searchQuery),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onSearchQueryChange("") }) {
                            Text(stringResource(R.string.clear_search), color = FlashRed)
                        }
                    }
                }
                else -> {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(state.displayedCards.size) { index ->
                            val card = state.displayedCards[index]
                            CardItem(
                                word = card.word,
                                type = card.partOfSpeech,
                                ipa = card.ipa,
                                imageUrl = card.imageUrl,
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedCardIds.contains(card.id),
                                onPlayAudio = {
                                    tts?.speak(card.word, TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                onClick = { 
                                    if (state.isSelectionMode) {
                                        onToggleCardSelection(card.id)
                                    } else {
                                        onNavigateToCardDetail(card.id)
                                    }
                                },
                                onLongClick = {
                                    if (!state.isSelectionMode && state.isOwner) {
                                        onToggleSelectionMode()
                                        onToggleCardSelection(card.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQuizConfig) {
        QuizConfigBottomSheet(
            onDismiss = { showQuizConfig = false },
            onStartQuiz = { config ->
                showQuizConfig = false
                onTakeQuiz(config)
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CardItem(
    word: String,
    type: String,
    ipa: String,
    imageUrl: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onPlayAudio: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox
            if (isSelectionMode) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) stringResource(R.string.selected_txt) else stringResource(R.string.select_txt),
                        tint = if (isSelected) FlashRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Image Thumbnail
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    word.take(1).uppercase(), 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (ipa.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(ipa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (!isSelectionMode) {
                // Audio Icon
                IconButton(onClick = onPlayAudio) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.play_audio),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizConfigBottomSheet(
    onDismiss: () -> Unit,
    onStartQuiz: (QuizConfig) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Practice Session", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            QuizModeCard(
                title = "⚡ Quick Sprint",
                subtitle = "Adaptive difficulty. Best for daily review.",
                onClick = { onStartQuiz(QuizConfig(QuizMode.SPRINT, 10)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            QuizModeCard(
                title = "🎯 VSTEP Drill",
                subtitle = "Exam simulation (Reading, Listening, Writing).",
                onClick = { onStartQuiz(QuizConfig(QuizMode.VSTEP_DRILL, 20)) }
            )
        }
    }
}

@Composable
private fun QuizModeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}