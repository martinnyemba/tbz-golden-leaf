package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

@Composable
fun PermitCorrectionScreen(
    localId: String,
    onDone: () -> Unit,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val permit by remember(localId) { viewModel.observeTransportPermit(localId) }.collectAsState()
    val uiState by viewModel.correctionState.collectAsState()
    val form = uiState.form
    val provinces by viewModel.provinces.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val districts by viewModel.districtsForProvince(form.originProvince).collectAsState(initial = emptyList())
    val c = glColors()

    LaunchedEffect(permit?.local_id) {
        permit?.let { viewModel.loadCorrectionFromPermit(it) }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Permit correction")
            if (permit == null) {
                GlEmptyState(title = "Permit not found", icon = "search")
            } else {
                ScrollableFormColumn {
                    GlBanner(title = "Status: RETURNED_FOR_CORRECTION", tone = GlTone.Warning, icon = "warning")
                    permit!!.correction_reason?.let {
                        GlBanner(title = "TBZ reason", subtitle = it, tone = GlTone.Warning, icon = "info")
                    }
                    Text(
                        permit!!.permit_number ?: "Transport permit",
                        color = c.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    GlSectionHeader(title = "Movement & grower")
                    GlDropdownField(
                        label = "Grower category",
                        options = PermitFormChoices.growerCategories,
                        selectedId = form.growerCategory,
                        onSelected = { viewModel.updateCorrectionForm { f -> f.copy(growerCategory = it) } },
                    )
                    GlTextField(
                        value = form.totalBales,
                        onValueChange = { viewModel.updateCorrectionForm { f -> f.copy(totalBales = it) } },
                        label = "Total bales",
                        error = uiState.fieldErrors["totalBales"],
                    )
                    GlTextField(
                        value = form.totalWeightKg,
                        onValueChange = { viewModel.updateCorrectionForm { f -> f.copy(totalWeightKg = it) } },
                        label = "Total weight (kg)",
                        error = uiState.fieldErrors["totalWeightKg"],
                    )
                    GlTextField(
                        value = form.licensePlate,
                        onValueChange = { viewModel.updateCorrectionForm { f -> f.copy(licensePlate = it) } },
                        label = "License plate",
                        error = uiState.fieldErrors["licensePlate"],
                    )
                    GlDropdownField(
                        label = "Origin province",
                        options = provinces.map { it.id to it.name },
                        selectedId = form.originProvince,
                        onSelected = {
                            viewModel.updateCorrectionForm { f ->
                                f.copy(originProvince = it, originDistrict = "")
                            }
                        },
                    )
                    GlDropdownField(
                        label = "Origin district",
                        options = districts.map { it.id to it.name },
                        selectedId = form.originDistrict,
                        onSelected = { viewModel.updateCorrectionForm { f -> f.copy(originDistrict = it) } },
                    )
                    GlDropdownField(
                        label = "Destination sales floor",
                        options = salesFloors.map { it.id to it.name },
                        selectedId = form.destinationSalesFloor,
                        onSelected = { viewModel.updateCorrectionForm { f -> f.copy(destinationSalesFloor = it) } },
                    )
                    GlDropdownField(
                        label = "Purpose",
                        options = PermitFormChoices.purposes,
                        selectedId = form.purpose,
                        onSelected = { viewModel.updateCorrectionForm { f -> f.copy(purpose = it) } },
                    )
                    GlSectionHeader(title = "Buyer & notes")
                    GlDropdownField(
                        label = "Buyer",
                        options = listOf("" to "None") + buyers.map { it.id to it.name },
                        selectedId = form.buyerId,
                        onSelected = { viewModel.updateCorrectionForm { f -> f.copy(buyerId = it) } },
                    )
                    GlCard(contentPadding = 12.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = form.isBought,
                                    onCheckedChange = { viewModel.updateCorrectionForm { f -> f.copy(isBought = it) } },
                                )
                                Text("Tobacco already bought", fontSize = 14.sp, color = c.text)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = form.buyerAccepted,
                                    onCheckedChange = { viewModel.updateCorrectionForm { f -> f.copy(buyerAccepted = it) } },
                                )
                                Text("Buyer accepted / confirmed", fontSize = 14.sp, color = c.text)
                            }
                        }
                    }
                    GlTextField(
                        value = form.comments,
                        onValueChange = { viewModel.updateCorrectionForm { f -> f.copy(comments = it) } },
                        label = "Comments",
                        singleLine = false,
                        minLines = 3,
                    )
                    uiState.saveError?.let { ErrorText(it) }
                    if (uiState.saveSuccess) {
                        GlBanner(title = "Corrections queued for sync", tone = GlTone.Success, icon = "cloud-up")
                    }
                    val remoteId = permit!!.remote_id ?: permit!!.local_id
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlButton(
                            text = if (uiState.isSaving) "Saving…" else "Save corrections",
                            onClick = { viewModel.saveTransportPermitCorrection(localId, remoteId) {} },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                        )
                        GlButton(
                            text = "Resubmit for review",
                            onClick = { viewModel.resubmitTransportPermitCorrection(localId, remoteId, onDone) },
                            variant = GlButtonVariant.Gold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    GlButton(
                        text = "Back",
                        onClick = onDone,
                        variant = GlButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
