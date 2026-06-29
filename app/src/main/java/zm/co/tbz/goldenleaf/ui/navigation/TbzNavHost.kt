package zm.co.tbz.goldenleaf.ui.navigation

import androidx.compose.foundation.layout.padding
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import zm.co.tbz.goldenleaf.ui.home.DashboardViewModel
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.ui.auth.ChangePasswordScreen
import zm.co.tbz.goldenleaf.ui.auth.ForgotPasswordScreen
import zm.co.tbz.goldenleaf.ui.auth.Login2FAScreen
import zm.co.tbz.goldenleaf.ui.auth.OnboardingScreen
import zm.co.tbz.goldenleaf.ui.auth.PortalLoginScreen
import zm.co.tbz.goldenleaf.ui.components.GlBottomNav
import zm.co.tbz.goldenleaf.ui.components.GlTab
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.home.DashboardScreen
import zm.co.tbz.goldenleaf.ui.inspection.CuringInspectionFormScreen
import zm.co.tbz.goldenleaf.ui.inspection.FieldInspectionFormScreen
import zm.co.tbz.goldenleaf.ui.inspection.HighRiskGrowersScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionDetailScreen
import zm.co.tbz.goldenleaf.ui.inspection.InspectionLookupScreen
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
import zm.co.tbz.goldenleaf.ui.arbitration.ArbitrationScreen
import zm.co.tbz.goldenleaf.ui.search.GlobalSearchScreen
import zm.co.tbz.goldenleaf.ui.content.AppStaticContent
import zm.co.tbz.goldenleaf.ui.inspection.InspectionHubScreen
import zm.co.tbz.goldenleaf.ui.modules.MoreOptionsSheet
import zm.co.tbz.goldenleaf.ui.modules.StaticContentScreen
import zm.co.tbz.goldenleaf.ui.renewal.RenewalScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitCorrectionScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitCreateScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitDetailScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitDetailScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitHubScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitStatusListScreen
import zm.co.tbz.goldenleaf.ui.permits.GroupPermitValidateScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitCorrectionScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitListScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitRequestScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitValidateScreen
import zm.co.tbz.goldenleaf.ui.permits.PermitsHubScreen
import zm.co.tbz.goldenleaf.ui.notifications.NotificationsScreen
import zm.co.tbz.goldenleaf.ui.profile.ProfileScreen
import zm.co.tbz.goldenleaf.ui.corrections.CorrectionsScreen
import zm.co.tbz.goldenleaf.ui.registration.CropAllocationScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerCorrectionScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerDetailScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerEditScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerListScreen
import zm.co.tbz.goldenleaf.ui.registration.GrowerUpdatesScreen
import zm.co.tbz.goldenleaf.ui.registration.LocalRegistrationsScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationStepFarmScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationStepIdentityScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationStepPhotosScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationStepTypeScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationSuccessScreen
import zm.co.tbz.goldenleaf.ui.registration.RegistrationViewModel
import zm.co.tbz.goldenleaf.ui.registration.ScanNrcScreen
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
            OnboardingScreen(
                onFinished = {
                    scope.launch {
                        onFinishOnboarding()
                        navController.navigate(Routes.PORTAL_LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    scope.launch {
                        onFinishOnboarding()
                        navController.navigate(Routes.PORTAL_LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
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
                onTerms = { navController.navigate(Routes.TERMS) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
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
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MAIN) {
            MainShell(navController = navController)
        }
        composable(Routes.SEARCH) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) },
                onOpenPermit = { navController.navigate(Routes.permitDetail(it)) },
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REGISTRATION) {
            GrowerListScreen(
                onBack = { navController.popBackStack() },
                onNewRegistration = {
                    navController.navigate(Routes.REGISTRATION_WIZARD) {
                        launchSingleTop = true
                    }
                },
                onScanId = { navController.navigate(Routes.REGISTRATION_SCAN_ID) },
                onOpenDetail = { navController.navigate(Routes.registrationDetail(it)) },
                onOpenUpdates = { navController.navigate(Routes.GROWER_UPDATES) },
            )
        }
        navigation(
            route = Routes.REGISTRATION_WIZARD,
            startDestination = Routes.WIZARD_STEP_TYPE,
        ) {
            composable(Routes.WIZARD_SCAN_ID) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                ScanNrcScreen(
                    onClose = { navController.popBackStack() },
                    onUseId = {
                        navController.navigate(Routes.WIZARD_STEP_IDENTITY) {
                            popUpTo(Routes.WIZARD_STEP_TYPE) { inclusive = false }
                        }
                    },
                    onManualEntry = { navController.navigate(Routes.WIZARD_STEP_IDENTITY) },
                    viewModel = vm,
                )
            }
            composable(Routes.WIZARD_STEP_TYPE) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                RegistrationStepTypeScreen(
                    onBack = { navController.popBackStack(Routes.REGISTRATION, inclusive = false) },
                    onContinue = { navController.navigate(Routes.WIZARD_STEP_IDENTITY) },
                    onScanId = { navController.navigate(Routes.WIZARD_SCAN_ID) },
                    viewModel = vm,
                )
            }
            composable(Routes.WIZARD_STEP_IDENTITY) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                RegistrationStepIdentityScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        vm.continueFromIdentity {
                            navController.navigate(Routes.WIZARD_STEP_FARM)
                        }
                    },
                    viewModel = vm,
                )
            }
            composable(Routes.WIZARD_STEP_FARM) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                RegistrationStepFarmScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        vm.continueFromFarm {
                            navController.navigate(Routes.WIZARD_STEP_PHOTOS)
                        }
                    },
                    viewModel = vm,
                )
            }
            composable(Routes.WIZARD_STEP_PHOTOS) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                RegistrationStepPhotosScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = {
                        vm.submitRegistration {
                            navController.navigate(Routes.WIZARD_SUCCESS) {
                                popUpTo(Routes.WIZARD_STEP_TYPE) { inclusive = false }
                            }
                        }
                    },
                    viewModel = vm,
                )
            }
            composable(Routes.WIZARD_SUCCESS) {
                val vm = hiltViewModel<RegistrationViewModel>(navController.getBackStackEntry(Routes.REGISTRATION_WIZARD))
                val lastId by vm.lastRegisteredId.collectAsState()
                val uiState by vm.uiState.collectAsState()
                val grower by remember(lastId) { vm.observeGrower(lastId.orEmpty()) }.collectAsState()
                RegistrationSuccessScreen(
                    growerName = uiState.lastRegisteredName ?: "Grower",
                    provisionalId = grower?.tbz_id ?: grower?.nrc_number ?: lastId?.take(12) ?: "Pending sync",
                    onViewGrower = {
                        navController.navigate(Routes.registrationDetail(lastId ?: return@RegistrationSuccessScreen)) {
                            popUpTo(Routes.REGISTRATION) { inclusive = false }
                        }
                    },
                    onBackToList = {
                        navController.popBackStack(Routes.REGISTRATION, inclusive = false)
                    },
                )
            }
        }
        composable(Routes.REGISTRATION_LIST) {
            GrowerListScreen(
                onOpenDetail = { navController.navigate(Routes.registrationDetail(it)) },
                onNewRegistration = { navController.navigate(Routes.REGISTRATION_WIZARD) },
                onScanId = { navController.navigate(Routes.REGISTRATION_SCAN_ID) },
            )
        }
        composable(Routes.REGISTRATION_LOCAL) {
            LocalRegistrationsScreen(onOpenDetail = { navController.navigate(Routes.registrationDetail(it)) })
        }
        composable(Routes.GROWER_UPDATES) {
            GrowerUpdatesScreen(
                onBack = { navController.popBackStack() },
                onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) },
            )
        }
        composable(
            route = Routes.REGISTRATION_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            GrowerDetailScreen(
                localId = localId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.registrationEdit(it)) },
                onCorrection = { navController.navigate(Routes.registrationCorrection(it)) },
                onAddCrop = { navController.navigate(Routes.registrationCrop(it)) },
                onNewPermit = { navController.navigate(Routes.PERMITS) },
                onNewInspection = { navController.navigate(Routes.INSPECTION) },
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
                onBack = { navController.popBackStack() },
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
            CorrectionsScreen(
                onBack = { navController.popBackStack() },
                onOpenGrowerCorrection = { navController.navigate(Routes.registrationCorrection(it)) },
                onOpenTransportPermitCorrection = { navController.navigate(Routes.permitCorrection(it)) },
                onOpenGroupPermitCorrection = { navController.navigate(Routes.groupPermitCorrection(it)) },
            )
        }
        dialog(
            route = Routes.MENU,
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val parentEntry = navController.getBackStackEntry(Routes.MAIN)
            val dashboardVm = hiltViewModel<DashboardViewModel>(parentEntry)
            val pendingSync by dashboardVm.pendingSyncCount.collectAsState()
            MoreOptionsSheet(
                pendingSyncCount = if (pendingSync > 0) pendingSync else 4,
                onSyncSettings = {
                    navController.popBackStack()
                    navController.navigate(Routes.SYNC_SETTINGS)
                },
                onAbout = {
                    navController.popBackStack()
                    navController.navigate(Routes.ABOUT)
                },
                onClose = { navController.popBackStack() },
            )
        }
        composable(Routes.INSPECTION) {
            InspectionHubScreen(
                onSchedule = { navController.navigate(Routes.INSPECTION_SCHEDULE) },
                onLocalSchedules = { navController.navigate(Routes.INSPECTION_SCHEDULES_LOCAL) },
                onPortalList = { navController.navigate(Routes.INSPECTION_PORTAL_LIST) },
                onReports = { navController.navigate(Routes.INSPECTION_REPORTS) },
                onHighRisk = { navController.navigate(Routes.INSPECTION_HIGH_RISK) },
                onLookup = { navController.navigate(Routes.INSPECTION_LOOKUP) },
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
            InspectionLookupScreen(
                onOpenGrower = { navController.navigate(Routes.registrationDetail(it)) },
                onScheduleForGrower = {
                    navController.navigate(Routes.INSPECTION_SCHEDULE)
                },
            )
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
                onCapture = { navController.navigate(Routes.SALES) },
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
            PermitListScreen(
                onOpenDetail = { navController.navigate(Routes.permitDetail(it)) },
                onValidate = { navController.navigate(Routes.PERMIT_VALIDATE) },
                onNew = { navController.navigate(Routes.PERMIT_REQUEST) },
            )
        }
        composable(
            route = Routes.PERMIT_DETAIL,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            PermitDetailScreen(
                localId = localId,
                onOpenCorrection = { navController.navigate(Routes.permitCorrection(it)) },
            )
        }
        composable(
            route = Routes.PERMIT_CORRECTION,
            arguments = listOf(navArgument("localId") { type = NavType.StringType }),
        ) { entry ->
            PermitCorrectionScreen(
                localId = entry.arguments?.getString("localId").orEmpty(),
                onDone = { navController.popBackStack() },
            )
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
        composable(Routes.ARBITRATION) {
            ArbitrationScreen(onSubmitted = { navController.popBackStack() })
        }
        composable(Routes.RENEWAL) {
            RenewalScreen(
                onOpenCropAllocation = { navController.navigate(Routes.registrationCrop(it)) },
            )
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
            StaticContentScreen("About App", AppStaticContent.ABOUT)
        }
        composable(Routes.TERMS) {
            StaticContentScreen("Terms & Conditions", AppStaticContent.TERMS)
        }
        composable(Routes.PRIVACY) {
            StaticContentScreen("Privacy Policy", AppStaticContent.PRIVACY)
        }
        composable(Routes.GUIDELINES) {
            StaticContentScreen("TBZ Guidelines", AppStaticContent.GUIDELINES)
        }
    }
}

