package zm.co.tbz.goldenleaf.ui.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zm.co.tbz.goldenleaf.data.local.preferences.AppPreferences
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzLockup
import zm.co.tbz.goldenleaf.ui.components.TbzMark
import zm.co.tbz.goldenleaf.ui.components.glColors

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val c = glColors()
    Scaffold(containerColor = c.primaryDeep) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TbzMark(size = 96.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Golden Leaf",
                    color = c.bg,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Tobacco, Our Green Gold",
                    color = c.gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Inspect anywhere, sync later",
                    color = c.bg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Offline-first field operations for TBZ staff — growers, permits, inspections and sales in one app.",
                    color = c.bg.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                GlButton(text = "Continue to Sign In", onClick = onFinished, variant = GlButtonVariant.Gold)
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
    val c = glColors()
    var showPassword by remember { mutableStateOf(false) }
    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TbzLockup()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in",
                color = c.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Use your TBZ portal credentials to continue.",
                color = c.textMuted,
                fontSize = 14.sp,
            )
            GlTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "you@tbz.co.zm",
                leadingIcon = "mail",
                keyboardType = KeyboardType.Email,
            )
            GlTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                placeholder = "••••••••",
                leadingIcon = "lock",
                isPassword = !showPassword,
                trailing = {
                    GlIcon(
                        name = if (showPassword) "eye-off" else "eye",
                        size = 20.dp,
                        tint = c.textMuted,
                        modifier = Modifier.clickable { showPassword = !showPassword },
                    )
                },
            )
            if (state.requiresOtp) {
                GlTextField(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChange,
                    label = "2FA Code",
                    leadingIcon = "shield",
                    keyboardType = KeyboardType.Number,
                )
            }
            state.error?.let { ErrorText(it) }
            state.connectionMessage?.let {
                GlBanner(title = it, tone = GlTone.Success, icon = "check-circle")
            }
            GlButton(
                text = "Sign In",
                onClick = {
                    if (state.requiresOtp && state.otp.isBlank()) onNeedsOtp()
                    viewModel.login(onLoginSuccess)
                },
                enabled = !state.isLoading,
            )
            GlButton(
                text = "Forgot password?",
                onClick = onForgotPassword,
                variant = GlButtonVariant.Ghost,
                size = GlButtonSize.Md,
            )
            GlButton(
                text = "API Settings",
                onClick = viewModel::toggleApiSettings,
                variant = GlButtonVariant.Outline,
                size = GlButtonSize.Md,
                leadingIcon = "settings",
            )
            if (state.isLoading) LoadingBox()
        }
    }
    if (state.showApiSettings) {
        AlertDialog(
            onDismissRequest = viewModel::toggleApiSettings,
            title = { Text("Portal Base URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlTextField(
                        value = state.portalUrl,
                        onValueChange = viewModel::onPortalUrlChange,
                        label = "Portal URL",
                    )
                    state.connectionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.isLoading) CircularProgressIndicator()
                }
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
    val c = glColors()
    val prefs by userPreferences.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())

    LaunchedEffect(prefs.portalBaseUrl) {
        val portalUrl = UserPreferences.normalizePortalUrl(prefs.portalBaseUrl)
        val forgotUrl = "$portalUrl/accounts/forgot-password/"
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(forgotUrl))
    }

    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlScreenHeader(title = "Forgot Password", onBack = onBack)
            GlBanner(
                title = "Opening the TBZ portal",
                subtitle = "Continue the password reset in your browser, then return to sign in.",
                tone = GlTone.Info,
                icon = "info",
            )
            GlButton(text = "Back to Sign In", onClick = onBack, variant = GlButtonVariant.Outline)
        }
    }
}

@Composable
fun Login2FAScreen(
    onVerified: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = glColors()
    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlIcon("shield-check", size = 48.dp, tint = c.primary)
            }
            Text(
                "Two-factor verification",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Enter the 6-digit code from your authenticator app.",
                color = c.textMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            GlTextField(
                value = state.otp,
                onValueChange = viewModel::onOtpChange,
                label = "Verification code",
                leadingIcon = "shield",
                keyboardType = KeyboardType.Number,
            )
            state.error?.let { ErrorText(it) }
            GlButton(text = "Verify & Continue", onClick = { viewModel.login(onVerified) })
        }
    }
}

@Composable
fun ChangePasswordScreen(onDone: () -> Unit, viewModel: ChangePasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val c = glColors()
    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlScreenHeader(title = "Change Password")
            GlTextField(
                value = state.oldPassword,
                onValueChange = viewModel::onOldPasswordChange,
                label = "Current password",
                leadingIcon = "lock",
                isPassword = true,
            )
            GlTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "New password",
                helper = "Minimum 10 characters",
                leadingIcon = "lock",
                isPassword = true,
            )
            state.error?.let { ErrorText(it) }
            GlButton(text = "Update Password", onClick = { viewModel.submit(onDone) })
        }
    }
}
