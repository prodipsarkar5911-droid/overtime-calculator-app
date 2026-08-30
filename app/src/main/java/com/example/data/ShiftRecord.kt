package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "shifts")
data class ShiftRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dateString: String, // "YYYY-MM-DD" e.g. "2026-08-30"
    val year: Int,
    val month: Int, // 1-12
    val day: Int, // 1-31
    val dayOfWeek: Int, // Calendar.SUNDAY (1), Calendar.MONDAY (2), etc.
    val inTimeFormatted: String, // "10:00 AM"
    val inHour: Int, // 0-23
    val inMinute: Int, // 0-59
    val outTimeFormatted: String, // "11:00 PM"
    val outHour: Int, // 0-23
    val outMinute: Int, // 0-59
    val totalHours: Double,
    val standardDutyHours: Double,
    val overtimeHours: Double,
    val isSunday: Boolean,
    val notes: String = ""
) {
    companion object {
        /**
         * Core Calculation Rule:
         * 1. Calculate elapsed hours between In Time and Out Time.
         * 2. If Sunday: full worked hours count as Overtime (Standard duty = 0).
         * 3. Normal days (Mon-Sat): subtract fixed 10 hours (9h duty + 1h break).
         *    Remaining hours saved as Overtime (OT).
         */
        fun calculateHours(
            inHour: Int,
            inMinute: Int,
            outHour: Int,
            outMinute: Int,
            isSunday: Boolean
        ): Triple<Double, Double, Double> {
            val inTotalMin = inHour * 60 + inMinute
            val outTotalMin = outHour * 60 + outMinute
            
            val elapsedMinutes = if (outTotalMin >= inTotalMin) {
                outTotalMin - inTotalMin
            } else {
                // Crosses midnight (e.g. 10:00 PM to 06:00 AM next day)
                (24 * 60 - inTotalMin) + outTotalMin
            }

            // Convert to hours with 2 decimal precision
            val rawTotalHours = elapsedMinutes / 60.0
            val totalHours = Math.round(rawTotalHours * 100.0) / 100.0

            if (totalHours <= 0.0) {
                return Triple(0.0, 0.0, 0.0)
            }

            return if (isSunday) {
                // Sunday: full worked hours counted directly as Overtime (Full OT)
                Triple(totalHours, 0.0, totalHours)
            } else {
                // Regular day (Mon-Sat):
                // Fixed 10 hours standard deduction (9h duty + 1h break)
                if (totalHours > 10.0) {
                    val ot = Math.round((totalHours - 10.0) * 100.0) / 100.0
                    Triple(totalHours, 10.0, ot)
                } else {
                    Triple(totalHours, totalHours, 0.0)
                }
            }
        }
    }
}
