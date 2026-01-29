package com.kotlin.flashlearn.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.CommunityFilter
import com.kotlin.flashlearn.domain.model.CommunitySortOption
import com.kotlin.flashlearn.domain.model.VSTEPLevel
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.CommunityInteractionRepository
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
 * Manages loading public topics, filtering, sorting, and bookmark/upvote operations.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val communityInteractionRepository: CommunityInteractionRepository,
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

            is CommunityAction.OnToggleBookmark -> {
                toggleBookmark(action.topicId)
            }

            is CommunityAction.OnToggleUpvote -> {
                toggleUpvote(action.topicId)
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
                    val bookmarkIds = if (userId != null) {
                        communityInteractionRepository.getBookmarkedTopicIdsOnce(userId).getOrNull()
                            ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val upvotedIds = if (userId != null) {
                        communityInteractionRepository.getUpvotedTopicIdsOnce(userId).getOrNull()
                            ?: emptyList()
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

                    // Filter by level (OR logic - show topic if it has ANY matching level)
                    if (currentState.activeFilter.levels.isNotEmpty()) {
                        val selectedLevelNames =
                            currentState.activeFilter.levels.map { it.displayName }
                        filteredTopics = filteredTopics.filter { topic ->
                            // Check if topic's wordLevels contains any selected level
                            topic.wordLevels.any { wordLevel ->
                                wordLevel in selectedLevelNames
                            } ||
                                    // Fallback: check topic name for level keywords
                                    selectedLevelNames.any { level ->
                                        topic.name.contains(level, ignoreCase = true)
                                    }
                        }
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
                            isBookmarked = topic.id in bookmarkIds,
                            isUpvoted = topic.id in upvotedIds
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
                        _uiEvent.emit(
                            CommunityUiEvent.ShowError(
                                error.message ?: "Failed to load topics"
                            )
                        )
                    }
                }
            )
        }
    }

    /**
     * Toggle bookmark status (private save for "Saved" tab).
     * Does NOT affect upvote count.
     */
    private fun toggleBookmark(topicId: String) {
        val userId = currentUserId ?: run {
            viewModelScope.launch {
                _uiEvent.emit(CommunityUiEvent.ShowError("Please sign in to save topics"))
            }
            return
        }

        viewModelScope.launch {
            val result = communityInteractionRepository.toggleBookmark(userId, topicId)

            result.fold(
                onSuccess = { isBookmarked ->
                    // Update local state immediately for responsive UI
                    _state.update { state ->
                        state.copy(
                            topics = state.topics.map { item ->
                                if (item.topic.id == topicId) {
                                    item.copy(isBookmarked = isBookmarked)
                                } else {
                                    item
                                }
                            }
                        )
                    }

                    val message = if (isBookmarked) "Saved" else "Removed from saved"
                    _uiEvent.emit(CommunityUiEvent.ShowSuccess(message))
                },
                onFailure = { error ->
                    _uiEvent.emit(
                        CommunityUiEvent.ShowError(
                            error.message ?: "Failed to save topic"
                        )
                    )
                }
            )
        }
    }

    /**
     * Toggle upvote status (public vote, affects ranking).
     * DOES affect upvote count on topic.
     */
    private fun toggleUpvote(topicId: String) {
        val userId = currentUserId ?: run {
            viewModelScope.launch {
                _uiEvent.emit(CommunityUiEvent.ShowError("Please sign in to upvote"))
            }
            return
        }

        viewModelScope.launch {
            val result = communityInteractionRepository.toggleUpvote(userId, topicId)

            result.fold(
                onSuccess = { isUpvoted ->
                    // Update local state immediately for responsive UI
                    _state.update { state ->
                        state.copy(
                            topics = state.topics.map { item ->
                                if (item.topic.id == topicId) {
                                    item.copy(
                                        isUpvoted = isUpvoted,
                                        topic = item.topic.copy(
                                            upvoteCount = if (isUpvoted) {
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
                },
                onFailure = { error ->
                    _uiEvent.emit(CommunityUiEvent.ShowError(error.message ?: "Failed to upvote"))
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
