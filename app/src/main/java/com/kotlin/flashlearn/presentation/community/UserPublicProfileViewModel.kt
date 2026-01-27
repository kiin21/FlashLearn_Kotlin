package com.kotlin.flashlearn.presentation.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.CommunityInteractionRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for viewing another user's public profile and topics.
 */
@HiltViewModel
class UserPublicProfileViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val communityInteractionRepository: CommunityInteractionRepository,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val userId: String = savedStateHandle.get<String>("userId").orEmpty()
    
    private val _state = MutableStateFlow(UserPublicProfileState())
    val state: StateFlow<UserPublicProfileState> = _state.asStateFlow()
    
    private val currentUserId: String?
        get() = authRepository.getSignedInUser()?.userId ?: firebaseAuth.currentUser?.uid
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // Fetch all public topics from this user
            topicRepository.getPublicTopics()
                .onSuccess { allTopics ->
                    val userTopics = allTopics.filter { it.createdBy == userId && it.isPublic }
                    
                    // Get user info from first topic (creatorName)
                    val creatorName = userTopics.firstOrNull()?.creatorName ?: "Unknown User"
                    val totalUpvotes = userTopics.sumOf { it.upvoteCount }
                    
                    // Fetch upvoted topic IDs for current user
                    val upvotedIds = currentUserId?.let { uid ->
                        communityInteractionRepository.getUpvotedTopicIdsOnce(uid).getOrNull() ?: emptyList()
                    } ?: emptyList()
                    
                    // Create topic items with upvote state
                    val topicItems = userTopics.map { topic ->
                        UserProfileTopicItem(
                            topic = topic,
                            isUpvoted = topic.id in upvotedIds
                        )
                    }
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        userId = userId,
                        userName = creatorName,
                        topicItems = topicItems,
                        totalUpvotes = totalUpvotes,
                        isOwnProfile = userId == currentUserId
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
        }
    }
    
    /**
     * Toggle upvote for a topic.
     */
    fun toggleUpvote(topicId: String) {
        val uid = currentUserId ?: return
        
        viewModelScope.launch {
            communityInteractionRepository.toggleUpvote(uid, topicId)
                .onSuccess { isNowUpvoted ->
                    // Update local state
                    val updatedItems = _state.value.topicItems.map { item ->
                        if (item.topic.id == topicId) {
                            val newCount = if (isNowUpvoted) 
                                item.topic.upvoteCount + 1 
                            else 
                                (item.topic.upvoteCount - 1).coerceAtLeast(0)
                            
                            item.copy(
                                topic = item.topic.copy(upvoteCount = newCount),
                                isUpvoted = isNowUpvoted
                            )
                        } else item
                    }
                    
                    // Recalculate total upvotes
                    val newTotal = updatedItems.sumOf { it.topic.upvoteCount }
                    
                    _state.value = _state.value.copy(
                        topicItems = updatedItems,
                        totalUpvotes = newTotal
                    )
                }
        }
    }
    
    fun retry() {
        _state.value = _state.value.copy(error = null)
        loadUserProfile()
    }
}

/**
 * Topic item with upvote state.
 */
data class UserProfileTopicItem(
    val topic: Topic,
    val isUpvoted: Boolean = false
)

/**
 * State for UserPublicProfileScreen.
 */
data class UserPublicProfileState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userId: String = "",
    val userName: String = "",
    val topicItems: List<UserProfileTopicItem> = emptyList(),
    val totalUpvotes: Int = 0,
    val isOwnProfile: Boolean = false
)
