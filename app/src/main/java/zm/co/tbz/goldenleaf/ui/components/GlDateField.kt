package zm.co.tbz.goldenleaf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

/**
 * Material3 date-picker field that stores its value as an ISO `yyyy-MM-dd`
 * string — the exact format the Django `DateField` endpoints expect — while
 * showing a friendly `dd MMM yyyy` label. All conversions go through UTC
 * start-of-day so the picked calendar day and the stored string never drift by
 * a timezone offset (the classic "off-by-one-day" date-picker bug).
 *
 * [minDateMillis]/[maxDateMillis] are UTC millis (use [todayUtcMillis]) that
 * bound the selectable range, e.g. a sale date that can't be in the future.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select date",
    required: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
) {
    val c = glColors()
    var showPicker by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)
    val hasValue = value.isNotBlank()
    val display = if (hasValue) isoDateToDisplay(value) else placeholder

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.padding(start = 4.dp)) {
            Text(label, color = c.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (required) Text(" *", color = c.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(c.surfaceAlt)
                .border(1.5.dp, if (error != null) c.danger else Color.Transparent, shape)
                .then(if (enabled) Modifier.clickable { showPicker = true } else Modifier)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlIcon("calendar", size = 20.dp, tint = c.textMuted)
            Text(
                display,
                color = if (hasValue) c.text else c.placeholder,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (hasValue && enabled) {
                GlIcon(
                    "x",
                    size = 16.dp,
                    tint = c.textSubtle,
                    modifier = Modifier.clickable { onValueChange("") },
                )
            }
        }
        if (error != null) {
            Text(error, color = c.danger, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp))
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(value) ?: maxDateMillis ?: todayUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (minDateMillis != null && utcTimeMillis < minDateMillis) return false
                    if (maxDateMillis != null && utcTimeMillis > maxDateMillis) return false
                    return true
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(utcMillisToIsoDate(it)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** UTC start-of-day millis for today — the anchor for min/max bounds. */
fun todayUtcMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Parse an ISO `yyyy-MM-dd` string to UTC start-of-day millis, or null if blank/invalid. */
fun isoDateToUtcMillis(iso: String): Long? = runCatching {
    LocalDate.parse(iso.trim(), ISO_DATE).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

private fun utcMillisToIsoDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(ISO_DATE)

private fun isoDateToDisplay(iso: String): String = runCatching {
    LocalDate.parse(iso.trim(), ISO_DATE).format(DISPLAY_DATE)
}.getOrDefault(iso)
