package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import com.kotlin.flashlearn.domain.repository.DictionaryRepository
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
    private val dictionaryRepository: DictionaryRepository,
    private val topicRepository: TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.getSignedInUser()?.userId ?: firebaseAuth.currentUser?.uid

    private val existingTopicId: String? = savedStateHandle.get<String>("topicId")?.takeIf { it != "new" }

    private val _uiState = MutableStateFlow(AddWordUiState(
        isEditMode = !existingTopicId.isNullOrBlank(),
        currentStep = if (!existingTopicId.isNullOrBlank()) 1 else 0
    ))
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    // Step management
    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 1) { // Now only 2 steps: 0 (Info) and 1 (Manual)
            _uiState.value = _uiState.value.copy(currentStep = current + 1)
        }
    }

    fun prevStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentStep = current - 1)
        }
    }

    fun setStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step.coerceIn(0, 1))
    }

    // New flow for search query input
    private val _manualWordFlow = MutableStateFlow("")

    // Local cache for search suggestions
    private val searchCache = mutableMapOf<String, List<WordSuggestion>>()

    init {
        loadTopics()
        setupManualWordFlow()
        setupAutocompleteSuggestionsFlow()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupManualWordFlow() {
        viewModelScope.launch {
            _manualWordFlow
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { word ->
                    fetchWordDetails(word)
                }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupAutocompleteSuggestionsFlow() {
        viewModelScope.launch {
            _manualWordFlow
                .debounce(250) // Faster for suggestions
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { query ->
                    fetchAutocompleteSuggestions(query)
                }
        }
    }

    private fun fetchAutocompleteSuggestions(query: String) {
        viewModelScope.launch {
            // Check cache first
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
                        manualDefinition = result.definition,
                        manualPartOfSpeech = result.partOfSpeech.lowercase().trim(),
                        manualIpa = result.ipa,
                        manualExample = result.example,
                        isFetchingDetails = false
                    )
                    
                    // If POS is still missing, try to fill it from Datamuse
                    if (result.partOfSpeech.isBlank()) {
                        fillMissingPosFromDatamuse(word)
                    }
                }
                .onFailure {
                    // Fallback to Datamuse if dictionary fails
                    datamuseRepository.searchWords(word)
                        .onSuccess { results ->
                            val match = results.firstOrNull { it.word.equals(word, ignoreCase = true) }
                                ?: results.firstOrNull()
                            
                            if (match != null) {
                                _uiState.value = _uiState.value.copy(
                                    manualDefinition = match.definition,
                                    manualPartOfSpeech = match.partOfSpeech.lowercase().trim(),
                                    manualIpa = match.ipa.ifBlank { "Not found" },
                                    isFetchingDetails = false
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    manualIpa = "Not found",
                                    isFetchingDetails = false
                                )
                            }
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                manualIpa = "Not found",
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
                        manualPartOfSpeech = match.partOfSpeech.lowercase().trim()
                    )
                }
            }
        }
    }
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
        val isEditMode = _uiState.value.isEditMode

        if (!isEditMode && topicName.isBlank()) {
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

            // Determine Topic ID
            val topicId = if (isEditMode && !existingTopicId.isNullOrBlank()) {
                existingTopicId
            } else {
                UUID.randomUUID().toString()
            }

            // Create Topic Object only if NOT in edit mode
            if (!isEditMode) {
                // Get creator name from AuthRepository
                val signedInUser = authRepository.getSignedInUser()
                val creatorName = signedInUser?.username
                    ?: firebaseAuth.currentUser?.displayName
                    ?: firebaseAuth.currentUser?.email?.substringBefore("@")
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

                topicRepository.createTopic(newTopic)
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(isCreatingTopic = false)
                        onError(error.message ?: "Failed to create topic")
                        return@launch
                    }
                    // If success, continue to save flashcards
            }

            // Convert VocabularyWord to Flashcard
            val flashcards = selectedWords.map { word ->
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    word = word.word.replaceFirstChar { it.uppercase() },
                    pronunciation = word.ipa,
                    partOfSpeech = word.partOfSpeech.uppercase(),
                    definition = word.definition,
                    exampleSentence = word.example,
                    imageUrl = word.imageUrl ?: ""
                )
            }

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
    }

    fun clearAll() {
        _uiState.value = _uiState.value.copy(
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

    // Manual Entry Functions
    fun onManualWordChange(value: String) { 
        _uiState.value = _uiState.value.copy(manualWord = value)
        _manualWordFlow.value = value
    }
    fun onManualDefinitionChange(value: String) { _uiState.value = _uiState.value.copy(manualDefinition = value) }
    fun onManualExampleChange(value: String) { _uiState.value = _uiState.value.copy(manualExample = value) }
    fun onManualIpaChange(value: String) { _uiState.value = _uiState.value.copy(manualIpa = value) }
    fun onManualPartOfSpeechChange(value: String) { _uiState.value = _uiState.value.copy(manualPartOfSpeech = value) }
    fun onManualImageUriChange(value: String?) { _uiState.value = _uiState.value.copy(manualImageUri = value) }

    fun addManualCard() {
        val state = _uiState.value
        if (state.manualWord.isBlank() || state.manualDefinition.isBlank()) return

        val newWord = VocabularyWord(
            word = state.manualWord.trim(),
            definition = state.manualDefinition.trim(),
            score = 0,
            tags = listOf("manual"),
            partOfSpeech = state.manualPartOfSpeech.trim(),
            ipa = state.manualIpa.trim(),
            example = state.manualExample.trim(),
            imageUrl = state.manualImageUri
        )

        val currentSelected = state.selectedWords.toMutableSet()
        currentSelected.add(newWord)

        _uiState.value = _uiState.value.copy(
            selectedWords = currentSelected,
            manualWord = "",
            manualDefinition = "",
            manualExample = "",
            manualIpa = "",
            manualPartOfSpeech = "",
            manualImageUri = null,
            // Keep expanded to allow adding more
            isManualEntryExpanded = true
        )
    }

    fun toggleManualEntryExpanded() {
        _uiState.value = _uiState.value.copy(
            isManualEntryExpanded = !_uiState.value.isManualEntryExpanded
        )
    }
}

data class AddWordUiState(
    // New topic creation
    val newTopicName: String = "",
    val newTopicDescription: String = "",
    val isCreatingTopic: Boolean = false,
    val isEditMode: Boolean = false,

    // Topic selection for word suggestions
    val availableTopics: List<Topic> = emptyList(),
    val selectedTopic: Topic? = null,
    val showTopicDropdown: Boolean = false,

    // Search mode - Deprecated in favor of auto-fetch in manual entry
    val isSearching: Boolean = false,
    val searchSuggestions: List<WordSuggestion> = emptyList(),
    val isFetchingDetails: Boolean = false,

    // Topic-based word suggestions
    val topicQuery: String = "",
    val isLoadingTopicWords: Boolean = false,
    val topicSuggestions: List<VocabularyWord> = emptyList(),

    // Selected words for the new topic
    val selectedWords: Set<VocabularyWord> = emptySet(),

    // Manual Entry State
    val manualWord: String = "",
    val manualDefinition: String = "",
    val manualExample: String = "",
    val manualIpa: String = "",
    val manualPartOfSpeech: String = "",
    val manualImageUri: String? = null,
    val isManualEntryExpanded: Boolean = true, // Default to expanded/visible
    val wordSuggestions: List<WordSuggestion> = emptyList(), // Autocomplete suggestions

    // Wizard Step State
    val currentStep: Int = 0 // 0: Info, 1: Manual Entry
)
