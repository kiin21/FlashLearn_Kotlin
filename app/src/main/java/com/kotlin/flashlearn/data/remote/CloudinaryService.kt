package com.kotlin.flashlearn.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.kotlin.flashlearn.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryService @Inject constructor() {

    companion object {
        private var isInitialized = false
        
        fun initialize(context: Context) {
            if (!isInitialized) {
                val config = mapOf(
                    "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                    "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                    "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
                )
                MediaManager.init(context, config)
                isInitialized = true
            }
        }
    }

    suspend fun uploadProfileImage(uri: Uri, userId: String): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .option("folder", "profile_images")
            .option("public_id", userId)
            .option("overwrite", true)
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val secureUrl = resultData?.get("secure_url") as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                    } else {
                        continuation.resumeWithException(Exception("Upload succeeded but no URL returned"))
                    }
                }
                
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resumeWithException(Exception(error?.description ?: "Upload failed"))
                }
                
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    // Handle reschedule if needed
                }
            })
            .dispatch()
    }

    suspend fun uploadFlashcardImage(uri: Uri, flashcardId: String): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .option("folder", "flashcard_images")
            .option("public_id", "${flashcardId}_${System.currentTimeMillis()}")
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val secureUrl = resultData?.get("secure_url") as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                    } else {
                        continuation.resumeWithException(Exception("Upload succeeded but no URL returned"))
                    }
                }
                
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resumeWithException(Exception(error?.description ?: "Upload failed"))
                }
                
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}
