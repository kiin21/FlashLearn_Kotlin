package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AddWordScreen - handles word search and topic-based suggestions.
 */
@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val datamuseRepository: DatamuseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    val topicId: String? = savedStateHandle.get<String>("topicId")
    
    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    
    /**
     * Search for words as user types (autocomplete).
     */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Wait 300ms after user stops typing
            
            if (query.length >= 2) {
                _uiState.value = _uiState.value.copy(isSearching = true)
                
                datamuseRepository.getAutocompleteSuggestions(query)
                    .onSuccess { suggestions ->
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
            } else {
                _uiState.value = _uiState.value.copy(searchSuggestions = emptyList())
            }
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
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        selectedWord = selectedWord,
                        searchSuggestions = emptyList(),
                        searchQuery = word
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingDetails = false)
                }
        }
    }
    
    /**
     * Get words related to a topic.
     */
    fun onTopicQueryChange(topic: String) {
        _uiState.value = _uiState.value.copy(topicQuery = topic)
    }
    
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
    
    fun clearSelectedWord() {
        _uiState.value = _uiState.value.copy(selectedWord = null)
    }
    
    fun clearAll() {
        _uiState.value = AddWordUiState()
    }
}

data class AddWordUiState(
    // Search mode
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchSuggestions: List<WordSuggestion> = emptyList(),
    val isLoadingDetails: Boolean = false,
    val selectedWord: VocabularyWord? = null,
    
    // Topic mode
    val topicQuery: String = "",
    val isLoadingTopicWords: Boolean = false,
    val topicSuggestions: List<VocabularyWord> = emptyList(),
    val selectedWords: Set<VocabularyWord> = emptySet()
)
