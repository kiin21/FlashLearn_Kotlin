package com.kotlin.flashlearn.presentation.community

import com.kotlin.flashlearn.domain.model.CommunityFilter
import com.kotlin.flashlearn.domain.model.CommunitySortOption
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.VSTEPLevel

/**
 * Represents the UI state for the Community screen.
 */
data class CommunityState(
    val topics: List<CommunityTopicItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val activeFilter: CommunityFilter = CommunityFilter(),
    val activeSort: CommunitySortOption = CommunitySortOption.UPVOTES,
    val isFilterSheetVisible: Boolean = false,
    // Temporary filter state (for bottom sheet - not yet applied)
    val tempSelectedLevels: List<VSTEPLevel> = emptyList()
) {
    /**
     * Returns the count of active filters for badge display.
     */
    val filterBadgeCount: Int
        get() = activeFilter.activeFilterCount
}

/**
 * Represents a topic item in the Community list with its interaction status.
 * 
 * @param topic The topic data
 * @param isFavorited Whether the user has saved this topic (private, for "Favorites" tab)
 * @param isUpvoted Whether the user has upvoted this topic (public, affects ranking)
 */
data class CommunityTopicItem(
    val topic: Topic,
    val isFavorited: Boolean = false,
    val isUpvoted: Boolean = false
)
