package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of FlashcardRepository with mock data.
 * Following Repository pattern from Clean Architecture.
 * TODO: Replace with actual Firestore implementation.
 */
@Singleton
class FlashcardRepositoryImpl @Inject constructor() : FlashcardRepository {
    
    // Mock data - simulating different topics
    private val mockFlashcards = listOf(
        // Topic 1: Environmental Science
        Flashcard(
            id = "1",
            topicId = "env_science_101",
            word = "Biodiversity",
            pronunciation = "/ˌbaɪ.əʊ.daɪˈvɜː.sə.ti/",
            partOfSpeech = "NOUN",
            definition = "The variety of plant and animal life in the world.",
            exampleSentence = "Preserving biodiversity is crucial for the ecosystem."
        ),
        Flashcard(
            id = "2",
            topicId = "env_science_101",
            word = "Ecosystem",
            pronunciation = "/ˈiː.kəʊˌsɪs.təm/",
            partOfSpeech = "NOUN",
            definition = "A biological community of interacting organisms and their physical environment.",
            exampleSentence = "The rainforest ecosystem supports millions of species."
        ),
        Flashcard(
            id = "3",
            topicId = "env_science_101",
            word = "Sustainable",
            pronunciation = "/səˈsteɪ.nə.bəl/",
            partOfSpeech = "ADJECTIVE",
            definition = "Able to be maintained at a certain rate or level without depleting natural resources.",
            exampleSentence = "We need to adopt sustainable farming practices."
        ),
        
        // Topic 2: Technology
        Flashcard(
            id = "4",
            topicId = "tech_101",
            word = "Algorithm",
            pronunciation = "/ˈæl.ɡə.rɪ.ðəm/",
            partOfSpeech = "NOUN",
            definition = "A process or set of rules to be followed in calculations or problem-solving operations.",
            exampleSentence = "The search algorithm efficiently finds the optimal solution."
        ),
        Flashcard(
            id = "5",
            topicId = "tech_101",
            word = "Interface",
            pronunciation = "/ˈɪn.tə.feɪs/",
            partOfSpeech = "NOUN",
            definition = "A point where two systems, subjects, or organizations meet and interact.",
            exampleSentence = "The user interface is intuitive and easy to navigate."
        ),
        
        // Topic 3: Business
        Flashcard(
            id = "6",
            topicId = "business_101",
            word = "Revenue",
            pronunciation = "/ˈrev.ən.juː/",
            partOfSpeech = "NOUN",
            definition = "Income generated from normal business operations.",
            exampleSentence = "The company's revenue increased by 20% this quarter."
        ),
        Flashcard(
            id = "7",
            topicId = "business_101",
            word = "Stakeholder",
            pronunciation = "/ˈsteɪkˌhəʊl.dər/",
            partOfSpeech = "NOUN",
            definition = "A person with an interest or concern in something, especially a business.",
            exampleSentence = "All stakeholders were consulted before the decision."
        )
    )
    
    override suspend fun getFlashcardsByTopicId(topicId: String): Result<List<Flashcard>> {
        return try {
            // Simulate network delay
            delay(500)
            
            val flashcards = mockFlashcards.filter { it.topicId == topicId }
            
            if (flashcards.isEmpty()) {
                // Return default flashcards if no match found (for demo purposes)
                Result.success(mockFlashcards.take(3))
            } else {
                Result.success(flashcards)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun markFlashcardAsMastered(flashcardId: String, userId: String): Result<Unit> {
        return try {
            // Simulate network delay
            delay(200)
            // TODO: Implement Firestore logic to mark card as mastered
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun markFlashcardForReview(flashcardId: String, userId: String): Result<Unit> {
        return try {
            // Simulate network delay
            delay(200)
            // TODO: Implement Firestore logic to mark card for review
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
