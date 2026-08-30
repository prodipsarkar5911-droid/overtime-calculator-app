package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ShiftRecord
import com.example.ui.components.AddShiftDialog
import com.example.ui.components.ExportResultDialog
import com.example.ui.components.MonthSelector
import com.example.ui.components.ShiftItemCard
import com.example.ui.components.SummaryDashboard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberAccentContainer
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.PurpleSunday
import com.example.ui.theme.PurpleSundayContainer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeScreen(
    viewModel: OvertimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftRecord?>(null) }
    var shiftToDelete by remember { mutableStateOf<ShiftRecord?>(null) }
    var showRulesInfoDialog by remember { mutableStateOf(false) }

    // Launcher for SAF "Save As" document creation
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            viewModel.saveToUri(context, uri)
            Toast.makeText(context, "Saved Excel file successfully!", Toast.LENGTH_LONG).show()
        }
    }

    // Handle export state error toasts
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Error) {
            Toast.makeText(context, (exportState as ExportState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(BluePrimary, Color(0xFF0369A1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Overtime & Shift Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Excel (.xlsx) Report Generator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRulesInfoDialog = true },
                        modifier = Modifier.testTag("rules_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Calculation Rules",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingShift = null
                    showAddDialog = true
                },
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 72.dp) // Offset above sticky export bar
                    .testTag("add_shift_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shift")
                    Text(
                        text = "Add Shift",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        bottomBar = {
            // Sticky Bottom Bar with Prominent "Export to Excel (.xlsx)" button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary info pill
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Report",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "${shifts.size} shifts • ${String.format(Locale.US, "%.1f", summary.totalOvertimeHours)}h OT",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Export Button
                    Button(
                        onClick = { viewModel.exportToExcel(context) },
                        modifier = Modifier.testTag("export_excel_button"),
                        enabled = exportState !is ExportState.Exporting && shifts.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        if (exportState is ExportState.Exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporting...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export to Excel (.xlsx)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Month Selector Bar
            item {
                MonthSelector(
                    monthDisplayName = viewModel.getMonthDisplayName(selectedMonth, selectedYear),
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onJumpToToday = { viewModel.jumpToToday() }
                )
            }

            // 2. Summary Dashboard
            item {
                SummaryDashboard(summary = summary)
            }

            // 3. Calculation Rules Notice Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Automated Overtime Rules",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "• Mon–Sat: Subtracts 10h (9h duty + 1h break). Remaining hours = OT.\n• Sunday: Full worked hours counted directly as Overtime (Full OT).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Section Header: Shift Records
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shift Records (${shifts.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = {
                            editingShift = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("quick_add_shift_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Shift")
                    }
                }
            }

            // 5. Shift List Items or Empty State
            if (shifts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = "No Shifts Logged for ${viewModel.getMonthDisplayName(selectedMonth, selectedYear)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Tap '+ Add Shift' to record In Time and Out Time for any day. Overtime and duty deductions will be calculated automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    editingShift = null
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Shift")
                            }
                        }
                    }
                }
            } else {
                items(shifts, key = { it.id }) { shift ->
                    ShiftItemCard(
                        shift = shift,
                        onEdit = {
                            editingShift = shift
                            showAddDialog = true
                        },
                        onDelete = {
                            shiftToDelete = shift
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Add / Edit Shift Dialog
    if (showAddDialog) {
        AddShiftDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            existingShift = editingShift,
            onDismiss = {
                showAddDialog = false
                editingShift = null
            },
            onSave = { dateString, year, month, day, dayOfWeek, inHour, inMinute, outHour, outMinute, notes, existingId ->
                viewModel.saveShift(
                    dateString = dateString,
                    year = year,
                    month = month,
                    day = day,
                    dayOfWeek = dayOfWeek,
                    inHour = inHour,
                    inMinute = inMinute,
                    outHour = outHour,
                    outMinute = outMinute,
                    notes = notes,
                    existingId = existingId
                )
                showAddDialog = false
                editingShift = null
                Toast.makeText(context, if (existingId != 0) "Shift updated!" else "Shift saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    shiftToDelete?.let { shift ->
        AlertDialog(
            onDismissRequest = { shiftToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Shift Record?") },
            text = {
                Text("Are you sure you want to delete the shift for ${shift.dateString} (${shift.inTimeFormatted} to ${shift.outTimeFormatted})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShift(shift)
                        shiftToDelete = null
                        Toast.makeText(context, "Shift deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Result Dialog
    (exportState as? ExportState.Success)?.let { successState ->
        ExportResultDialog(
            exportSuccess = successState,
            onDismiss = { viewModel.resetExportState() },
            onSaveToStorage = {
                saveDocumentLauncher.launch(successState.fileName)
            }
        )
    }

    // Calculation Rules Info Dialog
    if (showRulesInfoDialog) {
        AlertDialog(
            onDismissRequest = { showRulesInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Calculation Rules",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Time Input & Total Hours:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter daily In Time and Out Time. The app calculates total elapsed work duration (e.g., 10:00 AM to 11:00 PM = 13.0 hours).",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "2. Automated Standard Deduction (Mon - Sat):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "For regular days (Monday to Saturday), 10 hours (9h duty + 1h break) are deducted from total hours. The remaining hours are automatically recorded as Overtime (OT). (e.g., 13h total - 10h = 3h OT).",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "3. Sunday Special Logic (Full OT):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PurpleSunday
                    )
                    Text(
                        text = "When working on Sunday, the 10 hours standard deduction is waived. All worked hours (e.g., all 13 hours) are counted directly as Overtime (Full OT).",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "4. Excel (.xlsx) Monthly Export:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberAccent
                    )
                    Text(
                        text = "Generates a complete spreadsheet with columns for Date, In Time, Out Time, Standard Duty Hours, Overtime Hours, and a Total Overtime Summary row.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showRulesInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}
