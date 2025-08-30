// Dosya: app/src/main/java/com/rootcrack/aigarage/screens/SettingsScreen.kt
package com.rootcrack.aigarage.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rootcrack.aigarage.R
import com.rootcrack.aigarage.navigation.Screen
import com.rootcrack.aigarage.data.preferences.ThemePreferences
import com.rootcrack.aigarage.data.preferences.ThemeHelper
import com.rootcrack.aigarage.data.preferences.LanguagePreferences
import com.rootcrack.aigarage.data.preferences.LanguageManager
import com.rootcrack.aigarage.data.preferences.AppLanguage
import kotlinx.coroutines.launch

// Açıklama: Tema adı yardımcı fonksiyonu
private fun getThemeDisplayName(theme: String): String {
    return when (theme) {
        ThemePreferences.THEME_DARK_SPECIAL -> "Dark Special"
        ThemePreferences.THEME_BRIGHT_SPECIAL -> "Bright Special"
        ThemePreferences.THEME_LIGHT -> "Classic Light"
        ThemePreferences.THEME_DARK -> "Classic Dark"
        else -> "Dark Special"
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themePreference = ThemePreferences.getThemeFlow(context)
        .collectAsState(initial = ThemePreferences.THEME_DARK_SPECIAL)
    val currentTheme = themePreference.value
    val themeColors = ThemeHelper.getCurrentThemeColors(currentTheme)
    
    // Açıklama: Dil ayarları için state'ler
    val languagePreferences = LanguagePreferences(context)
    val currentLanguage = languagePreferences.getCurrentLanguage()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // Açıklama: Buton animasyonu için scale state
    var isBackButtonPressed by remember { mutableStateOf(false) }
    val backButtonScale by animateFloatAsState(
        targetValue = if (isBackButtonPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "back_button_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeHelper.getBackgroundBrush(currentTheme))
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Açıklama: Geri butonu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.scale(backButtonScale)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = themeColors.primary
                )
            }
        }
        
        Text(
            text = stringResource(R.string.settings_screen),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = themeColors.primary
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Açıklama: Tema seçimi kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showThemeDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeHelper.getSurfaceBrush(currentTheme))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Açıklama: Tema ikonu
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Icon",
                        tint = themeColors.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.theme_settings),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = themeColors.onSurface
                        )
                        Text(
                            text = getThemeDisplayName(currentTheme),
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Açıklama: Seçili tema göstergesi
                Text(
                    text = getThemeDisplayName(currentTheme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.primary
                )
            }
        }

        // Açıklama: Dil seçimi kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showLanguageDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                    .background(ThemeHelper.getSurfaceBrush(currentTheme))
                    .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Açıklama: Dil ikonu
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Icon",
                        tint = themeColors.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.language_settings),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = themeColors.onSurface
                        )
                        Text(
                            text = currentLanguage.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Açıklama: Seçili dil göstergesi
                Text(
                    text = currentLanguage.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))



        Spacer(modifier = Modifier.height(32.dp))

        // Açıklama: Animasyonlu geri butonu
        Button(
            onClick = { 
                navController.popBackStack() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .scale(backButtonScale)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    isBackButtonPressed = !isBackButtonPressed
                }
                .background(ThemeHelper.getAccentBrush(currentTheme), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColors.primary,
                contentColor = themeColors.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Geri",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
    
    // Açıklama: Tema seçim dialog'u
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.theme_settings),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = themeColors.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.selectableGroup()
        ) {
            Text(
                        text = "Seçtiğiniz tema tüm ekranlara uygulanacak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Açıklama: Tema seçenekleri
                    listOf(
                        ThemePreferences.THEME_DARK_SPECIAL,
                        ThemePreferences.THEME_BRIGHT_SPECIAL,
                        ThemePreferences.THEME_LIGHT,
                        ThemePreferences.THEME_DARK
                    ).forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (currentTheme == theme),
                                    onClick = { 
                                        coroutineScope.launch {
                                            ThemePreferences.setTheme(context, theme)
                                        }
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTheme == theme),
                                onClick = { 
                    coroutineScope.launch {
                                        ThemePreferences.setTheme(context, theme)
                                    }
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = themeColors.primary,
                                    unselectedColor = themeColors.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = getThemeDisplayName(theme),
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false }
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = themeColors.primary
                    )
                }
            }
        )
    }
    
    // Açıklama: Dil seçim dialog'u - AlertDialog kullanarak
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.language_settings),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    Text(
                        text = "Select your preferred language",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Açıklama: Dil seçenekleri
                    languagePreferences.getSupportedLanguages().forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (currentLanguage == language),
                                    onClick = { 
                                        LanguageManager.setAppLanguage(context, language.code)
                                        showLanguageDialog = false
                                        (context as? androidx.activity.ComponentActivity)?.recreate()
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentLanguage == language),
                                onClick = { 
                                    LanguageManager.setAppLanguage(context, language.code)
                                    showLanguageDialog = false
                                    (context as? androidx.activity.ComponentActivity)?.recreate()
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}