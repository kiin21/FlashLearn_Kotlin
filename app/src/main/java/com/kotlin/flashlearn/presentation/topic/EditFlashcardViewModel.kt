package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import com.kotlin.flashlearn.domain.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditFlashcardUiState(
    val word: String = "",
    val definition: String = "",
    val exampleSentence: String = "",
    val ipa: String = "",
    val partOfSpeech: String = "",
    val level: String = "",
    val imageUrl: String = "",
    val wordSuggestions: List<WordSuggestion> = emptyList(),
    val isFetchingDetails: Boolean = false,
    val initialFlashcard: Flashcard? = null
)

@HiltViewModel
class EditFlashcardViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val datamuseRepository: DatamuseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditFlashcardUiState())
    val uiState: StateFlow<EditFlashcardUiState> = _uiState.asStateFlow()

    private val _wordFlow = MutableStateFlow("")
    private val searchCache = mutableMapOf<String, List<WordSuggestion>>()

    init {
        setupWordFlow()
        setupAutocompleteSuggestionsFlow()
    }

    fun initialize(flashcard: Flashcard) {
        if (_uiState.value.initialFlashcard == null) {
            _uiState.value = _uiState.value.copy(
                word = flashcard.word,
                definition = flashcard.definition,
                exampleSentence = flashcard.exampleSentence,
                ipa = flashcard.ipa,
                partOfSpeech = flashcard.partOfSpeech,
                level = flashcard.level,
                imageUrl = flashcard.imageUrl,
                initialFlashcard = flashcard
            )
        }
    }

    fun onWordChange(value: String) {
        _uiState.value = _uiState.value.copy(word = value)
        _wordFlow.value = value
    }

    fun onDefinitionChange(value: String) {
        _uiState.value = _uiState.value.copy(definition = value)
    }

    fun onExampleChange(value: String) {
        _uiState.value = _uiState.value.copy(exampleSentence = value)
    }

    fun onIpaChange(value: String) {
        _uiState.value = _uiState.value.copy(ipa = value)
    }

    fun onPosChange(value: String) {
        _uiState.value = _uiState.value.copy(partOfSpeech = value)
    }

    fun onLevelChange(value: String) {
        _uiState.value = _uiState.value.copy(level = value)
    }

    fun onImageUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(imageUrl = value)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupWordFlow() {
        viewModelScope.launch {
            _wordFlow
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { word ->
                    // Only fetch if word is different from initial or manually changed
                    fetchWordDetails(word)
                }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupAutocompleteSuggestionsFlow() {
        viewModelScope.launch {
            _wordFlow
                .debounce(250)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { query ->
                    fetchAutocompleteSuggestions(query)
                }
        }
    }

    private fun fetchAutocompleteSuggestions(query: String) {
        viewModelScope.launch {
            if (searchCache.containsKey(query)) {
                _uiState.value = _uiState.value.copy(
                    wordSuggestions = searchCache[query]!!
                )
            } else {
                datamuseRepository.getAutocompleteSuggestions(query)
                    .onSuccess { suggestions ->
                        searchCache[query] = suggestions
                        _uiState.value = _uiState.value.copy(
                            wordSuggestions = suggestions
                        )
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(
                            wordSuggestions = emptyList()
                        )
                    }
            }
        }
    }

    private fun fetchWordDetails(word: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingDetails = true)
            
            dictionaryRepository.getWordExtendedDetails(word)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        definition = result.definition,
                        partOfSpeech = result.partOfSpeech.lowercase().trim(),
                        ipa = result.ipa,
                        exampleSentence = result.example,
                        isFetchingDetails = false
                    )
                    
                    if (result.partOfSpeech.isBlank()) {
                        fillMissingPosFromDatamuse(word)
                    }
                }
                .onFailure {
                    datamuseRepository.searchWords(word)
                        .onSuccess { results ->
                            val match = results.firstOrNull { it.word.equals(word, ignoreCase = true) }
                                ?: results.firstOrNull()
                            
                            if (match != null) {
                                _uiState.value = _uiState.value.copy(
                                    definition = match.definition,
                                    partOfSpeech = match.partOfSpeech.lowercase().trim(),
                                    ipa = match.ipa.ifBlank { "Not found" },
                                    isFetchingDetails = false
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isFetchingDetails = false
                                )
                            }
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                isFetchingDetails = false
                            )
                        }
                }
        }
    }

    private fun fillMissingPosFromDatamuse(word: String) {
        viewModelScope.launch {
            datamuseRepository.searchWords(word).onSuccess { results ->
                val match = results.firstOrNull { it.word.equals(word, ignoreCase = true) } ?: results.firstOrNull()
                if (match != null && match.partOfSpeech.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        partOfSpeech = match.partOfSpeech.lowercase().trim()
                    )
                }
            }
        }
    }

    fun getUpdatedFlashcard(): Flashcard? {
        val initial = _uiState.value.initialFlashcard ?: return null
        return initial.copy(
            word = _uiState.value.word,
            definition = _uiState.value.definition,
            exampleSentence = _uiState.value.exampleSentence,
            ipa = _uiState.value.ipa,
            partOfSpeech = _uiState.value.partOfSpeech,
            level = _uiState.value.level,
            imageUrl = _uiState.value.imageUrl
        )
    }
}
