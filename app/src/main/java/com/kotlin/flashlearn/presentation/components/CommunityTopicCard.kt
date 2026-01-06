package com.kotlin.flashlearn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kotlin.flashlearn.presentation.community.CommunityTopicItem
import com.kotlin.flashlearn.ui.theme.FlashRed

/**
 * Topic card for Community screen.
 * 
 * Design (Separate Upvote/Favorite):
 * - 👍 ThumbUp button: Upvote (public voting, affects ranking)
 * - 🔖 Bookmark button: Favorite (private save, for "Favorites" tab)
 * - Stats: Upvote count display
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityTopicCard(
    item: CommunityTopicItem,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onCreatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topic = item.topic
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left content
            Column(modifier = Modifier.weight(1f)) {
                // Topic name
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Creator name (clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCreatorClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = topic.creatorName.ifEmpty { "Anonymous" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tags (VSTEP Level + Category)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // VSTEP Level tag
                    val levelTag = topic.vstepLevel?.displayName 
                        ?: extractLevelFromName(topic.name)
                    
                    if (levelTag != null) {
                        TopicTag(text = levelTag)
                    }
                    
                    // Category tag
                    val categoryTag = extractCategoryFromName(topic.name)
                    if (categoryTag != null) {
                        TopicTag(text = categoryTag)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Stats row: Upvote count with clickable icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Upvote button + count
                    IconButton(
                        onClick = onUpvoteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isUpvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = if (item.isUpvoted) "Remove upvote" else "Upvote",
                            modifier = Modifier.size(18.dp),
                            tint = if (item.isUpvoted) FlashRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCount(topic.upvoteCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isUpvoted) FlashRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Right side - Bookmark button (Save to Favorites)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorited) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (item.isFavorited) "Remove from saved" else "Save for later",
                    tint = if (item.isFavorited) FlashRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tag chip component for displaying VSTEP level or category.
 */
@Composable
private fun TopicTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * Extracts VSTEP level from topic name (e.g., "VSTEP B1 Vocabulary" -> "B1").
 */
private fun extractLevelFromName(name: String): String? {
    val levels = listOf("B1", "B2", "C1", "C2", "A2")
    return levels.find { name.contains(it, ignoreCase = true) }
}

/**
 * Extracts category/skill from topic name.
 */
private fun extractCategoryFromName(name: String): String? {
    val categories = listOf(
        "Speaking", "Listening", "Reading", "Writing",
        "Vocabulary", "Vocab", "Grammar"
    )
    return categories.find { name.contains(it, ignoreCase = true) }
}

/**
 * Formats count for display (e.g., 1234 -> "1.2k").
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}
