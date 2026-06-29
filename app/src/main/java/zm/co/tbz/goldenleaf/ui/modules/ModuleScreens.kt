package zm.co.tbz.goldenleaf.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.search.GlobalSearchScreen

/** 06 · Module menu — bottom-sheet overlay from the handoff. */
@Composable
fun MoreOptionsSheet(
    pendingSyncCount: Int,
    onSyncSettings: () -> Unit,
    onAbout: () -> Unit,
    onClose: () -> Unit,
) {
    val c = glColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8C0F1E12))
            .clickable(onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.surface)
                .clickable(enabled = false, onClick = {})
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.outline)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "More options",
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
            GlCard(contentPadding = 4.dp) {
                Column {
                    GlRow(
                        title = "Sync settings",
                        subtitle = if (pendingSyncCount > 0) "$pendingSyncCount records pending" else "Offline queue & manual sync",
                        leadingIcon = "sync",
                        tone = GlTone.Primary,
                        onClick = onSyncSettings,
                    )
                    GlDivider()
                    GlRow(
                        title = "Help & support",
                        subtitle = "Contact TBZ field operations",
                        leadingIcon = "help",
                    )
                    GlDivider()
                    GlRow(
                        title = "About application",
                        leadingIcon = "info",
                        onClick = onAbout,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            GlButton(text = "Close", onClick = onClose, variant = GlButtonVariant.Ghost)
        }
    }
}

@Composable
fun SearchScreen(
    onOpenGrower: (String) -> Unit = {},
    onOpenPermit: (String) -> Unit = {},
) {
    GlobalSearchScreen(
        onOpenGrower = onOpenGrower,
        onOpenPermit = onOpenPermit,
    )
}

@Composable
fun StaticContentScreen(title: String, body: String) {
    val c = glColors()
    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = title)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(body, color = c.text, fontSize = 14.sp)
            }
        }
    }
}
