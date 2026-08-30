package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ShiftRepository(private val shiftDao: ShiftDao) {

    fun getShiftsForMonthFlow(year: Int, month: Int): Flow<List<ShiftRecord>> =
        shiftDao.getShiftsForMonthFlow(year, month)

    suspend fun getShiftsForMonth(year: Int, month: Int): List<ShiftRecord> =
        shiftDao.getShiftsForMonth(year, month)

    suspend fun insertOrUpdate(shift: ShiftRecord): Long =
        shiftDao.insertShift(shift)

    suspend fun delete(shift: ShiftRecord) =
        shiftDao.deleteShift(shift)

    suspend fun deleteById(id: Int) =
        shiftDao.deleteShiftById(id)

    suspend fun getShiftByDate(dateString: String): ShiftRecord? =
        shiftDao.getShiftByDate(dateString)

    suspend fun seedInitialDataIfNeeded(year: Int, month: Int) {
        val existing = shiftDao.getShiftsForMonth(year, month)
        if (existing.isEmpty()) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val sampleShifts = mutableListOf<ShiftRecord>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            // Seed a representative realistic batch of shifts for early days of the month
            val sampleDays = listOf(
                // (day, inHour, inMinute, outHour, outMinute, note)
                Triple(1, Pair(10, 0), Pair(23, 0)), // 10:00 AM to 11:00 PM = 13 hrs
                Triple(2, Pair(9, 30), Pair(21, 30)), // 09:30 AM to 09:30 PM = 12 hrs
                Triple(3, Pair(10, 0), Pair(20, 0)),  // 10:00 AM to 08:00 PM = 10 hrs
                Triple(4, Pair(9, 0), Pair(18, 0)),   // 09:00 AM to 06:00 PM = 9 hrs
                Triple(5, Pair(10, 0), Pair(22, 0)),  // 10:00 AM to 10:00 PM = 12 hrs
                Triple(6, Pair(8, 0), Pair(20, 0)),   // 08:00 AM to 08:00 PM = 12 hrs
                Triple(7, Pair(10, 0), Pair(23, 0))   // 10:00 AM to 11:00 PM = 13 hrs
            )

            for ((day, inTime, outTime) in sampleDays) {
                if (day <= daysInMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val isSunday = dayOfWeek == Calendar.SUNDAY
                    val dateString = dateFormat.format(calendar.time)

                    val (total, standard, ot) = ShiftRecord.calculateHours(
                        inHour = inTime.first,
                        inMinute = inTime.second,
                        outHour = outTime.first,
                        outMinute = outTime.second,
                        isSunday = isSunday
                    )

                    sampleShifts.add(
                        ShiftRecord(
                            dateString = dateString,
                            year = year,
                            month = month,
                            day = day,
                            dayOfWeek = dayOfWeek,
                            inTimeFormatted = formatTime(inTime.first, inTime.second),
                            inHour = inTime.first,
                            inMinute = inTime.second,
                            outTimeFormatted = formatTime(outTime.first, outTime.second),
                            outHour = outTime.first,
                            outMinute = outTime.second,
                            totalHours = total,
                            standardDutyHours = standard,
                            overtimeHours = ot,
                            isSunday = isSunday,
                            notes = if (isSunday) "Sunday Special Duty" else "Regular Day Shift"
                        )
                    )
                }
            }

            if (sampleShifts.isNotEmpty()) {
                shiftDao.insertAll(sampleShifts)
            }
        }
    }

    companion object {
        fun formatTime(hour: Int, minute: Int): String {
            val period = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
        }
    }
}
