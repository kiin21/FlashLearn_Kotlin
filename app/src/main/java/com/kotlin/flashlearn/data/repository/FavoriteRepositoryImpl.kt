package com.kotlin.flashlearn.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.domain.repository.FavoriteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firestore implementation of FavoriteRepository.
 * 
 * Firestore structure:
 * - users/{userId}/favoriteTopics/{topicId} -> { addedAt: timestamp }
 * - topics/{topicId} -> { upvoteCount: number }
 * 
 * Uses transactions to ensure atomic updates of upvoteCount.
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FavoriteRepository {

    private val usersCollection = firestore.collection("users")
    private val topicsCollection = firestore.collection("topics")

    override suspend fun addFavorite(userId: String, topicId: String): Result<Unit> {
        return runCatching {
            firestore.runTransaction { transaction ->
                // Add to user's favorites
                val favoriteRef = usersCollection
                    .document(userId)
                    .collection("favoriteTopics")
                    .document(topicId)
                
                val topicRef = topicsCollection.document(topicId)
                
                // Check if already favorited
                val favoriteDoc = transaction.get(favoriteRef)
                if (favoriteDoc.exists()) {
                    // Already favorited, do nothing
                    return@runTransaction
                }
                
                // Add favorite
                transaction.set(favoriteRef, mapOf(
                    "addedAt" to System.currentTimeMillis()
                ))
                
                // Increment upvote count on topic
                transaction.update(topicRef, "upvoteCount", FieldValue.increment(1))
            }.await()
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun removeFavorite(userId: String, topicId: String): Result<Unit> {
        return runCatching {
            firestore.runTransaction { transaction ->
                val favoriteRef = usersCollection
                    .document(userId)
                    .collection("favoriteTopics")
                    .document(topicId)
                
                val topicRef = topicsCollection.document(topicId)
                
                // ===== ALL READS FIRST =====
                val favoriteDoc = transaction.get(favoriteRef)
                val topicDoc = transaction.get(topicRef)
                
                // Check if favorited
                if (!favoriteDoc.exists()) {
                    // Not favorited, do nothing
                    return@runTransaction
                }
                
                // ===== ALL WRITES AFTER =====
                // Remove favorite
                transaction.delete(favoriteRef)
                
                // Decrement upvote count on topic (minimum 0)
                val currentCount = topicDoc.getLong("upvoteCount") ?: 0
                if (currentCount > 0) {
                    transaction.update(topicRef, "upvoteCount", FieldValue.increment(-1))
                }
            }.await()
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun toggleFavorite(userId: String, topicId: String): Result<Boolean> {
        return runCatching {
            var newStatus = false
            
            firestore.runTransaction { transaction ->
                val favoriteRef = usersCollection
                    .document(userId)
                    .collection("favoriteTopics")
                    .document(topicId)
                
                val topicRef = topicsCollection.document(topicId)
                
                // ===== ALL READS FIRST =====
                val favoriteDoc = transaction.get(favoriteRef)
                val topicDoc = transaction.get(topicRef)
                val currentCount = topicDoc.getLong("upvoteCount") ?: 0
                
                // ===== ALL WRITES AFTER =====
                if (favoriteDoc.exists()) {
                    // Currently favorited -> remove
                    transaction.delete(favoriteRef)
                    
                    if (currentCount > 0) {
                        transaction.update(topicRef, "upvoteCount", FieldValue.increment(-1))
                    }
                    newStatus = false
                } else {
                    // Not favorited -> add
                    transaction.set(favoriteRef, mapOf(
                        "addedAt" to System.currentTimeMillis()
                    ))
                    transaction.update(topicRef, "upvoteCount", FieldValue.increment(1))
                    newStatus = true
                }
            }.await()
            
            newStatus
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun isFavorited(userId: String, topicId: String): Result<Boolean> {
        return runCatching {
            val doc = usersCollection
                .document(userId)
                .collection("favoriteTopics")
                .document(topicId)
                .get(Source.DEFAULT)
                .await()
            
            doc.exists()
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override fun getFavoriteTopicIds(userId: String): Flow<List<String>> = callbackFlow {
        var registration: ListenerRegistration? = null
        
        try {
            registration = usersCollection
                .document(userId)
                .collection("favoriteTopics")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val ids = snapshot?.documents?.map { it.id } ?: emptyList()
                    trySend(ids)
                }
            
            awaitClose { registration?.remove() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            close(e)
        }
    }

    override suspend fun getFavoriteTopicIdsOnce(userId: String): Result<List<String>> {
        return runCatching {
            val snapshot = usersCollection
                .document(userId)
                .collection("favoriteTopics")
                .get(Source.DEFAULT)
                .await()
            
            snapshot.documents.map { it.id }
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }
}
