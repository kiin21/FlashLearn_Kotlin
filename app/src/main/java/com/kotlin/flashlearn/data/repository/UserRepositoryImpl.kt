package com.kotlin.flashlearn.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.domain.model.User
import com.kotlin.flashlearn.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: com.kotlin.flashlearn.data.remote.CloudinaryService
) : UserRepository {

    companion object {
        private const val COLLECTION_LINKED_EMAILS = "linked_emails"
    }

    override suspend fun isNewUser(userId: String): Boolean {
        val document = usersCollection.document(userId)
            .get(Source.SERVER)
            .await()
        return !document.exists()
    }

    override suspend fun createUser(user: User) {
        // Create user doc
        usersCollection.document(user.userId)
            .set(user)
            .await()
            
        // Initial email to subcollection if exists
        user.email?.let { email ->
             addEmailToSubcollection(user.userId, email)
        }
    }

    override suspend fun getUser(userId: String): User? {
        val cachedDoc = runCatching {
            usersCollection.document(userId)
                .get(Source.CACHE)
                .await()
        }.getOrNull()

        if (cachedDoc?.exists() == true) {
            return cachedDoc.toObject(User::class.java)
        }

        val serverDoc = usersCollection.document(userId)
            .get(Source.DEFAULT)
            .await()
            
        return serverDoc.toObject(User::class.java)
    }

    override suspend fun getUserByLoginUsername(loginUsername: String): User? {
        return usersCollection
            .whereEqualTo("loginUsername", loginUsername)
            .get()
            .await()
            .documents.firstOrNull()
            ?.toObject(User::class.java)
    }

    override suspend fun isLoginUsernameTaken(loginUsername: String): Boolean {
        return getUserByLoginUsername(loginUsername) != null
    }

    override suspend fun getUserByEmail(email: String): User? {
        // Strategy: 
        // 1. Try finding directly in users (Root email) - Optimization
        val rootMatch = usersCollection
            .whereEqualTo("email", email)
            .get()
            .await()
            .documents.firstOrNull()
            ?.toObject(User::class.java)
            
        if (rootMatch != null) return rootMatch

        // 2. Collection Group Query on linked_emails subcollection
        val subCollectionMatch = firestore.collectionGroup(COLLECTION_LINKED_EMAILS)
            .whereEqualTo("email", email)
            .get()
            .await()
            .documents.firstOrNull()
            
        return subCollectionMatch?.reference?.parent?.parent?.let { userRef ->
             userRef.get().await().toObject(User::class.java)
        }
    }

    override suspend fun getUserByGoogleId(googleId: String): User? {
        return usersCollection
            .whereArrayContains("googleIds", googleId)
            .get(Source.SERVER)
            .await()
            .documents.firstOrNull()
            ?.toObject(User::class.java)
    }

    override suspend fun linkGoogleAccount(userId: String, googleId: String, email: String) {
        val linkedAccount = com.kotlin.flashlearn.domain.model.LinkedAccount(
            accountId = googleId,
            email = email
        )
        
        usersCollection.document(userId).update(
            mapOf(
                "googleIds" to com.google.firebase.firestore.FieldValue.arrayUnion(googleId),
                "linkedGoogleAccounts" to com.google.firebase.firestore.FieldValue.arrayUnion(linkedAccount),
                "email" to email // Update main email if needed
            )
        ).await()
        
        // Add to subcollection
        addEmailToSubcollection(userId, email)
    }

    override suspend fun unlinkGoogleAccount(userId: String, googleId: String) {
        // We need to remove the specific LinkedAccount object.
        val user = getUser(userId) ?: return
        val accountToRemove = user.linkedGoogleAccounts.find { it.accountId == googleId }
        val emailToRemove = accountToRemove?.email
        
        if (accountToRemove != null) {
            usersCollection.document(userId).update(
                mapOf(
                    "googleIds" to com.google.firebase.firestore.FieldValue.arrayRemove(googleId),
                    "linkedGoogleAccounts" to com.google.firebase.firestore.FieldValue.arrayRemove(accountToRemove)
                )
            ).await()
            
            // cleanup from subcollection
            if (emailToRemove != null) {
                 removeEmailFromSubcollection(userId, emailToRemove)
            }
        }
    }

    override suspend fun updateEmail(userId: String, email: String) {
        usersCollection.document(userId).update("email", email).await()
        // Improve: Add new email to subcollection? 
        // Logic: Should we keep history or just current? 
        // Rule: Subcollection represents ALL currently valid emails for this user.
        // If main email updates, we add it. 
        // Ideally we should remove old main email from subcollection if it's no longer linked, 
        // but distinguishing "main email" vs "google email" in subcollection is tricky unless we store type.
        // For simplicity: Add new email.
        addEmailToSubcollection(userId, email)
    }
    
    // Helpers
    private suspend fun addEmailToSubcollection(userId: String, email: String) {
        val data = mapOf("email" to email, "uid" to userId)
        // Set document ID as email (sanitize?) or auto-id?
        // Auto-id allows duplicates if needed, but cleaner if unique per user-email pair.
        // Use a deterministic ID or query to check existence? 
        // Just add() for now, simpler.
        usersCollection.document(userId)
            .collection(COLLECTION_LINKED_EMAILS)
            .add(data)
            .await()
    }

    private suspend fun removeEmailFromSubcollection(userId: String, email: String) {
         val query = usersCollection.document(userId)
            .collection(COLLECTION_LINKED_EMAILS)
            .whereEqualTo("email", email)
            .get()
            .await()
            
         for (doc in query.documents) {
             doc.reference.delete().await()
         }
    }

    override suspend fun uploadProfilePicture(userId: String, uriString: String): String {
        val uri = android.net.Uri.parse(uriString)
        val downloadUrl = cloudinaryService.uploadProfileImage(uri, userId)
        
        usersCollection.document(userId).update("photoUrl", downloadUrl).await()
        
        return downloadUrl
    }

    override suspend fun deleteUser(userId: String) {
        runCatching {
            val savedTopicsRef = usersCollection.document(userId).collection("savedCommunityTopics")
            val savedTopics = savedTopicsRef.get().await()
            for (doc in savedTopics.documents) {
                runCatching { doc.reference.delete().await() }
            }
        }
        
        runCatching {
            val upvotedTopicsRef = usersCollection.document(userId).collection("upvotedTopics")
            val upvotedTopics = upvotedTopicsRef.get().await()
            for (doc in upvotedTopics.documents) {
                runCatching { doc.reference.delete().await() }
            }
        }
        
        // Delete user document
        usersCollection.document(userId).delete().await()
    }

    override suspend fun updatePasswordHash(userId: String, newPasswordHash: String) {
        usersCollection.document(userId).update("loginPasswordHash", newPasswordHash).await()
    }
    
    override suspend fun addFirebaseUid(userId: String, firebaseUid: String) {
        usersCollection.document(userId).update(
            "firebaseUids", com.google.firebase.firestore.FieldValue.arrayUnion(firebaseUid)
        ).await()
    }
    
    override suspend fun toggleTopicLike(userId: String, topicId: String, isLiked: Boolean) {
        val updateOp = if (isLiked) {
            com.google.firebase.firestore.FieldValue.arrayUnion(topicId)
        } else {
            com.google.firebase.firestore.FieldValue.arrayRemove(topicId)
        }
        
        usersCollection.document(userId).update("likedTopicIds", updateOp).await()
    }
}
