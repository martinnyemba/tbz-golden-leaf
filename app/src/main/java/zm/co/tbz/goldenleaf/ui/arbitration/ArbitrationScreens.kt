package zm.co.tbz.goldenleaf.ui.arbitration

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

@Composable
fun ArbitrationScreen(
    onSubmitted: () -> Unit = {},
    viewModel: ArbitrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.form
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Arbitration")
            ScrollableFormColumn {
                GlSectionHeader(title = "Bale arbitration")
                GlBanner(
                    title = "Grower and grade are resolved server-side from the bale ticket.",
                    tone = GlTone.Info,
                    icon = "info",
                )
                GlTextField(
                    value = form.baleId,
                    onValueChange = { viewModel.updateForm { f -> f.copy(baleId = it) } },
                    label = "Bale ticket ID",
                    error = uiState.fieldErrors["baleId"],
                )
                QrScanButton(
                    onScan = { viewModel.updateForm { f -> f.copy(baleId = it) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Scan bale barcode",
                )
                GlTextField(
                    value = form.arbitrationDate,
                    onValueChange = { viewModel.updateForm { f -> f.copy(arbitrationDate = it) } },
                    label = "Arbitration date (YYYY-MM-DD)",
                    error = uiState.fieldErrors["arbitrationDate"],
                )
                GlDropdownField(
                    label = "Rejected",
                    options = ArbitrationFormChoices.rejectedOptions.map { (value, label) ->
                        value.toString() to label
                    },
                    selectedId = form.isRejected.toString(),
                    onSelected = { viewModel.updateForm { f -> f.copy(isRejected = it.toBoolean()) } },
                )
                if (form.isRejected) {
                    GlDropdownField(
                        label = "Rejection reason",
                        options = ArbitrationFormChoices.rejectionReasons,
                        selectedId = form.rejectionReason,
                        onSelected = { viewModel.updateForm { f -> f.copy(rejectionReason = it) } },
                        error = uiState.fieldErrors["rejectionReason"],
                    )
                }
                GlTextField(
                    value = form.inspectorRemarks,
                    onValueChange = { viewModel.updateForm { f -> f.copy(inspectorRemarks = it) } },
                    label = "Inspector remarks",
                    singleLine = false,
                    minLines = 3,
                )
                GlCard(contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = form.isFinal,
                            onCheckedChange = { viewModel.updateForm { f -> f.copy(isFinal = it) } },
                        )
                        Text("Final decision (no further arbitration)", fontSize = 14.sp, color = c.text)
                    }
                }
                uiState.saveError?.let { ErrorText(it) }
                if (uiState.saveSuccess) {
                    GlBanner(title = "Arbitration queued for sync", tone = GlTone.Success, icon = "cloud-up")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlButton(
                        text = "Reset",
                        onClick = { viewModel.updateForm { ArbitrationForm() } },
                        variant = GlButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                    )
                    GlButton(
                        text = if (uiState.isSaving) "Submitting…" else "Submit arbitration",
                        onClick = { viewModel.submit(onSubmitted) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
