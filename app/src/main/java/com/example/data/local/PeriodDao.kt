package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PeriodRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_records WHERE gameMode = :mode ORDER BY periodId DESC LIMIT :limit")
    fun getRecentPeriods(mode: String, limit: Int = 1000): Flow<List<PeriodRecord>>

    @Query("SELECT * FROM period_records WHERE gameMode = :mode ORDER BY periodId DESC LIMIT :limit")
    suspend fun getRecentPeriodsList(mode: String, limit: Int = 1000): List<PeriodRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriods(periods: List<PeriodRecord>)

    @Query("DELETE FROM period_records WHERE gameMode = :mode")
    suspend fun clearPeriods(mode: String)

    @Query("SELECT COUNT(*) FROM period_records WHERE gameMode = :mode")
    fun getPeriodCount(mode: String): Flow<Int>
}
