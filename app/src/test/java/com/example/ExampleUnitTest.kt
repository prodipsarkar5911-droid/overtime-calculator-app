package com.example

import com.example.data.ShiftRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNormalDayOvertimeCalculation() {
        // 10:00 AM to 11:00 PM = 13 hours
        // Expected: Total 13h, Standard Duty 10h, OT 3h
        val (total, standard, ot) = ShiftRecord.calculateHours(
            inHour = 10,
            inMinute = 0,
            outHour = 23,
            outMinute = 0,
            isSunday = false
        )
        assertEquals(13.0, total, 0.01)
        assertEquals(10.0, standard, 0.01)
        assertEquals(3.0, ot, 0.01)
    }

    @Test
    fun testNormalDayUnderOrEqualStandardHours() {
        // 9:00 AM to 5:00 PM = 8 hours
        val (total8, standard8, ot8) = ShiftRecord.calculateHours(
            inHour = 9,
            inMinute = 0,
            outHour = 17,
            outMinute = 0,
            isSunday = false
        )
        assertEquals(8.0, total8, 0.01)
        assertEquals(8.0, standard8, 0.01)
        assertEquals(0.0, ot8, 0.01)

        // 9:00 AM to 7:00 PM = 10 hours
        val (total10, standard10, ot10) = ShiftRecord.calculateHours(
            inHour = 9,
            inMinute = 0,
            outHour = 19,
            outMinute = 0,
            isSunday = false
        )
        assertEquals(10.0, total10, 0.01)
        assertEquals(10.0, standard10, 0.01)
        assertEquals(0.0, ot10, 0.01)
    }

    @Test
    fun testSundaySpecialLogicFullOvertime() {
        // Sunday 10:00 AM to 11:00 PM = 13 hours full OT
        val (total, standard, ot) = ShiftRecord.calculateHours(
            inHour = 10,
            inMinute = 0,
            outHour = 23,
            outMinute = 0,
            isSunday = true
        )
        assertEquals(13.0, total, 0.01)
        assertEquals(0.0, standard, 0.01)
        assertEquals(13.0, ot, 0.01)

        // Sunday 8:00 AM to 4:00 PM = 8 hours full OT
        val (total8, standard8, ot8) = ShiftRecord.calculateHours(
            inHour = 8,
            inMinute = 0,
            outHour = 16,
            outMinute = 0,
            isSunday = true
        )
        assertEquals(8.0, total8, 0.01)
        assertEquals(0.0, standard8, 0.01)
        assertEquals(8.0, ot8, 0.01)
    }

    @Test
    fun testOvernightShift() {
        // 10:00 PM (22:00) to 6:00 AM = 8 hours
        val (total, standard, ot) = ShiftRecord.calculateHours(
            inHour = 22,
            inMinute = 0,
            outHour = 6,
            outMinute = 0,
            isSunday = false
        )
        assertEquals(8.0, total, 0.01)
        assertEquals(8.0, standard, 0.01)
        assertEquals(0.0, ot, 0.01)
    }
}
