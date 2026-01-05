package com.kotlin.flashlearn.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.CommunityFilter
import com.kotlin.flashlearn.domain.model.CommunitySortOption
import com.kotlin.flashlearn.domain.model.VSTEPLevel
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FavoriteRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Community screen.
 * Manages loading public topics, filtering, sorting, and favorite operations.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val favoriteRepository: FavoriteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CommunityUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Cache the current user ID for favorite operations
    private val currentUserId: String?
        get() = authRepository.getSignedInUser()?.userId

    init {
        loadTopics()
    }

    /**
     * Handles user actions from the UI.
     */
    fun onAction(action: CommunityAction) {
        when (action) {
            is CommunityAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
                loadTopics()
            }
            is CommunityAction.OnSortChange -> {
                _state.update { it.copy(activeSort = action.sort) }
                loadTopics()
            }
            is CommunityAction.OnFilterApply -> {
                // Apply: copy temp state to active filter, close sheet, reload
                _state.update { state ->
                    state.copy(
                        activeFilter = state.activeFilter.copy(levels = state.tempSelectedLevels),
                        tempSelectedLevels = emptyList(),
                        isFilterSheetVisible = false
                    ) 
                }
                loadTopics()
            }
            is CommunityAction.OnToggleFavorite -> {
                toggleFavorite(action.topicId)
            }
            is CommunityAction.OnTopicClick -> {
                viewModelScope.launch {
                    _uiEvent.emit(CommunityUiEvent.NavigateToTopicDetail(action.topicId))
                }
            }
            is CommunityAction.OnLevelFilterToggle -> {
                // Toggle in TEMPORARY state (not applied yet)
                toggleTempLevelFilter(action.level)
            }
            CommunityAction.OnFilterSheetOpen -> {
                // Copy current active filter to temp state when opening sheet
                _state.update { 
                    it.copy(
                        isFilterSheetVisible = true,
                        tempSelectedLevels = it.activeFilter.levels
                    ) 
                }
            }
            CommunityAction.OnFilterSheetDismiss -> {
                // Cancel: discard temp state, close sheet
                _state.update { 
                    it.copy(
                        isFilterSheetVisible = false,
                        tempSelectedLevels = emptyList()
                    ) 
                }
            }
            CommunityAction.OnClearFilters -> {
                // Clear All: immediately apply empty filter and close
                _state.update { 
                    it.copy(
                        activeFilter = CommunityFilter(),
                        tempSelectedLevels = emptyList(),
                        isFilterSheetVisible = false
                    ) 
                }
                loadTopics()
            }
            CommunityAction.OnRefresh -> {
                loadTopics()
            }
        }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = topicRepository.getPublicTopics()

            result.fold(
                onSuccess = { topics ->
                    val userId = currentUserId
                    val favoriteIds = if (userId != null) {
                        favoriteRepository.getFavoriteTopicIdsOnce(userId).getOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // Filter: Community shows user-created topics from OTHER users only
                    // - Exclude system topics
                    // - Exclude current user's own topics (they're in Topic screen)
                    var filteredTopics = topics.filter { topic ->
                        !topic.isSystemTopic && topic.createdBy != userId
                    }
                    
                    val currentState = _state.value

                    // Filter by search query (name, description, creatorName)
                    if (currentState.searchQuery.isNotBlank()) {
                        val query = currentState.searchQuery.trim()
                        filteredTopics = filteredTopics.filter { topic ->
                            topic.name.contains(query, ignoreCase = true) ||
                            topic.description.contains(query, ignoreCase = true) ||
                            topic.creatorName.contains(query, ignoreCase = true)
                        }
                    }

                    // Filter by VSTEP level - check vstepLevel field FIRST, then fallback to name
                    if (currentState.activeFilter.levels.isNotEmpty()) {
                        filteredTopics = filteredTopics.filter { topic ->
                            // Primary: check vstepLevel field
                            topic.vstepLevel in currentState.activeFilter.levels ||
                            // Fallback: check if name contains level string
                            currentState.activeFilter.levels.any { level ->
                                topic.name.contains(level.displayName, ignoreCase = true)
                            }
                        }
                    }

                    // Filter by creator
                    currentState.activeFilter.creatorId?.let { creatorId ->
                        filteredTopics = filteredTopics.filter { it.createdBy == creatorId }
                    }

                    // Sort topics (all descending - most/newest first)
                    val sortedTopics = when (currentState.activeSort) {
                        CommunitySortOption.UPVOTES -> {
                            filteredTopics.sortedByDescending { it.upvoteCount }
                        }
                        CommunitySortOption.NEWEST -> {
                            filteredTopics.sortedByDescending { it.createdAt }
                        }
                    }

                    // Map to UI items
                    val topicItems = sortedTopics.map { topic ->
                        CommunityTopicItem(
                            topic = topic,
                            isFavorited = topic.id in favoriteIds,
                            isDownloaded = false // TODO: Implement download tracking
                        )
                    }

                    _state.update { 
                        it.copy(
                            topics = topicItems,
                            isLoading = false
                        ) 
                    }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load topics"
                        ) 
                    }
                    viewModelScope.launch {
                        _uiEvent.emit(CommunityUiEvent.ShowError(error.message ?: "Failed to load topics"))
                    }
                }
            )
        }
    }

    private fun toggleFavorite(topicId: String) {
        val userId = currentUserId ?: run {
            viewModelScope.launch {
                _uiEvent.emit(CommunityUiEvent.ShowError("Please sign in to favorite topics"))
            }
            return
        }

        viewModelScope.launch {
            val result = favoriteRepository.toggleFavorite(userId, topicId)

            result.fold(
                onSuccess = { isFavorited ->
                    // Update local state immediately for responsive UI
                    _state.update { state ->
                        state.copy(
                            topics = state.topics.map { item ->
                                if (item.topic.id == topicId) {
                                    item.copy(
                                        isFavorited = isFavorited,
                                        topic = item.topic.copy(
                                            upvoteCount = if (isFavorited) {
                                                item.topic.upvoteCount + 1
                                            } else {
                                                (item.topic.upvoteCount - 1).coerceAtLeast(0)
                                            }
                                        )
                                    )
                                } else {
                                    item
                                }
                            }
                        )
                    }

                    val message = if (isFavorited) "Added to favorites" else "Removed from favorites"
                    _uiEvent.emit(CommunityUiEvent.ShowSuccess(message))
                },
                onFailure = { error ->
                    _uiEvent.emit(CommunityUiEvent.ShowError(error.message ?: "Failed to update favorite"))
                }
            )
        }
    }

    private fun toggleTempLevelFilter(level: VSTEPLevel) {
        _state.update { state ->
            val currentLevels = state.tempSelectedLevels.toMutableList()
            if (level in currentLevels) {
                currentLevels.remove(level)
            } else {
                currentLevels.add(level)
            }
            state.copy(tempSelectedLevels = currentLevels)
        }
    }
}
