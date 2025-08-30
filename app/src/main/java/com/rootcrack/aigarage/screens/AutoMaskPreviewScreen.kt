package com.rootcrack.aigarage.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.rootcrack.aigarage.navigation.EditTypeValues
import com.rootcrack.aigarage.navigation.NavArgs
import com.rootcrack.aigarage.navigation.Screen
import com.rootcrack.aigarage.segmentation.DeepLabV3XceptionSegmenter
import com.rootcrack.aigarage.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint

// Dosya: AutoMaskPreviewScreen.kt
// Açıklama: Offline maskeleme önizleme ekranı

private const val TAG_AUTO_MASK_PREVIEW = "AutoMaskPreviewScreen"
private val MASK_OVERLAY_COLOR_PREVIEW = Color(0xFF00FF88).copy(alpha = 0.4f)
private val BACKGROUND_MASK_OVERLAY_COLOR_PREVIEW = Color(0xFF00BFFF).copy(alpha = 0.4f)

val CITYSCAPES_LABELS = arrayOf(
    "road", "sidewalk", "building", "wall", "fence", "pole", "traffic light",
    "traffic sign", "vegetation", "terrain", "sky", "person", "rider", "car",
    "truck", "bus", "train", "motorcycle", "bicycle"
)

const val DEFAULT_TARGET_CLASS_NAME = "car"

