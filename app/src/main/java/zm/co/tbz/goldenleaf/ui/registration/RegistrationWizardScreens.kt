package zm.co.tbz.goldenleaf.ui.registration

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.ui.window.Dialog
import zm.co.tbz.goldenleaf.ui.scan.CameraCaptureScreen
import zm.co.tbz.goldenleaf.ui.scan.IdDocumentScanning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.NrcCameraPreview
import zm.co.tbz.goldenleaf.ui.components.NrcScanButton
import zm.co.tbz.goldenleaf.ui.components.GlImageSlot
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlDateField
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.todayUtcMillis
import zm.co.tbz.goldenleaf.ui.components.GlToggle
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

private val wizardLabels = GrowerFormChoices.wizardStepLabels

@Composable
private fun WizardFooter(
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryIcon: String? = null,
    primaryEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(glColors().surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlButton(
            text = secondaryText,
            onClick = onSecondary,
            variant = GlButtonVariant.Ghost,
            fillMaxWidth = false,
            modifier = Modifier.weight(1f),
        )
        GlButton(
            text = primaryText,
            onClick = onPrimary,
            trailingIcon = primaryIcon,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 11 · Step 1 · Grower type */
@Composable
fun RegistrationStepTypeScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onScanId: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val c = glColors()

    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Cancel", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
            GlScreenHeader(title = "New grower", subtitle = "1 of 4", onBack = onBack)
            GlStepper(step = 1, total = 4, stepLabels = wizardLabels)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Grower type", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "This determines the fields required for registration.",
                    color = c.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GrowerFormChoices.growerTypeOptions.forEach { (key, meta) ->
                        val selected = personal.growerTypeKey == key
                        GlCard(
                            onClick = { viewModel.applyGrowerTypeKey(key) },
                            contentPadding = 14.dp,
                            elevated = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) c.primary else c.outlineSoft,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) c.primary else c.surfaceAlt),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    GlIcon(
                                        when (key) {
                                            "company" -> "users"
                                            "commercial" -> "building"
                                            else -> "profile"
                                        },
                                        size = 22.dp,
                                        tint = if (selected) Color.White else c.textMuted,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(meta.first, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(meta.second, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        meta.third,
                                        color = if (selected) c.primary else c.text,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Text(
                                        "in district",
                                        color = c.textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                GlSectionHeader(title = "Quick capture", modifier = Modifier.padding(top = 24.dp))
                GlCard(
                    onClick = onScanId,
                    contentPadding = 14.dp,
                    modifier = Modifier.background(c.goldSoft),
                ) {
                    GlRow(
                        title = "Scan NRC card",
                        subtitle = "Auto-fill fields from a national ID",
                        leadingIcon = "qr",
                        tone = GlTone.Gold,
                        trailing = { GlIcon("chevron-right", size = 18.dp, tint = c.textSubtle) },
                    )
                }
            }
            }
        }
    }
}

/** 12 · Step 2 · Identity */
@Composable
fun RegistrationStepIdentityScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val c = glColors()

    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Back", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
            GlScreenHeader(title = "New grower", subtitle = "2 of 4", onBack = onBack)
            GlStepper(step = 2, total = 4, stepLabels = wizardLabels)
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlBanner(
                    title = "NRC required",
                    subtitle = "National Registration Card is the primary identifier.",
                    tone = GlTone.Primary,
                    icon = "info",
                )
                GlTextField(
                    value = personal.firstName,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(firstName = it) } },
                    label = "First name",
                    placeholder = "Mary",
                    required = true,
                    error = uiState.personalErrors["firstName"],
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlTextField(
                        value = personal.middleName,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(middleName = it) } },
                        label = "Middle name",
                        modifier = Modifier.weight(1f),
                    )
                    GlTextField(
                        value = personal.lastName,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(lastName = it) } },
                        label = "Last name",
                        placeholder = "Phiri",
                        required = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlTextField(
                    value = personal.nrcNumber,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(nrcNumber = it) } },
                    label = "NRC / Passport / PACRA",
                    placeholder = "000000/00/0",
                    required = true,
                    leadingIcon = "badge",
                    trailing = {
                        if (personal.nrcNumber.isNotBlank()) {
                            GlPill(text = "Valid", tone = GlTone.Success, size = GlPillSize.Sm, leadingIcon = "check")
                        }
                    },
                )
                NrcScanButton(
                    onScan = { viewModel.applyScannedNrc(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlDropdownField(
                        label = "Sex",
                        options = GrowerFormChoices.sexOptions,
                        selectedId = personal.sex,
                        onSelected = { sex -> viewModel.updatePersonal { p -> p.copy(sex = sex) } },
                        modifier = Modifier.weight(1f),
                        required = true,
                    )
                    GlDateField(
                        value = personal.dateOfBirth,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(dateOfBirth = it) } },
                        label = "Date of birth",
                        maxDateMillis = todayUtcMillis(),
                        modifier = Modifier.weight(1f),
                    )
                }
                GlDropdownField(
                    label = "Grower category",
                    options = GrowerFormChoices.categories,
                    selectedId = personal.category,
                    onSelected = { cat -> viewModel.updatePersonal { p -> p.copy(category = cat) } },
                    required = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlDropdownField(
                        label = "Country",
                        options = GrowerFormChoices.phoneCountries,
                        selectedId = personal.country,
                        onSelected = { country -> viewModel.updatePersonal { p -> p.copy(country = country) } },
                        modifier = Modifier.weight(1f),
                        required = true,
                    )
                    GlTextField(
                        value = personal.localPhone,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(localPhone = it) } },
                        label = "Local number",
                        placeholder = "977000000",
                        leadingIcon = "phone",
                        modifier = Modifier.weight(1f),
                        required = true,
                        error = uiState.personalErrors["localPhone"],
                    )
                }
                GlTextField(
                    value = personal.email,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(email = it) } },
                    label = "Email address",
                    placeholder = "name@example.com",
                    leadingIcon = "mail",
                )
            }
            }
        }
    }
}

