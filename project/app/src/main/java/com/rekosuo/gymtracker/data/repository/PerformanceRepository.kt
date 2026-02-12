package com.rekosuo.gymtracker.data.repository

import com.rekosuo.gymtracker.data.local.dao.PerformanceDao
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.local.entity.SetEntry as EntitySetEntry
import com.rekosuo.gymtracker.domain.model.Performance
import com.rekosuo.gymtracker.domain.model.SetEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for performance data.
 * Converts between Entity and Domain models.
 * Provides clean API to ViewModels.
 */
@Singleton
class PerformanceRepository @Inject constructor(
    private val performanceDao: PerformanceDao
) {

    // Get all performances for a specific exercise
    fun getPerformancesForExercise(exerciseId: Long): Flow<List<Performance>> {
        return performanceDao.getPerformancesForExercise(exerciseId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Get performance by ID
    suspend fun getPerformanceById(id: Long): Performance? {
        return performanceDao.getPerformanceById(id)?.toDomain()
    }

    // Get latest performance for an exercise
    suspend fun getLatestPerformance(exerciseId: Long): Performance? {
        return performanceDao.getLatestPerformance(exerciseId)?.toDomain()
    }

    // Get performance for a specific date
    suspend fun getPerformanceByDate(exerciseId: Long, date: Long): Performance? {
        return performanceDao.getPerformanceByDate(exerciseId, date)?.toDomain()
    }

    // Get performances within a date range
    fun getPerformancesByDateRange(
        exerciseId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<Performance>> {
        return performanceDao.getPerformancesByDateRange(exerciseId, startDate, endDate)
            .map { entities -> entities.map { it.toDomain() } }
    }

    // Insert new performance
    suspend fun insertPerformance(performance: Performance): Long {
        return performanceDao.insertPerformance(performance.toEntity())
    }

    // Update existing performance
    suspend fun updatePerformance(performance: Performance) {
        performanceDao.updatePerformance(performance.toEntity())
    }

    // Delete performance
    suspend fun deletePerformance(performance: Performance) {
        performanceDao.deletePerformance(performance.toEntity())
    }

    // Delete all performances for an exercise
    suspend fun deleteAllPerformancesForExercise(exerciseId: Long) {
        performanceDao.deleteAllPerformancesForExercise(exerciseId)
    }

    // Mapper functions - Convert between Entity and Domain models
    private fun PerformanceEntity.toDomain() = Performance(
        id = id,
        exerciseId = exerciseId,
        date = date,
        sets = sets.map { it.toDomain() },
        notes = notes
    )

    private fun Performance.toEntity() = PerformanceEntity(
        id = id,
        exerciseId = exerciseId,
        date = date,
        sets = sets.map { it.toEntity() },
        notes = notes
    )

    // SetEntry converters
    private fun EntitySetEntry.toDomain() = SetEntry(
        weight = weight,
        reps = reps,
        order = order
    )

    private fun SetEntry.toEntity() = EntitySetEntry(
        weight = weight,
        reps = reps,
        order = order
    )
}