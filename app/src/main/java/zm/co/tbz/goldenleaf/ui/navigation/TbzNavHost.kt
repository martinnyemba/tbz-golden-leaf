package zm.co.tbz.goldenleaf.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.auth.ChangePasswordScreen
import zm.co.tbz.goldenleaf.ui.auth.Login2FAScreen
import zm.co.tbz.goldenleaf.ui.auth.OnboardingScreen
import zm.co.tbz.goldenleaf.ui.auth.PortalLoginScreen
import zm.co.tbz.goldenleaf.ui.home.DashboardScreen
import zm.co.tbz.goldenleaf.ui.modules.ArbitrationScreen
import zm.co.tbz.goldenleaf.ui.modules.InspectionHubScreen
import zm.co.tbz.goldenleaf.ui.modules.MarketingHubScreen
import zm.co.tbz.goldenleaf.ui.modules.MenuScreen
import zm.co.tbz.goldenleaf.ui.modules.PermitsHubScreen
import zm.co.tbz.goldenleaf.ui.modules.SearchScreen
import zm.co.tbz.goldenleaf.ui.modules.StaticContentScreen
import zm.co.tbz.goldenleaf.ui.profile.ProfileScreen
import zm.co.tbz.goldenleaf.ui.registration.CorrectionsScreen
import zm.co.tbz.goldenleaf.ui.registration.CropAllocationScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerCorrectionScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerDetailScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerEditScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerListScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerUpdatesScreen
import zm.co.tbz.goldenleaf.ui.registration.LocalRegistrationsScreen
import zm.co.tbz.goldenleaf.ui.registration.NewGrowerRegistrationScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationHubScreen
import zm.co.tbz.goldenleaf.ui.sync.SyncSettingsScreen

@Composable
fun TbzNavHost(
    navController: NavHostController,
    startDestination: String,
    userPreferences: UserPreferences,
    onFinishOnboarding: suspend () -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            val scope = rememberCoroutineScope()
            OnboardingScreen {
                scope.launch {
                    onFinishOnboarding()
                    navController.navigate(Routes.PORTAL_LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.PORTAL_LOGIN) {
            PortalLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PORTAL_LOGIN) { inclusive = true }
                    }
                },
                onNeedsOtp = { navController.navigate(Routes.LOGIN_2FA) },
            )
        }
        composable(Routes.LOGIN_2FA) {
            Login2FAScreen(
                onVerified = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PORTAL_LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainShell(navController = navController, userPreferences = userPreferences)
        }
        composable(Routes.REGISTRATION) {
            RegistrationHubScreen(
                onNewRegistration = { navController.navigate(Routes.REGISTRATION_NEW) },
                onGrowerList = { navController.navigate(Routes.REGISTRATION_LIST) },
                onLocalRegistrations = { navController.navigate(Routes.REGISTRATION_LOCAL) },
                onGrowerUpdates = { navController.navigate(Routes.GROWER_UPDATES) },
                onCorrections = { navController.navigate(Routes.CORRECTIONS) },
            )
        }
        composable(Routes.REGISTRATION_NEW) {
            NewGrowerRegistrationScreen(onSaved = { navController.popBackStack() })
        }
        composable(Routes.REGISTRATION_LIST) {
            GrowerListScreen(onOpenDetail = { navController.navigate(Routes.registrationDetail(it)) })
        }
        composable(Routes.REGISTRATION_LOCAL) {
            LocalRegistrationsScreen(onOpenDetail = { navController.navigate(Routes.registrationDetail(it)) })
        }
        composable(Routes.GROWER_UPDATES) {
            GrowerUpdatesScreen(onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) })
        }
        composable(
            route = Routes.REGISTRATION_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            GrowerDetailScreen(
                localId = localId,
                onEdit = { navController.navigate(Routes.registrationEdit(it)) },
                onCorrection = { navController.navigate(Routes.registrationCorrection(it)) },
                onAddCrop = { navController.navigate(Routes.registrationCrop(it)) },
            )
        }
        composable(
            route = Routes.REGISTRATION_EDIT,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            GrowerEditScreen(
                localId = entry.arguments?.getString("localId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.REGISTRATION_CORRECTION,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            GrowerCorrectionScreen(
                localId = entry.arguments?.getString("localId").orEmpty(),
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.REGISTRATION_CROP,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            CropAllocationScreen(
                growerLocalId = entry.arguments?.getString("localId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.CORRECTIONS) {
            CorrectionsScreen(onOpenCorrection = { navController.navigate(Routes.registrationCorrection(it)) })
        }
        composable(Routes.INSPECTION) { InspectionHubScreen() }
        composable(Routes.MARKETING) { MarketingHubScreen() }
        composable(Routes.PERMITS) { PermitsHubScreen() }
        composable(Routes.ARBITRATION) { ArbitrationScreen() }
        composable(Routes.SYNC_SETTINGS) { SyncSettingsScreen() }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onDone = {
                navController.navigate(Routes.PORTAL_LOGIN) {
                    popUpTo(Routes.MAIN) { inclusive = true }
                }
            })
        }
        composable(Routes.ABOUT) {
            StaticContentScreen("About App", "TBZ Golden Leaf — Tobacco Registration, Marketing & Compliance field app.")
        }
        composable(Routes.TERMS) {
            StaticContentScreen("Terms & Conditions", "Terms content loaded from TBZ policy.")
        }
        composable(Routes.PRIVACY) {
            StaticContentScreen("Privacy Policy", "Privacy policy content.")
        }
        composable(Routes.GUIDELINES) {
            StaticContentScreen("TBZ Guidelines", "Regulatory guidance for field officers.")
        }
    }
}

@Composable
private fun MainShell(
    navController: NavHostController,
    userPreferences: UserPreferences,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text("More") },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            0 -> DashboardScreen(
                modifier = contentModifier,
                onOpenRegistration = { navController.navigate(Routes.REGISTRATION) },
                onOpenCorrections = { navController.navigate(Routes.CORRECTIONS) },
                onOpenSync = { navController.navigate(Routes.SYNC_SETTINGS) },
            )
            1 -> Box(contentModifier) { SearchScreen() }
            2 -> Box(contentModifier) {
                MenuScreen(
                onRegistration = { navController.navigate(Routes.REGISTRATION) },
                onInspection = { navController.navigate(Routes.INSPECTION) },
                onMarketing = { navController.navigate(Routes.MARKETING) },
                onPermits = { navController.navigate(Routes.PERMITS) },
                onArbitration = { navController.navigate(Routes.ARBITRATION) },
                onSyncSettings = { navController.navigate(Routes.SYNC_SETTINGS) },
                )
            }
            3 -> Box(contentModifier) {
                ProfileScreen(
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onSyncSettings = { navController.navigate(Routes.SYNC_SETTINGS) },
                onLogout = {
                    navController.navigate(Routes.PORTAL_LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                )
            }
        }
    }
}