@Composable
private fun MainShell(
    navController: NavHostController,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(GlTab.Home.name) }
    val selectedTab = GlTab.valueOf(selectedTabName)
    val c = glColors()
    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            GlBottomNav(
                active = selectedTab,
                onSelect = { selectedTabName = it.name },
                onScan = { navController.navigate(Routes.PERMIT_VALIDATE) },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            GlTab.Home -> DashboardScreen(
                modifier = contentModifier,
                onOpenRegistration = { navController.navigate(Routes.REGISTRATION) },
                onOpenPermits = { selectedTabName = GlTab.Permits.name },
                onOpenInspection = { selectedTabName = GlTab.Inspection.name },
                onOpenMarketing = { navController.navigate(Routes.MARKETING) },
                onOpenCorrections = { navController.navigate(Routes.CORRECTIONS) },
                onOpenArbitration = { navController.navigate(Routes.ARBITRATION) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenPermitValidate = { navController.navigate(Routes.PERMIT_VALIDATE) },
                onOpenInspectionDetail = { navController.navigate(Routes.inspectionDetail(it)) },
                onOpenMenu = { navController.navigate(Routes.MENU) },
            )
            GlTab.Permits -> PermitListScreen(
                modifier = contentModifier,
                onOpenDetail = { navController.navigate(Routes.permitDetail(it)) },
                onValidate = { navController.navigate(Routes.PERMIT_VALIDATE) },
                onNew = { navController.navigate(Routes.PERMIT_REQUEST) },
            )
            GlTab.Inspection -> InspectionPortalListScreen(
                modifier = contentModifier,
                onOpenDetail = { navController.navigate(Routes.inspectionDetail(it)) },
                onSchedule = { navController.navigate(Routes.INSPECTION_SCHEDULE) },
            )
            GlTab.Profile -> ProfileScreen(
                modifier = contentModifier,
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
