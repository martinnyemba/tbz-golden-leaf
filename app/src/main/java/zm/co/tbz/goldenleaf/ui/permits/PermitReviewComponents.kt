package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField

@Composable
fun PermitReviewPanel(
    state: PermitReviewUiState,
    onFormChange: (PermitReviewForm) -> Unit,
    onSubmit: () -> Unit,
) {
    val form = state.form
    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormSectionTitle("Review permit")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    PermitReviewActions.APPROVE to "Approve",
                    PermitReviewActions.REJECT to "Reject",
                    PermitReviewActions.RETURN to "Return",
                ).forEach { (action, label) ->
                    FilterChip(
                        selected = form.action == action,
                        onClick = { onFormChange(form.copy(action = action)) },
                        label = { Text(label) },
                    )
                }
            }
            if (form.action == PermitReviewActions.APPROVE) {
                FormTextField(
                    form.validFrom,
                    { onFormChange(form.copy(validFrom = it)) },
                    "Valid from (YYYY-MM-DD)",
                    error = state.fieldErrors["validFrom"],
                )
                FormTextField(
                    form.validTo,
                    { onFormChange(form.copy(validTo = it)) },
                    "Valid to (YYYY-MM-DD)",
                    error = state.fieldErrors["validTo"],
                )
            } else {
                FormTextField(
                    form.reason,
                    { onFormChange(form.copy(reason = it)) },
                    "Reason",
                    singleLine = false,
                    error = state.fieldErrors["reason"],
                )
            }
            state.submitError?.let { ErrorText(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSubmitting) "Submitting…" else "Submit review")
                }
                OutlinedButton(
                    onClick = { onFormChange(PermitReviewForm(action = form.action)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
            }
        }
    }
}

@Composable
fun PermitReviewSuccessBanner(show: Boolean) {
    if (show) {
        Text("Review queued for sync", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    }
}
