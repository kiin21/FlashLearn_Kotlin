package com.kotlin.flashlearn.presentation.community

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kotlin.flashlearn.domain.model.CommunitySortOption
import com.kotlin.flashlearn.presentation.components.BottomNavBar
import com.kotlin.flashlearn.presentation.components.CommunityTopicCard
import com.kotlin.flashlearn.presentation.components.FilterBottomSheet
import com.kotlin.flashlearn.presentation.components.SearchBar
import com.kotlin.flashlearn.ui.theme.FlashRed
import kotlinx.coroutines.flow.collectLatest

/**
 * Community screen displaying shared public topics.
 * 
 * Design:
 * - Two tabs: Discover (browse all) / Saved (user's bookmarked topics)
 * - Sort dropdown for ordering topics
 * - Filter by VSTEP level
 * - Search by name, description, creator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Tab state: 0 = Discover, 1 = Saved
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is CommunityUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is CommunityUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is CommunityUiEvent.NavigateToTopicDetail -> {
                    onNavigateToTopicDetail(event.topicId)
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = "community",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "topic" -> onNavigateToTopic()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Text(
                text = "Community",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            // Search Bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onAction(CommunityAction.OnSearchQueryChange(it)) },
                placeholder = "Search topics, creators...",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sort and Filter Row (Sort left, Filter right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort dropdown on left
                SortDropdownButton(
                    selectedSort = state.activeSort,
                    onSortChange = { viewModel.onAction(CommunityAction.OnSortChange(it)) }
                )
                
                // Filter button with badge on right
                FilterButton(
                    filterCount = state.filterBadgeCount,
                    onClick = { viewModel.onAction(CommunityAction.OnFilterSheetOpen) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tabs: Discover / Saved
            MainTabs(
                selectedTabIndex = selectedTabIndex,
                onTabChange = { selectedTabIndex = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Content based on tab
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlashRed)
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "An error occurred",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to retry",
                                color = FlashRed,
                                modifier = Modifier.clickable { 
                                    viewModel.onAction(CommunityAction.OnRefresh) 
                                }
                            )
                        }
                    }
                }
                else -> {
                    // Filter topics based on selected tab
                    val displayedTopics = if (selectedTabIndex == 0) {
                        state.topics // Discover: all topics
                    } else {
                        state.topics.filter { it.isFavorited } // Favorites: only liked topics
                    }
                    
                    if (displayedTopics.isEmpty()) {
                        EmptyState(
                            isDiscoverTab = selectedTabIndex == 0,
                            hasFilters = state.filterBadgeCount > 0 || state.searchQuery.isNotBlank()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            
                            items(
                                items = displayedTopics,
                                key = { it.topic.id }
                            ) { item ->
                                CommunityTopicCard(
                                    item = item,
                                    onCardClick = { 
                                        viewModel.onAction(CommunityAction.OnTopicClick(item.topic.id)) 
                                    },
                                    onFavoriteClick = { 
                                        viewModel.onAction(CommunityAction.OnToggleFavorite(item.topic.id)) 
                                    },
                                    onUpvoteClick = {
                                        viewModel.onAction(CommunityAction.OnToggleUpvote(item.topic.id))
                                    },
                                    onCreatorClick = {
                                        item.topic.createdBy?.let { userId ->
                                            onNavigateToUserProfile(userId)
                                        }
                                    }
                                )
                            }
                            
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
        
        // Filter bottom sheet
        if (state.isFilterSheetVisible) {
            FilterBottomSheet(
                sheetState = filterSheetState,
                currentFilter = state.activeFilter,
                selectedLevels = state.tempSelectedLevels,
                onLevelToggle = { level ->
                    viewModel.onAction(CommunityAction.OnLevelFilterToggle(level))
                },
                onApply = {
                    viewModel.onAction(CommunityAction.OnFilterApply(state.activeFilter))
                },
                onClearAll = {
                    viewModel.onAction(CommunityAction.OnClearFilters)
                },
                onDismiss = {
                    viewModel.onAction(CommunityAction.OnFilterSheetDismiss)
                }
            )
        }
    }
}

/**
 * Main tabs: Discover / Saved
 */
@Composable
private fun MainTabs(
    selectedTabIndex: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(3.dp)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(FlashRed)
            )
        },
        divider = {}
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabChange(0) },
            text = {
                Text(
                    text = "Discover",
                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabChange(1) },
            text = {
                Text(
                    text = "Saved",
                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Filter button with badge showing active filter count.
 */
@Composable
private fun FilterButton(
    filterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Filter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Badge
        if (filterCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(FlashRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filterCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sort dropdown button showing current sort option.
 */
@Composable
private fun SortDropdownButton(
    selectedSort: CommunitySortOption,
    onSortChange: (CommunitySortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedSort.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select sort option",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CommunitySortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selectedSort) FlashRed else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Empty state for when no topics are found.
 */
@Composable
private fun EmptyState(
    isDiscoverTab: Boolean,
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isDiscoverTab) {
                    if (hasFilters) "No topics found" else "No topics yet"
                } else {
                    "No saved topics yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isDiscoverTab) {
                    if (hasFilters) "Try adjusting your filters or search" else "Be the first to share a topic!"
                } else {
                    "Bookmark topics to save them here"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
