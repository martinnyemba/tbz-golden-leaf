package zm.co.tbz.goldenleaf.ui.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zm.co.tbz.goldenleaf.R
import zm.co.tbz.goldenleaf.data.local.preferences.AppPreferences
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.theme.TbzColors

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    Scaffold(
        containerColor = TbzColors.DeepLeafGreen,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "TBZ Golden Leaf",
                modifier = Modifier.fillMaxWidth(0.7f),
                contentScale = ContentScale.Fit,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TBZ Field App", color = TbzColors.WarmIvory)
                Text(
                    "Offline-first field operations for TBZ staff.",
                    color = TbzColors.WarmBeige,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue to Sign In")
                }
            }
        }
    }
}

@Composable
fun PortalLoginScreen(
    onLoginSuccess: () -> Unit,
    onNeedsOtp: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TbzTopBar("Portal Login") }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_tbz_golden_leaf_256),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.55f).align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Fit,
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            if (state.requiresOtp) {
                OutlinedTextField(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChange,
                    label = { Text("2FA Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            state.error?.let { ErrorText(it) }
            state.connectionMessage?.let { Text(it, color = TbzColors.LeafGreen) }
            Button(
                onClick = {
                    if (state.requiresOtp && state.otp.isBlank()) onNeedsOtp()
                    viewModel.login(onLoginSuccess)
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign In") }
            TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
                Text("Forgot password?")
            }
            OutlinedButton(onClick = viewModel::toggleApiSettings, modifier = Modifier.fillMaxWidth()) {
                Text("API Settings")
            }
            if (state.isLoading) LoadingBox()
        }
    }
    if (state.showApiSettings) {
        AlertDialog(
            onDismissRequest = viewModel::toggleApiSettings,
            title = { Text("Portal Base URL") },
            text = {
                OutlinedTextField(
                    value = state.portalUrl,
                    onValueChange = viewModel::onPortalUrlChange,
                    label = { Text("Portal URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.savePortalUrl()
                    viewModel.toggleApiSettings()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::testConnection) { Text("Test") }
            },
        )
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
) {
    val context = LocalContext.current
    val prefs by userPreferences.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())

    LaunchedEffect(prefs.portalBaseUrl) {
        val portalUrl = UserPreferences.normalizePortalUrl(prefs.portalBaseUrl)
        val forgotUrl = "$portalUrl/accounts/forgot-password/"
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(forgotUrl))
    }

    Scaffold(topBar = { TbzTopBar("Forgot Password") }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Opening the TBZ portal password reset page in your browser.")
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Sign In")
            }
        }
    }
}

@Composable
fun Login2FAScreen(
    onVerified: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TbzTopBar("Two-Factor Login") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enter the 6-digit code from your authenticator app.")
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::onOtpChange,
                label = { Text("Verification PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { ErrorText(it) }
            Button(onClick = { viewModel.login(onVerified) }, modifier = Modifier.fillMaxWidth()) {
                Text("Verify & Continue")
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(onDone: () -> Unit, viewModel: ChangePasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TbzTopBar("Change Password") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.oldPassword,
                onValueChange = viewModel::onOldPasswordChange,
                label = { Text("Current password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text("New password (min 10 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { ErrorText(it) }
            Button(onClick = { viewModel.submit(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Update Password")
            }
        }
    }
}
