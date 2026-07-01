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
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlDateField
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlToggle
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
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
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Inspections", subtitle = "Field, nursery, curing & validation")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlKpiTile("Scheduled", stats.scheduled.toString(), Modifier.weight(1f), tone = GlTone.Primary, icon = "calendar")
                        GlKpiTile("In progress", stats.inProgress.toString(), Modifier.weight(1f), icon = "sync")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlKpiTile("Completed", stats.completed.toString(), Modifier.weight(1f), tone = GlTone.Success, icon = "check-circle")
                        GlKpiTile("High risk", stats.highRisk.toString(), Modifier.weight(1f), tone = GlTone.Danger, icon = "warning")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlKpiTile("Schedules pending", stats.schedulesPendingSync.toString(), Modifier.weight(1f), tone = GlTone.Gold, icon = "cloud-up")
                        GlKpiTile("Reports pending", stats.reportsPendingSync.toString(), Modifier.weight(1f), tone = GlTone.Gold, icon = "cloud-up")
                    }
                }
                GlButton(text = "Sync now", onClick = viewModel::triggerSync, variant = GlButtonVariant.Outline, leadingIcon = "sync")
                GlSectionHeader(title = "Quick actions")
                GlCard(contentPadding = 4.dp) {
                    Column {
                        GlRow("Schedule inspection", subtitle = "Offline-first grower inspection schedule", leadingIcon = "calendar", onClick = onSchedule)
                        GlRow("Local schedules", subtitle = "Pending and failed schedule sync", leadingIcon = "cloud-up", onClick = onLocalSchedules)
                        GlRow("Portal inspections", subtitle = "Cached server inspections", leadingIcon = "cloud", onClick = onPortalList)
                        GlRow("Conducted reports", subtitle = "Field, nursery, curing, validation queue", leadingIcon = "clipboard", onClick = onReports)
                        GlRow("High-risk growers", subtitle = "Validations with risk score ≥ 70", leadingIcon = "warning", tone = GlTone.Danger, onClick = onHighRisk)
                        GlRow("Grower lookup", subtitle = "Search cached growers for scheduling", leadingIcon = "search", onClick = onLookup)
                    }
                }
            }
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
    val c = glColors()

    LaunchedEffect(Unit) { viewModel.initScheduleForm() }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Schedule inspection")
            ScrollableFormColumn {
                GlTextField(
                    value = schedule.growerSearch,
                    onValueChange = { viewModel.updateSchedule { s -> s.copy(growerSearch = it) } },
                    label = "Grower (name, TBZ ID, NRC)",
                    leadingIcon = "search",
                    error = uiState.scheduleErrors["growerId"],
                )
                if (schedule.growerName.isNotBlank()) {
                    GlCard(contentPadding = 12.dp) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            GlFieldRow(label = "Selected", value = schedule.growerName, modifier = Modifier.weight(1f))
                        }
                    }
                    GlButton(text = "Clear selection", onClick = viewModel::clearScheduleGrower, variant = GlButtonVariant.Ghost, size = GlButtonSize.Sm)
                }
                growerResults.forEach { grower ->
                    GlCard(onClick = { viewModel.selectGrowerForSchedule(grower) }, contentPadding = 12.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            GlFieldRow(label = "Name", value = "${grower.first_name} ${grower.last_name}")
                            grower.tbz_id?.let { GlFieldRow(label = "TBZ ID", value = it, mono = true) }
                            GlFieldRow(label = "NRC", value = grower.nrc_number)
                        }
                    }
                }

                GlTextField(
                    value = schedule.inspectorName,
                    onValueChange = { viewModel.updateSchedule { s -> s.copy(inspectorName = it) } },
                    label = "Assign inspector",
                    helper = "Inspector ID: ${schedule.inspectorId.ifBlank { "signed-in user" }}",
                )

                GlDropdownField(
                    label = "Inspection type",
                    options = InspectionFormChoices.inspectionTypes,
                    selectedId = schedule.inspectionType,
                    onSelected = { viewModel.updateSchedule { s -> s.copy(inspectionType = it) } },
                )

                GlDateField(
                    value = schedule.scheduledDate,
                    onValueChange = { viewModel.updateSchedule { s -> s.copy(scheduledDate = it) } },
                    label = "Scheduled date",
                    required = true,
                    error = uiState.scheduleErrors["scheduledDate"],
                )

                GlDropdownField(
                    label = "Province",
                    options = provinces.map { it.id to it.name },
                    selectedId = schedule.provinceId,
                    onSelected = { viewModel.updateSchedule { s -> s.copy(provinceId = it, districtId = "") } },
                )

                if (districts.isNotEmpty()) {
                    GlDropdownField(
                        label = "District",
                        options = districts.map { it.id to it.name },
                        selectedId = schedule.districtId,
                        onSelected = { viewModel.updateSchedule { s -> s.copy(districtId = it) } },
                    )
                } else {
                    GlTextField(
                        value = schedule.districtText,
                        onValueChange = { viewModel.updateSchedule { s -> s.copy(districtText = it) } },
                        label = "District",
                        error = uiState.scheduleErrors["districtId"],
                    )
                }

                GlTextField(
                    value = schedule.notes,
                    onValueChange = { viewModel.updateSchedule { s -> s.copy(notes = it) } },
                    label = "Notes",
                    singleLine = false,
                    minLines = 3,
                )

                uiState.saveError?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlButton(text = "Cancel", onClick = onScheduled, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f), enabled = !uiState.isSaving)
                    GlButton(
                        text = if (uiState.isSaving) "Saving…" else "Schedule",
                        onClick = { viewModel.scheduleInspection { onScheduled() } },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Local schedules")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlFilterPills(
                    options = InspectionFormChoices.syncFilters,
                    selected = uiState.syncFilter,
                    onSelect = viewModel::onSyncFilterChange,
                )
                GlButton(text = "Sync pending", onClick = viewModel::triggerSync, variant = GlButtonVariant.Outline, leadingIcon = "sync")
            }
            if (schedules.isEmpty()) {
                GlEmptyState(title = "No local schedules", subtitle = "Schedules you create will appear here until they sync.", icon = "calendar")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(schedules, key = { it.local_id }) { inspection ->
                        InspectionScheduleRow(inspection, onClick = { onOpenDetail(inspection.local_id) })
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionPortalListScreen(
    onOpenDetail: (String) -> Unit,
    onSchedule: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val inspections by viewModel.filteredInspections.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.hubStats.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg, modifier = modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = "Inspections",
                subtitle = "${stats.scheduled} scheduled",
                actions = {
                    GlButton(
                        text = "Schedule",
                        onClick = onSchedule,
                        variant = GlButtonVariant.Surface,
                        size = GlButtonSize.Sm,
                        fillMaxWidth = false,
                        leadingIcon = "calendar",
                    )
                },
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlKpiTile("Today", stats.scheduled.toString(), Modifier.weight(1f), tone = GlTone.Primary, icon = "calendar")
                GlKpiTile("High risk", stats.highRisk.toString(), Modifier.weight(1f), tone = GlTone.Danger, icon = "warning")
                GlKpiTile("Pending sync", (stats.schedulesPendingSync + stats.reportsPendingSync).toString(), Modifier.weight(1f), tone = GlTone.Gold, icon = "cloud-up")
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = "Search grower or location",
                )
                GlFilterPills(
                    options = InspectionFormChoices.typeFilters.take(5),
                    selected = uiState.typeFilter,
                    onSelect = viewModel::onTypeFilterChange,
                )
            }
            if (inspections.isEmpty()) {
                GlEmptyState(title = "No inspections found", subtitle = "Try a different search or filter.", icon = "search")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(inspections, key = { it.local_id }) { inspection ->
                        InspectionScheduleRow(inspection, onClick = { onOpenDetail(inspection.local_id) })
                    }
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
    val c = glColors()

    LaunchedEffect(inspection) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Inspection detail")
            val item = inspection
            if (item == null) {
                GlEmptyState(title = "Inspection not found", icon = "search")
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GlCard(accent = GlAccent.Primary, contentPadding = 14.dp) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlAvatar(name = item.grower_name ?: "Grower", gold = true)
                            Column(Modifier.weight(1f)) {
                                GlFieldRow(label = "Grower", value = item.grower_name ?: "—")
                                GlFieldRow(label = "Type", value = inspectionTypeLabel(item.inspection_type))
                            }
                        }
                    }
                    GlCard(contentPadding = 14.dp) {
                        Column {
                            GlFieldRow(label = "Status", value = item.status)
                            GlFieldRow(label = "Scheduled", value = item.scheduled_date)
                            GlFieldRow(label = "Province", value = item.province.orEmpty().ifBlank { "—" })
                            GlFieldRow(label = "District", value = item.district.orEmpty().ifBlank { "—" })
                            item.notes?.let { GlFieldRow(label = "Notes", value = it) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlSyncChip(status = item.sync_status)
                    }
                    item.last_sync_error?.let { ErrorText(it) }

                    GlButton(text = "Open grower profile", onClick = { onOpenGrower(item.grower_id) }, variant = GlButtonVariant.Outline, leadingIcon = "profile")

                    when (item.inspection_type) {
                        InspectionTypes.GROWER_VALIDATION ->
                            GlButton(text = "Start grower validation", onClick = { onStartValidation(localId) }, leadingIcon = "shield-check")
                        InspectionTypes.NURSERY_INSPECTION ->
                            GlButton(text = "Start nursery inspection", onClick = { onStartNursery(localId) }, leadingIcon = "leaf")
                        InspectionTypes.FIELD_INSPECTION ->
                            GlButton(text = "Start field inspection", onClick = { onStartField(localId) }, leadingIcon = "leaf")
                        InspectionTypes.CURING_INSPECTION ->
                            GlButton(text = "Start curing inspection", onClick = { onStartCuring(localId) }, leadingIcon = "building")
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
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Inspection reports")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlFilterPills(
                    options = InspectionFormChoices.syncFilters,
                    selected = uiState.syncFilter,
                    onSelect = viewModel::onSyncFilterChange,
                )
                GlButton(text = "Sync pending reports", onClick = viewModel::triggerSync, variant = GlButtonVariant.Outline, leadingIcon = "sync")
            }
            if (reports.isEmpty()) {
                GlEmptyState(title = "No reports yet", subtitle = "Conducted inspection reports will appear here.", icon = "clipboard")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
}

@Composable
fun HighRiskGrowersScreen(
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val rows by viewModel.highRiskValidations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "High-risk growers", subtitle = "${rows.size} flagged")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlBanner(
                    title = "Field check recommended",
                    subtitle = "Scores above 50 trigger an automatic field inspection request.",
                    tone = GlTone.Danger,
                    icon = "warning",
                )
                GlSearchBar(
                    value = uiState.highRiskSearch,
                    onValueChange = viewModel::onHighRiskSearchChange,
                    placeholder = "Search grower or NRC",
                )
            }
            if (rows.isEmpty()) {
                GlEmptyState(title = "No high-risk growers", subtitle = "Validations with a risk score ≥ 70 will appear here.", icon = "warning")
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows, key = { it.validationLocalId }) { row ->
                        GlCard(accent = GlAccent.Gold, contentPadding = 14.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(end = 4.dp),
                                ) {
                                    GlPill(text = "${row.riskScore}", tone = if (row.riskScore > 70) GlTone.Danger else GlTone.Gold, size = GlPillSize.Lg)
                                }
                                Column(Modifier.weight(1f)) {
                                    GlFieldRow(label = "Grower", value = row.growerName)
                                    GlFieldRow(label = "NRC", value = row.nrcNumber.ifBlank { "—" })
                                    GlFieldRow(label = "Location", value = "${row.province.ifBlank { "—" }} / ${row.district.ifBlank { "—" }}")
                                }
                            }
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
    val c = glColors()

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

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Field inspection", subtitle = form.growerName.ifBlank { null })
            ScrollableFormColumn {
                GlSectionHeader(title = "Field measurements")
                GlTextField(
                    form.transplantedHectarage,
                    { viewModel.updateField { f -> f.copy(transplantedHectarage = it) } },
                    label = "Transplanted hectarage",
                    error = uiState.fieldErrors["transplantedHectarage"],
                )
                GlDropdownField(
                    label = "Crop stage",
                    options = InspectionFormChoices.fieldCropStages,
                    selectedId = form.cropStage,
                    onSelected = { viewModel.updateField { f -> f.copy(cropStage = it) } },
                )
                GlDropdownField(
                    label = "Plant population",
                    options = InspectionFormChoices.plantPopulations,
                    selectedId = form.plantPopulation,
                    onSelected = { viewModel.updateField { f -> f.copy(plantPopulation = it) } },
                )
                GlDropdownField(
                    label = "Crop uniformity",
                    options = InspectionFormChoices.cropUniformities,
                    selectedId = form.cropUniformity,
                    onSelected = { viewModel.updateField { f -> f.copy(cropUniformity = it) } },
                )
                GlDropdownField(
                    label = "Fertilizer application",
                    options = InspectionFormChoices.fertilizerApplications,
                    selectedId = form.fertilizerApplication,
                    onSelected = { viewModel.updateField { f -> f.copy(fertilizerApplication = it) } },
                )
                GlDropdownField(
                    label = "Pest/disease status",
                    options = InspectionFormChoices.pestDiseaseStatuses,
                    selectedId = form.pestDiseaseStatus,
                    onSelected = { viewModel.updateField { f -> f.copy(pestDiseaseStatus = it) } },
                )
                GlDropdownField(
                    label = "Weed control",
                    options = InspectionFormChoices.weedControls,
                    selectedId = form.weedControl,
                    onSelected = { viewModel.updateField { f -> f.copy(weedControl = it) } },
                )
                GlDropdownField(
                    label = "Irrigation status",
                    options = InspectionFormChoices.irrigationStatuses,
                    selectedId = form.irrigationStatus,
                    onSelected = { viewModel.updateField { f -> f.copy(irrigationStatus = it) } },
                )
                GlSectionHeader(title = "Location")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlTextField(form.gpsLatitude, { viewModel.updateField { f -> f.copy(gpsLatitude = it) } }, label = "GPS latitude", modifier = Modifier.weight(1f))
                    GlTextField(form.gpsLongitude, { viewModel.updateField { f -> f.copy(gpsLongitude = it) } }, label = "GPS longitude", modifier = Modifier.weight(1f))
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
                GlSectionHeader(title = "Findings")
                GlTextField(
                    form.inspectorRemarks,
                    { viewModel.updateField { f -> f.copy(inspectorRemarks = it) } },
                    label = "Inspector remarks",
                    singleLine = false,
                    minLines = 3,
                    error = uiState.fieldErrors["inspectorRemarks"],
                )
                uiState.saveError?.let { ErrorText(it) }
                GlButton(
                    text = if (uiState.isSaving) "Saving…" else "Save inspection",
                    onClick = { viewModel.saveFieldReport(onSaved) },
                    enabled = !uiState.isSaving,
                )
            }
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
    val c = glColors()

    val inspection by viewModel.observeInspection(inspectionId).collectAsState()
    LaunchedEffect(inspection, deviceId) {
        inspection?.let { viewModel.loadFormsForInspection(it) }
        viewModel.updateNursery { it.copy(deviceId = deviceId, inspectionLocalId = inspectionId) }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Nursery inspection", subtitle = form.growerName.ifBlank { null })
            ScrollableFormColumn {
                GlTextField(form.seedVariety, { viewModel.updateNursery { f -> f.copy(seedVariety = it) } }, label = "Seed variety", error = uiState.nurseryErrors["seedVariety"])
                GlTextField(form.nurserySizeBeds, { viewModel.updateNursery { f -> f.copy(nurserySizeBeds = it) } }, label = "Nursery size / beds", error = uiState.nurseryErrors["nurserySizeBeds"])
                GlTextField(form.dateOfSowing, { viewModel.updateNursery { f -> f.copy(dateOfSowing = it) } }, label = "Date of sowing", placeholder = "YYYY-MM-DD", leadingIcon = "calendar", error = uiState.nurseryErrors["dateOfSowing"])
                GlDropdownField(
                    label = "Germination status",
                    options = InspectionFormChoices.germinationStatuses,
                    selectedId = form.germinationStatus,
                    onSelected = { viewModel.updateNursery { f -> f.copy(germinationStatus = it) } },
                )
                GlDropdownField(
                    label = "Seedling condition",
                    options = InspectionFormChoices.seedlingConditions,
                    selectedId = form.seedlingCondition,
                    onSelected = { viewModel.updateNursery { f -> f.copy(seedlingCondition = it) } },
                )
                GlDropdownField(
                    label = "Water source",
                    options = InspectionFormChoices.waterSources,
                    selectedId = form.waterSource,
                    onSelected = { viewModel.updateNursery { f -> f.copy(waterSource = it) } },
                )
                GlSectionHeader(title = "Crop protection")
                LabeledToggleRow(
                    label = "Pest/disease presence",
                    checked = form.pestDiseasePresent,
                    onCheckedChange = { viewModel.updateNursery { f -> f.copy(pestDiseasePresent = it) } },
                )
                GlTextField(form.pestDiseaseNotes, { viewModel.updateNursery { f -> f.copy(pestDiseaseNotes = it) } }, label = "Pest/disease notes", singleLine = false)
                LabeledToggleRow(
                    label = "Fertilizer used",
                    checked = form.fertilizerUsed,
                    onCheckedChange = { viewModel.updateNursery { f -> f.copy(fertilizerUsed = it) } },
                )
                GlTextField(form.fertilizerNotes, { viewModel.updateNursery { f -> f.copy(fertilizerNotes = it) } }, label = "Fertilizer notes", singleLine = false)
                LabeledToggleRow(
                    label = "Chemicals used",
                    checked = form.chemicalsUsed,
                    onCheckedChange = { viewModel.updateNursery { f -> f.copy(chemicalsUsed = it) } },
                )
                GlTextField(form.chemicalsNotes, { viewModel.updateNursery { f -> f.copy(chemicalsNotes = it) } }, label = "Chemicals notes", singleLine = false)
                GlSectionHeader(title = "Findings")
                GlTextField(form.inspectorRemarks, { viewModel.updateNursery { f -> f.copy(inspectorRemarks = it) } }, label = "Inspector remarks", singleLine = false, minLines = 3, error = uiState.nurseryErrors["inspectorRemarks"])
                uiState.saveError?.let { ErrorText(it) }
                GlButton(
                    text = if (uiState.isSaving) "Saving…" else "Save inspection",
                    onClick = { viewModel.saveNurseryReport(onSaved) },
                    enabled = !uiState.isSaving,
                )
            }
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
    val c = glColors()

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

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Curing inspection", subtitle = form.growerName.ifBlank { null })
            ScrollableFormColumn {
                GlTextField(form.numberOfBarns, { viewModel.updateCuring { f -> f.copy(numberOfBarns = it) } }, label = "Number of barns", error = uiState.curingErrors["numberOfBarns"])
                GlDropdownField(
                    label = "Type of barns",
                    options = barnOptions,
                    selectedId = form.barnType,
                    onSelected = { viewModel.updateCuring { f -> f.copy(barnType = it) } },
                )
                GlTextField(form.curingCycles, { viewModel.updateCuring { f -> f.copy(curingCycles = it) } }, label = "Curing cycles", error = uiState.curingErrors["curingCycles"])
                GlDropdownField(
                    label = "Fuel source",
                    options = InspectionFormChoices.fuelSources,
                    selectedId = form.fuelSource,
                    onSelected = { viewModel.updateCuring { f -> f.copy(fuelSource = it) } },
                )
                GlDropdownField(
                    label = "Curing status",
                    options = InspectionFormChoices.curingStatuses,
                    selectedId = form.curingStatus,
                    onSelected = { viewModel.updateCuring { f -> f.copy(curingStatus = it) } },
                )
                GlDropdownField(
                    label = "Leaf quality",
                    options = InspectionFormChoices.leafQualities,
                    selectedId = form.leafQuality,
                    onSelected = { viewModel.updateCuring { f -> f.copy(leafQuality = it) } },
                )
                GlDropdownField(
                    label = "Grading status",
                    options = InspectionFormChoices.gradingStatuses,
                    selectedId = form.gradingStatus,
                    onSelected = { viewModel.updateCuring { f -> f.copy(gradingStatus = it) } },
                )
                GlSectionHeader(title = "Findings")
                GlTextField(form.inspectorRemarks, { viewModel.updateCuring { f -> f.copy(inspectorRemarks = it) } }, label = "Inspector remarks", singleLine = false, minLines = 3, error = uiState.curingErrors["inspectorRemarks"])
                uiState.saveError?.let { ErrorText(it) }
                GlButton(
                    text = if (uiState.isSaving) "Saving…" else "Save inspection",
                    onClick = { viewModel.saveCuringReport(onSaved) },
                    enabled = !uiState.isSaving,
                )
            }
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
    val c = glColors()

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

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Grower validation", subtitle = form.growerName.ifBlank { null })
            ScrollableFormColumn {
                GlCard(contentPadding = 14.dp) {
                    Column {
                        GlFieldRow(label = "Grower", value = form.growerName.ifBlank { "—" })
                        GlFieldRow(label = "NRC number", value = form.growerNrc.ifBlank { "—" })
                        GlFieldRow(label = "Device ID", value = form.deviceId.ifBlank { "—" }, mono = true)
                    }
                }
                GlDropdownField(
                    label = "Sex",
                    options = InspectionFormChoices.validationSexOptions,
                    selectedId = form.sex,
                    onSelected = { viewModel.updateValidation { f -> f.copy(sex = it) } },
                )
                GlSectionHeader(title = "Location")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlTextField(form.gpsLatitude, { viewModel.updateValidation { f -> f.copy(gpsLatitude = it) } }, label = "GPS latitude", modifier = Modifier.weight(1f), error = uiState.validationErrors["gpsLatitude"])
                    GlTextField(form.gpsLongitude, { viewModel.updateValidation { f -> f.copy(gpsLongitude = it) } }, label = "GPS longitude", modifier = Modifier.weight(1f), error = uiState.validationErrors["gpsLongitude"])
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
                GlSectionHeader(title = "Crop & yield")
                GlDropdownField(
                    label = "Crop stage",
                    options = InspectionFormChoices.validationCropStages,
                    selectedId = form.cropStage,
                    onSelected = { viewModel.updateValidation { f -> f.copy(cropStage = it) } },
                )
                GlDropdownField(
                    label = "Tobacco type",
                    options = tobaccoOptions,
                    selectedId = form.tobaccoType,
                    onSelected = { viewModel.updateValidation { f -> f.copy(tobaccoType = it) } },
                )
                GlTextField(form.tobaccoVariety, { viewModel.updateValidation { f -> f.copy(tobaccoVariety = it) } }, label = "Tobacco variety", error = uiState.validationErrors["tobaccoVariety"])
                GlTextField(form.validatedHectarage, viewModel::onValidatedHectarageChange, label = "Validated hectarage", error = uiState.validationErrors["validatedHectarage"])
                GlTextField(form.yieldPerHa, { viewModel.updateValidation { f -> f.copy(yieldPerHa = it) } }, label = "Yield per ha (kg)", error = uiState.validationErrors["yieldPerHa"])
                GlDropdownField(
                    label = "Sponsor",
                    options = listOf("" to "Self-sponsored") + sponsors.map { it.id to it.name },
                    selectedId = form.sponsorId,
                    onSelected = { viewModel.updateValidation { f -> f.copy(sponsorId = it) } },
                )
                GlSectionHeader(title = "Curing capacity")
                GlDropdownField(
                    label = "Types of barns",
                    options = barnOptions,
                    selectedId = form.barnType,
                    onSelected = { viewModel.updateValidation { f -> f.copy(barnType = it) } },
                )
                GlTextField(form.numberOfBarns, { viewModel.updateValidation { f -> f.copy(numberOfBarns = it) } }, label = "Number of barns", error = uiState.validationErrors["numberOfBarns"])
                LabeledToggleRow(
                    label = "Is barn capacity sufficient?",
                    checked = form.barnCapacitySufficient,
                    onCheckedChange = { viewModel.updateValidation { f -> f.copy(barnCapacitySufficient = it) } },
                )
                GlSectionHeader(title = "Stakeholders present (optional)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TBZ", "ZTB", "GROWER").forEach { code ->
                        val active = form.stakeholdersPresent.contains(code)
                        GlPill(
                            text = code,
                            tone = if (active) GlTone.Primary else GlTone.Default,
                            modifier = Modifier.clickable { viewModel.toggleStakeholder(code) },
                        )
                    }
                }
                GlSectionHeader(title = "Location")
                GlDropdownField(
                    label = "Province",
                    options = provinces.map { it.id to it.name },
                    selectedId = form.provinceId,
                    onSelected = { viewModel.updateValidation { f -> f.copy(provinceId = it, districtId = "") } },
                )
                if (districts.isNotEmpty()) {
                    GlDropdownField(
                        label = "District",
                        options = districts.map { it.id to it.name },
                        selectedId = form.districtId,
                        onSelected = { viewModel.updateValidation { f -> f.copy(districtId = it) } },
                    )
                } else {
                    GlTextField(form.districtText, { viewModel.updateValidation { f -> f.copy(districtText = it) } }, label = "District", error = uiState.validationErrors["districtId"])
                }
                GlSectionHeader(title = "Findings")
                GlTextField(form.inspectorRemarks, { viewModel.updateValidation { f -> f.copy(inspectorRemarks = it) } }, label = "Remark by inspector", singleLine = false, minLines = 3)
                uiState.saveError?.let { ErrorText(it) }
                GlButton(
                    text = if (uiState.isSaving) "Saving…" else "Submit validation",
                    onClick = { viewModel.saveValidation(onSaved) },
                    variant = GlButtonVariant.Gold,
                    enabled = !uiState.isSaving,
                )
            }
        }
    }
}

