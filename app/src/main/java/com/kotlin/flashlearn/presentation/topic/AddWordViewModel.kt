package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for AddWordScreen - handles topic creation with word selection.
 */
@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val datamuseRepository: DatamuseRepository,
    private val topicRepository: TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid
    
    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()
    
    // New flow for search query input
    private val _searchQueryFlow = MutableStateFlow("")
    
    // Local cache for search suggestions
    private val searchCache = mutableMapOf<String, List<WordSuggestion>>()
    
    init {
        loadTopics()
        setupSearchFlow()
    }
    
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupSearchFlow() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(250) // Wait 250ms for no new changes
                .filter { it.length >= 2 } // Only search if query has at least 2 chars
                .distinctUntilChanged() // Only call API if query is different from previous
                .collectLatest { query ->
                    // Check cache first
                    if (searchCache.containsKey(query)) {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false, 
                            searchSuggestions = searchCache[query]!!
                        )
                    } else {
                        // Call API
                        _uiState.value = _uiState.value.copy(isSearching = true)
                        datamuseRepository.getAutocompleteSuggestions(query)
                            .onSuccess { suggestions ->
                                searchCache[query] = suggestions
                                _uiState.value = _uiState.value.copy(
                                    isSearching = false,
                                    searchSuggestions = suggestions
                                )
                            }
                            .onFailure {
                                _uiState.value = _uiState.value.copy(
                                    isSearching = false,
                                    searchSuggestions = emptyList()
                                )
                            }
                    }
                }
        }
    }
    
    /**
     * Load all available topics for suggestion dropdown.
     */
    private fun loadTopics() {
        viewModelScope.launch {
            topicRepository.getPublicTopics()
                .onSuccess { topics ->
                    _uiState.value = _uiState.value.copy(availableTopics = topics)
                }
        }
    }
    
    /**
     * Update new topic name input.
     */
    fun onNewTopicNameChange(name: String) {
        _uiState.value = _uiState.value.copy(newTopicName = name)
    }
    
    /**
     * Update new topic description input.
     */
    fun onNewTopicDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(newTopicDescription = description)
    }
    
    /**
     * Select a topic from the dropdown for word suggestions.
     */
    fun onTopicSelected(topic: Topic) {
        _uiState.value = _uiState.value.copy(
            selectedTopic = topic,
            topicQuery = extractKeyword(topic.name),
            showTopicDropdown = false
        )
        loadWordsByTopic()
    }
    
    fun toggleTopicDropdown() {
        _uiState.value = _uiState.value.copy(
            showTopicDropdown = !_uiState.value.showTopicDropdown
        )
    }
    
    /**
     * Search for words as user types (autocomplete).
     */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQueryFlow.value = query
        
        // Clear suggestions if query is too short
        if (query.length < 2) {
             _uiState.value = _uiState.value.copy(searchSuggestions = emptyList())
        }
    }
    
    /**
     * Get word details with definition when user selects a suggestion.
     */
    fun onSuggestionSelected(word: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetails = true)
            
            datamuseRepository.searchWords("$word")
                .onSuccess { words ->
                    val selectedWord = words.find { it.word.equals(word, ignoreCase = true) }
                    if (selectedWord != null) {
                        // Add to selected words
                        val currentSelected = _uiState.value.selectedWords.toMutableSet()
                        currentSelected.add(selectedWord)
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetails = false,
                            selectedWords = currentSelected,
                            searchSuggestions = emptyList(),
                            searchQuery = ""
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingDetails = false)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingDetails = false)
                }
        }
    }
    
    /**
     * Update topic query for manual input.
     */
    fun onTopicQueryChange(topic: String) {
        _uiState.value = _uiState.value.copy(topicQuery = topic)
    }
    
    /**
     * Load vocabulary words related to the selected/entered topic.
     */
    fun loadWordsByTopic() {
        val topic = _uiState.value.topicQuery
        if (topic.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTopicWords = true)
            
            datamuseRepository.getWordsByTopic(topic)
                .onSuccess { words ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingTopicWords = false,
                        topicSuggestions = words
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingTopicWords = false,
                        topicSuggestions = emptyList()
                    )
                }
        }
    }
    
    /**
     * Toggle word selection for batch add.
     */
    fun toggleWordSelection(word: VocabularyWord) {
        val currentSelected = _uiState.value.selectedWords.toMutableSet()
        if (currentSelected.contains(word)) {
            currentSelected.remove(word)
        } else {
            currentSelected.add(word)
        }
        _uiState.value = _uiState.value.copy(selectedWords = currentSelected)
    }
    
    /**
     * Remove a word from selected words.
     */
    fun removeSelectedWord(word: VocabularyWord) {
        val currentSelected = _uiState.value.selectedWords.toMutableSet()
        currentSelected.remove(word)
        _uiState.value = _uiState.value.copy(selectedWords = currentSelected)
    }
    
    /**
     * Create a new topic with selected words and save to database.
     */
    fun createTopic(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val topicName = _uiState.value.newTopicName.trim()
        val description = _uiState.value.newTopicDescription.trim()
        val selectedWords = _uiState.value.selectedWords
        
        if (topicName.isBlank()) {
            onError("Please enter a topic name")
            return
        }
        
        if (selectedWords.isEmpty()) {
            onError("Please select at least one word")
            return
        }
        
        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            onError("Please sign in to create topics")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingTopic = true)
            
            val topicId = UUID.randomUUID().toString()
            
            // Get creator name from Firebase Auth
            val currentUser = firebaseAuth.currentUser
            val creatorName = currentUser?.displayName 
                ?: currentUser?.email?.substringBefore("@") 
                ?: "Anonymous"
            
            val newTopic = Topic(
                id = topicId,
                name = topicName,
                description = description.ifBlank { "Custom vocabulary collection" },
                isSystemTopic = false,
                isPublic = false,
                createdBy = userId,
                creatorName = creatorName
            )
            
            // Convert VocabularyWord to Flashcard
            val flashcards = selectedWords.map { word ->
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    word = word.word.replaceFirstChar { it.uppercase() },
                    pronunciation = "",
                    partOfSpeech = word.partOfSpeech.uppercase(),
                    definition = word.definition,
                    exampleSentence = ""
                )
            }
            
            topicRepository.createTopic(newTopic)
                .onSuccess { _ ->
                    // Save flashcards to repository
                    flashcardRepository.saveFlashcardsForTopic(topicId, flashcards)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(isCreatingTopic = false)
                            onSuccess()
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(isCreatingTopic = false)
                            onError(error.message ?: "Failed to save flashcards")
                        }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isCreatingTopic = false)
                    onError(error.message ?: "Failed to create topic")
                }
        }
    }
    
    fun clearAll() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchSuggestions = emptyList(),
            topicSuggestions = emptyList(),
            selectedWords = emptySet()
        )
    }
    
    /**
     * Extracts the main keyword from topic name by removing common prefixes.
     */
    private fun extractKeyword(topicName: String): String {
        val levelPrefixPattern = Regex("^[A-C][1-2]\\s+", RegexOption.IGNORE_CASE)
        val withoutPrefix = topicName.replace(levelPrefixPattern, "")
        return withoutPrefix.ifBlank { topicName }
    }
}

data class AddWordUiState(
    // New topic creation
    val newTopicName: String = "",
    val newTopicDescription: String = "",
    val isCreatingTopic: Boolean = false,
    
    // Topic selection for word suggestions
    val availableTopics: List<Topic> = emptyList(),
    val selectedTopic: Topic? = null,
    val showTopicDropdown: Boolean = false,
    
    // Search mode
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<WordSuggestion> = emptyList(),
    val isLoadingDetails: Boolean = false,
    
    // Topic-based word suggestions
    val topicQuery: String = "",
    val isLoadingTopicWords: Boolean = false,
    val topicSuggestions: List<VocabularyWord> = emptyList(),
    
    // Selected words for the new topic
    val selectedWords: Set<VocabularyWord> = emptySet()
)
