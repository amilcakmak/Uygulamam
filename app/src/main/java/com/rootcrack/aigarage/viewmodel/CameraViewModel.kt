package com.rootcrack.aigarage.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope


import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE)


    // State flows for UI updates
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _maskedImage = MutableStateFlow<Bitmap?>(null)
    val maskedImage: StateFlow<Bitmap?> = _maskedImage.asStateFlow()

    private val _processedImage = MutableStateFlow<Bitmap?>(null)
    val processedImage: StateFlow<Bitmap?> = _processedImage.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()



    private var adDisplayDelayJob: Job? = null

    companion object {
        const val PHOTOS_TAKEN_COUNT_KEY = "photos_taken_count_for_ad"
        const val AD_INTERVAL = 1 // Her 2 başarılı kayıttan sonra reklam
        const val AD_DISPLAY_DELAY_MS = 3000L // Reklamı göstermeden önceki gecikme (isteğe bağlı)
    }





    private fun getPhotosTakenCountSinceLastAd(): Int {
        return sharedPreferences.getInt(PHOTOS_TAKEN_COUNT_KEY, 0)
    }

    private fun incrementPhotosTakenCountAndPersist() {
        val currentCount = getPhotosTakenCountSinceLastAd()
        sharedPreferences.edit {
            putInt(PHOTOS_TAKEN_COUNT_KEY, currentCount + 1)
        }
        Log.d("CameraViewModel", "Photos taken count since last ad updated to: ${currentCount + 1}")
    }

    /**
     * CameraScreen tarafından çağrılır.
     * Bir fotoğraf başarıyla kaydedildikten sonra reklam gösterilip gösterilmeyeceğini kontrol eder.
     * Sayaç artırılır ve AD_INTERVAL'e ulaşıldıysa true döner.
     */
    fun incrementPhotoCounterAndCheckAd(): Boolean {
        incrementPhotosTakenCountAndPersist()
        val currentTotalPhotos = getPhotosTakenCountSinceLastAd()
        Log.d("CameraViewModel", "Photo saved. Total photos since last ad for interval: $currentTotalPhotos")

        val shouldDisplay = currentTotalPhotos >= AD_INTERVAL
        if (shouldDisplay) {
            Log.d("CameraViewModel", "AD_INTERVAL ($AD_INTERVAL) reached. Ad should be shown.")
        }
        return shouldDisplay
    }

    /**
     * CameraScreen tarafından çağrılır.
     * Mevcut durumda reklam gösterilip gösterilmeyeceğini SADECE kontrol eder, sayacı artırmaz.
     */
    fun shouldShowAd(): Boolean {
        val photosTaken = getPhotosTakenCountSinceLastAd()
        val shouldShow = photosTaken >= AD_INTERVAL
        Log.d("CameraViewModel", "shouldShowAd() called. Photos since last ad: $photosTaken, AD_INTERVAL: $AD_INTERVAL. Should show: $shouldShow")
        return shouldShow
    }

    /**
     * CameraScreen tarafından reklam başarıyla gösterildikten sonra çağrılır.
     * Reklam sayacını sıfırlar.
     */
    fun resetAdCounter() {
        sharedPreferences.edit {
            putInt(PHOTOS_TAKEN_COUNT_KEY, 0)
        }
        Log.d("CameraViewModel", "Ad counter reset to 0.")
    }


    
    // Görüntüleri temizle
    fun clearImages() {
        _maskedImage.value = null
        _processedImage.value = null
        _statusMessage.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        adDisplayDelayJob?.cancel()
        Log.d("CameraViewModel", "ViewModel cleared, ad display job cancelled.")
    }
}