@Composable
private fun LabeledToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = glColors()
    GlCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Text(
                label,
                color = c.text,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InspectionScheduleRow(
    inspection: InspectionEntity,
    onClick: () -> Unit,
) {
    val c = glColors()
    GlCard(onClick = onClick, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text(
                    inspection.grower_name ?: "Grower",
                    color = c.text,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                GlPill(text = inspectionTypeLabel(inspection.inspection_type), size = GlPillSize.Sm, tone = GlTone.Primary)
            }
            GlFieldRow(label = "Location", value = "${inspection.province.orEmpty().ifBlank { "—" }} / ${inspection.district.orEmpty().ifBlank { "—" }}")
            GlFieldRow(label = "Scheduled", value = inspection.scheduled_date)
            GlFieldRow(label = "Status", value = inspection.status)
            GlSyncChip(status = inspection.sync_status)
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
    GlCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlFieldRow(label = "Type", value = inspectionTypeLabel(report.type))
            GlFieldRow(label = "Created", value = timestamp)
            GlSyncChip(status = report.sync_status)
            GlButton(text = "Delete", onClick = onDelete, variant = GlButtonVariant.DangerOutline, size = GlButtonSize.Sm)
        }
    }
}

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
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Inspection lookup")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                GlSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = "Search grower name, TBZ ID, NRC",
                )
            }
            if (results.isEmpty()) {
                GlEmptyState(title = "No growers found", subtitle = "Search by name, TBZ ID, or NRC.", icon = "search")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(results, key = { it.local_id }) { grower ->
                        GlCard(onClick = { onOpenGrower(grower.local_id) }, contentPadding = 14.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                androidx.compose.material3.Text(
                                    "${grower.first_name} ${grower.last_name}",
                                    color = c.text,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                )
                                grower.tbz_id?.let { GlFieldRow(label = "TBZ ID", value = it, mono = true) }
                                GlFieldRow(label = "NRC", value = grower.nrc_number)
                                GlFieldRow(label = "Location", value = "${grower.province.orEmpty().ifBlank { "—" }} / ${grower.district.orEmpty().ifBlank { "—" }}")
                                GlFieldRow(label = "Status", value = grower.status)
                                GlButton(text = "Schedule inspection", onClick = { onScheduleForGrower(grower.local_id) }, size = GlButtonSize.Sm, leadingIcon = "calendar")
                            }
                        }
                    }
                }
            }
        }
    }
}
