package com.kotlin.flashlearn.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.remote.PixabayApi
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.VSTEPLevel
import com.kotlin.flashlearn.domain.repository.TopicRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val pixabayApi: PixabayApi
) : TopicRepository {

    private val topicsCollection = firestore.collection("topics")

    override suspend fun getPublicTopics(): Result<List<Topic>> {
        return runCatching {
            // Fetch from server first for Community - needs fresh upvote counts
            val systemTopics = topicsCollection
                .whereEqualTo("isSystemTopic", true)
                .get(Source.DEFAULT)
                .await()
                .documents.mapNotNull { it.toTopic() }

            val publicTopics = topicsCollection
                .whereEqualTo("isPublic", true)
                .get(Source.DEFAULT)
                .await()
                .documents.mapNotNull { it.toTopic() }

            combineAndSort(systemTopics + publicTopics)
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun getUserTopics(userId: String): Result<List<Topic>> {
        return runCatching {
            topicsCollection
                .whereEqualTo("createdBy", userId)
                .orderBy("name", Query.Direction.ASCENDING)
                .getWithCacheFirst()
                .documents.mapNotNull { it.toTopic() }
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun getVisibleTopics(userId: String?): Result<List<Topic>> {
        return runCatching {
            // System topics - built-in learning content
            val systemTopics = topicsCollection
                .whereEqualTo("isSystemTopic", true)
                .getWithCacheFirst()
                .documents.mapNotNull { it.toTopic() }

            // User's own topics (both public and private)
            val userTopics = if (!userId.isNullOrBlank()) {
                topicsCollection
                    .whereEqualTo("createdBy", userId)
                    .getWithCacheFirst()
                    .documents.mapNotNull { it.toTopic() }
            } else {
                emptyList()
            }

            // Note: Public topics from OTHER users are shown in Community, not here
            combineAndSort(systemTopics + userTopics)
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun getTopicById(topicId: String): Result<Topic?> {
        return runCatching {
            getDocWithCacheFirst(topicId).toTopic()
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun createTopic(topic: Topic): Result<Topic> {
        return runCatching {
            val imageUrl = fetchImageIfNeeded(topic)
            val id = topic.id.ifBlank { UUID.randomUUID().toString() }
            val newTopic = topic.copy(id = id, imageUrl = imageUrl)

            topicsCollection.document(id)
                .set(newTopic.toMap())
                .await()

            newTopic
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun updateTopic(topic: Topic): Result<Topic> {
        return runCatching {
            topicsCollection.document(topic.id)
                .set(topic.toMap())
                .await()
            topic
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun searchTopics(query: String, userId: String?): Result<List<Topic>> {
        return runCatching {
            val allTopics = getVisibleTopics(userId).getOrThrow()
            allTopics.filter { it.name.contains(query, ignoreCase = true) }
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun deleteTopic(topicId: String): Result<Unit> {
        return runCatching {
            val flashcardsRef = topicsCollection.document(topicId).collection("flashcards")
            val flashcards = flashcardsRef.get().await()
            
            flashcards.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            topicsCollection.document(topicId).delete().await()
            Unit
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun regenerateTopicImage(topicId: String): Result<String> {
        return runCatching {
            val topic = getTopicById(topicId).getOrThrow() 
                ?: throw Exception("Topic not found")

            val response = pixabayApi.searchImages(BuildConfig.PIXABAY_API_KEY, topic.name)
            val hits = response.hits.take(10)
            
            if (hits.isEmpty()) {
                return Result.success("")
            }

            val newImageUrl = hits.random().webformatUrl
            val updatedTopic = topic.copy(imageUrl = newImageUrl)
            updateTopic(updatedTopic)

            newImageUrl
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun cloneTopicToUser(
        originalTopicId: String,
        targetUserId: String,
        targetUserName: String
    ): Result<Topic> {
        return runCatching {
            // 1. Fetch original topic
            val originalTopic = getTopicById(originalTopicId).getOrNull()
                ?: throw Exception("Topic not found")
            
            // 2. Fetch original flashcards
            val flashcardsSnapshot = topicsCollection
                .document(originalTopicId)
                .collection("flashcards")
                .get(Source.DEFAULT)
                .await()
            
            // 3. Create new topic with new ID
            val newTopicId = UUID.randomUUID().toString()
            val clonedTopic = originalTopic.copy(
                id = newTopicId,
                createdBy = targetUserId,
                creatorName = targetUserName,
                isPublic = false,  // Private by default
                isSystemTopic = false,
                upvoteCount = 0,
                downloadCount = 0,
                createdAt = System.currentTimeMillis(),
                clonedFrom = originalTopicId,
                originalCreator = originalTopic.creatorName.ifEmpty { "Unknown" }
            )
            
            // 4. Save cloned topic
            topicsCollection.document(newTopicId).set(clonedTopic.toMap()).await()
            
            // 5. Clone flashcards
            val newFlashcardsRef = topicsCollection.document(newTopicId).collection("flashcards")
            flashcardsSnapshot.documents.forEach { doc ->
                val newFlashcardId = UUID.randomUUID().toString()
                val flashcardData = doc.data?.toMutableMap() ?: mutableMapOf()
                flashcardData["id"] = newFlashcardId
                newFlashcardsRef.document(newFlashcardId).set(flashcardData).await()
            }
            
            // 6. Increment downloadCount on original topic
            topicsCollection.document(originalTopicId).update(
                "downloadCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            
            clonedTopic
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun getTopRecommendedTopics(limit: Int): Result<List<Topic>> {
        return runCatching {
            topicsCollection
                .whereEqualTo("isPublic", true)
                .orderBy("upvoteCount", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .getWithCacheFirst()
                .documents.mapNotNull { it.toTopic() }
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    private suspend fun Query.getWithCacheFirst(): QuerySnapshot {
        val cached = runCatching { 
            get(Source.CACHE).await() 
        }.getOrNull()

        return if (cached?.documents?.isNotEmpty() == true) {
            cached
        } else {
            get(Source.DEFAULT).await()
        }
    }

    private suspend fun getDocWithCacheFirst(topicId: String): DocumentSnapshot {
        val cached = runCatching {
            topicsCollection.document(topicId).get(Source.CACHE).await()
        }.getOrNull()

        return if (cached?.exists() == true) {
            cached
        } else {
            topicsCollection.document(topicId).get(Source.DEFAULT).await()
        }
    }

    private suspend fun fetchImageIfNeeded(topic: Topic): String? {
        if (!topic.imageUrl.isNullOrBlank()) {
            return topic.imageUrl
        }

        return runCatching {
            val response = pixabayApi.searchImages(BuildConfig.PIXABAY_API_KEY, topic.name)
            response.hits.firstOrNull()?.webformatUrl
        }.getOrNull()
    }

    private fun combineAndSort(topics: List<Topic>): List<Topic> {
        return topics
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Topic> { it.isSystemTopic }.thenBy { it.name })
    }

    private fun DocumentSnapshot.toTopic(): Topic? {
        if (!exists()) return null

        return runCatching {
            Topic(
                id = id,
                name = getString("name") ?: return null,
                description = getString("description") ?: "",
                iconType = getString("iconType") ?: "book",
                isSystemTopic = getBoolean("isSystemTopic") ?: false,
                isPublic = getBoolean("isPublic") ?: true,
                createdBy = getString("createdBy"),
                imageUrl = getString("imageUrl"),
                // Community fields

                upvoteCount = getLong("upvoteCount")?.toInt() ?: 0,
                downloadCount = getLong("downloadCount")?.toInt() ?: 0,
                creatorName = getString("creatorName") ?: "",
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                // Clone attribution
                clonedFrom = getString("clonedFrom"),
                originalCreator = getString("originalCreator"),
                // Word-level data
                wordLevels = (get("wordLevels") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }.getOrNull()
    }

    private fun Topic.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "iconType" to iconType,
            "isSystemTopic" to isSystemTopic,
            "isPublic" to isPublic,
            "createdBy" to createdBy,
            "imageUrl" to imageUrl,
            // Community fields

            "upvoteCount" to upvoteCount,
            "downloadCount" to downloadCount,
            "creatorName" to creatorName,
            "createdAt" to createdAt,
            // Clone attribution
            "clonedFrom" to clonedFrom,
            "originalCreator" to originalCreator,
            // Word-level data
            "wordLevels" to wordLevels
        )
    }
}
