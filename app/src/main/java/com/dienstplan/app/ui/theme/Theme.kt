package com.dienstplan.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Fallback-Farbschemata für Geräte mit älterer Android-Version (unter Android 12)
private val DunklesFarbschema = darkColorScheme()
private val HellesFarbschema = lightColorScheme()

/**
 * Haupt-Theme der Dienstplan-App.
 * Unterstützt automatisch den Dark-/Light-Mode des Systems sowie
 * Material You (Dynamic Colors) ab Android 12.
 */
@Composable
fun DienstplanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DunklesFarbschema
        else -> HellesFarbschema
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
