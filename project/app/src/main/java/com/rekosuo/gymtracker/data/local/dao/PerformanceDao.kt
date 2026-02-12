package com.rekosuo.gymtracker.data.local.dao

import androidx.room.*
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface that defines what database operations are possible.
 * Room generates implementations at compile time.
 */
@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performances WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun getPerformancesForExercise(exerciseId: Long): Flow<List<PerformanceEntity>>

    @Query("SELECT * FROM performances WHERE exerciseId = :exerciseId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPerformance(exerciseId: Long): PerformanceEntity?
    
    @Query("SELECT * FROM performances WHERE exerciseId = :exerciseId AND date = :date LIMIT 1")
    suspend fun getPerformanceByDate(exerciseId: Long, date: Long): PerformanceEntity?
    
    @Query("SELECT * FROM performances WHERE id = :id")
    suspend fun getPerformanceById(id: Long): PerformanceEntity?
    
    @Query("SELECT * FROM performances WHERE exerciseId = :exerciseId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getPerformancesByDateRange(exerciseId: Long, startDate: Long, endDate: Long): Flow<List<PerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: PerformanceEntity): Long
    
    @Update
    suspend fun updatePerformance(performance: PerformanceEntity)
    
    @Delete
    suspend fun deletePerformance(performance: PerformanceEntity)
    
    @Query("DELETE FROM performances WHERE exerciseId = :exerciseId")
    suspend fun deleteAllPerformancesForExercise(exerciseId: Long)
}
