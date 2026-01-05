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
        get() = authRepository.getSignedInUser()?.id

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
                _state.update { 
                    it.copy(
                        activeFilter = action.filter,
                        isFilterSheetVisible = false
                    ) 
                }
                loadTopics()
            }
            is CommunityAction.OnToggleFavorite -> {
                toggleFavorite(action.topicId)
            }
            is CommunityAction.OnDownloadTopic -> {
                downloadTopic(action.topicId)
            }
            is CommunityAction.OnTopicClick -> {
                viewModelScope.launch {
                    _uiEvent.emit(CommunityUiEvent.NavigateToTopicDetail(action.topicId))
                }
            }
            is CommunityAction.OnLevelFilterToggle -> {
                toggleLevelFilter(action.level)
            }
            CommunityAction.OnFilterSheetOpen -> {
                _state.update { it.copy(isFilterSheetVisible = true) }
            }
            CommunityAction.OnFilterSheetDismiss -> {
                _state.update { it.copy(isFilterSheetVisible = false) }
            }
            CommunityAction.OnClearFilters -> {
                _state.update { 
                    it.copy(
                        activeFilter = CommunityFilter(),
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

                    // Apply filters
                    var filteredTopics = topics
                    
                    val currentState = _state.value

                    // Filter by search query
                    if (currentState.searchQuery.isNotBlank()) {
                        filteredTopics = filteredTopics.filter { topic ->
                            topic.name.contains(currentState.searchQuery, ignoreCase = true) ||
                            topic.description.contains(currentState.searchQuery, ignoreCase = true)
                        }
                    }

                    // Filter by VSTEP level
                    if (currentState.activeFilter.levels.isNotEmpty()) {
                        filteredTopics = filteredTopics.filter { topic ->
                            // Check if topic name contains any of the selected levels
                            currentState.activeFilter.levels.any { level ->
                                topic.name.contains(level.displayName, ignoreCase = true)
                            }
                        }
                    }

                    // Filter by creator
                    currentState.activeFilter.creatorId?.let { creatorId ->
                        filteredTopics = filteredTopics.filter { it.createdBy == creatorId }
                    }

                    // Sort topics
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

    private fun downloadTopic(topicId: String) {
        // TODO: Implement download functionality
        viewModelScope.launch {
            _uiEvent.emit(CommunityUiEvent.ShowSuccess("Download feature coming soon!"))
        }
    }

    private fun toggleLevelFilter(level: VSTEPLevel) {
        _state.update { state ->
            val currentLevels = state.activeFilter.levels.toMutableList()
            if (level in currentLevels) {
                currentLevels.remove(level)
            } else {
                currentLevels.add(level)
            }
            state.copy(
                activeFilter = state.activeFilter.copy(levels = currentLevels)
            )
        }
    }
}
