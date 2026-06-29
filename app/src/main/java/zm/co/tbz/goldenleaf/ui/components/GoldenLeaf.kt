package zm.co.tbz.goldenleaf.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.exposedDropdownSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.R
import zm.co.tbz.goldenleaf.ui.theme.GlColorScheme
import zm.co.tbz.goldenleaf.ui.theme.GlStatus
import zm.co.tbz.goldenleaf.ui.theme.LocalGlColors

// ─────────────────────────────────────────────────────────────────────────────
// Golden Leaf design system — Compose component library.
// Mirrors design-system.jsx from the Claude Design handoff. Every screen builds
// from these so the app matches the design 1:1 in light and dark.
// ─────────────────────────────────────────────────────────────────────────────

/** Active design-system colour scheme. */
@Composable
@ReadOnlyComposable
fun glColors(): GlColorScheme = LocalGlColors.current

// ─────────────────────────────────────────────────────────────────────────────
// ICONS — maps the design's stroke-icon names to Material Icons Extended.
// ─────────────────────────────────────────────────────────────────────────────
fun glIconFor(name: String, filled: Boolean = false): ImageVector = when (name) {
    "home" -> if (filled) Icons.Filled.Home else Icons.Outlined.Home
    "permit", "document" -> Icons.Outlined.Description
    "inspection" -> Icons.Outlined.Search
    "profile" -> if (filled) Icons.Filled.Person else Icons.Outlined.Person
    "scan", "qr" -> Icons.Outlined.QrCodeScanner
    "users" -> Icons.Outlined.Groups
    "bale" -> if (filled) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
    "warning" -> Icons.Outlined.WarningAmber
    "gavel" -> Icons.Outlined.Gavel
    "bell" -> Icons.Outlined.Notifications
    "search" -> Icons.Outlined.Search
    "mail" -> Icons.Outlined.MailOutline
    "lock" -> Icons.Outlined.Lock
    "eye" -> Icons.Outlined.Visibility
    "eye-off" -> Icons.Outlined.VisibilityOff
    "shield" -> Icons.Outlined.Shield
    "shield-check" -> Icons.Outlined.VerifiedUser
    "arrow-left" -> Icons.AutoMirrored.Filled.ArrowBack
    "arrow-right" -> Icons.AutoMirrored.Filled.ArrowForward
    "chevron-right" -> Icons.Outlined.ChevronRight
    "chevron-down" -> Icons.Outlined.KeyboardArrowDown
    "clock" -> Icons.Outlined.Schedule
    "map-pin", "gps" -> Icons.Outlined.Place
    "sync", "flip" -> Icons.Outlined.Sync
    "cloud-up" -> Icons.Outlined.CloudUpload
    "cloud" -> Icons.Outlined.Cloud
    "calendar" -> Icons.Outlined.CalendarMonth
    "info" -> Icons.Outlined.Info
    "sun" -> Icons.Outlined.LightMode
    "moon" -> Icons.Outlined.DarkMode
    "logout" -> Icons.AutoMirrored.Filled.Logout
    "sliders" -> Icons.Outlined.Tune
    "share" -> Icons.Outlined.Share
    "check" -> Icons.Outlined.Check
    "check-circle" -> Icons.Outlined.CheckCircle
    "clipboard" -> Icons.Outlined.Assignment
    "truck" -> Icons.Outlined.LocalShipping
    "help" -> Icons.AutoMirrored.Outlined.HelpOutline
    "camera" -> Icons.Outlined.CameraAlt
    "image" -> Icons.Outlined.Image
    "edit" -> Icons.Outlined.Edit
    "settings" -> Icons.Outlined.Settings
    "menu" -> if (filled) Icons.Filled.Menu else Icons.Outlined.Menu
    "leaf", "plant" -> Icons.Outlined.Eco
    "plus" -> Icons.Outlined.Add
    "x" -> Icons.Outlined.Close
    "more" -> Icons.Outlined.MoreVert
    "building" -> Icons.Outlined.Apartment
    "badge" -> Icons.Outlined.Assignment
    "flash" -> Icons.Outlined.Tune
    else -> Icons.Outlined.Circle
}

