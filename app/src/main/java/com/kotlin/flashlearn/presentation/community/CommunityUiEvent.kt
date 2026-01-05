package com.kotlin.flashlearn.presentation.community

import com.kotlin.flashlearn.domain.model.CommunityFilter
import com.kotlin.flashlearn.domain.model.CommunitySortOption
import com.kotlin.flashlearn.domain.model.VSTEPLevel

/**
 * UI events that can be triggered from the Community screen.
 */
sealed class CommunityUiEvent {
    data class ShowError(val message: String) : CommunityUiEvent()
    data class ShowSuccess(val message: String) : CommunityUiEvent()
    data class NavigateToTopicDetail(val topicId: String) : CommunityUiEvent()
}

/**
 * User actions/intents from the Community screen.
 */
sealed class CommunityAction {
    data class OnSearchQueryChange(val query: String) : CommunityAction()
    data class OnSortChange(val sort: CommunitySortOption) : CommunityAction()
    data class OnFilterApply(val filter: CommunityFilter) : CommunityAction()
    data class OnToggleFavorite(val topicId: String) : CommunityAction()
    data class OnDownloadTopic(val topicId: String) : CommunityAction()
    data class OnTopicClick(val topicId: String) : CommunityAction()
    data class OnLevelFilterToggle(val level: VSTEPLevel) : CommunityAction()
    data object OnFilterSheetOpen : CommunityAction()
    data object OnFilterSheetDismiss : CommunityAction()
    data object OnClearFilters : CommunityAction()
    data object OnRefresh : CommunityAction()
}
