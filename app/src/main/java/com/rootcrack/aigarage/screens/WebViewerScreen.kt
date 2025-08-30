// Dosya: app/src/main/java/com/rootcrack/aigarage/screens/WebViewerScreen.kt
package com.rootcrack.aigarage.screens

import android.annotation.SuppressLint
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.rootcrack.aigarage.data.preferences.ThemeHelper
import com.rootcrack.aigarage.data.preferences.ThemePreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Açıklama: Şifrelenmiş URL'yi çözen fonksiyon
private fun decryptUrl(encryptedUrl: String): String {
    return try {
        // Açıklama: Base64 decode + XOR şifreleme + ek güvenlik
        val decoded = Base64.decode(encryptedUrl, Base64.DEFAULT)
        val key = "AIGarage2024".toByteArray()
        val decrypted = ByteArray(decoded.size)
        
        for (i in decoded.indices) {
            decrypted[i] = (decoded[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        
        val result = String(decrypted)
        // Açıklama: URL doğrulama
        if (result.startsWith("https://") && result.contains("hf.space")) {
            result
        } else {
            "https://amilcakmak-ai-garage-sam.hf.space/"
        }
    } catch (e: Exception) {
        // Açıklama: Hata durumunda varsayılan URL
        "https://amilcakmak-ai-garage-sam.hf.space/"
    }
}

// Açıklama: Şifrelenmiş URL'yi oluşturan fonksiyon
private fun encryptUrl(url: String): String {
    val key = "AIGarage2024".toByteArray()
    val encrypted = ByteArray(url.length)
    
    for (i in url.indices) {
        encrypted[i] = (url[i].code.toByte().toInt() xor key[i % key.size].toInt()).toByte()
    }
    
    return Base64.encodeToString(encrypted, Base64.DEFAULT)
}

// Açıklama: Şifrelenmiş URL'yi oluşturan fonksiyon
private fun getEncryptedUrl(): String {
    // Açıklama: AI Garage SAM Hugging Face Space URL'si şifreleniyor
    return encryptUrl("https://amilcakmak-ai-garage-sam.hf.space/")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewerScreen(navController: NavController) {
    val context = LocalContext.current
    
    // Açıklama: Tam ekran modu etkinleştir
    LaunchedEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            val window = it.window
            // Açıklama: Sistem bar'larını gizle
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(android.view.WindowInsets.Type.systemBars())
        }
    }
    
    val themePreference = ThemePreferences.getThemeFlow(context)
        .collectAsState(initial = ThemePreferences.THEME_DARK_SPECIAL)
    val currentTheme = themePreference.value
    val themeColors = ThemeHelper.getCurrentThemeColors(currentTheme)
    
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isDesktopMode by remember { mutableStateOf(true) }
    
    val encryptedUrl = remember {
        getEncryptedUrl()
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            currentTime = timeFormat.format(Date())
            delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Toolbar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AIGARAGE",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primary,
                    modifier = Modifier
                        .clickable { showDialog = true }
                        .padding(vertical = 8.dp)
                )
                
                Text(
                    text = currentTime,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColors.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Açıklama: Desktop/Mobile toggle butonu
                IconButton(
                    onClick = { 
                        isDesktopMode = !isDesktopMode
                        // Açıklama: WebView'ı yeniden yükle
                        webView?.reload()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isDesktopMode) themeColors.primary.copy(alpha = 0.2f) else themeColors.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                        contentDescription = if (isDesktopMode) "Mobil Görünüme Geç" else "Masaüstü Görünüme Geç",
                        tint = if (isDesktopMode) themeColors.primary else themeColors.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { webView?.reload() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = themeColors.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sayfayı Yenile",
                        tint = themeColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // WebView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportMultipleWindows(false)
                            allowFileAccess = true
                            allowContentAccess = true
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            mediaPlaybackRequiresUserGesture = false
                            
                            // Açıklama: Dinamik görünüm ayarları
                            userAgentString = if (isDesktopMode) {
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            } else {
                                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }
                            setGeolocationEnabled(false)
                            databaseEnabled = true
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                // Açıklama: Sayfa yüklenmeye başladığında dinamik görünüm için JavaScript enjeksiyonu
                                val viewModeScript = """
                                    (function() {
                                        var isDesktop = $isDesktopMode;
                                        
                                        // Açıklama: Viewport meta tag'ini ayarla
                                        var viewport = document.querySelector('meta[name="viewport"]');
                                        if (viewport) {
                                            if (isDesktop) {
                                                viewport.setAttribute('content', 'width=1024, initial-scale=1.0');
                                            } else {
                                                viewport.setAttribute('content', 'width=device-width, initial-scale=1.0');
                                            }
                                        } else {
                                            var meta = document.createElement('meta');
                                            meta.name = 'viewport';
                                            meta.content = isDesktop ? 'width=1024, initial-scale=1.0' : 'width=device-width, initial-scale=1.0';
                                            document.head.appendChild(meta);
                                        }
                                        
                                        // Açıklama: Body'ye class ekle
                                        document.body.classList.remove('desktop-view', 'mobile-view');
                                        document.body.classList.add(isDesktop ? 'desktop-view' : 'mobile-view');
                                        
                                        // Açıklama: CSS ile görünümü ayarla
                                        var viewCSS = document.createElement('style');
                                        if (isDesktop) {
                                            viewCSS.innerHTML = `
                                                body { min-width: 1024px !important; }
                                                * { max-width: none !important; }
                                                [class*="mobile"], [class*="Mobile"] { display: none !important; }
                                                [class*="tablet"], [class*="Tablet"] { display: none !important; }
                                            `;
                                        } else {
                                            viewCSS.innerHTML = `
                                                body { min-width: auto !important; }
                                                * { max-width: 100% !important; }
                                                [class*="desktop"], [class*="Desktop"] { display: none !important; }
                                            `;
                                        }
                                        document.head.appendChild(viewCSS);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(viewModeScript, null)
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // Açıklama: Header'ı gizlemek için CSS enjeksiyonu
                                val css = """
                                    <style>
                                        /* Hugging Face header'ını gizle - Ana hedef */
                                        div.flex.items-center.justify-between.xl\\:min-w-0,
                                        h1.my-2.flex.w-full.min-w-0.flex-wrap.items-center.gap-y-2.text-lg.leading-tight.xl\\:flex-nowrap,
                                        div[class*="flex"][class*="items-center"][class*="justify-between"],
                                        div[class*="flex"][class*="items-center"],
                                        div[class*="justify-between"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            height: 0 !important;
                                            overflow: hidden !important;
                                            position: absolute !important;
                                            top: -9999px !important;
                                            left: -9999px !important;
                                            z-index: -9999 !important;
                                        }
                                        
                                        /* Tüm header elementlerini gizle */
                                        header, .header, [class*="header"], 
                                        [class*="Header"], [class*="HEADER"],
                                        [class*="navbar"], [class*="Navbar"],
                                        [class*="navigation"], [class*="Navigation"],
                                        [class*="topbar"], [class*="Topbar"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            height: 0 !important;
                                            overflow: hidden !important;
                                            position: absolute !important;
                                            top: -9999px !important;
                                        }
                                        
                                        /* Navigation bar'ı gizle */
                                        nav, .nav, [class*="nav"], 
                                        [class*="Nav"], [class*="NAV"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                        }
                                        
                                        /* Breadcrumb'ları gizle */
                                        [class*="breadcrumb"], [class*="Breadcrumb"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                        }
                                        
                                        /* Top bar'ı gizle */
                                        [class*="top"], [class*="Top"], [class*="TOP"],
                                        [class*="toolbar"], [class*="Toolbar"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                        }
                                        
                                        /* Sayfa üstündeki boşluğu kaldır */
                                        body {
                                            margin-top: 0 !important;
                                            padding-top: 0 !important;
                                        }
                                        
                                        /* Ana içeriği yukarı taşı */
                                        main, [class*="main"], [class*="Main"] {
                                            margin-top: 0 !important;
                                            padding-top: 0 !important;
                                        }
                                        
                                        /* Container'ları düzenle */
                                        [class*="container"], [class*="Container"] {
                                            margin-top: 0 !important;
                                            padding-top: 0 !important;
                                        }
                                        
                                        /* Gradio app container'ını düzenle */
                                        #component-0, [id*="component"], [class*="gradio"] {
                                            margin-top: 0 !important;
                                            padding-top: 0 !important;
                                        }
                                        
                                        /* File upload butonlarını etkinleştir */
                                        input[type="file"], [class*="upload"], [class*="Upload"] {
                                            pointer-events: auto !important;
                                            opacity: 1 !important;
                                            visibility: visible !important;
                                            z-index: 9999 !important;
                                        }
                                        
                                        /* Butonları etkinleştir */
                                        button, [class*="button"], [class*="Button"] {
                                            pointer-events: auto !important;
                                            opacity: 1 !important;
                                            visibility: visible !important;
                                            z-index: 9999 !important;
                                        }
                                        
                                        /* Gradio interface elementlerini etkinleştir */
                                        [class*="gradio"], [id*="gradio"] {
                                            pointer-events: auto !important;
                                            opacity: 1 !important;
                                            visibility: visible !important;
                                        }
                                        
                                        /* Dinamik görünüm kuralları */
                                        body {
                                            min-width: ${if (isDesktopMode) "1024px" else "auto"} !important;
                                            width: 100% !important;
                                        }
                                        
                                        * {
                                            max-width: ${if (isDesktopMode) "none" else "100%"} !important;
                                        }
                                        
                                        ${if (isDesktopMode) {
                                            """
                                            [class*="mobile"], [class*="Mobile"] {
                                                display: none !important;
                                            }
                                            
                                            [class*="tablet"], [class*="Tablet"] {
                                                display: none !important;
                                            }
                                            """
                                        } else {
                                            """
                                            [class*="desktop"], [class*="Desktop"] {
                                                display: none !important;
                                            }
                                            """
                                        }}
                                        
                                        /* Responsive tasarımı ayarla */
                                        @media (max-width: 768px) {
                                            * {
                                                width: ${if (isDesktopMode) "auto" else "100%"} !important;
                                                min-width: ${if (isDesktopMode) "auto" else "auto"} !important;
                                            }
                                        }
                                    </style>
                                """.trimIndent()
                                
                                val js = """
                                    (function() {
                                        var style = document.createElement('style');
                                        style.innerHTML = `$css`;
                                        document.head.appendChild(style);
                                        
                                        // Açıklama: Mevcut elementleri gizle
                                        function hideElements() {
                                            var selectors = [
                                                'div.flex.items-center.justify-between.xl\\\\:min-w-0',
                                                'h1.my-2.flex.w-full.min-w-0.flex-wrap.items-center.gap-y-2.text-lg.leading-tight.xl\\\\:flex-nowrap',
                                                'div[class*="flex"][class*="items-center"][class*="justify-between"]',
                                                'header', '.header', '[class*="header"]',
                                                'nav', '.nav', '[class*="nav"]',
                                                '[class*="breadcrumb"]', '[class*="top"]', '[class*="toolbar"]',
                                                '[class*="navbar"]', '[class*="navigation"]'
                                            ];
                                            
                                            selectors.forEach(function(selector) {
                                                var elements = document.querySelectorAll(selector);
                                                elements.forEach(function(el) {
                                                    el.style.display = 'none';
                                                    el.style.visibility = 'hidden';
                                                    el.style.opacity = '0';
                                                    el.style.height = '0';
                                                    el.style.overflow = 'hidden';
                                                    el.style.position = 'absolute';
                                                    el.style.top = '-9999px';
                                                    el.style.left = '-9999px';
                                                });
                                            });
                                            
                                            // Açıklama: Gradio app container'ını yukarı taşı
                                            var gradioContainer = document.querySelector('#component-0, [id*="component"], [class*="gradio"]');
                                            if (gradioContainer) {
                                                gradioContainer.style.marginTop = '0';
                                                gradioContainer.style.paddingTop = '0';
                                            }
                                            
                                            // Açıklama: File upload butonlarını etkinleştir
                                            var uploadButtons = document.querySelectorAll('input[type="file"], [class*="upload"], [class*="Upload"]');
                                            uploadButtons.forEach(function(button) {
                                                button.style.pointerEvents = 'auto';
                                                button.style.opacity = '1';
                                                button.style.visibility = 'visible';
                                            });
                                            
                                                                                         // Açıklama: Tüm butonları etkinleştir
                                             var allButtons = document.querySelectorAll('button, [class*="button"], [class*="Button"]');
                                             allButtons.forEach(function(button) {
                                                 button.style.pointerEvents = 'auto';
                                                 button.style.opacity = '1';
                                                 button.style.visibility = 'visible';
                                             });
                                             
                                             // Açıklama: Dinamik görünüm ayarları
                                             var isDesktop = $isDesktopMode;
                                             
                                             if (isDesktop) {
                                                 document.body.style.minWidth = '1024px';
                                                 document.body.style.width = '100%';
                                                 
                                                 // Açıklama: Mobile/tablet elementlerini gizle
                                                 var mobileElements = document.querySelectorAll('[class*="mobile"], [class*="Mobile"], [class*="tablet"], [class*="Tablet"]');
                                                 mobileElements.forEach(function(el) {
                                                     el.style.display = 'none';
                                                 });
                                                 
                                                 // Açıklama: Viewport meta tag'ini güncelle
                                                 var viewport = document.querySelector('meta[name="viewport"]');
                                                 if (viewport) {
                                                     viewport.setAttribute('content', 'width=1024, initial-scale=1.0');
                                                 }
                                             } else {
                                                 document.body.style.minWidth = 'auto';
                                                 document.body.style.width = '100%';
                                                 
                                                 // Açıklama: Desktop elementlerini gizle
                                                 var desktopElements = document.querySelectorAll('[class*="desktop"], [class*="Desktop"]');
                                                 desktopElements.forEach(function(el) {
                                                     el.style.display = 'none';
                                                 });
                                                 
                                                 // Açıklama: Viewport meta tag'ini güncelle
                                                 var viewport = document.querySelector('meta[name="viewport"]');
                                                 if (viewport) {
                                                     viewport.setAttribute('content', 'width=device-width, initial-scale=1.0');
                                                 }
                                             }
                                         }
                                        }
                                        
                                        // Açıklama: Sayfa yüklendiğinde çalıştır
                                        hideElements();
                                        
                                        // Açıklama: Dinamik olarak eklenen elementleri de gizle
                                        var observer = new MutationObserver(function(mutations) {
                                            mutations.forEach(function(mutation) {
                                                mutation.addedNodes.forEach(function(node) {
                                                    if (node.nodeType === 1) {
                                                        hideElements();
                                                    }
                                                });
                                            });
                                        });
                                        
                                        observer.observe(document.body, {
                                            childList: true,
                                            subtree: true
                                        });
                                        
                                        // Açıklama: Periyodik olarak kontrol et
                                        setInterval(hideElements, 500);
                                        
                                        // Açıklama: Sayfa tamamen yüklendiğinde tekrar çalıştır
                                        setTimeout(hideElements, 2000);
                                        setTimeout(hideElements, 5000);
                                    })();
                                """.trimIndent()
                                
                                view?.evaluateJavascript(js, null)
                            }
                        }
                        
                        webView = this
                        loadUrl(decryptUrl(encryptedUrl))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = themeColors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Text(
                                text = "Sayfa Yükleniyor...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = themeColors.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Açıklama: Geri tuşu ile çıkış
    BackHandler {
        navController.popBackStack()
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AIGarage Hakkında",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = themeColors.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🚗 AI Garage - Yapay Zeka Destekli Resim Düzenleme Uygulaması",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = themeColors.primary
                    )
                    
                    Text(
                        text = "Bu uygulama, TensorFlow Lite ve Vertex AI teknolojilerini kullanarak resimlerinizi yapay zeka ile düzenlemenizi sağlar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface
                    )
                    
                    Text(
                        text = "✨ Özellikler:\n• Otomatik nesne segmentasyonu\n• AI destekli resim düzenleme\n• Çoklu tema desteği\n• Modern kullanıcı arayüzü",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface
                    )
                    
                    Text(
                        text = "🔒 Güvenlik: Tüm işlemleriniz cihazınızda güvenli şekilde gerçekleştirilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primary
                    )
                ) {
                    Text("Anladım")
                }
            },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.onSurface,
            textContentColor = themeColors.onSurface
        )
    }
}