@Composable
fun GlIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
) {
    Icon(
        imageVector = glIconFor(name, filled),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

private val Mono = FontFamily.Monospace

// ─────────────────────────────────────────────────────────────────────────────
// PILL / STATUS CHIP
// ─────────────────────────────────────────────────────────────────────────────
enum class GlTone { Default, Primary, Gold, Success, Warning, Danger, Info, Active, Pending, Failed, Draft, Synced, Review, Returned, Blocked, Outline }
enum class GlPillSize { Sm, Md, Lg }

@Composable
private fun toneColors(tone: GlTone): GlStatus {
    val c = glColors()
    return when (tone) {
        GlTone.Default -> GlStatus(c.surfaceAlt, c.textMuted)
        GlTone.Primary -> GlStatus(c.primarySoft, c.primary)
        GlTone.Gold -> GlStatus(c.goldSoft, c.goldDeep)
        GlTone.Success -> GlStatus(c.successSoft, c.success)
        GlTone.Warning -> GlStatus(c.warningSoft, c.goldDeep)
        GlTone.Danger -> GlStatus(c.dangerSoft, c.danger)
        GlTone.Info -> GlStatus(c.infoSoft, c.info)
        GlTone.Active -> c.statusActive
        GlTone.Pending -> c.statusPending
        GlTone.Failed -> c.statusFailed
        GlTone.Draft -> c.statusDraft
        GlTone.Synced -> c.statusSynced
        GlTone.Review -> c.statusReview
        GlTone.Returned -> c.statusReturned
        GlTone.Blocked -> c.statusBlocked
        GlTone.Outline -> GlStatus(Color.Transparent, c.text)
    }
}

@Composable
fun GlPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: GlTone = GlTone.Default,
    size: GlPillSize = GlPillSize.Md,
    leadingIcon: String? = null,
) {
    val c = glColors()
    val t = toneColors(tone)
    val (fs, vpad, hpad, h) = when (size) {
        GlPillSize.Sm -> arrayOf(11, 3, 8, 22)
        GlPillSize.Md -> arrayOf(12, 4, 10, 26)
        GlPillSize.Lg -> arrayOf(13, 6, 12, 30)
    }
    Row(
        modifier = modifier
            .heightIn(min = (h as Int).dp)
            .clip(CircleShape)
            .background(t.bg)
            .then(if (tone == GlTone.Outline) Modifier.border(1.dp, c.outline, CircleShape) else Modifier)
            .padding(horizontal = (hpad as Int).dp, vertical = (vpad as Int).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leadingIcon != null) GlIcon(leadingIcon, size = ((fs as Int) + 1).dp, tint = t.fg)
        Text(
            text = text,
            color = t.fg,
            fontSize = (fs as Int).sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BUTTON
// ─────────────────────────────────────────────────────────────────────────────
enum class GlButtonVariant { Primary, Gold, Secondary, Outline, Ghost, Danger, DangerOutline, Surface }
enum class GlButtonSize { Sm, Md, Lg, Xl }

@Composable
fun GlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlButtonVariant = GlButtonVariant.Primary,
    size: GlButtonSize = GlButtonSize.Lg,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
) {
    val c = glColors()
    val (h, fs, hpad) = when (size) {
        GlButtonSize.Sm -> Triple(36, 13, 16)
        GlButtonSize.Md -> Triple(44, 14, 20)
        GlButtonSize.Lg -> Triple(56, 15, 24)
        GlButtonSize.Xl -> Triple(64, 16, 28)
    }
    val bg: Color
    val fg: Color
    var border: BorderStroke? = null
    when (variant) {
        GlButtonVariant.Primary -> { bg = c.primary; fg = c.onPrimary }
        GlButtonVariant.Gold -> { bg = c.gold; fg = c.onGold }
        GlButtonVariant.Secondary -> { bg = c.primarySoft; fg = c.primary }
        GlButtonVariant.Outline -> { bg = Color.Transparent; fg = c.primary; border = BorderStroke(1.5.dp, c.primary) }
        GlButtonVariant.Ghost -> { bg = Color.Transparent; fg = c.text }
        GlButtonVariant.Danger -> { bg = c.danger; fg = Color.White }
        GlButtonVariant.DangerOutline -> { bg = Color.Transparent; fg = c.danger; border = BorderStroke(1.5.dp, c.danger) }
        GlButtonVariant.Surface -> { bg = c.surface; fg = c.text; border = BorderStroke(1.dp, c.outline) }
    }
    val shape = CircleShape
    Row(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(h.dp)
            .then(
                if (variant == GlButtonVariant.Primary && enabled)
                    Modifier.shadow(8.dp, shape, ambientColor = c.primary, spotColor = c.primary)
                else Modifier,
            )
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = 0.5f))
            .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = hpad.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor = if (enabled) fg else fg.copy(alpha = 0.7f)
        if (leadingIcon != null) {
            GlIcon(leadingIcon, size = (fs + 4).dp, tint = contentColor)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, fontSize = fs.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            GlIcon(trailingIcon, size = (fs + 4).dp, tint = contentColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TEXT FIELD (pill-shaped, label above, surfaceAlt fill)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    helper: String? = null,
    error: String? = null,
    required: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    val c = glColors()
    var focused by remember { mutableStateOf(false) }
    val multiline = !singleLine || minLines > 1
    val borderColor = when {
        error != null -> c.danger
        focused -> c.primary
        else -> Color.Transparent
    }
    val shape = if (multiline) RoundedCornerShape(20.dp) else RoundedCornerShape(28.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Row(modifier = Modifier.padding(start = 4.dp)) {
                Text(label, color = c.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (required) Text(" *", color = c.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (multiline) Modifier.heightIn(min = 96.dp) else Modifier.height(56.dp))
                .clip(shape)
                .background(c.surfaceAlt)
                .border(1.5.dp, borderColor, shape)
                .padding(horizontal = 18.dp, vertical = if (multiline) 14.dp else 0.dp),
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon != null) GlIcon(leadingIcon, size = 20.dp, tint = c.textMuted)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = TextStyle(color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.primary),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
                )
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = c.placeholder, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (trailing != null) trailing()
        }
        if (error != null || helper != null) {
            Text(
                text = error ?: helper.orEmpty(),
                color = if (error != null) c.danger else c.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** Dropdown/select field styled to match [GlTextField]. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlDropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select",
    required: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
) {
    val c = glColors()
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second
    val borderColor = if (error != null) c.danger else Color.Transparent
    val shape = RoundedCornerShape(28.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.padding(start = 4.dp)) {
            Text(label, color = c.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (required) Text(" *", color = c.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        androidx.compose.material3.ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it },
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(c.surfaceAlt)
                    .border(1.5.dp, borderColor, shape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = selectedLabel ?: placeholder,
                        color = if (selectedLabel != null) c.text else c.placeholder,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                GlIcon("chevron-down", size = 20.dp, tint = c.textMuted)
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(),
            ) {
                options.forEach { (id, name) ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = c.danger, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CARD
// ─────────────────────────────────────────────────────────────────────────────
enum class GlAccent { None, Primary, Gold }

@Composable
fun GlCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevated: Boolean = false,
    accent: GlAccent = GlAccent.None,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val c = glColors()
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(10.dp, shape, spotColor = Color(0x29183219)) else Modifier)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outlineSoft, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(IntrinsicSize.Min),
    ) {
        if (accent != GlAccent.None) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(if (accent == GlAccent.Gold) c.gold else c.primary),
            )
        }
        Box(modifier = Modifier.weight(1f).padding(contentPadding)) { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AVATAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, gold: Boolean = false) {
    val c = glColors()
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (gold) c.gold else c.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = if (gold) c.onGold else c.onPrimary,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN HEADER (in-content app bar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    actions: @Composable (() -> Unit)? = null,
) {
    val c = glColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceAlt)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { GlIcon("arrow-left", size = 20.dp, tint = c.text) }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = c.text,
                fontSize = if (large) 22.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, color = c.textMuted, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (actions != null) actions()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = glColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                color = c.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KPI TILE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlKpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: GlTone = GlTone.Default,
    icon: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = glColors()
    val bg: Color
    val fg: Color
    val accent: Color
    when (tone) {
        GlTone.Primary -> { bg = c.primarySoft; fg = c.primary; accent = c.primary }
        GlTone.Gold -> { bg = c.goldSoft; fg = c.goldDeep; accent = c.gold }
        GlTone.Danger -> { bg = c.dangerSoft; fg = c.danger; accent = c.danger }
        GlTone.Success -> { bg = c.successSoft; fg = c.success; accent = c.success }
        GlTone.Info -> { bg = c.infoSoft; fg = c.info; accent = c.info }
        else -> { bg = c.surface; fg = c.text; accent = c.primary }
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(if (tone == GlTone.Default) Modifier.border(1.dp, c.outlineSoft, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            GlIcon(icon, size = 22.dp, tint = accent)
            Spacer(Modifier.height(6.dp))
        }
        Text(value, color = fg, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            label.uppercase(),
            color = if (tone == GlTone.Default) c.textMuted else fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: String? = null,
    tone: GlTone = GlTone.Default,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = glColors()
    val titleColor = if (danger) c.danger else c.text
    val iconBg: Color
    val iconFg: Color
    when (tone) {
        GlTone.Gold -> { iconBg = c.goldSoft; iconFg = c.goldDeep }
        GlTone.Danger -> { iconBg = c.dangerSoft; iconFg = c.danger }
        GlTone.Primary -> { iconBg = c.primarySoft; iconFg = c.primary }
        else -> { iconBg = c.surfaceAlt; iconFg = c.textMuted }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) { GlIcon(leadingIcon, size = 20.dp, tint = iconFg) }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle, color = c.textMuted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null) GlIcon("chevron-right", size = 18.dp, tint = c.textSubtle)
    }
}

/** Hairline divider used inside grouped cards. */
@Composable
fun GlDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(glColors().outlineSoft))
}

// ─────────────────────────────────────────────────────────────────────────────
// FILTER PILLS (horizontally scrollable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlFilterPills(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) c.primary else c.surfaceAlt)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    opt,
                    color = if (active) c.onPrimary else c.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    onScan: (() -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = glColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(c.surfaceAlt)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlIcon("search", size = 20.dp, tint = c.textMuted)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !readOnly && onClick == null,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.primary),
            )
            if (value.isEmpty()) {
                Text(placeholder, color = c.textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (onScan != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center,
            ) { GlIcon("qr", size = 16.dp, tint = c.onPrimary) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOGGLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = glColors()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = c.primary,
            checkedBorderColor = c.primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = c.outline,
            uncheckedBorderColor = c.outline,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BANNER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: GlTone = GlTone.Info,
    icon: String? = "info",
) {
    val c = glColors()
    val t = toneColors(tone)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(t.bg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) GlIcon(icon, size = 18.dp, tint = t.fg)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = t.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = t.fg.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: String = "info",
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val c = glColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(c.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) { GlIcon(icon, size = 32.dp, tint = c.textMuted) }
        Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, color = c.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (action != null) action()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SYNC CHIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlSyncChip(status: String, modifier: Modifier = Modifier, count: Int = 0) {
    val c = glColors()
    data class Cfg(val bg: Color, val fg: Color, val dot: Color, val label: String)
    val cfg = when (status) {
        "synced" -> Cfg(c.successSoft, c.success, c.success, "Synced")
        "pending" -> Cfg(c.goldSoft, c.goldDeep, c.gold, if (count > 0) "$count pending" else "Pending")
        "offline" -> Cfg(c.surfaceAlt, c.textMuted, c.textSubtle, "Offline")
        "syncing" -> Cfg(c.infoSoft, c.info, c.info, "Syncing…")
        "failed" -> Cfg(c.dangerSoft, c.danger, c.danger, "Failed")
        else -> Cfg(c.surfaceAlt, c.textMuted, c.textMuted, status)
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(cfg.bg)
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(cfg.dot))
        Text(cfg.label, color = cfg.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEPPER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlStepper(step: Int, total: Int, modifier: Modifier = Modifier, label: String? = null) {
    val c = glColors()
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i < step) c.primary else c.outline),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append("STEP $step OF $total")
                if (label != null) append(" · ${label.uppercase()}")
            },
            color = c.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FIELD ROW (label / value)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlFieldRow(label: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    val c = glColors()
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = c.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            fontFamily = if (mono) Mono else FontFamily.Default,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TBZ LOGO MARK + LOCKUP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TbzMark(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Icon(
        painter = painterResource(R.drawable.icon_tbz),
        contentDescription = "Tobacco Board of Zambia",
        tint = Color.Unspecified,
        modifier = modifier.size(size),
    )
}

@Composable
fun TbzLockup(modifier: Modifier = Modifier, markSize: Dp = 44.dp) {
    val c = glColors()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TbzMark(size = markSize)
        Column {
            Text("TOBACCO BOARD OF ZAMBIA", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
            Text("Tobacco, Our Green Gold", color = c.gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAVIGATION — pill tab bar with centre scan FAB
// ─────────────────────────────────────────────────────────────────────────────
enum class GlTab(val icon: String, val label: String) {
    Home("home", "Home"),
    Permits("permit", "Permits"),
    Inspection("inspection", "Inspect"),
    Profile("profile", "Profile"),
}

@Composable
fun GlBottomNav(
    active: GlTab,
    onSelect: (GlTab) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlBottomTab(GlTab.Home, active, onSelect, Modifier.weight(1f))
        GlBottomTab(GlTab.Permits, active, onSelect, Modifier.weight(1f))
        // centre scan FAB
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center,
            ) { GlIcon("scan", size = 26.dp, tint = c.onPrimary) }
        }
        GlBottomTab(GlTab.Inspection, active, onSelect, Modifier.weight(1f))
        GlBottomTab(GlTab.Profile, active, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun GlBottomTab(tab: GlTab, active: GlTab, onSelect: (GlTab) -> Unit, modifier: Modifier = Modifier) {
    val c = glColors()
    val isActive = tab == active
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        GlIcon(tab.icon, size = 22.dp, tint = if (isActive) c.primary else c.textSubtle, filled = isActive)
        Text(tab.label, color = if (isActive) c.primary else c.textSubtle, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
