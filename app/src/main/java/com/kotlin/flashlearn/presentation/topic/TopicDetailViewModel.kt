package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId").orEmpty()
    
    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    private val _state = MutableStateFlow(TopicDetailState())
    val state: StateFlow<TopicDetailState> = _state.asStateFlow()

    init {
        loadTopicAndCards()
    }

    private fun loadTopicAndCards() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Get topic details for title/description
            val topicResult = topicRepository.getTopicById(topicId)
            
            topicResult.fold(
                onSuccess = { topic ->
                    if (topic == null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Topic not found"
                        )
                        return@launch
                    }
                    
                    val isOwner = topic.createdBy == currentUserId
                    
                    _state.value = _state.value.copy(
                        topicTitle = topic.name,
                        topicDescription = topic.description,
                        isOwner = isOwner,
                        imageUrl = topic.imageUrl ?: "",
                        isPublic = topic.isPublic
                    )
                    
                    // Load flashcards from repository (backed by Datamuse API)
                    loadFlashcards()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topic"
                    )
                }
            )
        }
    }
    
    private suspend fun loadFlashcards() {
        val flashcardResult = flashcardRepository.getFlashcardsByTopicId(topicId)
        
        flashcardResult.fold(
            onSuccess = { flashcards ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    cards = flashcards,
                    error = null,
                    // Reset selection when reloading
                    isSelectionMode = false,
                    selectedCardIds = emptySet()
                )
            },
            onFailure = { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    cards = emptyList(),
                    error = e.message ?: "Failed to load vocabulary"
                )
            }
        )
    }
    
    fun refreshCards() {
        loadTopicAndCards()
    }
    
    fun toggleSelectionMode() {
        _state.value = _state.value.copy(
            isSelectionMode = !_state.value.isSelectionMode,
            selectedCardIds = emptySet()
        )
    }
    
    fun toggleCardSelection(cardId: String) {
        val currentSelected = _state.value.selectedCardIds.toMutableSet()
        if (currentSelected.contains(cardId)) {
            currentSelected.remove(cardId)
        } else {
            currentSelected.add(cardId)
        }
        
        // Auto-exit selection mode if deselecting the last one? 
        // Or strictly strictly only exit if explicit cancel or empty after delete.
        // Let's keep selection mode active even if empty, until explicitly cancelled or deleted.
        
        _state.value = _state.value.copy(selectedCardIds = currentSelected)
    }
    
    fun selectAllCards() {
        val allIds = _state.value.cards.map { it.id }.toSet()
        _state.value = _state.value.copy(selectedCardIds = allIds)
    }

    fun deleteSelectedCards() {
        viewModelScope.launch {
            val selectedIds = _state.value.selectedCardIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            _state.value = _state.value.copy(isLoading = true)
            
            flashcardRepository.deleteFlashcards(selectedIds)
                .onSuccess {
                    loadFlashcards() // Reload to refresh list and exit selection
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete cards"
                    )
                }
        }
    }
    
    fun deleteTopic(onSuccess: () -> Unit) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isLoading = true)
             topicRepository.deleteTopic(topicId)
                 .onSuccess {
                     onSuccess()
                 }
                 .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete topic"
                    )
                 }
        }
    }

    fun updateTopic(newName: String, newDescription: String, newImageUrl: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val currentTopicResult = topicRepository.getTopicById(topicId)
            val currentTopic = currentTopicResult.getOrNull()
            
            if (currentTopic != null) {
                val updatedTopic = currentTopic.copy(
                    name = newName,
                    description = newDescription,
                    imageUrl = newImageUrl
                )
                
                topicRepository.updateTopic(updatedTopic)
                    .onSuccess { topic ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            topicTitle = topic.name,
                            topicDescription = topic.description,
                            imageUrl = topic.imageUrl ?: ""
                        )
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to update topic"
                        )
                    }
            }
        }
    }
    
    fun regenerateImage() {
        viewModelScope.launch {
             _state.value = _state.value.copy(isLoading = true)
             topicRepository.regenerateTopicImage(topicId)
                 .onSuccess { newUrl ->
                     _state.value = _state.value.copy(
                         isLoading = false,
                         imageUrl = newUrl
                     )
                 }
                 .onFailure { e ->
                     _state.value = _state.value.copy(
                         isLoading = false,
                         error = e.message ?: "Failed to regenerate image"
                     )
                 }
        }
    }
    
    /**
     * Toggle topic public/private status.
     * When public, topic appears in Community for others to discover.
     */
    fun togglePublicStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val currentTopicResult = topicRepository.getTopicById(topicId)
            val currentTopic = currentTopicResult.getOrNull()
            
            if (currentTopic != null) {
                val newIsPublic = !currentTopic.isPublic
                val updatedTopic = currentTopic.copy(isPublic = newIsPublic)
                
                topicRepository.updateTopic(updatedTopic)
                    .onSuccess { topic ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isPublic = topic.isPublic
                        )
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to update visibility"
                        )
                    }
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
