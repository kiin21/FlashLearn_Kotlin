package com.kotlin.flashlearn.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.entity.FlashcardEntity
import com.kotlin.flashlearn.data.local.entity.TopicEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val topicDao: TopicDao,
    private val flashcardDao: FlashcardDao,
) : SyncRepository {
    private val topicsCollection = firestore.collection("topics")

    override suspend fun syncAll(userId: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        runCatching {
            // 1) Fetch topics (system + user)
            val topics = fetchEligibleTopics(userId)

            // 2) Upsert topics to Room FIRST (important for FK)
            topicDao.insertTopics(topics)

            // 3) Fetch flashcards for those topics, then upsert to Room
            val flashcards = fetchAllFlashcardsForTopics(topics.map { it.id })
            if (flashcards.isNotEmpty()) {
                flashcardDao.insertFlashcards(flashcards)
            }
        }.onFailure {
            throw it
        }
    }

    private suspend fun fetchEligibleTopics(userId: String): List<TopicEntity> {
        val snapshot = topicsCollection.get(Source.DEFAULT).await()

        val all = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val description = doc.getString("description") ?: ""
            val iconType = doc.getString("iconType") ?: "book"
            val isSystemTopic = doc.getBoolean("isSystemTopic") ?: false
            val isPublic = doc.getBoolean("isPublic") ?: true
            val createdBy = doc.getString("createdBy")
            val wordCount = (doc.getLong("wordCount") ?: 0L).toInt()
            val imageUrl = doc.getString("imageUrl")
            val upvoteCount = (doc.getLong("upvoteCount") ?: 0L).toInt()
            val downloadCount = (doc.getLong("downloadCount") ?: 0L).toInt()
            val creatorName = doc.getString("creatorName") ?: ""
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

            @Suppress("UNCHECKED_CAST")
            val wordLevels = (doc.get("wordLevels") as? List<*>)?.mapNotNull { it as? String }
                ?: emptyList()

            TopicEntity(
                id = id,
                name = name,
                description = description,
                iconType = iconType,
                isSystemTopic = isSystemTopic,
                isPublic = isPublic,
                createdBy = createdBy,
                wordCount = wordCount,
                imageUrl = imageUrl,

                upvoteCount = upvoteCount,
                downloadCount = downloadCount,
                creatorName = creatorName,
                createdAt = createdAt,
                wordLevels = wordLevels
            )
        }

        return all.filter { it.isSystemTopic || it.createdBy == userId }
    }

    private suspend fun fetchAllFlashcardsForTopics(topicIds: List<String>): List<FlashcardEntity> =
        coroutineScope {
            topicIds.map { topicId ->
                async {
                    val snapshot = topicsCollection
                        .document(topicId)
                        .collection("flashcards")
                        .get(Source.DEFAULT)
                        .await()

                    snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val word = doc.getString("word") ?: return@mapNotNull null
                        val pronunciation = doc.getString("pronunciation") ?: ""
                        val partOfSpeech = doc.getString("partOfSpeech") ?: ""
                        val definition = doc.getString("definition") ?: ""
                        val exampleSentence = doc.getString("exampleSentence") ?: ""
                        val ipa = doc.getString("ipa") ?: ""
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val pronunciationUrl = doc.getString("pronunciationUrl") ?: ""
                        val level = doc.getString("level") ?: ""
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        @Suppress("UNCHECKED_CAST")
                        val synonyms =
                            (doc.get("synonyms") as? List<*>)?.mapNotNull { it as? String }
                                ?: emptyList()

                        FlashcardEntity(
                            id = id,
                            topicId = topicId,
                            word = word,
                            pronunciation = pronunciation,
                            partOfSpeech = partOfSpeech,
                            definition = definition,
                            exampleSentence = exampleSentence,
                            ipa = ipa,
                            imageUrl = imageUrl,
                            pronunciationUrl = pronunciationUrl,
                            synonyms = synonyms,
                            level = level,
                            createdAt = createdAt
                        )
                    }
                }
            }.awaitAll().flatten()
        }
}
