package zm.co.tbz.goldenleaf.ui.arbitration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormActionRow
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import zm.co.tbz.goldenleaf.ui.registration.TbzDropdownField

@Composable
fun ArbitrationScreen(
    onSubmitted: () -> Unit = {},
    viewModel: ArbitrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.form

    Scaffold(topBar = { TbzTopBar("Arbitration") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormSectionTitle("Bale arbitration")
            InfoBanner("Grower and grade are resolved server-side from the bale ticket.")
            FormTextField(
                form.baleId,
                { viewModel.updateForm { f -> f.copy(baleId = it) } },
                "Bale ticket ID",
                error = uiState.fieldErrors["baleId"],
            )
            QrScanButton(
                onScan = { viewModel.updateForm { f -> f.copy(baleId = it) } },
                modifier = Modifier.fillMaxWidth(),
                label = "Scan bale barcode",
            )
            FormTextField(
                form.arbitrationDate,
                { viewModel.updateForm { f -> f.copy(arbitrationDate = it) } },
                "Arbitration date (YYYY-MM-DD)",
                error = uiState.fieldErrors["arbitrationDate"],
            )
            TbzDropdownField(
                label = "Rejected",
                options = ArbitrationFormChoices.rejectedOptions.map { (value, label) ->
                    value.toString() to label
                },
                selectedId = form.isRejected.toString(),
                onSelected = { viewModel.updateForm { f -> f.copy(isRejected = it.toBoolean()) } },
            )
            if (form.isRejected) {
                TbzDropdownField(
                    label = "Rejection reason",
                    options = ArbitrationFormChoices.rejectionReasons,
                    selectedId = form.rejectionReason,
                    onSelected = { viewModel.updateForm { f -> f.copy(rejectionReason = it) } },
                )
                uiState.fieldErrors["rejectionReason"]?.let { ErrorText(it) }
            }
            FormTextField(
                form.inspectorRemarks,
                { viewModel.updateForm { f -> f.copy(inspectorRemarks = it) } },
                "Inspector remarks",
                singleLine = false,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = form.isFinal,
                    onCheckedChange = { viewModel.updateForm { f -> f.copy(isFinal = it) } },
                )
                Text("Final decision (no further arbitration)")
            }
            uiState.saveError?.let { ErrorText(it) }
            if (uiState.saveSuccess) {
                InfoBanner("Arbitration queued for sync")
            }
            FormActionRow(
                primaryLabel = if (uiState.isSaving) "Submitting…" else "Submit arbitration",
                onPrimary = { viewModel.submit(onSubmitted) },
                secondaryLabel = "Reset",
                onSecondary = {
                    viewModel.updateForm { ArbitrationForm() }
                },
                primaryEnabled = !uiState.isSaving,
            )
        }
    }
}
