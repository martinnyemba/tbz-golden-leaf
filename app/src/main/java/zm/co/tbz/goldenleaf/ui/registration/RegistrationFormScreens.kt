package zm.co.tbz.goldenleaf.ui.registration

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlImageSlot
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import zm.co.tbz.goldenleaf.ui.scan.IdDocumentScanning
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlDateField
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.todayUtcMillis
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.NrcScanButton
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.components.ErrorText

@Composable
fun NewGrowerRegistrationScreen(
    onSaved: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val tobaccoTypes by viewModel.tobaccoTypes.collectAsState()
    val sponsors by viewModel.sponsors.collectAsState()
    val barnTypes by viewModel.barnTypes.collectAsState()
    val personal = uiState.personal
    val crop = uiState.crop
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = "New registration",
                subtitle = if (uiState.registrationStep == 1) "Personal details" else "Crop details",
            )
            GlStepper(step = uiState.registrationStep, total = 2)
            when (uiState.registrationStep) {
                1 -> PersonalDetailsStep(
                    personal = personal,
                    errors = uiState.personalErrors,
                    provinces = provinces.map { it.id to it.name },
                    districts = districts.map { it.id to it.name },
                    onPersonalChange = { updated -> viewModel.updatePersonal { updated } },
                    onNext = { viewModel.goToCropStep() },
                    onCancel = onSaved,
                )
                else -> CropDetailsStep(
                    crop = crop,
                    errors = uiState.cropErrors,
                    tobaccoTypes = tobaccoTypes.map { it.id to it.name },
                    sponsors = sponsors.map { it.id to it.name },
                    barnTypes = barnTypes.map { it.id to it.name },
                    onCropChange = { updated -> viewModel.updateCrop { updated } },
                    onBack = { viewModel.goToPersonalStep() },
                    onSubmit = { viewModel.submitRegistration { onSaved() } },
                    isSaving = uiState.isSaving,
                    saveError = uiState.saveError,
                )
            }
        }
    }
}

