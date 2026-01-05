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
 * - users/{userId}/favoriteTopics/{topicId} -> { addedAt: timestamp }  (Private save)
 * - users/{userId}/upvotedTopics/{topicId} -> { addedAt: timestamp }   (Public vote)
 * - topics/{topicId} -> { upvoteCount: number }
 * 
 * Two separate concepts:
 * - Favorite: Personal save, does NOT affect upvoteCount
 * - Upvote: Public vote, DOES affect upvoteCount
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FavoriteRepository {

    private val usersCollection = firestore.collection("users")
    private val topicsCollection = firestore.collection("topics")

    // ==================== FAVORITE (Private Save) ====================

    override suspend fun toggleFavorite(userId: String, topicId: String): Result<Boolean> {
        return runCatching {
            var newStatus = false
            
            val favoriteRef = usersCollection
                .document(userId)
                .collection("favoriteTopics")
                .document(topicId)
            
            // Check current status
            val favoriteDoc = favoriteRef.get(Source.DEFAULT).await()
            
            if (favoriteDoc.exists()) {
                // Currently saved -> remove
                favoriteRef.delete().await()
                newStatus = false
            } else {
                // Not saved -> add
                favoriteRef.set(mapOf(
                    "addedAt" to System.currentTimeMillis()
                )).await()
                newStatus = true
            }
            
            newStatus
        }.onFailure {
            if (it is CancellationException) throw it
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

    // ==================== UPVOTE (Public Voting) ====================

    override suspend fun toggleUpvote(userId: String, topicId: String): Result<Boolean> {
        return runCatching {
            var newStatus = false
            
            firestore.runTransaction { transaction ->
                val upvoteRef = usersCollection
                    .document(userId)
                    .collection("upvotedTopics")
                    .document(topicId)
                
                val topicRef = topicsCollection.document(topicId)
                
                // ===== ALL READS FIRST =====
                val upvoteDoc = transaction.get(upvoteRef)
                val topicDoc = transaction.get(topicRef)
                val currentCount = topicDoc.getLong("upvoteCount") ?: 0
                
                // ===== ALL WRITES AFTER =====
                if (upvoteDoc.exists()) {
                    // Currently upvoted -> remove upvote
                    transaction.delete(upvoteRef)
                    
                    if (currentCount > 0) {
                        transaction.update(topicRef, "upvoteCount", FieldValue.increment(-1))
                    }
                    newStatus = false
                } else {
                    // Not upvoted -> add upvote
                    transaction.set(upvoteRef, mapOf(
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

    override suspend fun getUpvotedTopicIdsOnce(userId: String): Result<List<String>> {
        return runCatching {
            val snapshot = usersCollection
                .document(userId)
                .collection("upvotedTopics")
                .get(Source.DEFAULT)
                .await()
            
            snapshot.documents.map { it.id }
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override fun getUpvotedTopicIds(userId: String): Flow<List<String>> = callbackFlow {
        var registration: ListenerRegistration? = null
        
        try {
            registration = usersCollection
                .document(userId)
                .collection("upvotedTopics")
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
}
