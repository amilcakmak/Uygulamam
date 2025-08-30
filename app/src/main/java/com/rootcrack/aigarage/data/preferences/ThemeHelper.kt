// Dosya: app/src/main/java/com/rootcrack/aigarage/data/preferences/ThemeHelper.kt
package com.rootcrack.aigarage.data.preferences

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Açıklama: Tema sabitleri (ThemePreferences'dan alınacak)

// Açıklama: Tema renk paletleri
data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onPrimary: Color,
    val accent: Color,
    val error: Color,
    val backgroundGradient: Brush,
    val surfaceGradient: Brush,
    val accentGradient: Brush
)

object ThemeHelper {
    
    // Açıklama: Dark Special tema (mevcut tema)
    private val darkSpecialColors = ThemeColors(
        primary = Color(0xFF00FF88),
        secondary = Color(0xFF16213E),
        background = Color(0xFF1A1A2E),
        surface = Color(0xFF16213E),
        onSurface = Color.White,
        onPrimary = Color.Black,
        accent = Color(0xFF00FF88),
        error = Color(0xFFFF6B6B),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1A2E),
                Color(0xFF16213E),
                Color(0xFF0F172A)
            )
        ),
        surfaceGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF16213E),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        ),
        accentGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF00FF88),
                Color(0xFF00E676),
                Color(0xFF4CAF50)
            )
        )
    )
    
    // Açıklama: Bright Special tema (aydınlık neon tema)
    private val brightSpecialColors = ThemeColors(
        primary = Color(0xFF6200EA),
        secondary = Color(0xFFE8EAF6),
        background = Color(0xFFF3E5F5),
        surface = Color(0xFFE8EAF6),
        onSurface = Color(0xFF1A237E),
        onPrimary = Color.White,
        accent = Color(0xFF6200EA),
        error = Color(0xFFD32F2F),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3E5F5),
                Color(0xFFE8EAF6),
                Color(0xFFE1F5FE),
                Color(0xFFF0F4C3)
            )
        ),
        surfaceGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8EAF6),
                Color(0xFFE1F5FE),
                Color(0xFFF0F4C3)
            )
        ),
        accentGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF6200EA),
                Color(0xFF3F51B5),
                Color(0xFF2196F3),
                Color(0xFF009688)
            )
        )
    )
    
    // Açıklama: Classic Light tema (sade beyaz)
    private val classicLightColors = ThemeColors(
        primary = Color(0xFF6200EA),
        secondary = Color(0xFFF5F5F5),
        background = Color.White,
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF212121),
        onPrimary = Color.White,
        accent = Color(0xFF6200EA),
        error = Color(0xFFD32F2F),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFAFAFA),
                Color(0xFFF5F5F5),
                Color(0xFFEEEEEE)
            )
        ),
        surfaceGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAFAFA),
                Color(0xFFF5F5F5),
                Color(0xFFEEEEEE)
            )
        ),
        accentGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF6200EA),
                Color(0xFF7C4DFF),
                Color(0xFF536DFE)
            )
        )
    )
    
    // Açıklama: Classic Dark tema (sade siyah)
    private val classicDarkColors = ThemeColors(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF3700B3),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE0E0E0),
        onPrimary = Color.Black,
        accent = Color(0xFFBB86FC),
        error = Color(0xFFCF6679),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF121212),
                Color(0xFF1E1E1E),
                Color(0xFF2C2C2C),
                Color(0xFF3C3C3C)
            )
        ),
        surfaceGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF2C2C2C),
                Color(0xFF3C3C3C)
            )
        ),
        accentGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFBB86FC),
                Color(0xFF7C4DFF),
                Color(0xFF536DFE)
            )
        )
    )
    
    // Açıklama: Tema renklerini al
    fun getCurrentThemeColors(theme: String): ThemeColors {
        return when (theme) {
            "dark_special" -> darkSpecialColors
            "bright_special" -> brightSpecialColors
            "light" -> classicLightColors
            "dark" -> classicDarkColors
            else -> darkSpecialColors
        }
    }
    
    // Açıklama: Arka plan gradyanını al
    fun getBackgroundBrush(theme: String): Brush {
        return getCurrentThemeColors(theme).backgroundGradient
    }
    
    // Açıklama: Surface gradyanını al
    fun getSurfaceBrush(theme: String): Brush {
        return getCurrentThemeColors(theme).surfaceGradient
    }
    
    // Açıklama: Accent gradyanını al
    fun getAccentBrush(theme: String): Brush {
        return getCurrentThemeColors(theme).accentGradient
    }
    
    // Açıklama: Tema adlarını al
    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            "dark_special" -> "Dark Special"
            "bright_special" -> "Bright Special"
            "light" -> "Classic Light"
            "dark" -> "Classic Dark"
            else -> "Dark Special"
        }
    }
    
    // Açıklama: Tüm temalar listesi
    fun getAllThemes(): List<String> {
        return listOf(
            "dark_special",
            "bright_special",
            "light",
            "dark"
        )
    }
}
