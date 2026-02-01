package com.kotlin.flashlearn.presentation.topic

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.components.SearchBar
import com.kotlin.flashlearn.ui.theme.FlashRed
import kotlin.math.roundToInt

@Composable
fun TopicScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateToAddTopic: () -> Unit = {},
    viewModel: TopicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topicWordCounts by viewModel.topicWordCounts.collectAsStateWithLifecycle()
    val topicProgress by viewModel.topicProgress.collectAsStateWithLifecycle()
    var showPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTopics()
    }

    if (showPromptDialog) {
        TopicPromptDialog(
            onDismiss = { showPromptDialog = false },
            onCreate = { prompt ->
                showPromptDialog = false
                viewModel.createTopicWithPrompt(prompt, onNavigateToTopicDetail)
            }
        )
    }

    if (uiState.isGenerating) {
        GeneratingTopicScreen()
    } else {
        Scaffold(
            floatingActionButton = {
                ExpandableDraggableFab(
                    onAddTopicManual = onNavigateToAddTopic,
                    onAddTopicPrompt = { showPromptDialog = true },
                    showAddTopic = true,
                    modifier = Modifier
                        .padding(16.dp)
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = "topic",
                    onNavigate = { route ->
                        when (route) {
                            "home" -> onNavigateToHome()
                            "profile" -> onNavigateToProfile()
                            "community" -> onNavigateToCommunity()
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Header
                TopicHeader()
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar (reusable component)
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    placeholder = stringResource(R.string.search_collections)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                FilterChipRow(
                    activeFilter = uiState.activeFilter,
                    onFilterChange = { viewModel.updateFilter(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = FlashRed)
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.error ?: stringResource(R.string.unknown_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.loadTopics() }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }

                    uiState.displayedTopics.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank())
                                        stringResource(
                                            R.string.no_topics_found_query,
                                            uiState.searchQuery
                                        )
                                    else if (uiState.activeFilter != TopicFilter.ALL)
                                        stringResource(
                                            R.string.no_filtered_topics_yet,
                                            stringResource(uiState.activeFilter.resId).lowercase()
                                        )
                                    else
                                        stringResource(R.string.no_topics_yet_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.searchQuery.isNotBlank() || uiState.activeFilter != TopicFilter.ALL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        viewModel.updateSearchQuery("")
                                        viewModel.updateFilter(TopicFilter.ALL)
                                    }) {
                                        Text(
                                            stringResource(R.string.clear_filters),
                                            color = FlashRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        TopicList(
                            topics = uiState.displayedTopics,
                            topicWordCounts = topicWordCounts,
                            topicProgress = topicProgress,
                            likedTopicIds = uiState.likedTopicIds,
                            onTopicClick = onNavigateToTopicDetail,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TopicHeader() {
    Text(
        text = stringResource(R.string.my_collections),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/**
 * Filter chip row for topic filtering.
 */
@Composable
fun FilterChipRow(
    activeFilter: TopicFilter,
    onFilterChange: (TopicFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(TopicFilter.entries) { filter ->
            FilterChipItem(
                text = stringResource(filter.resId),
                isSelected = activeFilter == filter,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

/**
 * Single filter chip item.
 */
@Composable
private fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) FlashRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun TopicList(
    topics: List<Topic>,
    topicWordCounts: Map<String, Int>,
    topicProgress: Map<String, Pair<Int, Int>>,
    likedTopicIds: Set<String> = emptySet(),
    onTopicClick: (String) -> Unit,
    viewModel: TopicViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(topics, key = { it.id }) { topic ->
            val wordCount = topicWordCounts[topic.id] ?: 0
            val isOwner = topic.createdBy != null && topic.createdBy == viewModel.currentUserId
            val isLiked = topic.id in likedTopicIds

            // Calculate progress from mastered/total flashcards
            val (masteredCount, totalCount) = topicProgress[topic.id] ?: Pair(0, wordCount)
            val progress =
                if (totalCount > 0) masteredCount.toFloat() / totalCount.toFloat() else 0f

            com.kotlin.flashlearn.presentation.components.TopicItem(
                title = topic.name,
                description = topic.description,
                wordCount = wordCount,
                imageUrl = topic.imageUrl ?: "",
                progress = progress,
                isOwner = isOwner,
                isLiked = isLiked,
                onClick = { onTopicClick(topic.id) },
                onToggleLike = { viewModel.toggleTopicLike(topic.id) },
                onDelete = { viewModel.deleteTopic(topic.id) }
            )
        }
    }
}

@Composable
fun ExpandableDraggableFab(
    onAddTopicManual: () -> Unit,
    onAddTopicPrompt: () -> Unit,
    showAddTopic: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sub-items
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Topic Item
                    if (showAddTopic) {
                        // Create with Prompt
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shadowElevation = 2.dp,
                                onClick = {
                                    isExpanded = false
                                    onAddTopicPrompt()
                                }
                            ) {
                                Text(
                                    text = "Create with Prompt",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.SmallFloatingActionButton(
                                onClick = {
                                    isExpanded = false
                                    onAddTopicPrompt()
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Create with Prompt"
                                )
                            }
                        }

                        // Create Manually
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shadowElevation = 2.dp,
                                onClick = {
                                    isExpanded = false
                                    onAddTopicManual()
                                }
                            ) {
                                Text(
                                    text = "Create Manually",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.SmallFloatingActionButton(
                                onClick = {
                                    isExpanded = false
                                    onAddTopicManual()
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    Icons.Default.LibraryAdd,
                                    contentDescription = null
                                ) // Using LibraryAdd as "Add Topic" icon
                            }
                        }
                    }
                }
            }

            // Main Toggle Button
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = FlashRed,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close" else stringResource(R.string.add_new_topic),
                    tint = Color.White
                )
            }
        }
    }
}
