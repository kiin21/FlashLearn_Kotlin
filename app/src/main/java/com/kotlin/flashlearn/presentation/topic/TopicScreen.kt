package com.kotlin.flashlearn.presentation.topic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.ui.theme.FlashLightGrey
import com.kotlin.flashlearn.ui.theme.FlashRed
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Layers
import com.kotlin.flashlearn.presentation.navigation.Route
import com.kotlin.flashlearn.ui.theme.FlashBlack

@Composable
fun TopicScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
) {
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            TopicFabMenu(
                expanded = isFabExpanded,
                onFabClick = { isFabExpanded = !isFabExpanded },
                onAddTopic = { /* TODO */ },
                onAddCard = { /* TODO */ }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "topic",
                onNavigate = { route ->
                    if (route == "home") onNavigateToHome()
                    else if (route == "profile") onNavigateToProfile()
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

            // Searh Bar
            TopicSearchBar()
            Spacer(modifier = Modifier.height(16.dp))

            // List
            TopicList(
                onTopicClick = onNavigateToTopicDetail
            )
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
    onTopicClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopicCard(
            topicId = "b1_environment",
            title = "B1 Environment",
            words = 4,
            description = "Essential vocabulary for nature",
            progress = 0.7f,
            onClick = onTopicClick
        )
        TopicCard(
            topicId = "b1_environment",
            title = "B1 Environment",
            words = 4,
            description = "Essential vocabulary for nature",
            progress = 0.7f,
            onClick = onTopicClick
        )
        TopicCard(
            topicId = "b1_environment",
            title = "B1 Environment",
            words = 4,
            description = "Essential vocabulary for nature",
            progress = 0.7f,
            onClick = onTopicClick
        )
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
            Text("$words ${if (words > 1) "words" else "word"}  • $description", fontSize = 12.sp, color = Color.Gray)
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
