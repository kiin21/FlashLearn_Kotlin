package com.kotlin.flashlearn.presentation.topic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed

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
    
    // Load topics on first composition (already done in ViewModel init, but kept for clarity)
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
                    contentDescription = "Add New Topic"
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

            // Search Bar
            TopicSearchBar()
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
                                text = uiState.error ?: "Unknown error",
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadTopics() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                uiState.allTopics.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No topics yet. Create your first topic!",
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    TopicList(
                        topics = uiState.allTopics,
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
        text = "My Collections",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSearchBar() {
    var searchQuery by remember { mutableStateOf("") }
    TextField(
        value = searchQuery,
        onValueChange = { newValue ->
            searchQuery = newValue
        },
        placeholder = { Text("Search your decks...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            cursorColor = FlashRed,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun TopicList(
    topics: List<Topic>,
    topicWordCounts: Map<String, Int>,
    onTopicClick: (String) -> Unit,
    viewModel: TopicViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(topics) { topic ->
            val wordCount = topicWordCounts[topic.id] ?: 0
            TopicCard(
                topicId = topic.id,
                title = topic.name,
                words = wordCount,
                description = topic.description,
                progress = 0f, // TODO: Calculate from user progress
                onClick = onTopicClick
            )
        }
    }
}

@Composable
fun TopicCard(
    topicId: String,
    title: String,
    words: Int,
    description: String,
    progress: Float,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(topicId) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$words ${if (words != 1) "words" else "word"}  • $description", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = FlashRed,
                trackColor = Color.LightGray
            )
        }
    }
}
