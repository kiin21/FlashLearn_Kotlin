package com.kotlin.flashlearn.domain.model

import com.kotlin.flashlearn.R

/**
 * Filter options for Community screen.
 * Supports filtering by VSTEP levels.
 */
data class CommunityFilter(
    val query: String = "",
    val levels: List<VSTEPLevel> = emptyList()
) {
    /**
     * Returns the count of active filters (for badge display).
     */
    val activeFilterCount: Int
        get() = levels.size
    
    /**
     * Returns true if any filter is active.
     */
    val hasActiveFilters: Boolean
        get() = levels.isNotEmpty()
    
    /**
     * Clears all filters.
     */
    fun clear(): CommunityFilter = CommunityFilter()
}

/**
 * Sort options for Community topics list.
 * All options sort in descending order (most/newest first).
 */
enum class CommunitySortOption(val resId: Int) {
    UPVOTES(R.string.sort_most_liked),
    NEWEST(R.string.sort_newest)
}
