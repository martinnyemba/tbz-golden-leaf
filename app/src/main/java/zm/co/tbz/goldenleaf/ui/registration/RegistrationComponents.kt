package zm.co.tbz.goldenleaf.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.glColors

@Composable
fun TbzDropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlDropdownField(
        label = label,
        options = options,
        selectedId = selectedId,
        onSelected = onSelected,
        modifier = modifier,
        placeholder = "Select $label",
        enabled = enabled,
    )
}

@Composable
fun TbzRadioGroup(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = c.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        options.forEach { (value, display) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(selected = selected == value, onClick = { onSelected(value) })
                Text(display, color = c.text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
) {
    GlTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        singleLine = singleLine,
    )
}

@Composable
fun FormSectionTitle(title: String) {
    val c = glColors()
    Text(
        title,
        color = c.text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun SelfSponsoredCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text("Self-sponsored (no sponsor)", color = c.text, fontSize = 14.sp)
    }
}

@Composable
fun FormActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (secondaryLabel != null && onSecondary != null) {
            GlButton(text = secondaryLabel, onClick = onSecondary, variant = GlButtonVariant.Outline)
        }
        GlButton(text = primaryLabel, onClick = onPrimary, enabled = primaryEnabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollableFormColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imeNestedScroll()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
