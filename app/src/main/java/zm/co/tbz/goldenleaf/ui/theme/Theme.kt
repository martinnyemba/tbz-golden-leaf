package zm.co.tbz.goldenleaf.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TbzColors.LeafGreen,
    onPrimary = TbzColors.WarmIvory,
    primaryContainer = TbzColors.WarmBeige,
    onPrimaryContainer = TbzColors.WarmCharcoal,
    secondary = TbzColors.GoldenBrown,
    onSecondary = TbzColors.WarmIvory,
    tertiary = TbzColors.HarvestGold,
    onTertiary = TbzColors.WarmCharcoal,
    background = TbzColors.WarmIvory,
    onBackground = TbzColors.WarmCharcoal,
    surface = TbzColors.WarmIvory,
    onSurface = TbzColors.WarmCharcoal,
    surfaceVariant = TbzColors.WarmBeige,
    onSurfaceVariant = TbzColors.GoldenBrown,
    outline = TbzColors.GoldenBrown.copy(alpha = 0.5f),
    inverseSurface = TbzColors.DeepLeafGreen,
    inverseOnSurface = TbzColors.WarmIvory,
    inversePrimary = TbzColors.HarvestGold,
)

private val DarkColorScheme = darkColorScheme(
    primary = TbzColors.HarvestGold,
    onPrimary = TbzColors.WarmCharcoal,
    primaryContainer = TbzColors.LeafGreen,
    onPrimaryContainer = TbzColors.WarmIvory,
    secondary = TbzColors.GoldenBrown,
    onSecondary = TbzColors.WarmIvory,
    tertiary = TbzColors.HarvestGold,
    onTertiary = TbzColors.WarmCharcoal,
    background = TbzColors.WarmCharcoal,
    onBackground = TbzColors.WarmIvory,
    surface = TbzColors.DeepLeafGreen,
    onSurface = TbzColors.WarmIvory,
    surfaceVariant = TbzColors.LeafGreen,
    onSurfaceVariant = TbzColors.WarmBeige,
    outline = TbzColors.GoldenBrown,
    inverseSurface = TbzColors.WarmIvory,
    inverseOnSurface = TbzColors.WarmCharcoal,
    inversePrimary = TbzColors.LeafGreen,
)

@Composable
fun TBZGoldenLeafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                TbzColors.WarmCharcoal.toArgb()
            } else {
                TbzColors.DeepLeafGreen.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
