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
import kotlinx.coroutines.launch

@Composable
fun TopicScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateToAddTopic: () -> Unit = {},
    onNavigateToAddWord: (String?) -> Unit = {},
    viewModel: TopicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topicWords by viewModel.topicWords.collectAsStateWithLifecycle()
    
    var isFabExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val showNotImplementedMessage: (String) -> Unit = { featureName ->
        scope.launch {
            snackbarHostState.showSnackbar("$featureName is coming soon!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            TopicFabMenu(
                expanded = isFabExpanded,
                onFabClick = { isFabExpanded = !isFabExpanded },
                onAddTopic = { 
                    onNavigateToAddTopic()
                    isFabExpanded = false
                },
                onAddCard = { 
                    onNavigateToAddWord(null)
                    isFabExpanded = false
                }
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

            // Search Bar
            TopicSearchBar(
                onSearchClick = { showNotImplementedMessage("Search") }
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
                        topicWords = topicWords,
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
fun TopicSearchBar(
    onSearchClick: () -> Unit = {}
) {
    TextField(
        value = "",
        onValueChange = {},
        placeholder = { Text("Search your decks...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSearchClick() },
        shape = RoundedCornerShape(12.dp),
        enabled = false,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FlashLightGrey,
            unfocusedContainerColor = FlashLightGrey,
            disabledContainerColor = FlashLightGrey,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.Gray,
            focusedPlaceholderColor = Color.Gray,
            unfocusedPlaceholderColor = Color.Gray,
            disabledPlaceholderColor = Color.Gray,
            focusedLeadingIconColor = Color.Gray,
            unfocusedLeadingIconColor = Color.Gray,
            disabledLeadingIconColor = Color.Gray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun TopicList(
    topics: List<Topic>,
    topicWords: Map<String, List<com.kotlin.flashlearn.domain.model.VocabularyWord>>,
    onTopicClick: (String) -> Unit,
    viewModel: TopicViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(topics) { topic ->
            val wordCount = topicWords[topic.id]?.size ?: 0
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

@Composable
fun TopicFabMenu(
    expanded: Boolean,
    onFabClick: () -> Unit,
    onAddTopic: () -> Unit,
    onAddCard: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = Alignment.End) {
                FabItem("Add new topic", Icons.Default.Layers, onAddTopic)
                Spacer(modifier = Modifier.height(8.dp))
                FabItem("Add new card", Icons.Default.AddCard, onAddCard)
            }
        }

        FloatingActionButton(
            onClick = onFabClick,
            containerColor = FlashRed,
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = null
            )
        }
    }
}

@Composable
fun FabItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(text, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            containerColor = Color.White
        ) {
            Icon(icon, contentDescription = null, tint = FlashRed)
        }
    }
}
