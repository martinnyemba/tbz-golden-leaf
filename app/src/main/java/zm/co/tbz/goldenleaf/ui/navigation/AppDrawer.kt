package zm.co.tbz.goldenleaf.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlTab
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import zm.co.tbz.goldenleaf.ui.components.toTitleCase

private data class DrawerEntry(
    val label: String,
    val icon: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val badge: Int? = null,
    val danger: Boolean = false,
)

/**
 * Body of the app navigation drawer. Module rows are gated by [access] (the same
 * RBAC checks the bottom nav / dashboard use), tab rows switch the bottom-nav
 * selection, and everything else navigates by route. The caller is responsible
 * for closing the drawer in the callbacks.
 */
@Composable
fun GlDrawerContent(
    profileName: String,
    profileEmail: String,
    roles: List<String>,
    access: MenuAccessState,
    activeTab: GlTab,
    pendingSyncCount: Int,
    onClose: () -> Unit,
    onSelectTab: (GlTab) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val c = glColors()
    Column(
        Modifier
            .fillMaxHeight()
            .background(c.surface)
            .glVerticalScroll(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(c.primaryDeep, c.primary)))
                .padding(20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlAvatar(name = profileName, size = 56.dp, gold = true)
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Text(profileName.toTitleCase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    if (profileEmail.isNotBlank()) {
                        Text(profileEmail, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(4.dp),
                ) {
                    GlIcon("x", size = 24.dp, tint = Color.White)
                }
            }
            if (roles.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.take(3).forEach { role -> GlPill(text = role.toTitleCase(), tone = GlTone.Gold, size = GlPillSize.Sm) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        DrawerSection("Modules")
        DrawerItem(DrawerEntry("Home", "home", { onSelectTab(GlTab.Home) }, active = activeTab == GlTab.Home))
        if (access.canRegistration) {
            DrawerItem(DrawerEntry("Registration", "users", { onNavigate(Routes.REGISTRATION) }))
        }
        DrawerItem(DrawerEntry("Permits", "permit", { onSelectTab(GlTab.Permits) }, active = activeTab == GlTab.Permits))
        if (access.canInspection) {
            DrawerItem(DrawerEntry("Inspections", "inspection", { onSelectTab(GlTab.Inspection) }, active = activeTab == GlTab.Inspection))
        }
        if (access.canMarketing) {
            DrawerItem(DrawerEntry("Marketing / Sales", "bale", { onNavigate(Routes.MARKETING) }))
        }
        if (access.canArbitration) {
            DrawerItem(DrawerEntry("Arbitration", "gavel", { onNavigate(Routes.ARBITRATION) }))
        }

        GlDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        DrawerSection("Workspace")
        DrawerItem(DrawerEntry("Corrections", "warning", { onNavigate(Routes.CORRECTIONS) }))
        DrawerItem(DrawerEntry("Notifications", "bell", { onNavigate(Routes.NOTIFICATIONS) }))
        DrawerItem(DrawerEntry("Search", "search", { onNavigate(Routes.SEARCH) }))
        DrawerItem(
            DrawerEntry(
                "Sync settings",
                "sync",
                { onNavigate(Routes.SYNC_SETTINGS) },
                badge = pendingSyncCount.takeIf { it > 0 },
            ),
        )

        GlDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        DrawerSection("Account")
        DrawerItem(DrawerEntry("Profile", "profile", { onSelectTab(GlTab.Profile) }, active = activeTab == GlTab.Profile))
        DrawerItem(DrawerEntry("Change password", "lock", { onNavigate(Routes.CHANGE_PASSWORD) }))
        DrawerItem(DrawerEntry("About", "info", { onNavigate(Routes.ABOUT) }))
        DrawerItem(DrawerEntry("Log out", "logout", onLogout, danger = true))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        title.uppercase(),
        color = glColors().textSubtle,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 26.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerItem(entry: DrawerEntry) {
    val c = glColors()
    val background = if (entry.active) c.primarySoft else Color.Transparent
    val tint = when {
        entry.danger -> c.danger
        entry.active -> c.primary
        else -> c.textMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlIcon(entry.icon, size = 22.dp, tint = tint)
        Text(
            entry.label,
            color = if (entry.danger) c.danger else c.text,
            fontSize = 15.sp,
            fontWeight = if (entry.active) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        entry.badge?.let { badge ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.gold)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.toString(), color = c.onGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
