package zm.co.tbz.goldenleaf.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GlLightColors.primary,
    onPrimary = GlLightColors.onPrimary,
    primaryContainer = GlLightColors.primarySoft,
    onPrimaryContainer = GlLightColors.primary,
    secondary = GlLightColors.gold,
    onSecondary = GlLightColors.onGold,
    secondaryContainer = GlLightColors.goldSoft,
    onSecondaryContainer = GlLightColors.goldDeep,
    tertiary = GlLightColors.gold,
    onTertiary = GlLightColors.onGold,
    background = GlLightColors.bg,
    onBackground = GlLightColors.text,
    surface = GlLightColors.surface,
    onSurface = GlLightColors.text,
    surfaceVariant = GlLightColors.surfaceAlt,
    onSurfaceVariant = GlLightColors.textMuted,
    outline = GlLightColors.outline,
    outlineVariant = GlLightColors.outlineSoft,
    error = GlLightColors.danger,
    onError = GlLightColors.onPrimary,
    errorContainer = GlLightColors.dangerSoft,
    onErrorContainer = GlLightColors.danger,
    inverseSurface = GlLightColors.primaryDeep,
    inverseOnSurface = GlLightColors.bg,
    inversePrimary = GlLightColors.gold,
)

private val DarkColorScheme = darkColorScheme(
    primary = GlDarkColors.primary,
    onPrimary = GlDarkColors.onPrimary,
    primaryContainer = GlDarkColors.primarySoft,
    onPrimaryContainer = GlDarkColors.text,
    secondary = GlDarkColors.gold,
    onSecondary = GlDarkColors.onGold,
    secondaryContainer = GlDarkColors.goldSoft,
    onSecondaryContainer = GlDarkColors.gold,
    tertiary = GlDarkColors.gold,
    onTertiary = GlDarkColors.onGold,
    background = GlDarkColors.bg,
    onBackground = GlDarkColors.text,
    surface = GlDarkColors.surface,
    onSurface = GlDarkColors.text,
    surfaceVariant = GlDarkColors.surfaceAlt,
    onSurfaceVariant = GlDarkColors.textMuted,
    outline = GlDarkColors.outline,
    outlineVariant = GlDarkColors.outlineSoft,
    error = GlDarkColors.danger,
    onError = GlDarkColors.onPrimary,
    errorContainer = GlDarkColors.dangerSoft,
    onErrorContainer = GlDarkColors.danger,
    inverseSurface = GlDarkColors.text,
    inverseOnSurface = GlDarkColors.bg,
    inversePrimary = GlDarkColors.primary,
)

@Composable
fun TBZGoldenLeafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glColors = if (darkTheme) GlDarkColors else GlLightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                GlDarkColors.bg.toArgb()
            } else {
                GlLightColors.primaryDeep.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    CompositionLocalProvider(LocalGlColors provides glColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
