package com.rootcrack.aigarage.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.rootcrack.aigarage.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

// Dosya: HuggingFaceService.kt
// Açıklama: HuggingFace API ile iletişim için servis sınıfı

class HuggingFaceService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val imageMediaType = "image/png".toMediaType()
    
    companion object {
        private const val TAG = "HuggingFaceService"
        private const val BASE_URL = "https://api-inference.huggingface.co/models"
        private const val SPACES_URL = "https://huggingface.co/spaces"
        
        // Açıklama: Desteklenen modeller
        const val MODEL_SAM = "facebook/sam-vit-base"
        const val MODEL_SEGMENTATION = "nvidia/segformer-b0-finetuned-ade-512-512"
        const val MODEL_INPAINTING = "runwayml/stable-diffusion-inpainting"
        
        // Açıklama: HuggingFace Spaces
        const val SPACE_SAM = "amilcakmak-ai-garage-sam"
        const val SPACE_SEGMENTATION = "nvidia/segformer"
    }
    
    // Açıklama: SAM (Segment Anything Model) ile maskeleme
    suspend fun segmentWithSAM(
        image: Bitmap,
        apiKey: String,
        modelName: String = MODEL_SAM
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SAM maskeleme başlatılıyor...")
            
            // Açıklama: Görüntüyü base64'e çevir
            val imageBase64 = bitmapToBase64(image)
            
            // Açıklama: API isteği için JSON hazırla
            val requestBody = JSONObject().apply {
                put("inputs", imageBase64)
                put("parameters", JSONObject().apply {
                    put("return_mask", true)
                    put("return_image", false)
                })
            }.toString().toRequestBody(jsonMediaType)
            
            // Açıklama: HTTP isteği oluştur
            val request = Request.Builder()
                .url("$BASE_URL/$modelName")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            // Açıklama: İsteği gönder
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "SAM API hatası: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }
                
                val responseBody = response.body?.bytes()
                if (responseBody == null) {
                    Log.e(TAG, "Boş API yanıtı")
                    return@withContext Result.failure(Exception("Empty response"))
                }
                
                // Açıklama: Base64 maskeyi bitmap'e çevir
                val maskBitmap = base64ToBitmap(String(responseBody))
                if (maskBitmap != null) {
                    Log.d(TAG, "SAM maskeleme başarılı")
                    Result.success(maskBitmap)
                } else {
                    Log.e(TAG, "Mask bitmap oluşturulamadı")
                    Result.failure(Exception("Failed to create mask bitmap"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "SAM maskeleme hatası", e)
            Result.failure(e)
        }
    }
    
    // Açıklama: Segmentasyon modeli ile maskeleme
    suspend fun segmentWithModel(
        image: Bitmap,
        apiKey: String,
        modelName: String = MODEL_SEGMENTATION
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Segmentasyon maskeleme başlatılıyor...")
            
            val imageBase64 = bitmapToBase64(image)
            
            val requestBody = JSONObject().apply {
                put("inputs", imageBase64)
            }.toString().toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL/$modelName")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Segmentasyon API hatası: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }
                
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "Boş segmentasyon yanıtı")
                    return@withContext Result.failure(Exception("Empty response"))
                }
                
                // Açıklama: JSON yanıtını işle
                val maskBitmap = parseSegmentationResponse(responseBody, image.width, image.height)
                if (maskBitmap != null) {
                    Log.d(TAG, "Segmentasyon maskeleme başarılı")
                    Result.success(maskBitmap)
                } else {
                    Log.e(TAG, "Segmentasyon maskesi oluşturulamadı")
                    Result.failure(Exception("Failed to create segmentation mask"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Segmentasyon maskeleme hatası", e)
            Result.failure(e)
        }
    }
    
    // Açıklama: Inpainting (görüntü tamamlama) işlemi
    suspend fun inpainting(
        originalImage: Bitmap,
        mask: Bitmap,
        prompt: String,
        apiKey: String,
        modelName: String = MODEL_INPAINTING
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Inpainting başlatılıyor...")
            
            val imageBase64 = bitmapToBase64(originalImage)
            val maskBase64 = bitmapToBase64(mask)
            
            val requestBody = JSONObject().apply {
                put("inputs", JSONObject().apply {
                    put("image", imageBase64)
                    put("mask", maskBase64)
                    put("prompt", prompt)
                })
                put("parameters", JSONObject().apply {
                    put("num_inference_steps", 20)
                    put("guidance_scale", 7.5)
                })
            }.toString().toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL/$modelName")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Inpainting API hatası: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }
                
                val responseBody = response.body?.bytes()
                if (responseBody == null) {
                    Log.e(TAG, "Boş inpainting yanıtı")
                    return@withContext Result.failure(Exception("Empty response"))
                }
                
                val resultBitmap = base64ToBitmap(String(responseBody))
                if (resultBitmap != null) {
                    Log.d(TAG, "Inpainting başarılı")
                    Result.success(resultBitmap)
                } else {
                    Log.e(TAG, "Inpainting sonucu oluşturulamadı")
                    Result.failure(Exception("Failed to create inpainting result"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Inpainting hatası", e)
            Result.failure(e)
        }
    }
    
    // Açıklama: Bitmap'i base64'e çevir
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }
    
    // Açıklama: Base64'ü bitmap'e çevir
    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Base64 to bitmap hatası", e)
            null
        }
    }
    
    // Açıklama: Segmentasyon yanıtını işle
    private fun parseSegmentationResponse(response: String, width: Int, height: Int): Bitmap? {
        return try {
            val jsonObject = JSONObject(response)
            val maskData = jsonObject.getJSONArray("mask")
            
            // Açıklama: Mask verilerini bitmap'e çevir
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            
            for (i in 0 until maskData.length()) {
                val pixelValue = maskData.getInt(i)
                pixels[i] = if (pixelValue > 0) 0xFFFFFFFF.toInt() else 0x00000000.toInt()
            }
            
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Segmentasyon yanıtı işleme hatası", e)
            null
        }
    }
    
    // Açıklama: Gradio API yanıtını işle
    private fun parseGradioResponse(response: String, width: Int, height: Int): Bitmap? {
        return try {
            val jsonObject = JSONObject(response)
            val dataArray = jsonObject.getJSONArray("data")
            
            if (dataArray.length() > 0) {
                val maskBase64 = dataArray.getString(0)
                // Açıklama: Base64 maskeyi bitmap'e çevir
                val maskBitmap = base64ToBitmap(maskBase64)
                if (maskBitmap != null) {
                    // Açıklama: Mask boyutunu orijinal görüntü boyutuna ayarla
                    return Bitmap.createScaledBitmap(maskBitmap, width, height, true)
                }
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Gradio yanıtı işleme hatası", e)
            null
        }
    }
    
    // Açıklama: HuggingFace Spaces'e resim gönder ve maskeleme yap
    suspend fun segmentWithSpaces(
        image: Bitmap,
        spaceName: String = SPACE_SAM
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "HuggingFace Spaces maskeleme başlatılıyor...")
            
            // Açıklama: Görüntüyü base64'e çevir
            val imageBase64 = bitmapToBase64(image)
            
            // Açıklama: Gradio API endpoint'i
            val gradioApiUrl = "https://amilcakmak-ai-garage-sam.hf.space/api/predict"
            
            // Açıklama: Gradio API isteği için JSON hazırla
            val requestBody = JSONObject().apply {
                put("data", JSONArray().apply {
                    put(imageBase64)
                })
            }.toString().toRequestBody(jsonMediaType)
            
            // Açıklama: HTTP isteği oluştur
            val request = Request.Builder()
                .url(gradioApiUrl)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            // Açıklama: İsteği gönder
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gradio API hatası: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }
                
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "Boş Gradio API yanıtı")
                    return@withContext Result.failure(Exception("Empty response"))
                }
                
                // Açıklama: Gradio yanıtını işle
                val maskBitmap = parseGradioResponse(responseBody, image.width, image.height)
                if (maskBitmap != null) {
                    Log.d(TAG, "HuggingFace Spaces maskeleme başarılı")
                    Result.success(maskBitmap)
                } else {
                    Log.e(TAG, "Mask bitmap oluşturulamadı")
                    Result.failure(Exception("Failed to create mask bitmap"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "HuggingFace Spaces maskeleme hatası", e)
            Result.failure(e)
        }
    }
    
    // Açıklama: Servisi kapat
    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
