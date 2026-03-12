package com.rekosuo.gymtracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.local.entity.SetEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for PerformanceDao.
 *
 * PerformanceDao has a unique testing challenge: the `sets` field stores a
 * List<SetEntry> as JSON via Room TypeConverters. We need to verify that
 * the JSON serialization/deserialization roundtrip works correctly — this is
 * a common source of subtle bugs (data loss, corrupted workouts).
 *
 * WHAT WE'RE TESTING:
 * - CRUD operations for performance records
 * - JSON TypeConverter roundtrip (List<SetEntry> survives storage and retrieval)
 * - Query filtering: by exerciseId, by date, by date range
 * - Ordering: latest performance uses ORDER BY date DESC LIMIT 1
 */
@RunWith(AndroidJUnit4::class)
class PerformanceDaoTest {

    private lateinit var database: GymDatabase
    private lateinit var performanceDao: PerformanceDao
    private lateinit var exerciseDao: ExerciseDao

    private var exerciseId: Long = 0

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymDatabase::class.java
        ).allowMainThreadQueries().build()

        performanceDao = database.performanceDao()
        exerciseDao = database.exerciseDao()

        // Create an exercise to associate performances with.
        // PerformanceEntity.exerciseId is a logical foreign key.
        exerciseId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve_setsSerializationRoundtrip() = runTest {
        // This is the most important test: verify that List<SetEntry> survives
        // being serialized to JSON (on insert) and deserialized back (on read).
        // If the Converters class has a bug, this test catches it.
        val sets = listOf(
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 20f, reps = 10, order = 1),
            SetEntry(weight = 25f, reps = 8, order = 2)
        )
        val performance = PerformanceEntity(
            exerciseId = exerciseId,
            date = 1000L,
            sets = sets,
            notes = "Felt strong"
        )

        val id = performanceDao.insertPerformance(performance)
        val retrieved = performanceDao.getPerformanceById(id)

        assertNotNull(retrieved)
        assertEquals(3, retrieved!!.sets.size)
        assertEquals(20f, retrieved.sets[0].weight)
        assertEquals(10, retrieved.sets[0].reps)
        assertEquals(0, retrieved.sets[0].order)
        assertEquals(25f, retrieved.sets[2].weight)
        assertEquals(8, retrieved.sets[2].reps)
        assertEquals("Felt strong", retrieved.notes)
    }

    @Test
    fun getPerformancesForExercise_orderedByDateDesc() = runTest {
        // The query orders by date DESC — most recent first.
        // This is what the UI uses to show workout history.
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L)
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 3000L)
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 2000L)
        )

        val performances = performanceDao.getPerformancesForExercise(exerciseId).first()

        assertEquals(3, performances.size)
        assertEquals(3000L, performances[0].date) // most recent first
        assertEquals(2000L, performances[1].date)
        assertEquals(1000L, performances[2].date)
    }

    @Test
    fun getPerformancesForExercise_onlyReturnsMatchingExercise() = runTest {
        // Verify that performances for a different exercise don't leak in.
        val otherExerciseId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat", createdAt = 1000L)
        )

        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L)
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = otherExerciseId, date = 2000L)
        )

        val benchPerformances = performanceDao.getPerformancesForExercise(exerciseId).first()
        assertEquals(1, benchPerformances.size)
        assertEquals(exerciseId, benchPerformances[0].exerciseId)
    }

    @Test
    fun getLatestPerformance_returnsMostRecent() = runTest {
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L, notes = "old")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 3000L, notes = "newest")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 2000L, notes = "middle")
        )

        val latest = performanceDao.getLatestPerformance(exerciseId)

        assertNotNull(latest)
        assertEquals("newest", latest!!.notes)
        assertEquals(3000L, latest.date)
    }

    @Test
    fun getLatestPerformance_returnsNullWhenNoneExist() = runTest {
        val result = performanceDao.getLatestPerformance(exerciseId)
        assertNull(result)
    }

    @Test
    fun getPerformanceByDate_exactMatch() = runTest {
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L, notes = "target")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 2000L, notes = "other")
        )

        val result = performanceDao.getPerformanceByDate(exerciseId, 1000L)

        assertNotNull(result)
        assertEquals("target", result!!.notes)
    }

    @Test
    fun getPerformanceByDate_returnsNullWhenNoMatch() = runTest {
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L)
        )

        val result = performanceDao.getPerformanceByDate(exerciseId, 9999L)
        assertNull(result)
    }

    @Test
    fun getPerformancesByDateRange_filtersCorrectly() = runTest {
        // BETWEEN is inclusive on both ends in SQL
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L, notes = "before")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 2000L, notes = "in range start")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 3000L, notes = "in range middle")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 4000L, notes = "in range end")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 5000L, notes = "after")
        )

        val rangeResults = performanceDao.getPerformancesByDateRange(
            exerciseId, startDate = 2000L, endDate = 4000L
        ).first()

        assertEquals(3, rangeResults.size)
        // ORDER BY date ASC
        assertEquals(2000L, rangeResults[0].date)
        assertEquals(3000L, rangeResults[1].date)
        assertEquals(4000L, rangeResults[2].date)
    }

    @Test
    fun updatePerformance_modifiesExistingRecord() = runTest {
        val id = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = exerciseId,
                date = 1000L,
                sets = listOf(SetEntry(20f, 10, 0)),
                notes = "original"
            )
        )

        val updated = PerformanceEntity(
            id = id,
            exerciseId = exerciseId,
            date = 1000L,
            sets = listOf(SetEntry(25f, 8, 0), SetEntry(25f, 6, 1)),
            notes = "updated"
        )
        performanceDao.updatePerformance(updated)

        val result = performanceDao.getPerformanceById(id)
        assertEquals("updated", result!!.notes)
        assertEquals(2, result.sets.size)
        assertEquals(25f, result.sets[0].weight)
    }

    @Test
    fun deletePerformance_removesRecord() = runTest {
        val id = performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L)
        )

        val toDelete = performanceDao.getPerformanceById(id)!!
        performanceDao.deletePerformance(toDelete)

        assertNull(performanceDao.getPerformanceById(id))
    }

    @Test
    fun insertPerformance_withEmptySets() = runTest {
        // Edge case: a performance with no sets should still store and retrieve
        // correctly (the JSON converter should handle empty lists).
        val id = performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = exerciseId, date = 1000L, sets = emptyList())
        )

        val result = performanceDao.getPerformanceById(id)
        assertNotNull(result)
        assertTrue(result!!.sets.isEmpty())
    }
}