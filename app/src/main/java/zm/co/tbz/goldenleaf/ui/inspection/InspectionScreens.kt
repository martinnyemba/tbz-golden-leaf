package zm.co.tbz.goldenleaf.ui.inspection

import android.Manifest
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.ModuleHubCard
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzStatCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormActionRow
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import zm.co.tbz.goldenleaf.ui.registration.TbzDropdownField
import zm.co.tbz.goldenleaf.ui.registration.TbzRadioGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InspectionHubScreen(
    onSchedule: () -> Unit,
    onLocalSchedules: () -> Unit,
    onPortalList: () -> Unit,
    onReports: () -> Unit,
    onHighRisk: () -> Unit,
    onLookup: () -> Unit = {},
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()

    Scaffold(topBar = { TbzTopBar("Inspection") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Scheduled", stats.scheduled.toString(), Modifier.weight(1f))
                TbzStatCard("In progress", stats.inProgress.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Completed", stats.completed.toString(), Modifier.weight(1f))
                TbzStatCard("High risk", stats.highRisk.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Schedules pending", stats.schedulesPendingSync.toString(), Modifier.weight(1f))
                TbzStatCard("Reports pending", stats.reportsPendingSync.toString(), Modifier.weight(1f))
            }
            Button(onClick = viewModel::triggerSync, modifier = Modifier.fillMaxWidth()) {
                Text("Sync now")
            }
            ModuleHubCard("Schedule inspection", "Offline-first grower inspection schedule", onSchedule)
            ModuleHubCard("Local schedules", "Pending and failed schedule sync", onLocalSchedules)
            ModuleHubCard("Portal inspections", "Cached server inspections", onPortalList)
            ModuleHubCard("Conducted reports", "Field, nursery, curing, validation queue", onReports)
            ModuleHubCard("High-risk growers", "Validations with risk score ≥ 70", onHighRisk)
            ModuleHubCard("Grower lookup", "Search cached growers for scheduling", onLookup)
        }
    }
}

@Composable
fun ScheduleInspectionScreen(
    onScheduled: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val growerResults by viewModel.growerSearchResults.collectAsState()
    val schedule = uiState.schedule
    val districts by viewModel.districtsForProvince(schedule.provinceId).collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { viewModel.initScheduleForm() }

    Scaffold(topBar = { TbzTopBar("Schedule inspection") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormTextField(
                value = schedule.growerSearch,
                onValueChange = { viewModel.updateSchedule { s -> s.copy(growerSearch = it) } },
                label = "Grower (name, TBZ ID, NRC)",
                error = uiState.scheduleErrors["growerId"],
            )
            if (schedule.growerName.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Selected: ${schedule.growerName}", fontWeight = FontWeight.Medium)
                    OutlinedButton(onClick = viewModel::clearScheduleGrower) { Text("Clear") }
                }
            }
            growerResults.forEach { grower ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectGrowerForSchedule(grower) },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${grower.first_name} ${grower.last_name}", fontWeight = FontWeight.SemiBold)
                        grower.tbz_id?.let { Text("TBZ ID: $it") }
                        Text("NRC: ${grower.nrc_number}")
                    }
                }
            }

            FormTextField(
                value = schedule.inspectorName,
                onValueChange = { viewModel.updateSchedule { s -> s.copy(inspectorName = it) } },
                label = "Assign inspector",
            )
            Text(
                "Inspector ID: ${schedule.inspectorId.ifBlank { "signed-in user" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TbzDropdownField(
                label = "Inspection type",
                options = InspectionFormChoices.inspectionTypes,
                selectedId = schedule.inspectionType,
                onSelected = { viewModel.updateSchedule { s -> s.copy(inspectionType = it) } },
            )

            FormTextField(
                value = schedule.scheduledDate,
                onValueChange = { viewModel.updateSchedule { s -> s.copy(scheduledDate = it) } },
                label = "Scheduled date (YYYY-MM-DD)",
                error = uiState.scheduleErrors["scheduledDate"],
            )

            TbzDropdownField(
                label = "Province",
                options = provinces.map { it.id to it.name },
                selectedId = schedule.provinceId,
                onSelected = { viewModel.updateSchedule { s -> s.copy(provinceId = it, districtId = "") } },
            )

            if (districts.isNotEmpty()) {
                TbzDropdownField(
                    label = "District",
                    options = districts.map { it.id to it.name },
                    selectedId = schedule.districtId,
                    onSelected = { viewModel.updateSchedule { s -> s.copy(districtId = it) } },
                )
            } else {
                FormTextField(
                    value = schedule.districtText,
                    onValueChange = { viewModel.updateSchedule { s -> s.copy(districtText = it) } },
                    label = "District",
                    error = uiState.scheduleErrors["districtId"],
                )
            }

            FormTextField(
                value = schedule.notes,
                onValueChange = { viewModel.updateSchedule { s -> s.copy(notes = it) } },
                label = "Notes",
                singleLine = false,
            )

            uiState.saveError?.let { ErrorText(it) }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Saving…" else "Schedule",
                onPrimary = { viewModel.scheduleInspection { onScheduled() } },
                secondaryLabel = "Cancel",
                onSecondary = onScheduled,
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}

