@file:Suppress("AssignedValueIsNeverRead", "VariableNeverRead", "unused")

package com.rootcrack.aigarage.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rootcrack.aigarage.data.preferences.ThemePreferences
import com.rootcrack.aigarage.data.preferences.ThemeHelper
import com.rootcrack.aigarage.services.HuggingFaceService

import java.io.File

// Açıklama: Maskelenebilir nesne seçenekleri
val MASKABLE_OBJECTS_GALLERY = mapOf(
    "Araba" to "car",
    "Motosiklet" to "motorcycle",
    "İnsan" to "person",
    "Bisiklet" to "bicycle",
    "Kamyon" to "truck",
    "Otobüs" to "bus",
    "Tren" to "train",
    "Arka Plan" to "background"
)

val SORTED_MASKABLE_OBJECTS_GALLERY = listOf(
    "Araba", "Motosiklet", "İnsan", "Bisiklet", "Kamyon", "Otobüs", "Tren", "Arka Plan"
)

// Açıklama: Modern adım adım galeri ekranı
@SuppressLint("AutoboxingStateCreation")
@Composable
fun GalleryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Açıklama: Tema sistemi
    val themePreference = ThemePreferences.getThemeFlow(context)
        .collectAsState(initial = ThemePreferences.THEME_DARK_SPECIAL)
    val currentTheme = themePreference.value
    val themeColors = ThemeHelper.getCurrentThemeColors(currentTheme)

    val galleryDir = remember {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "AIGarage").apply { mkdirs() }
    }

    val imageFiles = remember { mutableStateListOf<File>() }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // Açıklama: AI işlemleri için state'ler
    var showModelSelectionDialog by remember { mutableStateOf(false) }
    val selectedMaskObjects = remember { mutableStateListOf<String>() }
    var selectedModelPath by remember { mutableStateOf<String?>(null) }
    var currentSelectedFile by remember { mutableStateOf<File?>(null) }
    
    // Açıklama: HuggingFace servisi
    val huggingFaceService = remember { HuggingFaceService(context) }
    


    // Açıklama: Dosyaları yükle
    fun loadFiles() {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val files = galleryDir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "png", "jpeg", "webp") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            
            Log.d("GalleryScreen", "Found ${files.size} images")
            
            withContext(Dispatchers.Main) {
                imageFiles.clear()
                imageFiles.addAll(files)
                if (files.isNotEmpty() && selectedImageIndex >= files.size) {
                    selectedImageIndex = maxOf(0, files.size - 1)
                }
                isLoading = false
            }
        }
    }

    // Açıklama: Resim seçici launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                uris.forEachIndexed { index, uri ->
                    try {
                        val fileName = "imported_${System.currentTimeMillis()}_$index.jpg"
                        val destFile = File(galleryDir, fileName)
                        
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d("GalleryScreen", "Imported: $fileName")
                    } catch (e: Exception) {
                        Log.e("GalleryScreen", "Import error for URI: $uri", e)
                    }
                }
                withContext(Dispatchers.Main) {
                    loadFiles()
                }
            }
        }
    }

    // Açıklama: İlk yükleme
    LaunchedEffect(Unit) {
        loadFiles()
    }
    
    // Açıklama: Seçili resim değiştiğinde currentSelectedFile'ı güncelle
    LaunchedEffect(selectedImageIndex, imageFiles) {
        if (imageFiles.isNotEmpty() && selectedImageIndex < imageFiles.size) {
            currentSelectedFile = imageFiles[selectedImageIndex]
        }
    }

    // Açıklama: Ana ekran tasarımı
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeHelper.getBackgroundBrush(currentTheme))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Açıklama: Başlık
        Text(
            text = "Aracını Yeniden Tasarla",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = themeColors.primary
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        


        // Açıklama: Adım 1 - Fotoğraf Yükleme
        StepHeader(number = 1, title = "Beğendiğin bir fotoğraf ekle")
        
        Spacer(modifier = Modifier.height(16.dp))

        // Açıklama: Seçilen fotoğraf önizlemesi
        if (imageFiles.isNotEmpty() && selectedImageIndex < imageFiles.size) {
            val selectedFile = imageFiles[selectedImageIndex]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF16213E))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(selectedFile),
                    contentDescription = "Seçilen fotoğraf",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Açıklama: Kaldırma butonu
                IconButton(
                    onClick = {
        coroutineScope.launch(Dispatchers.IO) {
                            selectedFile.delete()
                            withContext(Dispatchers.Main) {
                                loadFiles()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fotoğrafı kaldır",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                

            }
        } else {
            // Açıklama: Fotoğraf yükleme alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeHelper.getSurfaceBrush(currentTheme))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
        Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Fotoğraf ekle",
                        tint = themeColors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fotoğraf eklemek için dokun",
                        style = MaterialTheme.typography.bodyLarge,
                        color = themeColors.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Açıklama: Şablon seçenekleri
        Text(
            text = "Veya şablonlardan seç",
            style = MaterialTheme.typography.titleMedium,
            color = themeColors.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(imageFiles.take(6)) { file ->
                TemplateCard(
                    file = file,
                    onClick = {
                        selectedImageIndex = imageFiles.indexOf(file)
                    },
                    isSelected = imageFiles.indexOf(file) == selectedImageIndex
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Açıklama: Adım 2 - Maskeleme Seçenekleri
        StepHeader(number = 2, title = "Bir seçenek belirle")
        
        Spacer(modifier = Modifier.height(16.dp))

        // Açıklama: Maskeleme seçenekleri grid'i
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(SORTED_MASKABLE_OBJECTS_GALLERY) { displayName ->
                MaskingOptionCard(
                    displayName = displayName,
                    apiKey = MASKABLE_OBJECTS_GALLERY[displayName] ?: "",
                    isSelected = selectedMaskObjects.contains(MASKABLE_OBJECTS_GALLERY[displayName]),
                    onClick = {
                        val key = MASKABLE_OBJECTS_GALLERY[displayName] ?: ""
                        if (selectedMaskObjects.contains(key)) {
                            selectedMaskObjects.remove(key)
                                } else {
                            selectedMaskObjects.add(key)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Açıklama: Devam Et butonu
        Button(
            onClick = {
                if (imageFiles.isNotEmpty() && selectedMaskObjects.isNotEmpty()) {
                    currentSelectedFile = imageFiles[selectedImageIndex]
                    showModelSelectionDialog = true
            } else {
                    Toast.makeText(
                        context,
                        "Lütfen bir fotoğraf seçin ve maskeleme seçeneklerini belirleyin",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00FF88),
                contentColor = Color.Black
            ),
            enabled = imageFiles.isNotEmpty() && selectedMaskObjects.isNotEmpty()
        ) {
            Text(
                text = "Devam Et",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    // Açıklama: Model seçimi dialog'u gösterildiğinde
        if (showModelSelectionDialog) {
            ModelSelectionDialog(
            onDismissRequest = { 
                    showModelSelectionDialog = false
                selectedModelPath = null
                currentSelectedFile = null
                },
                onModelSelected = { modelPath ->
                    selectedModelPath = modelPath
                    showModelSelectionDialog = false
                
                // Açıklama: Model seçimine göre yönlendirme
                currentSelectedFile?.let { file ->
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider", // Açıklama: Dynamic authority from manifest
                        file
                    ).toString()
                    
                    if (modelPath == "sam_masking") {
                        // Açıklama: HuggingFace Spaces'e resim gönder ve maskeleme yap
                        Log.d("GalleryScreen", "Starting HuggingFace Spaces maskeleme")
                        
                        coroutineScope.launch {
                            try {
                                // Açıklama: Resmi bitmap'e çevir
                                val bitmap = withContext(Dispatchers.IO) {
                                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                }
                                
                                if (bitmap != null) {
                                    // Açıklama: HuggingFace Spaces'e gönder
                                    val result = huggingFaceService.segmentWithSpaces(bitmap)
                                    
                                    if (result.isSuccess) {
                                        val maskBitmap = result.getOrNull()
                                        if (maskBitmap != null) {
                                            // Açıklama: Maskeyi kaydet
                                            val maskFile = File(galleryDir, "mask_${System.currentTimeMillis()}.png")
                                            maskFile.outputStream().use { out ->
                                                maskBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                            }
                                            
                                            // Açıklama: Başarı mesajı
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Maskeleme başarılı! Mask kaydedildi.", Toast.LENGTH_LONG).show()
                                            }
                                            
                                            // Açıklama: Maskeyi temizle
                                            maskBitmap.recycle()
                                        }
                                    } else {
                                        val error = result.exceptionOrNull()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Maskeleme hatası: ${error?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    
                                    // Açıklama: Bitmap'i temizle
                                    bitmap.recycle()
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Resim yüklenemedi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("GalleryScreen", "HuggingFace Spaces maskeleme hatası", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Maskeleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        // Açıklama: AutoMaskPreview'e yönlendir
                        val objectsToMask = selectedMaskObjects.joinToString(",")
                        
                        val routeWithArguments = com.rootcrack.aigarage.navigation.Screen.AutoMaskPreview.route
                            .replace("{${com.rootcrack.aigarage.navigation.NavArgs.IMAGE_URI}}", Uri.encode(fileUri))
                            .replace("{${com.rootcrack.aigarage.navigation.NavArgs.OBJECT_TO_MASK}}", Uri.encode(objectsToMask))
                            .replace("{${com.rootcrack.aigarage.navigation.NavArgs.MODEL_PATH}}", Uri.encode(selectedModelPath ?: "mask.tflite"))
                        
                        Log.d("GalleryScreen", "Navigating to AutoMaskPreview with: $routeWithArguments")
                        navController.navigate(routeWithArguments)
                    }
                }
            }
        )
    }
        

    }

// Açıklama: Adım başlığı composable'ı
@Composable
private fun StepHeader(number: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = Color(0xFF00FF88),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}

// Açıklama: Şablon kartı composable'ı
@Composable
private fun TemplateCard(
    file: File,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    val context = LocalContext.current
    val themePreference = ThemePreferences.getThemeFlow(context).collectAsState(initial = ThemePreferences.THEME_DARK_SPECIAL)
    val themeColors = ThemeHelper.getCurrentThemeColors(themePreference.value)

    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColors.primary else themeColors.surface
        )
    ) {
                Image(
            painter = rememberAsyncImagePainter(file),
            contentDescription = "Şablon",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// Açıklama: Maskeleme seçenek kartı composable'ı
@Composable
private fun MaskingOptionCard(
    displayName: String,
    apiKey: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val themePreference = ThemePreferences.getThemeFlow(context).collectAsState(initial = ThemePreferences.THEME_DARK_SPECIAL)
    val themeColors = ThemeHelper.getCurrentThemeColors(themePreference.value)

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColors.primary else themeColors.surface
        )
    ) {
        Column(
                    modifier = Modifier
                        .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when(displayName) {
                    "Araba" -> "🚗"
                    "Motosiklet" -> "🏍️"
                    "İnsan" -> "👤"
                    "Bisiklet" -> "🚲"
                    "Kamyon" -> "🚛"
                    "Otobüs" -> "🚌"
                    "Tren" -> "🚆"
                    "Arka Plan" -> "🌅"
                    else -> "📷"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) themeColors.onPrimary else themeColors.onSurface
            )
        }
    }
}

// Açıklama: Model seçim dialog composable'ı
@Composable
private fun ModelSelectionDialog(
    onDismissRequest: () -> Unit,
    onModelSelected: (modelPath: String) -> Unit
) {
    val themeColors = ThemeHelper.getCurrentThemeColors("dark_special") // Açıklama: Default tema kullan
    var tempSelectedModel by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
                    modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI Model Seçimi",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hangi AI modelini kullanmak istiyorsunuz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Açıklama: Hızlı versiyon seçeneği
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (tempSelectedModel == "mask.tflite"),
                            onClick = { tempSelectedModel = "mask.tflite" }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tempSelectedModel == "mask.tflite")
                            Color(0xFF00FF88) else Color(0xFF0F3460)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = if (tempSelectedModel == "mask.tflite") Color.Black else Color(0xFF00FF88),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hızlı Maskeleme",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tempSelectedModel == "mask.tflite") Color.Black else Color.White
                            )
                            Text(
                                text = "Daha hızlı sonuç, daha az detay",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tempSelectedModel == "mask.tflite") Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Açıklama: Detaylı versiyon seçeneği
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (tempSelectedModel == "deeplabv3-xception65.tflite"),
                            onClick = { tempSelectedModel = "deeplabv3-xception65.tflite" }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tempSelectedModel == "deeplabv3-xception65.tflite")
                            Color(0xFF00FF88) else Color(0xFF0F3460)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HighQuality,
                            contentDescription = null,
                            tint = if (tempSelectedModel == "deeplabv3-xception65.tflite") Color.Black else Color(0xFF00FF88),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Detaylı Maskeleme",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tempSelectedModel == "deeplabv3-xception65.tflite") Color.Black else Color.White
                            )
                            Text(
                                text = "Daha yavaş ama daha kaliteli sonuç",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tempSelectedModel == "deeplabv3-xception65.tflite") Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Açıklama: SAM maskeleme seçeneği
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (tempSelectedModel == "sam_masking"),
                            onClick = { tempSelectedModel = "sam_masking" }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tempSelectedModel == "sam_masking")
                            Color(0xFF00FF88) else Color(0xFF0F3460)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = if (tempSelectedModel == "sam_masking") Color.Black else Color(0xFF00FF88),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Online Maskeleme",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tempSelectedModel == "sam_masking") Color.Black else Color.White
                        )
                        Text(
                                text = "Otomatik AI maskeleme (Online)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tempSelectedModel == "sam_masking") Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))



                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("İptal", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            tempSelectedModel?.let { onModelSelected(it) }
                        },
                        enabled = tempSelectedModel != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF88),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Devam Et")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
    }
}