@Composable
private fun PersonalDetailsStep(
    personal: PersonalDetailsForm,
    errors: Map<String, String>,
    provinces: List<Pair<String, String>>,
    districts: List<Pair<String, String>>,
    onPersonalChange: (PersonalDetailsForm) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            scope.launch {
                val loc = suspendCancellableCoroutine { cont ->
                    locationClient.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
                if (loc != null) {
                    onPersonalChange(
                        personal.copy(
                            gpsLatitude = loc.latitude.toString(),
                            gpsLongitude = loc.longitude.toString(),
                        ),
                    )
                }
            }
        }
    }

    var photoTarget by remember { mutableStateOf<String?>(null) }
    var scanningTarget by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val target = photoTarget
        photoTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        if (target == "profile") {
            onPersonalChange(personal.copy(profilePhotoPath = uri.toString()))
            return@rememberLauncherForActivityResult
        }
        scanningTarget = target
        scope.launch {
            val scannedPath = IdDocumentScanning.processPick(context, uri, target)
            when (target) {
                IdDocumentScanning.TARGET_ID_FRONT -> onPersonalChange(personal.copy(idFrontPath = scannedPath))
                IdDocumentScanning.TARGET_ID_BACK -> onPersonalChange(personal.copy(idBackPath = scannedPath))
            }
            scanningTarget = null
        }
    }

    ScrollableFormColumn {
        GlSectionHeader(title = "Grower type")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GrowerFormChoices.growerTypes.forEach { (value, label) ->
                GrowerTypeOption(
                    label = label,
                    icon = if (value == "COMPANY") "building" else "profile",
                    selected = personal.growerType == value,
                    onClick = { onPersonalChange(personal.copy(growerType = value)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        GlTextField(personal.firstName, { onPersonalChange(personal.copy(firstName = it)) }, label = "First name", required = true, error = errors["firstName"])
        GlTextField(personal.middleName, { onPersonalChange(personal.copy(middleName = it)) }, label = "Middle name")
        GlTextField(personal.lastName, { onPersonalChange(personal.copy(lastName = it)) }, label = "Last name", required = true)
        GlTextField(personal.nrcNumber, { onPersonalChange(personal.copy(nrcNumber = it)) }, label = "NRC / Passport / PACRA", required = true, error = errors["nrcNumber"])
        NrcScanButton(
            onScan = { onPersonalChange(personal.copy(nrcNumber = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        GlDropdownField("Sex", GrowerFormChoices.sexOptions, personal.sex, { onPersonalChange(personal.copy(sex = it)) })
        GlDateField(personal.dateOfBirth, { onPersonalChange(personal.copy(dateOfBirth = it)) }, label = "Date of birth", maxDateMillis = todayUtcMillis())
        GlDropdownField("Category", GrowerFormChoices.categories, personal.category, { onPersonalChange(personal.copy(category = it)) })
        GlDropdownField("Country", GrowerFormChoices.phoneCountries, personal.country, { onPersonalChange(personal.copy(country = it)) })
        GlTextField(personal.localPhone, { onPersonalChange(personal.copy(localPhone = it)) }, label = "Local phone number", leadingIcon = "mail", required = true, error = errors["localPhone"])
        GlTextField(personal.email, { onPersonalChange(personal.copy(email = it)) }, label = "Email")
        GlTextField(personal.address, { onPersonalChange(personal.copy(address = it)) }, label = "Address / farm / plot", required = true, error = errors["address"])
        GlTextField(personal.townVillage, { onPersonalChange(personal.copy(townVillage = it)) }, label = "Town / village", required = true, error = errors["townVillage"])
        GlDropdownField("Province", provinces, personal.provinceId, {
            onPersonalChange(personal.copy(provinceId = it, districtId = ""))
        })
        GlDropdownField("District", districts, personal.districtId, { onPersonalChange(personal.copy(districtId = it)) }, enabled = personal.provinceId.isNotBlank())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlTextField(personal.gpsLatitude, { onPersonalChange(personal.copy(gpsLatitude = it)) }, label = "GPS latitude", modifier = Modifier.weight(1f))
            GlTextField(personal.gpsLongitude, { onPersonalChange(personal.copy(gpsLongitude = it)) }, label = "GPS longitude", modifier = Modifier.weight(1f))
        }
        GlButton(
            text = "Capture GPS",
            onClick = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            variant = GlButtonVariant.Outline,
            leadingIcon = "gps",
        )

        GlSectionHeader(title = "Documents")
        PhotoPickRow("Profile photo", personal.profilePhotoPath, errors["profilePhoto"]) {
            photoTarget = "profile"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        PhotoPickRow("ID front", personal.idFrontPath, errors["idFront"], isProcessing = scanningTarget == "front") {
            photoTarget = "front"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        PhotoPickRow("ID back", personal.idBackPath, errors["idBack"], isProcessing = scanningTarget == "back") {
            photoTarget = "back"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(text = "Cancel", onClick = onCancel, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f))
            GlButton(text = "Next", onClick = onNext, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CropDetailsStep(
    crop: CropDetailsForm,
    errors: Map<String, String>,
    tobaccoTypes: List<Pair<String, String>>,
    sponsors: List<Pair<String, String>>,
    barnTypes: List<Pair<String, String>>,
    onCropChange: (CropDetailsForm) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    isSaving: Boolean,
    saveError: String?,
) {
    val hectarage = crop.hectarage.toDoubleOrNull()
    val yieldPerHa = hectarage?.let { calculateYieldPerHa(it) }

    ScrollableFormColumn {
        GlDropdownField("Crop type", tobaccoTypes, crop.tobaccoTypeId, { onCropChange(crop.copy(tobaccoTypeId = it)) }, required = true)
        SelfSponsoredCheckbox(
            checked = crop.isSelfSponsored,
            onCheckedChange = { checked ->
                onCropChange(crop.copy(isSelfSponsored = checked, sponsorId = if (checked) null else crop.sponsorId))
            },
        )
        if (!crop.isSelfSponsored) {
            GlDropdownField("Sponsor", sponsors, crop.sponsorId.orEmpty(), { onCropChange(crop.copy(sponsorId = it)) }, error = errors["sponsorId"])
        }
        GlTextField(crop.hectarage, { onCropChange(crop.copy(hectarage = it)) }, label = "Hectarage", required = true, error = errors["hectarage"])
        yieldPerHa?.let {
            GlBanner(title = "Estimated yield: $it kg/ha", tone = GlTone.Info, icon = "info")
        }
        GlTextField(crop.numberOfBarns, { onCropChange(crop.copy(numberOfBarns = it)) }, label = "Number of barns", error = errors["numberOfBarns"])
        GlDropdownField("Barn type", barnTypes, crop.barnTypeId, { onCropChange(crop.copy(barnTypeId = it)) })
        GlTextField(crop.stringsPerBarn, { onCropChange(crop.copy(stringsPerBarn = it)) }, label = "Strings per barn", error = errors["stringsPerBarn"])
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlTextField(crop.gpsLatitude, { onCropChange(crop.copy(gpsLatitude = it)) }, label = "GPS latitude", modifier = Modifier.weight(1f))
            GlTextField(crop.gpsLongitude, { onCropChange(crop.copy(gpsLongitude = it)) }, label = "GPS longitude", modifier = Modifier.weight(1f))
        }
        saveError?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(text = "Back", onClick = onBack, variant = GlButtonVariant.Outline, enabled = !isSaving, modifier = Modifier.weight(1f))
            GlButton(
                text = if (isSaving) "Saving…" else "Save & queue sync",
                onClick = onSubmit,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GrowerTypeOption(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    GlCard(
        modifier = modifier,
        onClick = onClick,
        accent = if (selected) GlAccent.Primary else GlAccent.None,
        contentPadding = 14.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlIcon(icon, size = 26.dp, tint = if (selected) c.primary else c.textMuted)
            Text(
                label,
                color = if (selected) c.primary else c.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PhotoPickRow(
    label: String,
    path: String?,
    error: String?,
    isProcessing: Boolean = false,
    onPick: () -> Unit,
) {
    Column {
        GlCard(contentPadding = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlIcon(if (path != null) "check-circle" else "camera", size = 20.dp, tint = if (path != null) glColors().success else glColors().textMuted)
                    Column {
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = glColors().text)
                        Text(
                            if (isProcessing) "Scanning…" else if (path != null) "Selected" else "Not selected",
                            fontSize = 12.sp,
                            color = glColors().textMuted,
                        )
                    }
                }
                GlButton(
                    text = if (isProcessing) "Scanning…" else if (path != null) "Replace" else "Upload",
                    onClick = onPick,
                    variant = GlButtonVariant.Outline,
                    size = GlButtonSize.Sm,
                    fillMaxWidth = false,
                    enabled = !isProcessing,
                )
            }
        }
        error?.let { ErrorText(it, Modifier.padding(top = 4.dp, start = 4.dp)) }
    }
}

@Composable
fun GrowerEditScreen(
    localId: String,
    onSaved: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by remember(localId) { viewModel.observeGrower(localId) }.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val personal = uiState.personal
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())
    val c = glColors()

    LaunchedEffect(grower?.local_id) {
        grower?.let { viewModel.loadPersonalFromGrower(it) }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Edit grower")
            ScrollableFormColumn {
                GlTextField(personal.firstName, { v -> viewModel.updatePersonal { it.copy(firstName = v) } }, label = "First name", required = true, error = uiState.personalErrors["firstName"])
                GlTextField(personal.middleName, { v -> viewModel.updatePersonal { it.copy(middleName = v) } }, label = "Middle name")
                GlTextField(personal.lastName, { v -> viewModel.updatePersonal { it.copy(lastName = v) } }, label = "Last name", required = true)
                GlTextField(personal.nrcNumber, { v -> viewModel.updatePersonal { it.copy(nrcNumber = v) } }, label = "NRC / PACRA", required = true)
                GlDropdownField("Sex", GrowerFormChoices.sexOptions, personal.sex, { v -> viewModel.updatePersonal { it.copy(sex = v) } })
                GlDateField(personal.dateOfBirth, { v -> viewModel.updatePersonal { it.copy(dateOfBirth = v) } }, label = "Date of birth", maxDateMillis = todayUtcMillis())
                GlTextField(personal.localPhone, { v -> viewModel.updatePersonal { it.copy(localPhone = v) } }, label = "Phone")
                GlTextField(personal.email, { v -> viewModel.updatePersonal { it.copy(email = v) } }, label = "Email")
                GlTextField(personal.address, { v -> viewModel.updatePersonal { it.copy(address = v) } }, label = "Address")
                GlTextField(personal.townVillage, { v -> viewModel.updatePersonal { it.copy(townVillage = v) } }, label = "Town / village")
                GlDropdownField("Province", provinces.map { it.id to it.name }, personal.provinceId, { v ->
                    viewModel.updatePersonal { it.copy(provinceId = v, districtId = "") }
                })
                GlDropdownField("District", districts.map { it.id to it.name }, personal.districtId, { v ->
                    viewModel.updatePersonal { it.copy(districtId = v) }
                })
                uiState.saveError?.let { ErrorText(it) }
                GlButton(
                    text = if (uiState.isSaving) "Saving…" else "Save changes",
                    onClick = { viewModel.saveGrowerEdit(localId, onSaved) },
                    enabled = !uiState.isSaving,
                )
            }
        }
    }
}

@Composable
fun GrowerCorrectionScreen(
    localId: String,
    onBack: (() -> Unit)? = null,
    onDone: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by remember(localId) { viewModel.observeGrower(localId) }.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val c = glColors()
    val name = grower?.let { listOfNotNull(it.first_name, it.middle_name, it.last_name).joinToString(" ") } ?: "Grower"
    val tbzId = grower?.tbz_id ?: grower?.nrc_number ?: localId.take(12)
    val correctionReason = grower?.correction_reason
    val correctionDate = grower?.correction_requested_at
    val correctionReviewer = grower?.correction_reviewer_name
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoTarget by remember { mutableStateOf<String?>(null) }
    var scanningTarget by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = photoTarget
        photoTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
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

    LaunchedEffect(grower?.local_id) {
        grower?.let { viewModel.loadPersonalFromGrower(it) }
    }

    GlScaffold(
        containerColor = c.bg,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(c.surface).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlButton(
                    text = if (uiState.correctionDraftSaved) "Saved locally" else "Save draft",
                    onClick = { viewModel.saveCorrectionDraft(localId) },
                    variant = GlButtonVariant.Outline,
                    fillMaxWidth = false,
                    leadingIcon = "check",
                    modifier = Modifier.weight(1f),
                )
                GlButton(
                    text = if (uiState.isSaving) "Submitting…" else "Fix & Resubmit",
                    onClick = { viewModel.resubmitCorrection(localId, onDone) },
                    enabled = !uiState.isSaving,
                    fillMaxWidth = false,
                    leadingIcon = "cloud-up",
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
            GlScreenHeader(title = "Fix & resubmit", subtitle = "Returned for correction", onBack = onBack)
            GlCard(
                contentPadding = 14.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .border(1.5.dp, c.gold, RoundedCornerShape(16.dp)),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.gold),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlIcon("warning", size = 18.dp, tint = c.text)
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlPill(text = "Returned for correction", tone = GlTone.Returned, size = GlPillSize.Sm)
                            correctionDate?.let {
                                Text(it, color = c.textMuted, fontSize = 11.sp)
                            }
                        }
                        correctionReviewer?.let {
                            Text("Reviewer: $it", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        }
                        Text(
                            correctionReason ?: "Please review and correct the flagged fields before resubmitting.",
                            color = c.text,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            GlCard(
                contentPadding = 12.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlAvatar(name = name, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(name, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(tbzId, color = c.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    GlPill(text = "Draft correction", tone = GlTone.Returned, size = GlPillSize.Sm)
                }
            }
            GlSectionHeader(title = "Correction fields", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlTextField(
                    value = personal.nrcNumber,
                    onValueChange = { v -> viewModel.updatePersonal { it.copy(nrcNumber = v) } },
                    label = "NRC / Passport / PACRA",
                    required = true,
                    leadingIcon = "badge",
                    helper = if (correctionReason != null) "Flagged by reviewer — verify against original document" else null,
                    error = uiState.personalErrors["nrcNumber"],
                )
                GlTextField(personal.firstName, { v -> viewModel.updatePersonal { it.copy(firstName = v) } }, label = "First name", required = true, error = uiState.personalErrors["firstName"])
                GlTextField(personal.lastName, { v -> viewModel.updatePersonal { it.copy(lastName = v) } }, label = "Last name", required = true, error = uiState.personalErrors["lastName"])
                GlDropdownField("Gender", listOf("FEMALE" to "Female", "MALE" to "Male"), personal.sex, { v -> viewModel.updatePersonal { it.copy(sex = v) } })
                GlTextField(personal.dateOfBirth, { v -> viewModel.updatePersonal { it.copy(dateOfBirth = v) } }, label = "Date of birth", leadingIcon = "calendar")
                GlTextField(personal.localPhone, { v -> viewModel.updatePersonal { it.copy(localPhone = v) } }, label = "Phone", leadingIcon = "phone")
            }
            GlSectionHeader(title = "Re-upload ID documents", modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                GlBanner(
                    title = "Reviewer flagged ID photo",
                    subtitle = "Re-photograph the NRC front clearly in good light.",
                    tone = GlTone.Warning,
                    icon = "camera",
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("NRC · Front", IdDocumentScanning.TARGET_ID_FRONT, personal.idFrontPath != null),
                        Triple("NRC · Back", IdDocumentScanning.TARGET_ID_BACK, personal.idBackPath != null),
                    ).forEach { (label, target, captured) ->
                        val processing = scanningTarget == target
                        val flagged = target == IdDocumentScanning.TARGET_ID_FRONT && correctionReason != null
                        GlCard(
                            contentPadding = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (flagged) 2.dp else 1.dp,
                                    color = if (flagged) c.gold else c.outlineSoft,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                            onClick = {
                                if (processing) return@GlCard
                                photoTarget = target
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            GlImageSlot(
                                label = when {
                                    processing -> "Scanning…"
                                    !captured -> if (flagged) "retake required" else "tap to capture"
                                    else -> ""
                                },
                                height = 100.dp,
                                rounded = 0.dp,
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                when {
                                    processing -> GlPill(text = "Scanning", tone = GlTone.Warning, size = GlPillSize.Sm)
                                    flagged && !captured -> GlPill(text = "Flagged", tone = GlTone.Warning, size = GlPillSize.Sm, leadingIcon = "warning")
                                    captured -> GlIcon("check-circle", size = 18.dp, tint = c.success)
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.correctionDraftSaved) {
                GlBanner(
                    title = "Correction saved locally",
                    subtitle = "Tap 'Fix & Resubmit' when ready to send for review.",
                    tone = GlTone.Success,
                    icon = "check-circle",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            uiState.saveError?.let { ErrorText(it, Modifier.padding(horizontal = 20.dp)) }
            Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CropAllocationScreen(
    growerLocalId: String,
    onSaved: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val crop = uiState.crop
    val tobaccoTypes by viewModel.tobaccoTypes.collectAsState()
    val sponsors by viewModel.sponsors.collectAsState()
    val barnTypes by viewModel.barnTypes.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Crop allocation")
            CropDetailsStep(
                crop = crop,
                errors = uiState.cropErrors,
                tobaccoTypes = tobaccoTypes.map { it.id to it.name },
                sponsors = sponsors.map { it.id to it.name },
                barnTypes = barnTypes.map { it.id to it.name },
                onCropChange = { updated -> viewModel.updateCrop { updated } },
                onBack = onSaved,
                onSubmit = { viewModel.submitCropAllocation(growerLocalId, onSaved) },
                isSaving = uiState.isSaving,
                saveError = uiState.saveError,
            )
        }
    }
}
