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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.auth.ChangePasswordScreen
import zm.co.tbz.goldenleaf.ui.auth.ForgotPasswordScreen
import zm.co.tbz.goldenleaf.ui.auth.Login2FAScreen
import zm.co.tbz.goldenleaf.ui.auth.OnboardingScreen
import zm.co.tbz.goldenleaf.ui.auth.PortalLoginScreen
import zm.co.tbz.goldenleaf.ui.home.DashboardScreen
import zm.co.tbz.goldenleaf.ui.inspection.CuringInspectionFormScreen
import zm.co.tbz.goldenleaf.ui.inspection.FieldInspectionFormScreen
import zm.co.tbz.goldenleaf.ui.inspection.HighRiskGrowersScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionDetailScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionHubScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionPortalListScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionReportsScreen
import zm.co.tbz.goldenleaf.ui.inspection.LocalSchedulesScreen
import zm.co.tbz.goldenleaf.ui.inspection.NurseryInspectionFormScreen
import zm.co.tbz.goldenleaf.ui.inspection.ScheduleInspectionScreen
import zm.co.tbz.goldenleaf.ui.inspection.ValidationFormScreen
import zm.co.tbz.goldenleaf.ui.marketing.EditPendingSaleScreen
import zm.co.tbz.goldenleaf.ui.marketing.MarketingHubScreen
import zm.co.tbz.goldenleaf.ui.marketing.PendingSalesScreen
import zm.co.tbz.goldenleaf.ui.marketing.SalesCaptureScreen
import zm.co.tbz.goldenleaf.ui.modules.ArbitrationScreen
import zm.co.tbz.goldenleaf.ui.modules.MenuScreen
import zm.co.tbz.goldenleaf.ui.modules.SearchScreen
import zm.co.tbz.goldenleaf.ui.modules.StaticContentScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitCorrectionScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitCreateScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitDetailScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitHubScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitStatusListScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitValidateScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitDetailScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitListScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitRequestScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitValidateScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitsHubScreen
import zm.co.tbz.goldenleaf.ui.notifications.NotificationsScreen
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
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                userPreferences = userPreferences,
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
            MainShell(navController = navController)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen()
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
        composable(Routes.INSPECTION) {
            InspectionHubScreen(
                onSchedule = { navController.navigate(Routes.INSPECTION_SCHEDULE) },
                onLocalSchedules = { navController.navigate(Routes.INSPECTION_SCHEDULES_LOCAL) },
                onPortalList = { navController.navigate(Routes.INSPECTION_PORTAL_LIST) },
                onReports = { navController.navigate(Routes.INSPECTION_REPORTS) },
                onHighRisk = { navController.navigate(Routes.INSPECTION_HIGH_RISK) },
            )
        }
        composable(Routes.INSPECTION_SCHEDULE) {
            ScheduleInspectionScreen(onScheduled = { navController.popBackStack() })
        }
        composable(Routes.INSPECTION_SCHEDULES_LOCAL) {
            LocalSchedulesScreen(
                onOpenDetail = { navController.navigate(Routes.inspectionDetail(it)) },
            )
        }
        composable(
            route = Routes.INSPECTION_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            InspectionDetailScreen(
                localId = localId,
                onStartField = { navController.navigate(Routes.inspectionField(it)) },
                onStartNursery = { navController.navigate(Routes.inspectionNursery(it)) },
                onStartCuring = { navController.navigate(Routes.inspectionCuring(it)) },
                onStartValidation = { navController.navigate(Routes.validation(it)) },
                onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) },
            )
        }
        composable(Routes.INSPECTION_PORTAL_LIST) {
            InspectionPortalListScreen(
                onOpenDetail = { navController.navigate(Routes.inspectionDetail(it)) },
            )
        }
        composable(Routes.INSPECTION_REPORTS) {
            InspectionReportsScreen()
        }
        composable(Routes.INSPECTION_HIGH_RISK) {
            HighRiskGrowersScreen()
        }
        composable(Routes.INSPECTION_LOOKUP) {
            StaticContentScreen("Inspection Lookup", "Search growers for inspection scheduling.")
        }
        composable(
            route = Routes.INSPECTION_FIELD,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) { entry ->
            FieldInspectionFormScreen(
                inspectionId = entry.arguments?.getString("inspectionId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.INSPECTION_NURSERY,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) { entry ->
            NurseryInspectionFormScreen(
                inspectionId = entry.arguments?.getString("inspectionId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.INSPECTION_CURING,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) { entry ->
            CuringInspectionFormScreen(
                inspectionId = entry.arguments?.getString("inspectionId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.VALIDATION,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) { entry ->
            ValidationFormScreen(
                inspectionId = entry.arguments?.getString("inspectionId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.MARKETING) {
            MarketingHubScreen(
                onSalesCapture = { navController.navigate(Routes.SALES) },
                onPendingSales = { navController.navigate(Routes.PENDING_SALES) },
            )
        }
        composable(Routes.SALES) {
            SalesCaptureScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.PENDING_SALES) {
            PendingSalesScreen(
                onEdit = { navController.navigate(Routes.editPendingSale(it)) },
            )
        }
        composable(
            route = Routes.EDIT_PENDING_SALE,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            EditPendingSaleScreen(
                localId = entry.arguments?.getString("localId").orEmpty(),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.PERMITS) {
            PermitsHubScreen(
                onRequest = { navController.navigate(Routes.PERMIT_REQUEST) },
                onValidate = { navController.navigate(Routes.PERMIT_VALIDATE) },
                onList = { navController.navigate(Routes.PERMIT_LIST) },
                onGroupPermits = { navController.navigate(Routes.PERMIT_GROUP) },
            )
        }
        composable(Routes.PERMIT_REQUEST) {
            PermitRequestScreen(onSubmitted = { navController.popBackStack() })
        }
        composable(Routes.PERMIT_VALIDATE) { PermitValidateScreen() }
        composable(Routes.PERMIT_LIST) {
            PermitListScreen(onOpenDetail = { navController.navigate(Routes.permitDetail(it)) })
        }
        composable(
            route = Routes.PERMIT_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            PermitDetailScreen(localId = entry.arguments?.getString("localId").orEmpty())
        }
        composable(Routes.PERMIT_GROUP) {
            GroupPermitHubScreen(
                onCreate = { navController.navigate(Routes.PERMIT_GROUP_CREATE) },
                onValidate = { navController.navigate(Routes.PERMIT_GROUP_VALIDATE) },
                onStatusList = { navController.navigate(Routes.PERMIT_GROUP_STATUS) },
            )
        }
        composable(Routes.PERMIT_GROUP_CREATE) {
            GroupPermitCreateScreen(onSubmitted = { navController.popBackStack() })
        }
        composable(Routes.PERMIT_GROUP_VALIDATE) {
            GroupPermitValidateScreen()
        }
        composable(Routes.PERMIT_GROUP_STATUS) {
            GroupPermitStatusListScreen(
                onOpenDetail = { navController.navigate(Routes.groupPermitDetail(it)) },
            )
        }
        composable(
            route = Routes.PERMIT_GROUP_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            GroupPermitDetailScreen(
                localId = localId,
                onOpenCorrection = { navController.navigate(Routes.groupPermitCorrection(it)) },
            )
        }
        composable(
            route = Routes.PERMIT_GROUP_CORRECTION,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            GroupPermitCorrectionScreen(
                localId = entry.arguments?.getString("localId").orEmpty(),
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.ARBITRATION) { ArbitrationScreen() }
        composable(Routes.RENEWAL) {
            StaticContentScreen("Renewal", "Grower renewal workflow.")
        }
        composable(
            route = Routes.PLACEHOLDER,
            arguments = listOf(navArgument("title") { type = NavType.StringType }),
        ) { entry ->
            val title = entry.arguments?.getString("title").orEmpty()
            StaticContentScreen(title, "Feature scaffold for $title.")
        }
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
    menuAccessViewModel: MenuAccessViewModel = hiltViewModel(),
) {
    val menuAccess by menuAccessViewModel.state.collectAsState()
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
            1 -> Box(contentModifier) {
                SearchScreen(
                    onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) },
                    onOpenPermit = { navController.navigate(Routes.permitDetail(it)) },
                )
            }
            2 -> Box(contentModifier) {
                MenuScreen(
                    onRegistration = { navController.navigate(Routes.REGISTRATION) },
                    onInspection = { navController.navigate(Routes.INSPECTION) },
                    onMarketing = { navController.navigate(Routes.MARKETING) },
                    onPermits = { navController.navigate(Routes.PERMITS) },
                    onArbitration = { navController.navigate(Routes.ARBITRATION) },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onSyncSettings = { navController.navigate(Routes.SYNC_SETTINGS) },
                    canRegistration = menuAccess.canRegistration,
                    canInspection = menuAccess.canInspection,
                    canMarketing = menuAccess.canMarketing,
                    canPermits = menuAccess.canPermits,
                    canArbitration = menuAccess.canArbitration,
                )
            }
            3 -> Box(contentModifier) {
                ProfileScreen(
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onAbout = { navController.navigate(Routes.ABOUT) },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
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
