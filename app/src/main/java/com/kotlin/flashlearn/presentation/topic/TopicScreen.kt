package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.components.SearchBar
import com.kotlin.flashlearn.ui.theme.FlashRed
import androidx.compose.ui.res.stringResource
import com.kotlin.flashlearn.R

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
    
    LaunchedEffect(Unit) {
        viewModel.loadTopics()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTopic,
                containerColor = FlashRed,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_new_topic)
                )
            }
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
                                    stringResource(R.string.no_topics_found_query, uiState.searchQuery) 
                                else if (uiState.activeFilter != TopicFilter.ALL)
                                    stringResource(R.string.no_filtered_topics_yet, stringResource(uiState.activeFilter.resId).lowercase())
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
                                    Text(stringResource(R.string.clear_filters), color = FlashRed)
                                }
                            }
                        }
                    }
                }
                else -> {
                    TopicList(
                        topics = uiState.displayedTopics,
                        topicWordCounts = topicWordCounts,
                        onTopicClick = onNavigateToTopicDetail,
                        viewModel = viewModel
                    )
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
    val backgroundColor = if (isSelected) FlashRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            
            com.kotlin.flashlearn.presentation.components.TopicItem(
                title = topic.name,
                description = topic.description,
                wordCount = wordCount,
                imageUrl = topic.imageUrl ?: "",
                progress = 0f, // TODO: Calculate from user progress
                isOwner = isOwner,
                onClick = { onTopicClick(topic.id) },
                onDelete = { viewModel.deleteTopic(topic.id) }
            )
        }
    }
}
