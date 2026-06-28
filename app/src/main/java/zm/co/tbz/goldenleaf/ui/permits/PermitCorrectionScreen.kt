package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormActionRow
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import zm.co.tbz.goldenleaf.ui.registration.TbzDropdownField

@Composable
fun PermitCorrectionScreen(
    localId: String,
    onDone: () -> Unit,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val permit by viewModel.observeTransportPermit(localId).collectAsState()
    val uiState by viewModel.correctionState.collectAsState()
    val form = uiState.form
    val provinces by viewModel.provinces.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val districts by viewModel.districtsForProvince(form.originProvince).collectAsState(initial = emptyList())

    LaunchedEffect(permit?.local_id) {
        permit?.let { viewModel.loadCorrectionFromPermit(it) }
    }

    Scaffold(topBar = { TbzTopBar("Permit correction") }) { padding ->
        if (permit == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Permit not found")
            }
        } else {
            ScrollableFormColumn(Modifier.padding(padding)) {
                InfoBanner("Status: RETURNED_FOR_CORRECTION")
                permit!!.correction_reason?.let { InfoBanner("TBZ reason: $it") }
                Text(
                    permit!!.permit_number ?: "Transport permit",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FormSectionTitle("Movement & grower")
                TbzDropdownField(
                    label = "Grower category",
                    options = PermitFormChoices.growerCategories,
                    selectedId = form.growerCategory,
                    onSelected = { viewModel.updateCorrectionForm { f -> f.copy(growerCategory = it) } },
                )
                FormTextField(
                    form.totalBales,
                    { viewModel.updateCorrectionForm { f -> f.copy(totalBales = it) } },
                    "Total bales",
                    error = uiState.fieldErrors["totalBales"],
                )
                FormTextField(
                    form.totalWeightKg,
                    { viewModel.updateCorrectionForm { f -> f.copy(totalWeightKg = it) } },
                    "Total weight (kg)",
                    error = uiState.fieldErrors["totalWeightKg"],
                )
                FormTextField(
                    form.licensePlate,
                    { viewModel.updateCorrectionForm { f -> f.copy(licensePlate = it) } },
                    "License plate",
                    error = uiState.fieldErrors["licensePlate"],
                )
                TbzDropdownField(
                    label = "Origin province",
                    options = provinces.map { it.id to it.name },
                    selectedId = form.originProvince,
                    onSelected = {
                        viewModel.updateCorrectionForm { f ->
                            f.copy(originProvince = it, originDistrict = "")
                        }
                    },
                )
                TbzDropdownField(
                    label = "Origin district",
                    options = districts.map { it.id to it.name },
                    selectedId = form.originDistrict,
                    onSelected = { viewModel.updateCorrectionForm { f -> f.copy(originDistrict = it) } },
                )
                TbzDropdownField(
                    label = "Destination sales floor",
                    options = salesFloors.map { it.id to it.name },
                    selectedId = form.destinationSalesFloor,
                    onSelected = { viewModel.updateCorrectionForm { f -> f.copy(destinationSalesFloor = it) } },
                )
                TbzDropdownField(
                    label = "Purpose",
                    options = PermitFormChoices.purposes,
                    selectedId = form.purpose,
                    onSelected = { viewModel.updateCorrectionForm { f -> f.copy(purpose = it) } },
                )
                FormSectionTitle("Buyer & notes")
                TbzDropdownField(
                    label = "Buyer",
                    options = listOf("" to "None") + buyers.map { it.id to it.name },
                    selectedId = form.buyerId,
                    onSelected = { viewModel.updateCorrectionForm { f -> f.copy(buyerId = it) } },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = form.isBought,
                        onCheckedChange = { viewModel.updateCorrectionForm { f -> f.copy(isBought = it) } },
                    )
                    Text("Tobacco already bought")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = form.buyerAccepted,
                        onCheckedChange = { viewModel.updateCorrectionForm { f -> f.copy(buyerAccepted = it) } },
                    )
                    Text("Buyer accepted / confirmed")
                }
                FormTextField(
                    form.comments,
                    { viewModel.updateCorrectionForm { f -> f.copy(comments = it) } },
                    "Comments",
                    singleLine = false,
                )
                uiState.saveError?.let { ErrorText(it) }
                if (uiState.saveSuccess) {
                    InfoBanner("Corrections queued for sync")
                }
                val remoteId = permit!!.remote_id ?: permit!!.local_id
                FormActionRow(
                    primaryLabel = if (uiState.isSaving) "Saving…" else "Save corrections",
                    onPrimary = { viewModel.saveTransportPermitCorrection(localId, remoteId) {} },
                    secondaryLabel = "Resubmit for review",
                    onSecondary = { viewModel.resubmitTransportPermitCorrection(localId, remoteId, onDone) },
                    primaryEnabled = !uiState.isSaving,
                )
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}
