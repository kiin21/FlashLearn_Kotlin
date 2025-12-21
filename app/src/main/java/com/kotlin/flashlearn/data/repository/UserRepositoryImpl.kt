package com.kotlin.flashlearn.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kotlin.flashlearn.domain.model.User
import com.kotlin.flashlearn.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun isNewUser(userId: String): Boolean {
        val document = firestore.collection("users").document(userId).get().await()
        return !document.exists()
    }

    override suspend fun createUser(user: User) {
        firestore.collection("users").document(user.userId).set(user).await()
    }

    override suspend fun getUser(userId: String): User? {
        val document = firestore.collection("users").document(userId).get().await()
        return document.toObject(User::class.java)
    }
}