fun getCityscapesIndexForObject(className: String): Int {
    val index = CITYSCAPES_LABELS.indexOfFirst { it.equals(className.trim(), ignoreCase = true) }
    return if (index != -1) {
        index
    } else {
        CITYSCAPES_LABELS.indexOf(DEFAULT_TARGET_CLASS_NAME)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoMaskPreviewScreen(
    navController: NavController,
    imageUriString: String,
    objectsToMaskCommaSeparated: String,
    modelPath: String
) {
    val context = LocalContext.current
    
    var isLoading by remember { mutableStateOf(true) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var foregroundMaskBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backgroundMaskBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmapWithForegroundMask by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmapWithBackgroundMask by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showFullScreenDialog by remember { mutableStateOf<Bitmap?>(null) }

    // Açıklama: Çoklu nesne seçimi için tüm nesneleri işle
    val targetObjectNames = remember(objectsToMaskCommaSeparated) {
        objectsToMaskCommaSeparated.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(DEFAULT_TARGET_CLASS_NAME) }
    }
    
    val targetClassIndices = remember(targetObjectNames) {
        targetObjectNames.map { getCityscapesIndexForObject(it) }
    }

    val imageUri = remember(imageUriString) { imageUriString.toUri() }

    // Açıklama: Fade-in animasyonu için state
    var showContent by remember { mutableStateOf(false) }

    // Açıklama: Offline maskeleme işlemi
    LaunchedEffect(imageUri, targetClassIndices, modelPath) {
        try {
            isLoading = true
            errorText = null
            showContent = false
            
            // Önceki bitmap'leri güvenli şekilde temizle
            originalBitmap?.recycle()
            displayedOriginalBitmap?.recycle()
            foregroundMaskBitmap?.recycle()
            backgroundMaskBitmap?.recycle()
            displayBitmapWithForegroundMask?.recycle()
            displayBitmapWithBackgroundMask?.recycle()
            
            originalBitmap = null
            displayedOriginalBitmap = null
            foregroundMaskBitmap = null
            backgroundMaskBitmap = null
            displayBitmapWithForegroundMask = null
            displayBitmapWithBackgroundMask = null

            var segmenter: DeepLabV3XceptionSegmenter? = null
            try {
                Log.d(TAG_AUTO_MASK_PREVIEW, "Yüklenen maskeleme modeli: $modelPath")
                Log.d(TAG_AUTO_MASK_PREVIEW, "Hedef nesneler: $targetObjectNames")
                
                // Bitmap'i yükle
                val loadedBitmap = withContext(Dispatchers.IO) { 
                    try {
                        loadBitmapFromUri(context, imageUri)
                    } catch (e: Exception) {
                        Log.e(TAG_AUTO_MASK_PREVIEW, "Bitmap yükleme hatası", e)
                        null
                    }
                }
                
                ensureActive()
                if (loadedBitmap == null) {
                    errorText = "Görsel yüklenemedi: $imageUriString"
                    isLoading = false
                    return@LaunchedEffect
                }
                
                originalBitmap = loadedBitmap
                displayedOriginalBitmap = loadedBitmap
                
                // Açıklama: Offline maskeleme (DeepLabV3XceptionSegmenter)
                segmenter = DeepLabV3XceptionSegmenter(context = context, modelAssetPath = modelPath)

                val initSuccess = segmenter.initialize()
                ensureActive()
                if (!initSuccess) {
                    errorText = "Otomatik maskeleme motoru başlatılamadı."
                    isLoading = false
                    return@LaunchedEffect
                }

                // Normal segmentasyon
                val multiSegmentationResult = withContext(Dispatchers.Default) {
                    try {
                        ensureActive()
                        segmenter?.segmentMultipleObjects(loadedBitmap, targetClassIndices)
                    } catch (e: Exception) {
                        Log.e(TAG_AUTO_MASK_PREVIEW, "Segmentasyon hatası", e)
                        null
                    }
                }

                ensureActive()
                if (multiSegmentationResult?.combinedMaskBitmap != null) {
                    val fgMask = multiSegmentationResult.combinedMaskBitmap
                    foregroundMaskBitmap = fgMask
                    
                    val bgMask = withContext(Dispatchers.IO) { 
                        try {
                            fgMask?.let { mask ->
                                ImageUtils.invertMaskBitmap(mask)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG_AUTO_MASK_PREVIEW, "Arka plan maskesi oluşturma hatası", e)
                            null
                        }
                    }
                    
                    if (bgMask != null) {
                        backgroundMaskBitmap = bgMask
                        
                        displayBitmapWithForegroundMask = withContext(Dispatchers.Default) { 
                            try {
                                fgMask?.let { mask ->
                                    overlayMaskOnBitmap(loadedBitmap, mask, MASK_OVERLAY_COLOR_PREVIEW.toArgb())
                                }
                            } catch (e: Exception) {
                                Log.e(TAG_AUTO_MASK_PREVIEW, "Foreground overlay hatası", e)
                                null
                            }
                        }
                        
                        displayBitmapWithBackgroundMask = withContext(Dispatchers.Default) { 
                            try {
                                bgMask?.let { mask ->
                                    overlayMaskOnBitmap(loadedBitmap, mask, BACKGROUND_MASK_OVERLAY_COLOR_PREVIEW.toArgb())
                                }
                            } catch (e: Exception) {
                                Log.e(TAG_AUTO_MASK_PREVIEW, "Background overlay hatası", e)
                                null
                            }
                        }
                
                        // Açıklama: Başarılı segmentasyonları logla
                        val successfulObjects = multiSegmentationResult.individualResults
                            .filter { it.value.maskBitmap != null }
                            .keys.joinToString(", ")
                        
                        Log.d(TAG_AUTO_MASK_PREVIEW, "Başarılı segmentasyonlar: $successfulObjects")
                        
                    } else {
                        errorText = "Arka plan maskesi oluşturulamadı."
                    }
                } else {
                    val objectNames = targetObjectNames.joinToString(", ")
                    errorText = "Maske oluşturulamadı. '$objectNames' nesneleri resimde bulunamadı."
                }

            } catch (e: Exception) {
                Log.e(TAG_AUTO_MASK_PREVIEW, "Maskeleme sırasında hata", e)
                errorText = "Maskeleme sırasında bir hata oluştu: ${e.message}"
            } finally {
                // Açıklama: Kaynakları temizle
                try {
                    Log.d(TAG_AUTO_MASK_PREVIEW, "Segmenter kapatılıyor.")
                    segmenter?.close()
                } catch (e: Exception) {
                    Log.e(TAG_AUTO_MASK_PREVIEW, "Segmenter kapatma hatası", e)
                }
                
                isLoading = false
                showContent = true
            }
        } catch (e: Exception) {
            Log.e(TAG_AUTO_MASK_PREVIEW, "LaunchedEffect genel hatası", e)
            errorText = "Beklenmeyen bir hata oluştu: ${e.message}"
            isLoading = false
            showContent = true
        }
    }

    // Açıklama: Modern koyu tema tasarımı
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Açıklama: Üst bar
        TopAppBar(
            title = { 
                val objectNames = targetObjectNames.joinToString(", ") { it.replaceFirstChar { char -> char.titlecase() } }
                Text(
                    text = "Maske Önizleme",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .background(
                            color = Color(0xFF16213E),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Geri",
                        tint = Color(0xFF00FF88)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF16213E),
                titleContentColor = Color.White
            )
        )

        // Açıklama: Ana içerik
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            when {
                isLoading -> {
                    LoadingScreen()
                }
                errorText != null -> {
                    ErrorScreen(errorText = errorText!!)
                }
                else -> {
                    if (showContent) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            MainContent(
                                originalBitmap = displayedOriginalBitmap,
                                foregroundMaskBitmap = displayBitmapWithForegroundMask,
                                backgroundMaskBitmap = displayBitmapWithBackgroundMask,
                                modelPath = modelPath,
                                onForegroundClick = {
                                    foregroundMaskBitmap?.let { mask ->
                                        val objectNames = targetObjectNames.joinToString(",")
                                        navigateToEditScreen(navController, imageUri, mask, objectNames, EditTypeValues.FOREGROUND)
                                    }
                                },
                                onBackgroundClick = {
                                    backgroundMaskBitmap?.let { mask ->
                                        navigateToEditScreen(navController, imageUri, mask, "background", EditTypeValues.BACKGROUND)
                                    }
                                },
                                onNoMaskClick = {
                                    navigateToEditScreen(navController, imageUri, null, "maskesiz", EditTypeValues.NONE)
                                },
                                onImageClick = { bitmap ->
                                    showFullScreenDialog = bitmap
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Açıklama: Tam ekran önizleme dialog'u
    showFullScreenDialog?.let { bitmap ->
        FullScreenImageDialog(
            bitmap = bitmap,
            onDismiss = { showFullScreenDialog = null }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF00FF88)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Maskeler oluşturuluyor...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
private fun ErrorScreen(errorText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B6B)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = errorText,
            color = Color(0xFFFF6B6B),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun MainContent(
    originalBitmap: Bitmap?,
    foregroundMaskBitmap: Bitmap?,
    backgroundMaskBitmap: Bitmap?,
    modelPath: String,
    onForegroundClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onNoMaskClick: () -> Unit,
    onImageClick: (Bitmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Açıklama: Orijinal görüntü
        if (originalBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF16213E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Orijinal Görüntü",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = "Orijinal görüntü",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(originalBitmap) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Açıklama: Maskeler - yan yana maskeleri göster
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Açıklama: Nesne maskesi - sol taraf
            MaskCard(
                title = "Nesne Maskesi",
                bitmap = foregroundMaskBitmap,
                onClick = { foregroundMaskBitmap?.let { onImageClick(it) } },
                onActionClick = onForegroundClick,
                actionColor = Color(0xFF00FF88),
                modifier = Modifier.weight(1f)
            )

            // Açıklama: Arka plan maskesi - sağ taraf
            MaskCard(
                title = "Arka Plan Maskesi",
                bitmap = backgroundMaskBitmap,
                onClick = { backgroundMaskBitmap?.let { onImageClick(it) } },
                onActionClick = onBackgroundClick,
                actionColor = Color(0xFF00BFFF),
                modifier = Modifier.weight(1f)
            )
        }

        // Açıklama: Maskesiz düzenleme butonu
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0xFF00FF88).copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16213E)
            )
        ) {
            Button(
                onClick = onNoMaskClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C5CE7),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Maskesiz Düzenle",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun MaskCard(
    title: String,
    bitmap: Bitmap?,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    actionColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = actionColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16213E)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Açıklama: Başlık
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            // Açıklama: Resim ve tik butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F3460))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "$title önizlemesi",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Açıklama: Tik butonu - sağ alt köşede
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(40.dp)
                            .background(
                                color = actionColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bu maskeyi kullan",
                            tint = if (actionColor == Color(0xFF00FF88)) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Açıklama: Tam ekran butonu - üst sağ köşede
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = Color(0xFF16213E).copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Tam ekran görüntüle",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = "Resim yok",
                        tint = Color(0xFF6C5CE7),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Tam ekran görüntü",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            
            // Açıklama: Kapat butonu
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Açıklama: Yardımcı fonksiyonlar
private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        Log.e(TAG_AUTO_MASK_PREVIEW, "Bitmap yükleme hatası", e)
        null
    }
}

private fun overlayMaskOnBitmap(originalBitmap: Bitmap, maskBitmap: Bitmap, @ColorInt overlayColor: Int): Bitmap {
    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(resultBitmap)
    val paint = AndroidPaint().apply {
        colorFilter = PorterDuffColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP)
    }
    
    canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
    return resultBitmap
}

private fun navigateToEditScreen(
    navController: NavController,
    imageUri: Uri,
    maskBitmap: Bitmap?,
    objectNames: String,
    editType: String
) {
    val maskBase64 = maskBitmap?.let { bitmap ->
        try {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG_AUTO_MASK_PREVIEW, "Mask base64 encoding hatası", e)
            null
        }
    } ?: ""

    val route = Screen.EditPhoto.route
        .replace("{${NavArgs.IMAGE_URI}}", Uri.encode(imageUri.toString()))
        .replace("{${NavArgs.INSTRUCTION}}", Uri.encode(""))
        .replace("{${NavArgs.MASK}}", Uri.encode(maskBase64))
        .replace("{${NavArgs.EDIT_TYPE}}", Uri.encode(editType))

    navController.navigate(route)
}
