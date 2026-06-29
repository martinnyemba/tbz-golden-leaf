package zm.co.tbz.goldenleaf.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw TBZ brand palette (OKLCH source of truth, June 2026).
 * Kept for backwards compatibility — existing screens reference these directly.
 */
object TbzColors {
    val WarmIvory = Color(0xFFF6F4E6)
    val WarmBeige = Color(0xFFF0E7D3)
    val GoldenBrown = Color(0xFF7E5709)
    val LeafGreen = Color(0xFF26471A)
    val DeepLeafGreen = Color(0xFF06320C)
    val HarvestGold = Color(0xFFFFC039)
    val WarmCharcoal = Color(0xFF151107)
}

/** A status chip colour pair (background + foreground). */
@Immutable
data class GlStatus(val bg: Color, val fg: Color)

/**
 * The Golden Leaf design-system colour scheme. This is the Compose mirror of the
 * `GL_TOKENS` object from the Claude Design handoff (design-system.jsx). Every
 * reusable component and screen reads colours from here via [LocalGlColors] so the
 * app matches the design 1:1 in both light and dark.
 */
@Immutable
data class GlColorScheme(
    // Surfaces
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceMuted: Color,
    // Text
    val text: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val placeholder: Color,
    // Brand primary — Leaf Green
    val primary: Color,
    val primarySoft: Color,
    val primaryDeep: Color,
    // Gold accent — Harvest Gold
    val gold: Color,
    val goldSoft: Color,
    val goldDeep: Color,
    // Semantic
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    // Strokes
    val outline: Color,
    val outlineSoft: Color,
    // Risk
    val riskHigh: Color,
    val riskMed: Color,
    val riskLow: Color,
    // On-accent text (text drawn on top of primary / gold fills)
    val onPrimary: Color,
    val onGold: Color,
    // Status chips
    val statusActive: GlStatus,
    val statusPending: GlStatus,
    val statusFailed: GlStatus,
    val statusDraft: GlStatus,
    val statusSynced: GlStatus,
    val statusReview: GlStatus,
    val statusReturned: GlStatus,
    val statusBlocked: GlStatus,
    val isDark: Boolean,
)

val GlLightColors = GlColorScheme(
    bg = Color(0xFFF6F4E6),
    surface = Color(0xFFF6F4E6),
    surfaceAlt = Color(0xFFF0E7D3),
    surfaceMuted = Color(0xFFE8DCCA),
    text = Color(0xFF151107),
    textMuted = Color(0xFF7E5709),
    textSubtle = Color(0xFFB08040),
    placeholder = Color(0xFFC4A06A),
    primary = Color(0xFF26471A),
    primarySoft = Color(0xFFDAE8D3),
    primaryDeep = Color(0xFF06320C),
    gold = Color(0xFFFFC039),
    goldSoft = Color(0xFFFFF0C8),
    goldDeep = Color(0xFF7E5709),
    success = Color(0xFF2E6B20),
    successSoft = Color(0xFFDFF2D8),
    warning = Color(0xFFFFC039),
    warningSoft = Color(0xFFFFF0C8),
    danger = Color(0xFFB33A3A),
    dangerSoft = Color(0xFFF4D9D9),
    info = Color(0xFF2A5FA8),
    infoSoft = Color(0xFFD9E8F8),
    outline = Color(0xFFD4C0A0),
    outlineSoft = Color(0xFFEDE4D0),
    riskHigh = Color(0xFFB33A3A),
    riskMed = Color(0xFFFFC039),
    riskLow = Color(0xFF2E6B20),
    onPrimary = Color(0xFFFFFFFF),
    onGold = Color(0xFF1A1308),
    statusActive = GlStatus(Color(0xFFDFF2D8), Color(0xFF26471A)),
    statusPending = GlStatus(Color(0xFFF0E7D3), Color(0xFF7E5709)),
    statusFailed = GlStatus(Color(0xFFF4D9D9), Color(0xFF8E2A2A)),
    statusDraft = GlStatus(Color(0xFFEEEBE0), Color(0xFF7E5709)),
    statusSynced = GlStatus(Color(0xFFDFF2D8), Color(0xFF26471A)),
    statusReview = GlStatus(Color(0xFFFFF0C8), Color(0xFF7E5709)),
    statusReturned = GlStatus(Color(0xFFFFF0C8), Color(0xFF7E5709)),
    statusBlocked = GlStatus(Color(0xFFF0E7D3), Color(0xFFB33A3A)),
    isDark = false,
)

val GlDarkColors = GlColorScheme(
    bg = Color(0xFF151107),
    surface = Color(0xFF06320C),
    surfaceAlt = Color(0xFF26471A),
    surfaceMuted = Color(0xFF1A3D13),
    text = Color(0xFFF6F4E6),
    textMuted = Color(0xFFF0E7D3),
    textSubtle = Color(0xFFC4B898),
    placeholder = Color(0xFF8A7A5A),
    primary = Color(0xFFFFC039),
    primarySoft = Color(0xFF26471A),
    primaryDeep = Color(0xFFFFC039),
    gold = Color(0xFFFFC039),
    goldSoft = Color(0xFF3A2E10),
    goldDeep = Color(0xFFFFC039),
    success = Color(0xFF7DD49C),
    successSoft = Color(0xFF1A3A28),
    warning = Color(0xFFFFC039),
    warningSoft = Color(0xFF3A2E10),
    danger = Color(0xFFE07070),
    dangerSoft = Color(0xFF3A1A1A),
    info = Color(0xFF7AB0E0),
    infoSoft = Color(0xFF1A2A3A),
    outline = Color(0xFF3A4A2A),
    outlineSoft = Color(0xFF1F2E1A),
    riskHigh = Color(0xFFE07070),
    riskMed = Color(0xFFFFC039),
    riskLow = Color(0xFF7DD49C),
    onPrimary = Color(0xFF151107),
    onGold = Color(0xFF1A1308),
    statusActive = GlStatus(Color(0xFF1A3A28), Color(0xFF7DD49C)),
    statusPending = GlStatus(Color(0xFF26471A), Color(0xFFF0E7D3)),
    statusFailed = GlStatus(Color(0xFF3A1A1A), Color(0xFFE07070)),
    statusDraft = GlStatus(Color(0xFF1F2A1A), Color(0xFFC4B898)),
    statusSynced = GlStatus(Color(0xFF1A3A28), Color(0xFF7DD49C)),
    statusReview = GlStatus(Color(0xFF3A2E10), Color(0xFFFFC039)),
    statusReturned = GlStatus(Color(0xFF3A2E10), Color(0xFFFFC039)),
    statusBlocked = GlStatus(Color(0xFF3A1A1A), Color(0xFFE07070)),
    isDark = true,
)

/** Provides the active [GlColorScheme] to the composition. Set in [TBZGoldenLeafTheme]. */
val LocalGlColors = staticCompositionLocalOf { GlLightColors }
