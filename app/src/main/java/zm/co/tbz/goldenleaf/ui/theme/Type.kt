package zm.co.tbz.goldenleaf.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale mirrored from the Golden Leaf design (design-system.jsx). The design
 * specifies Plus Jakarta Sans / Sora (geometric sans). Those font files are not
 * bundled in the project, so we render with the platform sans at the design's exact
 * weights, sizes, line-heights and letter-spacing. Swap [GlFont] to a bundled
 * FontFamily (res/font) to get pixel-identical glyphs.
 */
private val GlFont = FontFamily.Default

val Typography = Typography(
    // Big screen titles — "Sign in", "Two-factor verification" (26/800, tight)
    headlineLarge = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp,
    ),
    // 24/800
    headlineMedium = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
    ),
    // 22/800
    headlineSmall = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    // Card / section titles 18-20 / 800
    titleLarge = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp,
    ),
    // Screen header title 17/700, section header 15/700
    titleMedium = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Body 15/500
    bodyLarge = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
    // Secondary body 14/500
    bodyMedium = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Captions / metadata 12/500
    bodySmall = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp,
    ),
    // Button / strong row labels 14/700
    labelLarge = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp,
    ),
    // Pills / chips 12/600
    labelMedium = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.1.sp,
    ),
    // Tab labels / tiny uppercase 11/600
    labelSmall = TextStyle(
        fontFamily = GlFont, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp,
    ),
)
