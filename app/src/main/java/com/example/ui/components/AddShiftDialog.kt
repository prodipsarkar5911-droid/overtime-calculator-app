package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ShiftRecord
import com.example.data.ShiftRepository
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberAccentContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BluePrimaryContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.PurpleSunday
import com.example.ui.theme.PurpleSundayContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShiftDialog(
    initialYear: Int,
    initialMonth: Int,
    existingShift: ShiftRecord? = null,
    onDismiss: () -> Unit,
    onSave: (
        dateString: String,
        year: Int,
        month: Int,
        day: Int,
        dayOfWeek: Int,
        inHour: Int,
        inMinute: Int,
        outHour: Int,
        outMinute: Int,
        notes: String,
        existingId: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = existingShift != null

    val cal = remember {
        Calendar.getInstance().apply {
            if (existingShift != null) {
                set(Calendar.YEAR, existingShift.year)
                set(Calendar.MONTH, existingShift.month - 1)
                set(Calendar.DAY_OF_MONTH, existingShift.day)
            } else {
                set(Calendar.YEAR, initialYear)
                set(Calendar.MONTH, initialMonth - 1)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = get(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, currentDay.coerceAtMost(maxDay))
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) } // 1-12
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var selectedDayOfWeek by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_WEEK)) }

    // In Time state (Default: 10:00 AM)
    var inHour by remember { mutableIntStateOf(existingShift?.inHour ?: 10) }
    var inMinute by remember { mutableIntStateOf(existingShift?.inMinute ?: 0) }

    // Out Time state (Default: 11:00 PM = 23:00)
    var outHour by remember { mutableIntStateOf(existingShift?.outHour ?: 23) }
    var outMinute by remember { mutableIntStateOf(existingShift?.outMinute ?: 0) }

    var notes by remember { mutableStateOf(existingShift?.notes ?: "") }

    val isSunday = selectedDayOfWeek == Calendar.SUNDAY

    // Real-time calculation based on rules
    val (totalHours, standardDutyHours, overtimeHours) = remember(inHour, inMinute, outHour, outMinute, isSunday) {
        ShiftRecord.calculateHours(
            inHour = inHour,
            inMinute = inMinute,
            outHour = outHour,
            outMinute = outMinute,
            isSunday = isSunday
        )
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US) }

    val formattedDate = remember(selectedYear, selectedMonth, selectedDay) {
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        displayDateFormat.format(tempCal.time)
    }

    val dbDateString = remember(selectedYear, selectedMonth, selectedDay) {
        String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
    }

    fun openDatePicker() {
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month + 1
                selectedDay = dayOfMonth
                val tempCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
            },
            selectedYear,
            selectedMonth - 1,
            selectedDay
        )
        dpd.show()
    }

    fun openInTimePicker() {
        val tpd = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                inHour = hourOfDay
                inMinute = minute
            },
            inHour,
            inMinute,
            false // 12-hour format with AM/PM picker
        )
        tpd.show()
    }

    fun openOutTimePicker() {
        val tpd = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                outHour = hourOfDay
                outMinute = minute
            },
            outHour,
            outMinute,
            false // 12-hour format with AM/PM picker
        )
        tpd.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit Shift Log" else "Add Daily Shift",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatic duty deduction & overtime calculation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date Picker Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDatePicker() }
                        .testTag("date_picker_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSunday) PurpleSundayContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = if (isSunday) PurpleSunday else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isSunday) "⭐ Sunday (Special Rule Applies)" else "Regular Workday (Mon - Sat)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSunday) PurpleSunday else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Time Pickers (In Time & Out Time)
                Text(
                    text = "Shift Time",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // In Time Selector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openInTimePicker() }
                            .testTag("in_time_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "IN TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ShiftRepository.formatTime(inHour, inMinute),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap to set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Out Time Selector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openOutTimePicker() }
                            .testTag("out_time_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "OUT TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ShiftRepository.formatTime(outHour, outMinute),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = AmberAccent
                            )
                            Text(
                                text = "Tap to set",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberAccent.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Quick Preset Chips
                Text(
                    text = "Quick Shift Presets",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        Pair("10 AM - 11 PM (13h)", Pair(Pair(10, 0), Pair(23, 0))),
                        Pair("9:30 AM - 9:30 PM (12h)", Pair(Pair(9, 30), Pair(21, 30))),
                        Pair("10 AM - 8 PM (10h)", Pair(Pair(10, 0), Pair(20, 0))),
                        Pair("9 AM - 6 PM (9h)", Pair(Pair(9, 0), Pair(18, 0))),
                        Pair("8 AM - 8 PM (12h)", Pair(Pair(8, 0), Pair(20, 0)))
                    )

                    for ((label, times) in presets) {
                        val isSelected = inHour == times.first.first && inMinute == times.first.second &&
                                outHour == times.second.first && outMinute == times.second.second

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                inHour = times.first.first
                                inMinute = times.first.second
                                outHour = times.second.first
                                outMinute = times.second.second
                            },
                            label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Dynamic Live Calculation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSunday) {
                            PurpleSundayContainer.copy(alpha = 0.45f)
                        } else {
                            AmberAccentContainer.copy(alpha = 0.35f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSunday) Icons.Default.Star else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isSunday) PurpleSunday else OnAmberContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSunday) "Sunday Special Rule Active" else "Automated Standard Deduction",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSunday) PurpleSunday else OnAmberContainer
                            )
                        }

                        Text(
                            text = if (isSunday) {
                                "Full worked hours are counted directly as Overtime (Full OT). No 10 hours standard deduction."
                            } else {
                                "Subtracts fixed 10 hours (9h duty + 1h break). Remaining hours saved as Overtime (OT)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Real-time Results Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Worked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", totalHours)} hrs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (isSunday) "→" else "− 10h =",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isSunday) "Sunday OT" else "Overtime (OT)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", overtimeHours)} hrs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (overtimeHours > 0) AmberAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Shift Notes / Remarks (Optional)") },
                    placeholder = { Text("e.g. Special project, night shift, emergency cover") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shift_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_shift_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSave(
                                dbDateString,
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedDayOfWeek,
                                inHour,
                                inMinute,
                                outHour,
                                outMinute,
                                notes.trim(),
                                existingShift?.id ?: 0
                            )
                        },
                        modifier = Modifier.testTag("save_shift_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditMode) "Update Shift" else "Save Shift")
                    }
                }
            }
        }
    }
}
