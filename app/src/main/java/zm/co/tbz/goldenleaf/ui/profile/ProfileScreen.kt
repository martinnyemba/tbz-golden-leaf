package zm.co.tbz.goldenleaf.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
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
    onSyncSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    Scaffold(topBar = { TbzTopBar("Profile") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(profile?.full_name ?: "Not signed in")
            Text(profile?.email.orEmpty())
            Text("Roles: ${profile?.roles?.joinToString().orEmpty()}")
            RowSwitch(
                label = "Dark theme",
                checked = prefs?.darkTheme == true,
                onCheckedChange = viewModel::toggleTheme,
            )
            OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                Text("Change Password")
            }
            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text("About App") }
            OutlinedButton(onClick = onSyncSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Sync Settings")
            }
            Button(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }
        }
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
