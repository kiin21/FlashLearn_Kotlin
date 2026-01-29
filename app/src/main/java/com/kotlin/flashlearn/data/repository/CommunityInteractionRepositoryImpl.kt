package com.kotlin.flashlearn.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.domain.repository.CommunityInteractionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firestore implementation of CommunityInteractionRepository.
 *
 * Firestore structure:
 * - users/{userId}/savedCommunityTopics/{topicId} -> { addedAt: timestamp }  (Private save/bookmark)
 * - users/{userId}/upvotedTopics/{topicId} -> { addedAt: timestamp }   (Public vote)
 * - topics/{topicId} -> { upvoteCount: number }
 *
 * Two separate concepts:
 * - Bookmark: Personal save, does NOT affect upvoteCount (previously Favorite in Community)
 * - Upvote: Public vote, DOES affect upvoteCount
 */
@Singleton
class CommunityInteractionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommunityInteractionRepository {

    private val usersCollection = firestore.collection("users")
    private val topicsCollection = firestore.collection("topics")

    // ==================== BOOKMARK (Private Save) ====================

    override suspend fun toggleBookmark(userId: String, topicId: String): Result<Boolean> {
        return runCatching {
            var newStatus = false

            val bookmarkRef = usersCollection
                .document(userId)
                .collection("savedCommunityTopics")
                .document(topicId)

            // Check current status
            val bookmarkDoc = bookmarkRef.get(Source.DEFAULT).await()

            if (bookmarkDoc.exists()) {
                // Currently saved -> remove
                bookmarkRef.delete().await()
                newStatus = false
            } else {
                // Not saved -> add
                bookmarkRef.set(
                    mapOf(
                        "addedAt" to System.currentTimeMillis()
                    )
                ).await()
                newStatus = true
            }

            newStatus
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun getBookmarkedTopicIdsOnce(userId: String): Result<List<String>> {
        return runCatching {
            val snapshot = usersCollection
                .document(userId)
                .collection("savedCommunityTopics")
                .get(Source.DEFAULT)
                .await()

            snapshot.documents.map { it.id }
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override fun getBookmarkedTopicIds(userId: String): Flow<List<String>> = callbackFlow {
        var registration: ListenerRegistration? = null

        try {
            registration = usersCollection
                .document(userId)
                .collection("savedCommunityTopics")
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

                // If topic doesn't exist, we can't upvote it
                if (!topicDoc.exists()) {
                    // Transaction will fail automatically if we don't handle this but let's be safe.
                    // Can throw exception to abort.
                    throw IllegalStateException("Topic not found")
                }

                val currentCount = topicDoc.getLong("upvoteCount") ?: 0

                // ===== ALL WRITES AFTER =====
                if (upvoteDoc.exists()) {
                    // Currently upvoted -> remove upvote
                    transaction.delete(upvoteRef)

                    // Only decrement if > 0
                    if (currentCount > 0) {
                        transaction.update(topicRef, "upvoteCount", FieldValue.increment(-1))
                    }
                    newStatus = false
                } else {
                    // Not upvoted -> add upvote
                    transaction.set(
                        upvoteRef, mapOf(
                            "addedAt" to System.currentTimeMillis()
                        )
                    )
                    // Increment
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