/** 13 · Step 3 · Farm & GPS */
@Composable
fun RegistrationStepFarmScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val crop = uiState.crop
    val provinces by viewModel.provinces.collectAsState()
    val tobaccoTypes by viewModel.tobaccoTypes.collectAsState()
    val barnTypes by viewModel.barnTypes.collectAsState()
    val sponsors by viewModel.sponsors.collectAsState()
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())
    val c = glColors()
    val gpsLabel = if (personal.gpsLatitude.isNotBlank() && personal.gpsLongitude.isNotBlank()) {
        "${personal.gpsLatitude}, ${personal.gpsLongitude}"
    } else {
        "Not captured"
    }

    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Back", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
            GlScreenHeader(title = "New grower", subtitle = "3 of 4", onBack = onBack)
            GlStepper(step = 3, total = 4, stepLabels = wizardLabels)
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlTextField(
                    value = personal.address,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(address = it) } },
                    label = "Address / farm / plot",
                    required = true,
                    error = uiState.personalErrors["address"],
                )
                GlTextField(
                    value = personal.townVillage,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(townVillage = it) } },
                    label = "Town / village",
                    required = true,
                    error = uiState.personalErrors["townVillage"],
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlDropdownField(
                        label = "Province",
                        options = provinces.map { it.id to it.name },
                        selectedId = personal.provinceId,
                        onSelected = { id -> viewModel.updatePersonal { p -> p.copy(provinceId = id, districtId = "") } },
                        modifier = Modifier.weight(1f),
                        required = true,
                    )
                    GlDropdownField(
                        label = "District",
                        options = districts.map { it.id to it.name },
                        selectedId = personal.districtId,
                        onSelected = { id -> viewModel.updatePersonal { p -> p.copy(districtId = id) } },
                        modifier = Modifier.weight(1f),
                        required = true,
                        enabled = personal.provinceId.isNotBlank(),
                    )
                }
                GlCard(contentPadding = 0.dp) {
                    GlImageSlot(label = "map · GPS location", height = 120.dp, rounded = 0.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("GPS location", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(gpsLabel, color = c.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        GlButton(
                            text = "Re-pin",
                            onClick = {
                                viewModel.updatePersonal {
                                    it.copy(gpsLatitude = "-13.85044", gpsLongitude = "32.50217")
                                }
                            },
                            variant = GlButtonVariant.Secondary,
                            size = GlButtonSize.Sm,
                            fillMaxWidth = false,
                            leadingIcon = "gps",
                        )
                    }
                }

                GlSectionHeader(title = "Crop allocation", modifier = Modifier.padding(top = 8.dp))
                GlDropdownField(
                    label = "Crop type",
                    options = tobaccoTypes.map { it.id to it.name },
                    selectedId = crop.tobaccoTypeId,
                    onSelected = { id -> viewModel.updateCrop { it.copy(tobaccoTypeId = id) } },
                    required = true,
                    error = uiState.cropErrors["tobaccoTypeId"],
                )
                GlCard(contentPadding = 14.dp) {
                    GlRow(
                        title = "Self-sponsored",
                        subtitle = "Grower funds their own crop (no sponsor)",
                        leadingIcon = "check-circle",
                        tone = GlTone.Primary,
                        trailing = {
                            GlToggle(
                                checked = crop.isSelfSponsored,
                                onCheckedChange = { checked ->
                                    viewModel.updateCrop {
                                        it.copy(isSelfSponsored = checked, sponsorId = if (checked) null else it.sponsorId)
                                    }
                                },
                            )
                        },
                    )
                }
                if (!crop.isSelfSponsored) {
                    GlDropdownField(
                        label = "Sponsor",
                        options = sponsors.map { it.id to it.name },
                        selectedId = crop.sponsorId.orEmpty(),
                        onSelected = { id -> viewModel.updateCrop { it.copy(sponsorId = id) } },
                        required = true,
                        error = uiState.cropErrors["sponsorId"],
                    )
                }
                GlTextField(
                    value = crop.hectarage,
                    onValueChange = { value -> viewModel.updateCrop { it.copy(hectarage = value) } },
                    label = "Hectarage",
                    placeholder = "e.g. 4.5",
                    required = true,
                    error = uiState.cropErrors["hectarage"],
                )
                crop.hectarage.toDoubleOrNull()?.let {
                    GlBanner(
                        title = "Estimated yield: ${calculateYieldPerHa(it)} kg/ha",
                        subtitle = "≤ 9 ha = 1500 kg/ha · > 9 ha = 3000 kg/ha",
                        tone = GlTone.Info,
                        icon = "info",
                    )
                }
                GlDropdownField(
                    label = "Barn type",
                    options = barnTypes.map { it.id to it.name },
                    selectedId = crop.barnTypeId,
                    onSelected = { id -> viewModel.updateCrop { it.copy(barnTypeId = id) } },
                    required = true,
                    error = uiState.cropErrors["barnTypeId"],
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlTextField(
                        value = crop.numberOfBarns,
                        onValueChange = { value -> viewModel.updateCrop { it.copy(numberOfBarns = value) } },
                        label = "Number of barns",
                        modifier = Modifier.weight(1f),
                        error = uiState.cropErrors["numberOfBarns"],
                    )
                    GlTextField(
                        value = crop.stringsPerBarn,
                        onValueChange = { value -> viewModel.updateCrop { it.copy(stringsPerBarn = value) } },
                        label = "Strings per barn",
                        modifier = Modifier.weight(1f),
                        error = uiState.cropErrors["stringsPerBarn"],
                    )
                }
            }
            }
        }
    }
}

