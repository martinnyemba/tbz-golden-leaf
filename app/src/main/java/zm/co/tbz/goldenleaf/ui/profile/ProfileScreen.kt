package zm.co.tbz.goldenleaf.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.GlToggle
import zm.co.tbz.goldenleaf.ui.components.glColors
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    val profile = profileRepository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val prefs = userPreferences.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }
}

@Composable
fun ProfileScreen(
    onChangePassword: () -> Unit,
    onAbout: () -> Unit,
    onNotifications: () -> Unit = {},
    onSyncSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val c = glColors()
    val isDark = prefs?.darkTheme == true

    GlScaffold(containerColor = c.bg, modifier = modifier) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).glVerticalScroll(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(c.primaryDeep, c.primary)))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlAvatar(name = profile?.full_name ?: "?", size = 72.dp, gold = true)
                Spacer(Modifier.height(12.dp))
                Text(
                    profile?.full_name ?: "Not signed in",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    profile?.email.orEmpty(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
                val roles = profile?.roles.orEmpty()
                if (roles.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roles.forEach { role -> GlPill(text = role, tone = GlTone.Gold) }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlSectionHeader(title = "Preferences")
                GlCard {
                    Column {
                        GlRow(
                            title = "Dark theme",
                            leadingIcon = if (isDark) "moon" else "sun",
                            trailing = {
                                GlToggle(checked = isDark, onCheckedChange = viewModel::toggleTheme)
                            },
                        )
                        GlDivider()
                        GlRow(
                            title = "Change Password",
                            leadingIcon = "lock",
                            onClick = onChangePassword,
                        )
                        GlDivider()
                        GlRow(
                            title = "Sync Settings",
                            leadingIcon = "sync",
                            onClick = onSyncSettings,
                        )
                    }
                }

                GlSectionHeader(title = "Information")
                GlCard {
                    Column {
                        GlRow(
                            title = "Notifications",
                            leadingIcon = "bell",
                            onClick = onNotifications,
                        )
                        GlDivider()
                        GlRow(
                            title = "About App",
                            leadingIcon = "info",
                            onClick = onAbout,
                        )
                    }
                }

                GlButton(
                    text = "Log Out",
                    onClick = { viewModel.logout(onLogout) },
                    variant = GlButtonVariant.DangerOutline,
                )
            }
        }
    }
}
