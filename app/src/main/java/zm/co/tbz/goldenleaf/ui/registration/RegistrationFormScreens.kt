package zm.co.tbz.goldenleaf.ui.registration

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

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

    Scaffold(topBar = {
        TbzTopBar(
            if (uiState.registrationStep == 1) "Registration — Personal" else "Registration — Crop",
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { if (uiState.registrationStep == 1) 0.5f else 1f },
                modifier = Modifier.fillMaxWidth(),
            )
            when (uiState.registrationStep) {
                1 -> PersonalDetailsStep(
                    personal = personal,
                    errors = uiState.personalErrors,
                    provinces = provinces.map { it.id to it.name },
                    districts = districts.map { it.id to it.name },
                    onPersonalChange = { viewModel.updatePersonal { it } },
                    onNext = { viewModel.goToCropStep() },
                    onCancel = onSaved,
                )
                else -> CropDetailsStep(
                    crop = crop,
                    errors = uiState.cropErrors,
                    tobaccoTypes = tobaccoTypes.map { it.id to it.name },
                    sponsors = sponsors.map { it.id to it.name },
                    barnTypes = barnTypes.map { it.id to it.name },
                    onCropChange = { viewModel.updateCrop { it } },
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
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.toString()?.let { path ->
            when (photoTarget) {
                "profile" -> onPersonalChange(personal.copy(profilePhotoPath = path))
                "front" -> onPersonalChange(personal.copy(idFrontPath = path))
                "back" -> onPersonalChange(personal.copy(idBackPath = path))
            }
        }
        photoTarget = null
    }

    ScrollableFormColumn {
        TbzRadioGroup(
            label = "Grower type",
            options = GrowerFormChoices.growerTypes,
            selected = personal.growerType,
            onSelected = { onPersonalChange(personal.copy(growerType = it)) },
        )
        FormTextField(personal.firstName, { onPersonalChange(personal.copy(firstName = it)) }, "First name", error = errors["firstName"])
        FormTextField(personal.middleName, { onPersonalChange(personal.copy(middleName = it)) }, "Middle name")
        FormTextField(personal.lastName, { onPersonalChange(personal.copy(lastName = it)) }, "Last name")
        FormTextField(personal.nrcNumber, { onPersonalChange(personal.copy(nrcNumber = it)) }, "NRC / Passport / PACRA", error = errors["nrcNumber"])
        TbzDropdownField("Sex", GrowerFormChoices.sexOptions, personal.sex, { onPersonalChange(personal.copy(sex = it)) })
        FormTextField(personal.dateOfBirth, { onPersonalChange(personal.copy(dateOfBirth = it)) }, "Date of birth (YYYY-MM-DD)")
        TbzDropdownField("Category", GrowerFormChoices.categories, personal.category, { onPersonalChange(personal.copy(category = it)) })
        TbzDropdownField("Country", GrowerFormChoices.phoneCountries, personal.country, { onPersonalChange(personal.copy(country = it)) })
        FormTextField(personal.localPhone, { onPersonalChange(personal.copy(localPhone = it)) }, "Local phone number", error = errors["localPhone"])
        FormTextField(personal.email, { onPersonalChange(personal.copy(email = it)) }, "Email")
        FormTextField(personal.address, { onPersonalChange(personal.copy(address = it)) }, "Address / farm / plot", error = errors["address"])
        FormTextField(personal.townVillage, { onPersonalChange(personal.copy(townVillage = it)) }, "Town / village", error = errors["townVillage"])
        TbzDropdownField("Province", provinces, personal.provinceId, {
            onPersonalChange(personal.copy(provinceId = it, districtId = ""))
        })
        TbzDropdownField("District", districts, personal.districtId, { onPersonalChange(personal.copy(districtId = it)) }, enabled = personal.provinceId.isNotBlank())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTextField(personal.gpsLatitude, { onPersonalChange(personal.copy(gpsLatitude = it)) }, "GPS latitude", modifier = Modifier.weight(1f))
            FormTextField(personal.gpsLongitude, { onPersonalChange(personal.copy(gpsLongitude = it)) }, "GPS longitude", modifier = Modifier.weight(1f))
        }
        OutlinedButton(onClick = {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }) { Text("Capture GPS") }

        FormSectionTitle("Documents")
        PhotoPickRow("Profile photo", personal.profilePhotoPath, errors["profilePhoto"]) {
            photoTarget = "profile"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        PhotoPickRow("ID front", personal.idFrontPath, errors["idFront"]) {
            photoTarget = "front"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        PhotoPickRow("ID back", personal.idBackPath, errors["idBack"]) {
            photoTarget = "back"
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        FormActionRow("Next", onNext, secondaryLabel = "Cancel", onSecondary = onCancel)
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
        TbzDropdownField("Crop type", tobaccoTypes, crop.tobaccoTypeId, { onCropChange(crop.copy(tobaccoTypeId = it)) })
        SelfSponsoredCheckbox(
            checked = crop.isSelfSponsored,
            onCheckedChange = { checked ->
                onCropChange(crop.copy(isSelfSponsored = checked, sponsorId = if (checked) null else crop.sponsorId))
            },
        )
        if (!crop.isSelfSponsored) {
            TbzDropdownField("Sponsor", sponsors, crop.sponsorId.orEmpty(), { onCropChange(crop.copy(sponsorId = it)) })
            errors["sponsorId"]?.let { ErrorText(it) }
        }
        FormTextField(crop.hectarage, { onCropChange(crop.copy(hectarage = it)) }, "Hectarage", error = errors["hectarage"])
        yieldPerHa?.let {
            Text("Yield per ha: $it kg/ha", style = MaterialTheme.typography.bodyMedium)
        }
        FormTextField(crop.numberOfBarns, { onCropChange(crop.copy(numberOfBarns = it)) }, "Number of barns", error = errors["numberOfBarns"])
        TbzDropdownField("Barn type", barnTypes, crop.barnTypeId, { onCropChange(crop.copy(barnTypeId = it)) })
        FormTextField(crop.stringsPerBarn, { onCropChange(crop.copy(stringsPerBarn = it)) }, "Strings per barn", error = errors["stringsPerBarn"])
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTextField(crop.gpsLatitude, { onCropChange(crop.copy(gpsLatitude = it)) }, "GPS latitude", modifier = Modifier.weight(1f))
            FormTextField(crop.gpsLongitude, { onCropChange(crop.copy(gpsLongitude = it)) }, "GPS longitude", modifier = Modifier.weight(1f))
        }
        saveError?.let { ErrorText(it) }
        FormActionRow(
            primaryLabel = if (isSaving) "Saving…" else "Save & queue sync",
            onPrimary = onSubmit,
            secondaryLabel = "Back",
            onSecondary = onBack,
            primaryEnabled = !isSaving,
        )
    }
}

@Composable
private fun PhotoPickRow(
    label: String,
    path: String?,
    error: String?,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(label)
            Text(
                path?.let { "Selected" } ?: "Not selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { ErrorText(it) }
        }
        OutlinedButton(onClick = onPick) { Text("Upload") }
    }
}

@Composable
fun GrowerEditScreen(
    localId: String,
    onSaved: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by viewModel.observeGrower(localId).collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val personal = uiState.personal
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())

    LaunchedEffect(grower?.local_id) {
        grower?.let { viewModel.loadPersonalFromGrower(it) }
    }

    Scaffold(topBar = { TbzTopBar("Edit Grower") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableFormColumn {
                FormTextField(personal.firstName, { v -> viewModel.updatePersonal { it.copy(firstName = v) } }, "First name", error = uiState.personalErrors["firstName"])
                FormTextField(personal.middleName, { v -> viewModel.updatePersonal { it.copy(middleName = v) } }, "Middle name")
                FormTextField(personal.lastName, { v -> viewModel.updatePersonal { it.copy(lastName = v) } }, "Last name")
                FormTextField(personal.nrcNumber, { v -> viewModel.updatePersonal { it.copy(nrcNumber = v) } }, "NRC / PACRA")
                TbzDropdownField("Sex", GrowerFormChoices.sexOptions, personal.sex, { v -> viewModel.updatePersonal { it.copy(sex = v) } })
                FormTextField(personal.dateOfBirth, { v -> viewModel.updatePersonal { it.copy(dateOfBirth = v) } }, "Date of birth")
                FormTextField(personal.localPhone, { v -> viewModel.updatePersonal { it.copy(localPhone = v) } }, "Phone")
                FormTextField(personal.email, { v -> viewModel.updatePersonal { it.copy(email = v) } }, "Email")
                FormTextField(personal.address, { v -> viewModel.updatePersonal { it.copy(address = v) } }, "Address")
                FormTextField(personal.townVillage, { v -> viewModel.updatePersonal { it.copy(townVillage = v) } }, "Town / village")
                TbzDropdownField("Province", provinces.map { it.id to it.name }, personal.provinceId, { v ->
                    viewModel.updatePersonal { it.copy(provinceId = v, districtId = "") }
                })
                TbzDropdownField("District", districts.map { it.id to it.name }, personal.districtId, { v ->
                    viewModel.updatePersonal { it.copy(districtId = v) }
                })
                uiState.saveError?.let { ErrorText(it) }
                Button(
                    onClick = { viewModel.saveGrowerEdit(localId, onSaved) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                ) { Text(if (uiState.isSaving) "Saving…" else "Save changes") }
            }
        }
    }
}

@Composable
fun GrowerCorrectionScreen(
    localId: String,
    onDone: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by viewModel.observeGrower(localId).collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val personal = uiState.personal
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())

    LaunchedEffect(grower?.local_id) {
        grower?.let { viewModel.loadPersonalFromGrower(it) }
    }

    Scaffold(topBar = { TbzTopBar("Fix & Resubmit") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InfoBanner("Status: RETURNED_FOR_CORRECTION — update fields and resubmit for review.")
            ScrollableFormColumn {
                FormTextField(personal.firstName, { v -> viewModel.updatePersonal { it.copy(firstName = v) } }, "First name")
                FormTextField(personal.lastName, { v -> viewModel.updatePersonal { it.copy(lastName = v) } }, "Last name")
                FormTextField(personal.nrcNumber, { v -> viewModel.updatePersonal { it.copy(nrcNumber = v) } }, "NRC")
                FormTextField(personal.address, { v -> viewModel.updatePersonal { it.copy(address = v) } }, "Address")
                TbzDropdownField("Province", provinces.map { it.id to it.name }, personal.provinceId, { v ->
                    viewModel.updatePersonal { it.copy(provinceId = v, districtId = "") }
                })
                TbzDropdownField("District", districts.map { it.id to it.name }, personal.districtId, { v ->
                    viewModel.updatePersonal { it.copy(districtId = v) }
                })
                uiState.saveError?.let { ErrorText(it) }
                Button(
                    onClick = { viewModel.resubmitCorrection(localId, onDone) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                ) { Text(if (uiState.isSaving) "Submitting…" else "Save & resubmit") }
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

    Scaffold(topBar = { TbzTopBar("Crop allocation") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CropDetailsStep(
                crop = crop,
                errors = uiState.cropErrors,
                tobaccoTypes = tobaccoTypes.map { it.id to it.name },
                sponsors = sponsors.map { it.id to it.name },
                barnTypes = barnTypes.map { it.id to it.name },
                onCropChange = { viewModel.updateCrop { it } },
                onBack = onSaved,
                onSubmit = { viewModel.submitCropAllocation(growerLocalId, onSaved) },
                isSaving = uiState.isSaving,
                saveError = uiState.saveError,
            )
        }
    }
}
