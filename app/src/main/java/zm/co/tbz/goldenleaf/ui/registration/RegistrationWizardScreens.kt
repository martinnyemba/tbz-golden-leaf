package zm.co.tbz.goldenleaf.ui.registration

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import zm.co.tbz.goldenleaf.ui.components.GlImageSlot
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlTextField
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

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Cancel", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
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
                            onClick = { viewModel.updatePersonal { it.copy(growerTypeKey = key) } },
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

/** 12 · Step 2 · Identity */
@Composable
fun RegistrationStepIdentityScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: RegistrationViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val personal = uiState.personal
    val fullName = listOf(personal.firstName, personal.lastName).filter { it.isNotBlank() }.joinToString(" ")
    val c = glColors()

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Back", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
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
                    value = fullName,
                    onValueChange = { name ->
                        val parts = name.trim().split(" ").filter { it.isNotBlank() }
                        viewModel.updatePersonal {
                            it.copy(
                                firstName = parts.firstOrNull().orEmpty(),
                                lastName = parts.drop(1).joinToString(" "),
                            )
                        }
                    },
                    label = "Full name",
                    placeholder = "Mary Phiri",
                    required = true,
                )
                GlTextField(
                    value = personal.nrcNumber,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(nrcNumber = it) } },
                    label = "National Registration Card",
                    placeholder = "000000/00/0",
                    required = true,
                    leadingIcon = "badge",
                    trailing = {
                        if (personal.nrcNumber.isNotBlank()) {
                            GlPill(text = "Valid", tone = GlTone.Success, size = GlPillSize.Sm, leadingIcon = "check")
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlDropdownField(
                        label = "Gender",
                        options = listOf("FEMALE" to "Female", "MALE" to "Male"),
                        selectedId = personal.sex,
                        onSelected = { sex -> viewModel.updatePersonal { p -> p.copy(sex = sex) } },
                        modifier = Modifier.weight(1f),
                        required = true,
                    )
                    GlTextField(
                        value = personal.dateOfBirth,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(dateOfBirth = it) } },
                        label = "Date of birth",
                        placeholder = "DD / MM / YYYY",
                        leadingIcon = "calendar",
                        modifier = Modifier.weight(1f),
                    )
                }
                GlTextField(
                    value = personal.localPhone,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(localPhone = it) } },
                    label = "Phone number",
                    placeholder = "+260",
                    leadingIcon = "phone",
                )
                GlTextField(
                    value = personal.nextOfKin,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(nextOfKin = it) } },
                    label = "Next of kin",
                    placeholder = "Name & relationship",
                )
                GlDropdownField(
                    label = "Cooperative / association",
                    options = GrowerFormChoices.cooperativeOptions.map { it to it },
                    selectedId = personal.cooperative,
                    onSelected = { viewModel.updatePersonal { p -> p.copy(cooperative = it) } },
                )
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
    val provinces by viewModel.provinces.collectAsState()
    val districts by viewModel.districtsForProvince(personal.provinceId).collectAsState(initial = emptyList())
    val c = glColors()
    val gpsLabel = if (personal.gpsLatitude.isNotBlank() && personal.gpsLongitude.isNotBlank()) {
        "${personal.gpsLatitude}, ${personal.gpsLongitude} · ±4 m"
    } else {
        "-13.85044, 32.50217 · ±4 m"
    }

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            WizardFooter("Back", onBack, "Continue", onContinue, primaryIcon = "arrow-right")
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            GlScreenHeader(title = "New grower", subtitle = "3 of 4", onBack = onBack)
            GlStepper(step = 3, total = 4, stepLabels = wizardLabels)
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
                GlTextField(
                    value = personal.villageChief,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(villageChief = it) } },
                    label = "Village / chief",
                    placeholder = "Village (Chief)",
                )
                GlCard(contentPadding = 0.dp) {
                    GlImageSlot(label = "map · pinned 13.85°S 32.50°E", height = 120.dp, rounded = 0.dp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlTextField(
                        value = personal.totalAreaHa,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(totalAreaHa = it) } },
                        label = "Total area (ha)",
                        modifier = Modifier.weight(1f),
                    )
                    GlTextField(
                        value = personal.tobaccoAreaHa,
                        onValueChange = { viewModel.updatePersonal { p -> p.copy(tobaccoAreaHa = it) } },
                        label = "Tobacco area (ha)",
                        modifier = Modifier.weight(1f),
                    )
                }
                GlDropdownField(
                    label = "Tobacco type",
                    options = GrowerFormChoices.tobaccoTypeOptions.map { it to it },
                    selectedId = personal.tobaccoTypeName.ifBlank { "Burley" },
                    onSelected = { viewModel.updatePersonal { p -> p.copy(tobaccoTypeName = it) } },
                    required = true,
                )
                GlDropdownField(
                    label = "Curing structure",
                    options = GrowerFormChoices.curingStructureOptions.map { it to it },
                    selectedId = personal.curingStructure.ifBlank { "Open shed" },
                    onSelected = { viewModel.updatePersonal { p -> p.copy(curingStructure = it) } },
                )
                GlTextField(
                    value = personal.estimatedYieldKg,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(estimatedYieldKg = it) } },
                    label = "Estimated yield (kg)",
                )
                GlTextField(
                    value = personal.farmNotes,
                    onValueChange = { viewModel.updatePersonal { p -> p.copy(farmNotes = it) } },
                    label = "Notes",
                    placeholder = "Additional notes…",
                    singleLine = false,
                )
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
    val photoSlots = listOf(
        Triple("NRC · Front", personal.idFrontPath != null, personal.idFrontPath),
        Triple("NRC · Back", personal.idBackPath != null, personal.idBackPath),
        Triple("Farm overview", personal.farmOverviewPhotoPath != null, personal.farmOverviewPhotoPath),
        Triple("Curing barn", personal.curingBarnPhotoPath != null, personal.curingBarnPhotoPath),
    )

    Scaffold(
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
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            GlScreenHeader(title = "New grower", subtitle = "4 of 4", onBack = onBack)
            GlStepper(step = 4, total = 4, stepLabels = wizardLabels)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                GlBanner(
                    title = "3 photos required",
                    subtitle = "NRC front · NRC back · Farm overview. Optional: signature, additional photos.",
                    tone = GlTone.Warning,
                    icon = "camera",
                )
                GlSectionHeader(title = "Required", modifier = Modifier.padding(top = 14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    photoSlots.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (label, captured, _) ->
                                PhotoCaptureCard(
                                    label = label,
                                    captured = captured,
                                    onClick = {
                                        when (label) {
                                            "NRC · Front" -> viewModel.updatePersonal { it.copy(idFrontPath = "captured") }
                                            "NRC · Back" -> viewModel.updatePersonal { it.copy(idBackPath = "captured") }
                                            "Farm overview" -> viewModel.updatePersonal { it.copy(farmOverviewPhotoPath = "captured") }
                                            else -> viewModel.updatePersonal { it.copy(curingBarnPhotoPath = "captured") }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                GlSectionHeader(title = "Signature", modifier = Modifier.padding(top = 16.dp))
                GlCard(contentPadding = 14.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surfaceAlt),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (personal.signatureCaptured) {
                            GlIcon("check", size = 32.dp, tint = c.primary)
                        } else {
                            Text("Tap to sign", color = c.textMuted, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (personal.signatureCaptured) "Captured 09:42" else "",
                                color = c.textMuted,
                                fontSize = 10.sp,
                            )
                            Text(
                                if (personal.signatureCaptured) "Re-sign" else "Sign",
                                color = c.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    viewModel.updatePersonal { it.copy(signatureCaptured = !it.signatureCaptured) }
                                },
                            )
                        }
                    }
                }
                GlSectionHeader(title = "Consent", modifier = Modifier.padding(top = 16.dp))
                GlCard(contentPadding = 14.dp) {
                    GlRow(
                        title = "Grower has consented to data capture",
                        subtitle = "Per TBZ Privacy Policy v3",
                        leadingIcon = "check-circle",
                        tone = GlTone.Primary,
                        trailing = {
                            GlToggle(
                                checked = personal.consentGiven,
                                onCheckedChange = { checked ->
                                    viewModel.updatePersonal { it.copy(consentGiven = checked) }
                                },
                            )
                        },
                    )
                }
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
) {
    val c = glColors()
    GlCard(onClick = onClick, contentPadding = 0.dp, modifier = modifier) {
        GlImageSlot(
            label = if (captured) "" else "tap to capture",
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
    provisionalId: String = "TBZ-2024-04412",
) {
    val c = glColors()
    Scaffold(
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
                "Mary Phiri has been queued for sync. You'll receive a confirmation when the grower ID is issued.",
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
    val c = glColors()
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "align NRC inside frame",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        "224018/61/1",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                    )
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
                    Text("Detected · 224018/61/1", color = c.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Mary Phiri · F · 14 Jun 1986", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text("Confidence 96% · Eastern Province", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(14.dp))
                GlButton(
                    text = "Use this ID",
                    onClick = {
                        viewModel.applyScannedIdentity("224018/61/1", "Mary Phiri", "Female", "14 / 06 / 1986")
                        onUseId()
                    },
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
