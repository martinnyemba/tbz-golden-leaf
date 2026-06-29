package zm.co.tbz.goldenleaf.ui.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zm.co.tbz.goldenleaf.data.local.preferences.AppPreferences
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlOtpInput
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzMark
import zm.co.tbz.goldenleaf.ui.components.glColors

// ─────────────────────────────────────────────────────────────────────────────
// 01 · ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onSkip: () -> Unit = onFinished) {
    val c = glColors()
    var step by remember { mutableStateOf(0) }
    val slides = listOf(
        "Welcome to Golden Leaf" to
            "Your TBZ field companion for grower registration, inspection, and permit management — built for the road, online or off.",
        "Inspect anywhere, sync later" to
            "Capture nursery, field and curing inspections offline. Reports queue safely until you are back online.",
        "Permits in your pocket" to
            "Request, validate and issue transport permits. Scan QR codes at sales floors with confidence.",
    )
    val isLast = step == slides.lastIndex

    Scaffold(containerColor = c.bg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Skip",
                    color = c.textMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSkip),
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TbzMark(size = 120.dp)
                }
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        slides[step].first,
                        color = c.text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 31.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        slides[step].second,
                        color = c.textMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slides.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (index == step) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (index == step) c.primary else c.outline),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                GlButton(
                    text = if (isLast) "Get Started" else "Next",
                    onClick = { if (isLast) onFinished() else step += 1 },
                    trailingIcon = "arrow-right",
                )
                if (step > 0) {
                    Spacer(Modifier.height(8.dp))
                    GlButton(
                        text = "Back",
                        onClick = { step -= 1 },
                        variant = GlButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 02 · PORTAL LOGIN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PortalLoginScreen(
    onLoginSuccess: () -> Unit,
    onNeedsOtp: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = glColors()
    var showPassword by remember { mutableStateOf(false) }
    var showApi by remember { mutableStateOf(false) }

    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TbzMark(size = 56.dp)
                Column {
                    Text(
                        "TOBACCO BOARD OF ZAMBIA",
                        color = c.gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        "Golden Leaf",
                        color = c.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) {
                Text(
                    "Sign in",
                    color = c.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Use your TRMCS portal credentials to continue.",
                    color = c.textMuted,
                    fontSize = 14.sp,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    placeholder = "you@tbz.org.zm",
                    leadingIcon = "mail",
                    keyboardType = KeyboardType.Email,
                )
                GlTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    placeholder = "Enter password",
                    leadingIcon = "lock",
                    isPassword = !showPassword,
                    trailing = {
                        GlIcon(
                            name = if (showPassword) "eye-off" else "eye",
                            size = 18.dp,
                            tint = c.textMuted,
                            modifier = Modifier.clickable { showPassword = !showPassword },
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.clickable { showApi = !showApi },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        GlIcon("sliders", size = 14.dp, tint = c.textMuted)
                        Text(
                            "API settings",
                            color = c.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "Forgot password?",
                        color = c.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onForgotPassword),
                    )
                }
                if (showApi) {
                    GlCard(contentPadding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlTextField(
                                value = state.portalUrl,
                                onValueChange = viewModel::onPortalUrlChange,
                                label = "Portal Base URL",
                                leadingIcon = "cloud",
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GlButton(
                                    text = "Test connection",
                                    onClick = viewModel::testConnection,
                                    variant = GlButtonVariant.Outline,
                                    size = GlButtonSize.Sm,
                                    fillMaxWidth = false,
                                )
                                state.connectionMessage?.let { msg ->
                                    GlPill(
                                        text = msg,
                                        tone = GlTone.Success,
                                        size = GlPillSize.Sm,
                                        leadingIcon = "check",
                                    )
                                }
                            }
                            GlButton(
                                text = "Save URL",
                                onClick = viewModel::savePortalUrl,
                                variant = GlButtonVariant.Secondary,
                                size = GlButtonSize.Sm,
                            )
                        }
                    }
                }
                state.error?.let { ErrorText(it) }
                if (state.isLoading) LoadingBox()
                GlButton(
                    text = "Sign In",
                    onClick = {
                        viewModel.login(
                            onSuccess = onLoginSuccess,
                            onNeedsOtp = onNeedsOtp,
                        )
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.padding(top = 6.dp),
                )
                LoginTermsFooter(onTerms = onTerms, onPrivacy = onPrivacy)
            }
        }
    }
}

@Composable
private fun LoginTermsFooter(onTerms: () -> Unit, onPrivacy: () -> Unit) {
    val c = glColors()
    val annotated = buildAnnotatedString {
        append("By continuing you accept the TBZ ")
        pushStringAnnotation("terms", "terms")
        withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.SemiBold)) { append("Terms") }
        pop()
        append(" and ")
        pushStringAnnotation("privacy", "privacy")
        withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
        pop()
        append(".")
    }
    ClickableText(
        text = annotated,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        style = androidx.compose.ui.text.TextStyle(
            color = c.textSubtle,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("terms", offset, offset).firstOrNull()?.let { onTerms() }
            annotated.getStringAnnotations("privacy", offset, offset).firstOrNull()?.let { onPrivacy() }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 03 · FORGOT PASSWORD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
) {
    val context = LocalContext.current
    val c = glColors()
    val prefs by userPreferences.preferences.collectAsStateWithLifecycle(initialValue = AppPreferences())
    var stage by rememberSaveable { mutableStateOf("input") }
    var email by rememberSaveable { mutableStateOf("") }

    if (stage == "sent") {
        Scaffold(containerColor = c.bg) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(c.primarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlIcon("mail", size = 44.dp, tint = c.primary)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Check your email",
                        color = c.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        buildAnnotatedString {
                            append("If ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.text)) { append(email) }
                            append(" is registered, you'll receive a password reset link shortly.")
                        },
                        color = c.textMuted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    GlBanner(
                        title = "Web portal only",
                        subtitle = "Password resets are handled on the TBZ web portal. Open the link on your browser or another device.",
                        tone = GlTone.Info,
                        icon = "info",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    GlButton(
                        text = "Try a different email",
                        onClick = { stage = "input" },
                        variant = GlButtonVariant.Ghost,
                    )
                }
                GlButton(text = "Back to sign in", onClick = onBack, variant = GlButtonVariant.Ghost)
                Spacer(Modifier.height(24.dp))
            }
        }
        return
    }

    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GlScreenHeader(title = "Reset password", onBack = onBack)
                Column(Modifier.padding(horizontal = 28.dp, vertical = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.primarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlIcon("lock", size = 30.dp, tint = c.primary)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Forgot your password?",
                        color = c.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter your work email and we'll send a reset link through the TBZ web portal.",
                        color = c.textMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    GlTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Work email",
                        placeholder = "you@tbz.org.zm",
                        leadingIcon = "mail",
                        keyboardType = KeyboardType.Email,
                        required = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    GlBanner(
                        title = "Reset via web portal",
                        subtitle = "Password resets use the TBZ portal at your configured base URL — no API available on mobile.",
                        tone = GlTone.Warning,
                        icon = "info",
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlButton(
                    text = "Send reset link",
                    onClick = {
                        if (email.contains("@")) {
                            val portalUrl = UserPreferences.normalizePortalUrl(prefs.portalBaseUrl)
                            val forgotUrl = "$portalUrl/accounts/forgot-password/"
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(forgotUrl))
                            stage = "sent"
                        }
                    },
                    enabled = email.contains("@"),
                    leadingIcon = "mail",
                )
                GlButton(text = "Back to sign in", onClick = onBack, variant = GlButtonVariant.Ghost)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 04 · LOGIN 2FA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Login2FAScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "", onBack = onBack)
            Column(
                Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    GlIcon("shield-check", size = 32.dp, tint = c.primary)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Two-factor verification",
                    color = c.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter the 6-digit code from your authenticator app.",
                    color = c.textMuted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(28.dp))
                GlOtpInput(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChange,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    buildAnnotatedString {
                        append("Didn't get a code? ")
                        withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.Bold)) { append("Resend") }
                        append(" · Available in 24s")
                    },
                    color = c.textMuted,
                    fontSize = 12.sp,
                )
                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorText(it)
                }
                if (state.isLoading) {
                    Spacer(Modifier.height(12.dp))
                    LoadingBox()
                }
                Spacer(Modifier.height(28.dp))
                GlButton(
                    text = "Verify & Continue",
                    onClick = { viewModel.login(onSuccess = onVerified, onNeedsOtp = null) },
                    enabled = !state.isLoading && state.otp.length == 6,
                )
                Spacer(Modifier.height(8.dp))
                GlButton(text = "Use a recovery code", onClick = {}, variant = GlButtonVariant.Ghost)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Change password (profile flow — not in auth handoff group)
// ─────────────────────────────────────────────────────────────────────────────

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
            GlBanner(
                title = "Strong password requirements",
                subtitle = "At least 10 characters; must meet TBZ password policy (mix of letters, numbers, and a symbol).",
                tone = GlTone.Info,
                icon = "info",
            )
            GlTextField(
                value = state.oldPassword,
                onValueChange = viewModel::onOldPasswordChange,
                label = "Current password",
                placeholder = "••••••••",
                leadingIcon = "lock",
                isPassword = true,
                required = true,
            )
            GlTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "New password",
                helper = "Minimum 10 characters",
                placeholder = "At least 10 characters",
                leadingIcon = "lock",
                isPassword = true,
                required = true,
            )
            state.error?.let { ErrorText(it) }
            GlButton(text = "Update Password", onClick = { viewModel.submit(onDone) })
        }
    }
}
