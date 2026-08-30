package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY dateString ASC, inHour ASC, inMinute ASC")
    fun getAllShiftsFlow(): Flow<List<ShiftRecord>>

    @Query("SELECT * FROM shifts WHERE year = :year AND month = :month ORDER BY dateString ASC, inHour ASC, inMinute ASC")
    fun getShiftsForMonthFlow(year: Int, month: Int): Flow<List<ShiftRecord>>

    @Query("SELECT * FROM shifts WHERE year = :year AND month = :month ORDER BY dateString ASC, inHour ASC, inMinute ASC")
    suspend fun getShiftsForMonth(year: Int, month: Int): List<ShiftRecord>

    @Query("SELECT * FROM shifts WHERE dateString = :dateString LIMIT 1")
    suspend fun getShiftByDate(dateString: String): ShiftRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftRecord>)

    @Update
    suspend fun updateShift(shift: ShiftRecord)

    @Delete
    suspend fun deleteShift(shift: ShiftRecord)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Int)

    @Query("DELETE FROM shifts")
    suspend fun clearAll()
}
