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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
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
import com.kotlin.flashlearn.presentation.components.SortDropdown
import com.kotlin.flashlearn.ui.theme.FlashRed
import kotlinx.coroutines.flow.collectLatest

/**
 * Community screen displaying shared public topics.
 * Users can search, filter, sort, and favorite topics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
                .padding(horizontal = 16.dp)
        ) {
            // Title
            Text(
                text = "Community",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
            
            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onAction(CommunityAction.OnSearchQueryChange(it)) },
                placeholder = "Find \"IELTS\" or \"Speaking\"..."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sort & Filter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort dropdown
                SortDropdown(
                    selectedSort = state.activeSort,
                    onSortChange = { viewModel.onAction(CommunityAction.OnSortChange(it)) }
                )
                
                // Filter button with badge
                FilterButton(
                    filterCount = state.filterBadgeCount,
                    onClick = { viewModel.onAction(CommunityAction.OnFilterSheetOpen) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tabs (Upvoted / Newest)
            SortTabs(
                selectedSort = state.activeSort,
                onSortChange = { viewModel.onAction(CommunityAction.OnSortChange(it)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlashRed)
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.topics.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No topics found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Try adjusting your filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = state.topics,
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
                                onDownloadClick = { 
                                    viewModel.onAction(CommunityAction.OnDownloadTopic(item.topic.id)) 
                                }
                            )
                        }
                        
                        // Bottom spacing
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
        
        // Filter bottom sheet
        if (state.isFilterSheetVisible) {
            FilterBottomSheet(
                sheetState = filterSheetState,
                currentFilter = state.activeFilter,
                selectedLevels = state.activeFilter.levels,
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
                .background(MaterialTheme.colorScheme.surface)
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
                    .offset(x = 4.dp, y = (-4).dp)
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
 * Sort tabs (Upvoted / Newest).
 */
@Composable
private fun SortTabs(
    selectedSort: CommunitySortOption,
    onSortChange: (CommunitySortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember(selectedSort) {
        mutableIntStateOf(
            when (selectedSort) {
                CommunitySortOption.UPVOTES -> 0
                CommunitySortOption.NEWEST -> 1
            }
        )
    }
    
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
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(FlashRed)
            )
        },
        divider = {}
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = {
                selectedTabIndex = 0
                onSortChange(CommunitySortOption.UPVOTES)
            },
            text = {
                Text(
                    text = "Upvoted",
                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = {
                selectedTabIndex = 1
                onSortChange(CommunitySortOption.NEWEST)
            },
            text = {
                Text(
                    text = "Newest",
                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
