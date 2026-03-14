package com.rekosuo.gymtracker.testutil

import com.rekosuo.gymtracker.data.local.dao.PerformanceDao
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake PerformanceDao for unit tests.
 */
class FakePerformanceDao : PerformanceDao {

    private val performances = MutableStateFlow<List<PerformanceEntity>>(emptyList())
    private var nextId = 1L

    override fun getPerformancesForExercise(exerciseId: Long): Flow<List<PerformanceEntity>> {
        return performances.map { list ->
            list.filter { it.exerciseId == exerciseId }.sortedByDescending { it.date }
        }
    }

    override suspend fun getLatestPerformance(exerciseId: Long): PerformanceEntity? {
        return performances.value
            .filter { it.exerciseId == exerciseId }
            .maxByOrNull { it.date }
    }

    override suspend fun getPerformanceByDate(exerciseId: Long, date: Long): PerformanceEntity? {
        return performances.value.find { it.exerciseId == exerciseId && it.date == date }
    }

    override suspend fun getPerformanceById(id: Long): PerformanceEntity? {
        return performances.value.find { it.id == id }
    }

    override fun getPerformancesByDateRange(
        exerciseId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<PerformanceEntity>> {
        return performances.map { list ->
            list.filter {
                it.exerciseId == exerciseId && it.date in startDate..endDate
            }.sortedBy { it.date }
        }
    }

    override suspend fun insertPerformance(performance: PerformanceEntity): Long {
        val id = if (performance.id == 0L) nextId++ else performance.id
        val withId = performance.copy(id = id)
        performances.update { current -> current.filter { it.id != id } + withId }
        return id
    }

    override suspend fun updatePerformance(performance: PerformanceEntity) {
        performances.update { current ->
            current.map { if (it.id == performance.id) performance else it }
        }
    }

    override suspend fun deletePerformance(performance: PerformanceEntity) {
        performances.update { current -> current.filter { it.id != performance.id } }
    }
}