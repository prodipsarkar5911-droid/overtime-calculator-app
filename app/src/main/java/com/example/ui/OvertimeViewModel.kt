package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ShiftRecord
import com.example.data.ShiftRepository
import com.example.export.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlySummary(
    val totalShifts: Int = 0,
    val totalWorkedHours: Double = 0.0,
    val totalStandardHours: Double = 0.0,
    val totalOvertimeHours: Double = 0.0,
    val sundayOvertimeHours: Double = 0.0,
    val regularOvertimeHours: Double = 0.0,
    val sundayShiftCount: Int = 0,
    val regularShiftCount: Int = 0
)

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val file: File, val uri: Uri, val fileName: String, val count: Int) : ExportState
    data class Error(val message: String) : ExportState
}

@OptIn(ExperimentalCoroutinesApi::class)
class OvertimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShiftRepository

    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1) // 1-12
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ShiftRepository(db.shiftDao())

        // Preload/seed sample data if empty for the current month
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded(_selectedYear.value, _selectedMonth.value)
        }
    }

    val shifts: StateFlow<List<ShiftRecord>> = combine(_selectedYear, _selectedMonth) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        repository.getShiftsForMonthFlow(year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<MonthlySummary> = shifts.combine(_selectedMonth) { list, _ ->
        var totalWorked = 0.0
        var totalStandard = 0.0
        var totalOt = 0.0
        var sundayOt = 0.0
        var regularOt = 0.0
        var sundayCount = 0
        var regularCount = 0

        for (shift in list) {
            totalWorked += shift.totalHours
            totalStandard += shift.standardDutyHours
            totalOt += shift.overtimeHours
            if (shift.isSunday) {
                sundayOt += shift.overtimeHours
                sundayCount++
            } else {
                regularOt += shift.overtimeHours
                regularCount++
            }
        }

        MonthlySummary(
            totalShifts = list.size,
            totalWorkedHours = Math.round(totalWorked * 100.0) / 100.0,
            totalStandardHours = Math.round(totalStandard * 100.0) / 100.0,
            totalOvertimeHours = Math.round(totalOt * 100.0) / 100.0,
            sundayOvertimeHours = Math.round(sundayOt * 100.0) / 100.0,
            regularOvertimeHours = Math.round(regularOt * 100.0) / 100.0,
            sundayShiftCount = sundayCount,
            regularShiftCount = regularCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySummary()
    )

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = _selectedYear.value - 1
        } else {
            _selectedMonth.value = _selectedMonth.value - 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = _selectedYear.value + 1
        } else {
            _selectedMonth.value = _selectedMonth.value + 1
        }
    }

    fun jumpToToday() {
        val today = Calendar.getInstance()
        _selectedYear.value = today.get(Calendar.YEAR)
        _selectedMonth.value = today.get(Calendar.MONTH) + 1
    }

    fun saveShift(
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
        existingId: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSunday = dayOfWeek == Calendar.SUNDAY
            val (total, standard, ot) = ShiftRecord.calculateHours(
                inHour = inHour,
                inMinute = inMinute,
                outHour = outHour,
                outMinute = outMinute,
                isSunday = isSunday
            )

            val record = ShiftRecord(
                id = existingId,
                dateString = dateString,
                year = year,
                month = month,
                day = day,
                dayOfWeek = dayOfWeek,
                inTimeFormatted = ShiftRepository.formatTime(inHour, inMinute),
                inHour = inHour,
                inMinute = inMinute,
                outTimeFormatted = ShiftRepository.formatTime(outHour, outMinute),
                outHour = outHour,
                outMinute = outMinute,
                totalHours = total,
                standardDutyHours = standard,
                overtimeHours = ot,
                isSunday = isSunday,
                notes = notes
            )
            repository.insertOrUpdate(record)
        }
    }

    fun deleteShift(shift: ShiftRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(shift)
        }
    }

    fun exportToExcel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportState.value = ExportState.Exporting
            try {
                val currentShifts = repository.getShiftsForMonth(_selectedYear.value, _selectedMonth.value)
                if (currentShifts.isEmpty()) {
                    _exportState.value = ExportState.Error("No shifts to export for the selected month.")
                    return@launch
                }
                val (file, uri) = ExcelExporter.exportMonthlyReportToFile(
                    context = context,
                    year = _selectedYear.value,
                    month = _selectedMonth.value,
                    shifts = currentShifts
                )
                _exportState.value = ExportState.Success(
                    file = file,
                    uri = uri,
                    fileName = file.name,
                    count = currentShifts.size
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun saveToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentShifts = repository.getShiftsForMonth(_selectedYear.value, _selectedMonth.value)
                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    ExcelExporter.writeReportToStream(
                        outputStream = outStream,
                        year = _selectedYear.value,
                        month = _selectedMonth.value,
                        shifts = currentShifts
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportState.value = ExportState.Error("Save failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun getMonthDisplayName(month: Int = _selectedMonth.value, year: Int = _selectedYear.value): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        return SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }
}