/** 14 · Step 4 · Photos & review */
@Composable
fun RegistrationStepPhotosScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val c = glColors()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoTarget by remember { mutableStateOf<String?>(null) }
    var scanningTarget by remember { mutableStateOf<String?>(null) }
    var chooserTarget by remember { mutableStateOf<String?>(null) }
    var cameraCaptureTarget by remember { mutableStateOf<String?>(null) }
    var pendingCameraTarget by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }

    // Shared post-processing for both gallery pick and camera capture.
    val applyImage: (Uri, String) -> Unit = { uri, target ->
        if (target == IdDocumentScanning.TARGET_PROFILE) {
            viewModel.updatePersonal { it.copy(profilePhotoPath = uri.toString()) }
        } else {
            scanningTarget = target
            scope.launch {
                val scannedPath = IdDocumentScanning.processPick(context, uri, target)
                viewModel.updatePersonal {
                    when (target) {
                        IdDocumentScanning.TARGET_ID_FRONT -> it.copy(idFrontPath = scannedPath)
                        IdDocumentScanning.TARGET_ID_BACK -> it.copy(idBackPath = scannedPath)
                        else -> it
                    }
                }
                scanningTarget = null
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = photoTarget
        photoTarget = null
        if (uri != null && target != null) applyImage(uri, target)
    }

    // In-app capture (CameraX) instead of an external camera intent, so the
    // process is never killed and the in-progress registration survives.
    val launchCamera: (String) -> Unit = { target ->
        cameraCaptureTarget = target
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        val target = pendingCameraTarget
        pendingCameraTarget = null
        if (granted && target != null) launchCamera(target)
    }
    val photoSlots = listOf(
        Triple("Profile photo", personal.profilePhotoPath != null, IdDocumentScanning.TARGET_PROFILE),
        Triple("ID front", personal.idFrontPath != null, IdDocumentScanning.TARGET_ID_FRONT),
        Triple("ID back", personal.idBackPath != null, IdDocumentScanning.TARGET_ID_BACK),
    )

    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter(
                secondaryText = "Back",
                onSecondary = onBack,
                primaryText = if (uiState.isSaving) "Submitting…" else "Submit registration",
                onPrimary = onSubmit,
                primaryIcon = "check",
                primaryEnabled = !uiState.isSaving,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
            GlScreenHeader(title = "New grower", subtitle = "4 of 4", onBack = onBack)
            GlStepper(step = 4, total = 4, stepLabels = wizardLabels)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                GlBanner(
                    title = "3 photos required",
                    subtitle = "Profile photo · ID front · ID back.",
                    tone = GlTone.Warning,
                    icon = "camera",
                )
                uiState.personalErrors["profilePhoto"]?.let {
                    GlBanner(title = it, tone = GlTone.Danger, icon = "warning")
                }
                GlSectionHeader(title = "Required", modifier = Modifier.padding(top = 14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    photoSlots.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (label, captured, target) ->
                                val processing = scanningTarget == target
                                PhotoCaptureCard(
                                    label = label,
                                    captured = captured,
                                    processing = processing,
                                    onClick = {
                                        if (processing) return@PhotoCaptureCard
                                        chooserTarget = target
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                uiState.saveError?.let {
                    GlBanner(title = it, tone = GlTone.Danger, icon = "warning", modifier = Modifier.padding(top = 12.dp))
                }
            }
            }
        }
    }

    chooserTarget?.let { target ->
        PhotoSourceDialog(
            onDismiss = { chooserTarget = null },
            onCamera = {
                chooserTarget = null
                if (hasCameraPermission) {
                    launchCamera(target)
                } else {
                    pendingCameraTarget = target
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGallery = {
                chooserTarget = null
                photoTarget = target
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }

    cameraCaptureTarget?.let { target ->
        val label = when (target) {
            IdDocumentScanning.TARGET_ID_FRONT -> "ID front"
            IdDocumentScanning.TARGET_ID_BACK -> "ID back"
            else -> "Profile photo"
        }
        CameraCaptureScreen(
            title = label,
            onCaptured = { uri ->
                cameraCaptureTarget = null
                applyImage(uri, target)
            },
            onCancel = { cameraCaptureTarget = null },
        )
    }
}

/** Bottom chooser letting the user capture with the camera or pick from the gallery. */
@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    val c = glColors()
    Dialog(onDismissRequest = onDismiss) {
        GlCard(contentPadding = 8.dp) {
            Column {
                Text(
                    "Add photo",
                    color = c.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
                GlRow(
                    title = "Take photo",
                    subtitle = "Use the camera",
                    leadingIcon = "camera",
                    tone = GlTone.Primary,
                    onClick = onCamera,
                )
                GlDivider()
                GlRow(
                    title = "Choose from gallery",
                    subtitle = "Pick an existing photo",
                    leadingIcon = "image",
                    tone = GlTone.Primary,
                    onClick = onGallery,
                )
                Spacer(Modifier.height(6.dp))
                GlButton(text = "Cancel", onClick = onDismiss, variant = GlButtonVariant.Ghost)
            }
        }
    }
}

@Composable
private fun PhotoCaptureCard(
    label: String,
    captured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    processing: Boolean = false,
) {
    val c = glColors()
    GlCard(onClick = onClick, contentPadding = 0.dp, modifier = modifier) {
        GlImageSlot(
            label = when {
                processing -> "Scanning…"
                captured -> ""
                else -> "tap to capture"
            },
            height = 120.dp,
            rounded = 0.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            GlIcon(
                if (captured) "check-circle" else "camera",
                size = 18.dp,
                tint = if (captured) c.success else c.textMuted,
            )
        }
    }
}

/** 15 · Registration issued */
@Composable
fun RegistrationSuccessScreen(
    onViewGrower: () -> Unit,
    onBackToList: () -> Unit,
    growerName: String = "Grower",
    provisionalId: String = "Pending sync",
) {
    val c = glColors()
    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlButton(text = "View grower", onClick = onViewGrower)
                GlButton(text = "Back to growers list", onClick = onBackToList, variant = GlButtonVariant.Ghost)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(c.successSoft),
                contentAlignment = Alignment.Center,
            ) {
                GlIcon("check", size = 48.dp, tint = c.success)
            }
            Text(
                "Registered successfully",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "$growerName has been queued for sync. You'll receive a confirmation when the grower ID is issued.",
                color = c.textMuted,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            GlCard(contentPadding = 14.dp, modifier = Modifier.padding(top = 24.dp).fillMaxWidth()) {
                GlFieldRow(label = "Provisional ID", value = provisionalId, mono = true)
                GlDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status", color = c.textMuted, fontSize = 13.sp)
                    GlPill(text = "Pending sync", tone = GlTone.Warning, size = GlPillSize.Sm)
                }
                GlDivider()
                GlFieldRow(label = "District", value = "Chadiza")
            }
        }
    }
}

/** 10 · Scan NRC */
@Composable
fun ScanNrcScreen(
    onClose: () -> Unit,
    onUseId: () -> Unit,
    onManualEntry: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val context = LocalContext.current
    val c = glColors()
    var detectedNrc by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F0C)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    GlIcon("x", size = 20.dp, tint = Color.White)
                }
                Text("Scan NRC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlIcon("flash", size = 20.dp, tint = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B2C26)),
                ) {
                    if (!hasPermission) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Camera permission required",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                            )
                            GlButton(
                                text = "Grant permission",
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                variant = GlButtonVariant.Outline,
                                size = GlButtonSize.Sm,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    } else {
                        NrcCameraPreview(
                            onScan = { value ->
                                if (detectedNrc == null) detectedNrc = value
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(16.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    )
                    if (detectedNrc != null) {
                        Text(
                            detectedNrc!!,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(14.dp),
                ) {
                    if (detectedNrc != null) {
                        Text(
                            "Detected · $detectedNrc",
                            color = c.gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Confirm to autofill the NRC field",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        Text(
                            "Scanning…",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                        Text(
                            "Align the NRC or passport number within the frame",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                GlButton(
                    text = "Use this ID",
                    onClick = {
                        detectedNrc?.let { viewModel.applyScannedNrc(it) }
                        onUseId()
                    },
                    enabled = detectedNrc != null,
                )
                GlButton(
                    text = "Enter manually",
                    onClick = onManualEntry,
                    variant = GlButtonVariant.Ghost,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