@Composable
fun LocalSchedulesScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val schedules by viewModel.localSchedules.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Local schedules") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InspectionFormChoices.syncFilters.forEach { filter ->
                    FilterChip(
                        selected = uiState.syncFilter == filter,
                        onClick = { viewModel.onSyncFilterChange(filter) },
                        label = { Text(filter.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Button(
                onClick = viewModel::triggerSync,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) { Text("Sync pending") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(schedules, key = { it.local_id }) { inspection ->
                    InspectionScheduleRow(inspection, onClick = { onOpenDetail(inspection.local_id) })
                }
            }
        }
    }
}

@Composable
fun InspectionPortalListScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val inspections by viewModel.filteredInspections.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Portal inspections") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Search grower or location") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InspectionFormChoices.typeFilters.take(5).forEach { type ->
                    FilterChip(
                        selected = uiState.typeFilter == type,
                        onClick = { viewModel.onTypeFilterChange(type) },
                        label = {
                            Text(
                                if (type == "All") "All" else inspectionTypeLabel(type),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(inspections, key = { it.local_id }) { inspection ->
                    InspectionScheduleRow(inspection, onClick = { onOpenDetail(inspection.local_id) })
                }
            }
        }
    }
}

@Composable
fun InspectionDetailScreen(
    localId: String,
    onStartField: (String) -> Unit,
    onStartNursery: (String) -> Unit,
    onStartCuring: (String) -> Unit,
    onStartValidation: (String) -> Unit,
    onOpenGrower: (String) -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val inspection by viewModel.observeInspection(localId).collectAsState()

    LaunchedEffect(inspection) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
    }

    Scaffold(topBar = { TbzTopBar("Inspection detail") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val item = inspection
            if (item == null) {
                Text("Inspection not found")
            } else {
                Text(item.grower_name ?: "Grower", fontWeight = FontWeight.Bold)
                Text("Type: ${inspectionTypeLabel(item.inspection_type)}")
                Text("Status: ${item.status}")
                Text("Scheduled: ${item.scheduled_date}")
                Text("Province: ${item.province.orEmpty()}")
                Text("District: ${item.district.orEmpty()}")
                item.notes?.let { Text("Notes: $it") }
                SyncStatusChip(item.sync_status)
                item.last_sync_error?.let { ErrorText(it) }

                OutlinedButton(
                    onClick = { onOpenGrower(item.grower_id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open grower profile") }

                when (item.inspection_type) {
                    InspectionTypes.GROWER_VALIDATION ->
                        Button(onClick = { onStartValidation(localId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start grower validation")
                        }
                    InspectionTypes.NURSERY_INSPECTION ->
                        Button(onClick = { onStartNursery(localId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start nursery inspection")
                        }
                    InspectionTypes.FIELD_INSPECTION ->
                        Button(onClick = { onStartField(localId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start field inspection")
                        }
                    InspectionTypes.CURING_INSPECTION ->
                        Button(onClick = { onStartCuring(localId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start curing inspection")
                        }
                }
            }
        }
    }
}

@Composable
fun InspectionReportsScreen(
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val reports by viewModel.filteredReports.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Inspection reports") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InspectionFormChoices.syncFilters.forEach { filter ->
                    FilterChip(
                        selected = uiState.syncFilter == filter,
                        onClick = { viewModel.onSyncFilterChange(filter) },
                        label = { Text(filter.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Button(
                onClick = viewModel::triggerSync,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) { Text("Sync pending reports") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reports, key = { it.local_id }) { report ->
                    InspectionReportRow(
                        report = report,
                        onDelete = { viewModel.deleteReport(report) },
                    )
                }
            }
        }
    }
}

@Composable
fun HighRiskGrowersScreen(
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val rows by viewModel.highRiskValidations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("High-risk growers") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.highRiskSearch,
                onValueChange = viewModel::onHighRiskSearchChange,
                label = { Text("Search grower or NRC") },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.validationLocalId }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(row.growerName, fontWeight = FontWeight.SemiBold)
                            Text("NRC: ${row.nrcNumber}")
                            Text("${row.province} / ${row.district}")
                            Text("Risk score: ${row.riskScore}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldInspectionFormScreen(
    inspectionId: String,
    onSaved: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.field
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty() }

    val inspection by viewModel.observeInspection(inspectionId).collectAsState()
    LaunchedEffect(inspection, deviceId) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
        viewModel.updateField { it.copy(deviceId = deviceId, inspectionLocalId = inspectionId) }
    }

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
                    viewModel.updateField {
                        it.copy(
                            gpsLatitude = loc.latitude.toString(),
                            gpsLongitude = loc.longitude.toString(),
                        )
                    }
                }
            }
        }
    }

    Scaffold(topBar = { TbzTopBar("Field inspection") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormSectionTitle(form.growerName.ifBlank { "Field inspection" })
            FormTextField(
                form.transplantedHectarage,
                { viewModel.updateField { f -> f.copy(transplantedHectarage = it) } },
                "Transplanted hectarage",
                error = uiState.fieldErrors["transplantedHectarage"],
            )
            TbzDropdownField(
                label = "Crop stage",
                options = InspectionFormChoices.fieldCropStages,
                selectedId = form.cropStage,
                onSelected = { viewModel.updateField { f -> f.copy(cropStage = it) } },
            )
            TbzDropdownField(
                label = "Plant population",
                options = InspectionFormChoices.plantPopulations,
                selectedId = form.plantPopulation,
                onSelected = { viewModel.updateField { f -> f.copy(plantPopulation = it) } },
            )
            TbzDropdownField(
                label = "Crop uniformity",
                options = InspectionFormChoices.cropUniformities,
                selectedId = form.cropUniformity,
                onSelected = { viewModel.updateField { f -> f.copy(cropUniformity = it) } },
            )
            TbzDropdownField(
                label = "Fertilizer application",
                options = InspectionFormChoices.fertilizerApplications,
                selectedId = form.fertilizerApplication,
                onSelected = { viewModel.updateField { f -> f.copy(fertilizerApplication = it) } },
            )
            TbzDropdownField(
                label = "Pest/disease status",
                options = InspectionFormChoices.pestDiseaseStatuses,
                selectedId = form.pestDiseaseStatus,
                onSelected = { viewModel.updateField { f -> f.copy(pestDiseaseStatus = it) } },
            )
            TbzDropdownField(
                label = "Weed control",
                options = InspectionFormChoices.weedControls,
                selectedId = form.weedControl,
                onSelected = { viewModel.updateField { f -> f.copy(weedControl = it) } },
            )
            TbzDropdownField(
                label = "Irrigation status",
                options = InspectionFormChoices.irrigationStatuses,
                selectedId = form.irrigationStatus,
                onSelected = { viewModel.updateField { f -> f.copy(irrigationStatus = it) } },
            )
            FormTextField(form.gpsLatitude, { viewModel.updateField { f -> f.copy(gpsLatitude = it) } }, "GPS latitude")
            FormTextField(form.gpsLongitude, { viewModel.updateField { f -> f.copy(gpsLongitude = it) } }, "GPS longitude")
            OutlinedButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Capture GPS") }
            FormTextField(
                form.inspectorRemarks,
                { viewModel.updateField { f -> f.copy(inspectorRemarks = it) } },
                "Inspector remarks",
                singleLine = false,
                error = uiState.fieldErrors["inspectorRemarks"],
            )
            uiState.saveError?.let { ErrorText(it) }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Saving…" else "Save inspection",
                onPrimary = { viewModel.saveFieldReport(onSaved) },
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}

@Composable
fun NurseryInspectionFormScreen(
    inspectionId: String,
    onSaved: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.nursery
    val context = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty() }

    val inspection by viewModel.observeInspection(inspectionId).collectAsState()
    LaunchedEffect(inspection, deviceId) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
        viewModel.updateNursery { it.copy(deviceId = deviceId, inspectionLocalId = inspectionId) }
    }

    Scaffold(topBar = { TbzTopBar("Nursery inspection") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormSectionTitle(form.growerName.ifBlank { "Nursery inspection" })
            FormTextField(form.seedVariety, { viewModel.updateNursery { f -> f.copy(seedVariety = it) } }, "Seed variety", error = uiState.nurseryErrors["seedVariety"])
            FormTextField(form.nurserySizeBeds, { viewModel.updateNursery { f -> f.copy(nurserySizeBeds = it) } }, "Nursery size / beds", error = uiState.nurseryErrors["nurserySizeBeds"])
            FormTextField(form.dateOfSowing, { viewModel.updateNursery { f -> f.copy(dateOfSowing = it) } }, "Date of sowing (YYYY-MM-DD)", error = uiState.nurseryErrors["dateOfSowing"])
            TbzDropdownField(
                label = "Germination status",
                options = InspectionFormChoices.germinationStatuses,
                selectedId = form.germinationStatus,
                onSelected = { viewModel.updateNursery { f -> f.copy(germinationStatus = it) } },
            )
            TbzDropdownField(
                label = "Seedling condition",
                options = InspectionFormChoices.seedlingConditions,
                selectedId = form.seedlingCondition,
                onSelected = { viewModel.updateNursery { f -> f.copy(seedlingCondition = it) } },
            )
            TbzDropdownField(
                label = "Water source",
                options = InspectionFormChoices.waterSources,
                selectedId = form.waterSource,
                onSelected = { viewModel.updateNursery { f -> f.copy(waterSource = it) } },
            )
            TbzRadioGroup(
                label = "Pest/disease presence",
                options = InspectionFormChoices.yesNo.map { it.first.toString() to it.second },
                selected = form.pestDiseasePresent.toString(),
                onSelected = { viewModel.updateNursery { f -> f.copy(pestDiseasePresent = it.toBoolean()) } },
            )
            FormTextField(form.pestDiseaseNotes, { viewModel.updateNursery { f -> f.copy(pestDiseaseNotes = it) } }, "Pest/disease notes", singleLine = false)
            TbzRadioGroup(
                label = "Fertilizer used",
                options = InspectionFormChoices.yesNo.map { it.first.toString() to it.second },
                selected = form.fertilizerUsed.toString(),
                onSelected = { viewModel.updateNursery { f -> f.copy(fertilizerUsed = it.toBoolean()) } },
            )
            FormTextField(form.fertilizerNotes, { viewModel.updateNursery { f -> f.copy(fertilizerNotes = it) } }, "Fertilizer notes", singleLine = false)
            TbzRadioGroup(
                label = "Chemicals used",
                options = InspectionFormChoices.yesNo.map { it.first.toString() to it.second },
                selected = form.chemicalsUsed.toString(),
                onSelected = { viewModel.updateNursery { f -> f.copy(chemicalsUsed = it.toBoolean()) } },
            )
            FormTextField(form.chemicalsNotes, { viewModel.updateNursery { f -> f.copy(chemicalsNotes = it) } }, "Chemicals notes", singleLine = false)
            FormTextField(form.inspectorRemarks, { viewModel.updateNursery { f -> f.copy(inspectorRemarks = it) } }, "Inspector remarks", singleLine = false, error = uiState.nurseryErrors["inspectorRemarks"])
            uiState.saveError?.let { ErrorText(it) }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Saving…" else "Save inspection",
                onPrimary = { viewModel.saveNurseryReport(onSaved) },
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}

@Composable
fun CuringInspectionFormScreen(
    inspectionId: String,
    onSaved: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.curing
    val barnTypes by viewModel.barnTypes.collectAsState()
    val context = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty() }

    val inspection by viewModel.observeInspection(inspectionId).collectAsState()
    LaunchedEffect(inspection, deviceId) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
        viewModel.updateCuring { it.copy(deviceId = deviceId, inspectionLocalId = inspectionId) }
    }

    val barnOptions = if (barnTypes.isNotEmpty()) {
        barnTypes.map { it.id to it.name }
    } else {
        InspectionFormChoices.curingBarnTypes
    }

    Scaffold(topBar = { TbzTopBar("Curing inspection") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormSectionTitle(form.growerName.ifBlank { "Curing inspection" })
            FormTextField(form.numberOfBarns, { viewModel.updateCuring { f -> f.copy(numberOfBarns = it) } }, "Number of barns", error = uiState.curingErrors["numberOfBarns"])
            TbzDropdownField(
                label = "Type of barns",
                options = barnOptions,
                selectedId = form.barnType,
                onSelected = { viewModel.updateCuring { f -> f.copy(barnType = it) } },
            )
            FormTextField(form.curingCycles, { viewModel.updateCuring { f -> f.copy(curingCycles = it) } }, "Curing cycles", error = uiState.curingErrors["curingCycles"])
            TbzDropdownField(
                label = "Fuel source",
                options = InspectionFormChoices.fuelSources,
                selectedId = form.fuelSource,
                onSelected = { viewModel.updateCuring { f -> f.copy(fuelSource = it) } },
            )
            TbzDropdownField(
                label = "Curing status",
                options = InspectionFormChoices.curingStatuses,
                selectedId = form.curingStatus,
                onSelected = { viewModel.updateCuring { f -> f.copy(curingStatus = it) } },
            )
            TbzDropdownField(
                label = "Leaf quality",
                options = InspectionFormChoices.leafQualities,
                selectedId = form.leafQuality,
                onSelected = { viewModel.updateCuring { f -> f.copy(leafQuality = it) } },
            )
            TbzDropdownField(
                label = "Grading status",
                options = InspectionFormChoices.gradingStatuses,
                selectedId = form.gradingStatus,
                onSelected = { viewModel.updateCuring { f -> f.copy(gradingStatus = it) } },
            )
            FormTextField(form.inspectorRemarks, { viewModel.updateCuring { f -> f.copy(inspectorRemarks = it) } }, "Inspector remarks", singleLine = false, error = uiState.curingErrors["inspectorRemarks"])
            uiState.saveError?.let { ErrorText(it) }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Saving…" else "Save inspection",
                onPrimary = { viewModel.saveCuringReport(onSaved) },
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValidationFormScreen(
    inspectionId: String,
    onSaved: () -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.validation
    val provinces by viewModel.provinces.collectAsState()
    val tobaccoTypes by viewModel.tobaccoTypes.collectAsState()
    val barnTypes by viewModel.barnTypes.collectAsState()
    val sponsors by viewModel.sponsors.collectAsState()
    val districts by viewModel.districtsForProvince(form.provinceId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty() }

    val inspection by viewModel.observeInspection(inspectionId).collectAsState()
    LaunchedEffect(inspection, deviceId) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
        viewModel.updateValidation { it.copy(deviceId = deviceId, inspectionLocalId = inspectionId) }
    }

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
                    viewModel.updateValidation {
                        it.copy(
                            gpsLatitude = loc.latitude.toString(),
                            gpsLongitude = loc.longitude.toString(),
                        )
                    }
                }
            }
        }
    }

    val tobaccoOptions = if (tobaccoTypes.isNotEmpty()) {
        tobaccoTypes.map { it.code to it.name }
    } else {
        InspectionFormChoices.validationTobaccoTypes
    }
    val barnOptions = if (barnTypes.isNotEmpty()) {
        barnTypes.map { it.id to it.name }
    } else {
        InspectionFormChoices.validationBarnTypes
    }

    Scaffold(topBar = { TbzTopBar("Grower validation") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            OutlinedTextField(
                value = form.growerName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Grower") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.growerNrc,
                onValueChange = {},
                readOnly = true,
                label = { Text("NRC number") },
                modifier = Modifier.fillMaxWidth(),
            )
            TbzDropdownField(
                label = "Sex",
                options = InspectionFormChoices.validationSexOptions,
                selectedId = form.sex,
                onSelected = { viewModel.updateValidation { f -> f.copy(sex = it) } },
            )
            FormTextField(form.gpsLatitude, { viewModel.updateValidation { f -> f.copy(gpsLatitude = it) } }, "GPS latitude", error = uiState.validationErrors["gpsLatitude"])
            FormTextField(form.gpsLongitude, { viewModel.updateValidation { f -> f.copy(gpsLongitude = it) } }, "GPS longitude", error = uiState.validationErrors["gpsLongitude"])
            OutlinedButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Capture GPS") }
            TbzDropdownField(
                label = "Crop stage",
                options = InspectionFormChoices.validationCropStages,
                selectedId = form.cropStage,
                onSelected = { viewModel.updateValidation { f -> f.copy(cropStage = it) } },
            )
            TbzDropdownField(
                label = "Tobacco type",
                options = tobaccoOptions,
                selectedId = form.tobaccoType,
                onSelected = { viewModel.updateValidation { f -> f.copy(tobaccoType = it) } },
            )
            FormTextField(form.tobaccoVariety, { viewModel.updateValidation { f -> f.copy(tobaccoVariety = it) } }, "Tobacco variety", error = uiState.validationErrors["tobaccoVariety"])
            FormTextField(form.validatedHectarage, viewModel::onValidatedHectarageChange, "Validated hectarage", error = uiState.validationErrors["validatedHectarage"])
            FormTextField(form.yieldPerHa, { viewModel.updateValidation { f -> f.copy(yieldPerHa = it) } }, "Yield per ha (kg)", error = uiState.validationErrors["yieldPerHa"])
            TbzDropdownField(
                label = "Sponsor",
                options = listOf("" to "Self-sponsored") + sponsors.map { it.id to it.name },
                selectedId = form.sponsorId,
                onSelected = { viewModel.updateValidation { f -> f.copy(sponsorId = it) } },
            )
            TbzDropdownField(
                label = "Types of barns",
                options = barnOptions,
                selectedId = form.barnType,
                onSelected = { viewModel.updateValidation { f -> f.copy(barnType = it) } },
            )
            FormTextField(form.numberOfBarns, { viewModel.updateValidation { f -> f.copy(numberOfBarns = it) } }, "Number of barns", error = uiState.validationErrors["numberOfBarns"])
            TbzRadioGroup(
                label = "Is barn capacity sufficient?",
                options = InspectionFormChoices.barnCapacityOptions.map { it.first.toString() to it.second },
                selected = form.barnCapacitySufficient.toString(),
                onSelected = { viewModel.updateValidation { f -> f.copy(barnCapacitySufficient = it.toBoolean()) } },
            )
            FormSectionTitle("Stakeholders present (optional)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("TBZ", "ZTB", "GROWER").forEach { code ->
                    FilterChip(
                        selected = form.stakeholdersPresent.contains(code),
                        onClick = { viewModel.toggleStakeholder(code) },
                        label = { Text(code) },
                    )
                }
            }
            TbzDropdownField(
                label = "Province",
                options = provinces.map { it.id to it.name },
                selectedId = form.provinceId,
                onSelected = { viewModel.updateValidation { f -> f.copy(provinceId = it, districtId = "") } },
            )
            if (districts.isNotEmpty()) {
                TbzDropdownField(
                    label = "District",
                    options = districts.map { it.id to it.name },
                    selectedId = form.districtId,
                    onSelected = { viewModel.updateValidation { f -> f.copy(districtId = it) } },
                )
            } else {
                FormTextField(form.districtText, { viewModel.updateValidation { f -> f.copy(districtText = it) } }, "District", error = uiState.validationErrors["districtId"])
            }
            OutlinedTextField(
                value = form.deviceId,
                onValueChange = {},
                readOnly = true,
                label = { Text("Device ID") },
                modifier = Modifier.fillMaxWidth(),
            )
            FormTextField(form.inspectorRemarks, { viewModel.updateValidation { f -> f.copy(inspectorRemarks = it) } }, "Remark by inspector", singleLine = false)
            uiState.saveError?.let { ErrorText(it) }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Saving…" else "Submit validation",
                onPrimary = { viewModel.saveValidation(onSaved) },
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}

@Composable
private fun InspectionScheduleRow(
    inspection: InspectionEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(inspection.grower_name ?: "Grower", fontWeight = FontWeight.SemiBold)
            Text(inspectionTypeLabel(inspection.inspection_type))
            Text("${inspection.province.orEmpty()} / ${inspection.district.orEmpty()}")
            Text("Scheduled: ${inspection.scheduled_date}")
            Text("Status: ${inspection.status}")
            SyncStatusChip(inspection.sync_status)
            inspection.last_sync_error?.let { ErrorText(it) }
        }
    }
}

@Composable
private fun InspectionReportRow(
    report: InspectionReportEntity,
    onDelete: () -> Unit,
) {
    val timestamp = rememberReportTimestamp(report.created_at)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(report.type, fontWeight = FontWeight.SemiBold)
            Text(timestamp)
            SyncStatusChip(report.sync_status)
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun rememberReportTimestamp(createdAt: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(createdAt))
}

@Composable
fun InspectionLookupScreen(
    onOpenGrower: (String) -> Unit,
    onScheduleForGrower: (String) -> Unit,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val results by viewModel.growerSearchResults.collectAsState()

    Scaffold(topBar = { TbzTopBar("Inspection lookup") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Search grower name, TBZ ID, NRC") },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.local_id }) { grower ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenGrower(grower.local_id) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${grower.first_name} ${grower.last_name}", fontWeight = FontWeight.SemiBold)
                            grower.tbz_id?.let { Text("TBZ: $it") }
                            Text("NRC: ${grower.nrc_number}")
                            Text("${grower.province.orEmpty()} / ${grower.district.orEmpty()}")
                            Text("Status: ${grower.status}")
                            Button(onClick = { onScheduleForGrower(grower.local_id) }) {
                                Text("Schedule inspection")
                            }
                        }
                    }
                }
            }
        }
    }
}
