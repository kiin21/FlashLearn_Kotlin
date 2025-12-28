package com.kotlin.flashlearn.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.remote.FreeDictionaryApi
import com.kotlin.flashlearn.data.remote.PixabayApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background enrichment of flashcard data (images, IPA).
 * Runs in background to avoid blocking UI and survives app kill.
 */
@HiltWorker
class EnrichmentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val flashcardDao: FlashcardDao,
    private val freeDictionaryApi: FreeDictionaryApi,
    private val pixabayApi: PixabayApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val cardId = inputData.getString(KEY_CARD_ID) ?: return Result.failure()
        
        return try {
            val flashcard = flashcardDao.getFlashcardById(cardId) ?: return Result.failure()
            
            var ipa = flashcard.ipa
            var imageUrl = flashcard.imageUrl
            
            if (ipa.isBlank()) {
                try {
                    val response = freeDictionaryApi.getWordDetails(flashcard.word)
                    ipa = response.firstOrNull()?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                        ?: response.firstOrNull()?.phonetic
                        ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "IPA fetch error: ${e.message}")
                }
            }
            
            if (imageUrl.isBlank()) {
                try {
                    val response = pixabayApi.searchImages(
                        apiKey = BuildConfig.PIXABAY_API_KEY,
                        query = flashcard.word
                    )
                    imageUrl = response.hits.firstOrNull()?.webformatUrl ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "Pixabay error: ${e.message}")
                }
            }
            
            if (ipa != flashcard.ipa || imageUrl != flashcard.imageUrl) {
                val enrichedCard = flashcard.copy(
                    ipa = ipa,
                    imageUrl = imageUrl,
                    lastUpdated = System.currentTimeMillis()
                )
                flashcardDao.updateFlashcard(enrichedCard)
                Log.d(TAG, "Enriched: ${flashcard.word}")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Enrichment failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "EnrichmentWorker"
        const val KEY_CARD_ID = "card_id"
        const val WORK_NAME_PREFIX = "enrich_card_"
    }
}
