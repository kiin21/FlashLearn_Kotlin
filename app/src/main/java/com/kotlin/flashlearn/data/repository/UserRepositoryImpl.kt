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
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun isNewUser(userId: String): Boolean {
        val document = usersCollection.document(userId)
            .get(Source.SERVER)
            .await()
        return !document.exists()
    }

    override suspend fun createUser(user: User) {
        usersCollection.document(user.userId)
            .set(user)
            .await()
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

    override suspend fun getUserByGoogleId(googleId: String): User? {
        return usersCollection
            .whereEqualTo("googleId", googleId)
            .get()
            .await()
            .documents.firstOrNull()
            ?.toObject(User::class.java)
    }

    override suspend fun linkGoogleAccount(userId: String, googleId: String, email: String?) {
        usersCollection.document(userId).update(
            mapOf(
                "googleId" to googleId,
                "email" to email,
                "linkedProviders" to com.google.firebase.firestore.FieldValue.arrayUnion("google.com")
            )
        ).await()
    }
}
