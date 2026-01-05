package com.kotlin.flashlearn.domain.model

/**
 * Filter options for Community screen.
 * Supports filtering by search query, VSTEP levels, and creator.
 */
data class CommunityFilter(
    val query: String = "",
    val levels: List<VSTEPLevel> = emptyList(),
    val creatorId: String? = null
) {
    /**
     * Returns the count of active filters (for badge display).
     */
    val activeFilterCount: Int
        get() = levels.size + (if (creatorId != null) 1 else 0)
    
    /**
     * Returns true if any filter is active.
     */
    val hasActiveFilters: Boolean
        get() = levels.isNotEmpty() || creatorId != null
    
    /**
     * Clears all filters.
     */
    fun clear(): CommunityFilter = CommunityFilter()
}

/**
 * Sort options for Community topics list.
 */
enum class CommunitySortOption(val displayName: String) {
    UPVOTES("Upvoted"),
    NEWEST("Newest")
}
