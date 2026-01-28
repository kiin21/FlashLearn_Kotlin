package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.VSTEPLevel
import com.kotlin.flashlearn.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId").orEmpty()

    private val currentUserId: String?
        get() = authRepository.getSignedInUser()?.userId ?: firebaseAuth.currentUser?.uid

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
                        isPublic = topic.isPublic,
                        isSystemTopic = topic.isSystemTopic
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
                    displayedCards = flashcards,
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

    /**
     * Updates search query and filters flashcards.
     */
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applySearch()
    }

    /**
     * Filters flashcards based on search query and level filters.
     * Searches only by word (vocabulary term).
     */
    private fun applySearch() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allCards = _state.value.cards
        val selectedLevels = _state.value.selectedLevels

        var filtered = allCards

        // Apply search query filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { card ->
                card.word.lowercase().contains(query)
            }
        }

        // Apply level filter
        if (selectedLevels.isNotEmpty()) {
            filtered = filtered.filter { card ->
                selectedLevels.any { level -> level.displayName == card.level }
            }
        }

        _state.value = _state.value.copy(displayedCards = filtered)
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

    /**
     * Saves a community topic to user's own collection (clone).
     */
    fun saveToMyTopics() {
        val userId = currentUserId ?: run {
            _state.value = _state.value.copy(error = "Please sign in to save topics")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Get username from AuthRepository (works for username/password users)
            val signedInUser = authRepository.getSignedInUser()
            val userName = signedInUser?.username
                ?: firebaseAuth.currentUser?.displayName
                ?: firebaseAuth.currentUser?.email?.substringBefore("@")
                ?: "Anonymous"

            topicRepository.cloneTopicToUser(topicId, userId, userName)
                .onSuccess { clonedTopic ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Saved to My Topics!"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save topic"
                    )
                }
        }
    }

    fun updateFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            var cardToUpdate = flashcard

            // Check if image needs upload (starts with content://)
            if (flashcard.imageUrl.startsWith("content://")) {
                val uploadResult = flashcardRepository.uploadImage(flashcard.imageUrl, flashcard.id)
                uploadResult.fold(
                    onSuccess = { newUrl ->
                        cardToUpdate = flashcard.copy(imageUrl = newUrl)
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Image upload failed: ${e.message}"
                        )
                        return@launch
                    }
                )
            }

            flashcardRepository.updateFlashcard(topicId, cardToUpdate)
                .onSuccess {
                    loadFlashcards() // Reload list
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update card"
                    )
                }
        }
    }

    fun deleteFlashcard(flashcardId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            flashcardRepository.deleteFlashcards(listOf(flashcardId))
                .onSuccess {
                    loadFlashcards()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete card"
                    )
                }
        }
    }

    /**
     * Clears success/error messages after they've been shown.
     */
    fun clearMessages() {
        _state.value = _state.value.copy(
            successMessage = null,
            error = null
        )
    }

    /**
     * Opens the filter bottom sheet.
     */
    fun openFilterSheet() {
        _state.value = _state.value.copy(isFilterSheetVisible = true)
    }

    /**
     * Closes the filter bottom sheet.
     */
    fun dismissFilterSheet() {
        _state.value = _state.value.copy(isFilterSheetVisible = false)
    }

    /**
     * Toggles a level filter on/off.
     */
    fun toggleLevelFilter(level: VSTEPLevel) {
        val currentLevels = _state.value.selectedLevels
        val newLevels = if (currentLevels.contains(level)) {
            currentLevels - level
        } else {
            currentLevels + level
        }
        _state.value = _state.value.copy(selectedLevels = newLevels)
    }

    /**
     * Applies the selected filters and closes the sheet.
     */
    fun applyFilters() {
        applySearch() // This now includes filter logic
        dismissFilterSheet()
    }

    /**
     * Clears all filters.
     */
    fun clearFilters() {
        _state.value = _state.value.copy(selectedLevels = emptyList())
        applySearch()
        dismissFilterSheet()
    }

    /**
     * Resets all learning progress for this topic.
     * Deletes all mastered/review status for all cards in the topic.
     */
    fun resetTopicProgress() {
        viewModelScope.launch {
            val userId = currentUserId
            if (userId == null) {
                _state.value = _state.value.copy(error = "User not authenticated")
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            flashcardRepository.resetTopicProgress(topicId, userId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Progress reset successfully! You can study this topic from the beginning."
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to reset progress"
                    )
                }
        }
    }
}
