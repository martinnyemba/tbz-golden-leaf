package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlDateField
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.isoDateToUtcMillis
import zm.co.tbz.goldenleaf.ui.components.todayUtcMillis
import zm.co.tbz.goldenleaf.ui.components.GlTone

@Composable
fun PermitReviewPanel(
    state: PermitReviewUiState,
    onFormChange: (PermitReviewForm) -> Unit,
    onSubmit: () -> Unit,
) {
    val form = state.form
    GlCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlSectionHeader(title = "Review permit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PermitReviewActions.APPROVE to "Approve",
                    PermitReviewActions.REJECT to "Reject",
                    PermitReviewActions.RETURN to "Return",
                ).forEach { (action, label) ->
                    GlButton(
                        text = label,
                        onClick = { onFormChange(form.copy(action = action)) },
                        variant = if (form.action == action) GlButtonVariant.Secondary else GlButtonVariant.Outline,
                        size = GlButtonSize.Sm,
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (form.action == PermitReviewActions.APPROVE) {
                GlDateField(
                    value = form.validFrom,
                    onValueChange = { onFormChange(form.copy(validFrom = it)) },
                    label = "Valid from",
                    required = true,
                    minDateMillis = todayUtcMillis(),
                    error = state.fieldErrors["validFrom"],
                )
                GlDateField(
                    value = form.validTo,
                    onValueChange = { onFormChange(form.copy(validTo = it)) },
                    label = "Valid to",
                    required = true,
                    // Can't expire before it starts (falls back to today if 'from' unset).
                    minDateMillis = isoDateToUtcMillis(form.validFrom) ?: todayUtcMillis(),
                    error = state.fieldErrors["validTo"],
                )
            } else {
                GlTextField(
                    value = form.reason,
                    onValueChange = { onFormChange(form.copy(reason = it)) },
                    label = "Reason",
                    singleLine = false,
                    minLines = 3,
                    error = state.fieldErrors["reason"],
                )
            }
            state.submitError?.let { ErrorText(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlButton(
                    text = if (state.isSubmitting) "Submitting…" else "Submit review",
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
                GlButton(
                    text = "Reset",
                    onClick = { onFormChange(PermitReviewForm(action = form.action)) },
                    variant = GlButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun PermitReviewSuccessBanner(show: Boolean) {
    if (show) {
        GlBanner(title = "Review queued for sync", tone = GlTone.Success, icon = "cloud-up")
    }
}
