package com.rekosuo.gymtracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
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
 * Instrumented tests for ExerciseDao.
 *
 * These tests run on an Android device or emulator because Room needs an Android
 * Context to create the SQLite database. We use Room.inMemoryDatabaseBuilder() which
 * creates a temporary database in RAM — it's fast and automatically cleaned up.
 *
 * WHAT WE'RE TESTING:
 * - That Room-generated SQL implementations actually work correctly
 * - That queries return data in the right order (name ASC)
 * - That favorite filtering works
 * - That CRUD operations (Create, Read, Update, Delete) function properly
 * - That the targeted updateFavoriteStatus query modifies only the intended column
 */
@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: GymDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        // inMemoryDatabaseBuilder creates a database that lives only in RAM.
        // allowMainThreadQueries lets us run queries directly in tests without
        // needing a background thread — fine for testing, never do this in production.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymDatabase::class.java
        ).allowMainThreadQueries().build()

        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveExercise() = runTest {
        // Insert an exercise and verify we can retrieve it by ID.
        // This tests the most basic DAO operation.
        val exercise = ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        val id = exerciseDao.insertExercise(exercise)

        val retrieved = exerciseDao.getExerciseById(id)
        assertNotNull(retrieved)
        assertEquals("Bench Press", retrieved!!.name)
        assertEquals(id, retrieved.id)
    }

    @Test
    fun getAllExercises_returnsSortedByName() = runTest {
        // The DAO sorts by name ASC. We insert in random order and verify
        // they come back alphabetically.
        exerciseDao.insertExercise(ExerciseEntity(name = "Squat", createdAt = 1000L))
        exerciseDao.insertExercise(ExerciseEntity(name = "Bench Press", createdAt = 1000L))
        exerciseDao.insertExercise(ExerciseEntity(name = "Deadlift", createdAt = 1000L))

        // .first() collects the first emission from the Flow
        val exercises = exerciseDao.getAllExercises().first()

        assertEquals(3, exercises.size)
        assertEquals("Bench Press", exercises[0].name)
        assertEquals("Deadlift", exercises[1].name)
        assertEquals("Squat", exercises[2].name)
    }

    @Test
    fun getFavoriteExercises_onlyReturnsFavorites() = runTest {
        // Verify the WHERE isFavorite = 1 filter works correctly.
        exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", isFavorite = true, createdAt = 1000L)
        )
        exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat", isFavorite = false, createdAt = 1000L)
        )
        exerciseDao.insertExercise(
            ExerciseEntity(name = "Deadlift", isFavorite = true, createdAt = 1000L)
        )

        val favorites = exerciseDao.getFavoriteExercises().first()

        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
        assertEquals("Bench Press", favorites[0].name)
        assertEquals("Deadlift", favorites[1].name)
    }

    @Test
    fun updateFavoriteStatus_onlyChangeFavoriteField() = runTest {
        // updateFavoriteStatus uses a targeted SQL UPDATE that only modifies the
        // isFavorite column. We verify it doesn't accidentally reset other fields.
        val id = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", isFavorite = false, createdAt = 1000L)
        )

        exerciseDao.updateFavoriteStatus(id, true)

        val updated = exerciseDao.getExerciseById(id)
        assertNotNull(updated)
        assertTrue(updated!!.isFavorite)
        assertEquals("Bench Press", updated.name)  // name unchanged
        assertEquals(1000L, updated.createdAt)      // createdAt unchanged
    }

    @Test
    fun deleteExercise_removesFromDatabase() = runTest {
        val exercise = ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        val id = exerciseDao.insertExercise(exercise)

        // We need the full entity with the generated ID to delete
        val inserted = exerciseDao.getExerciseById(id)!!
        exerciseDao.deleteExercise(inserted)

        val afterDelete = exerciseDao.getExerciseById(id)
        assertNull(afterDelete)
    }

    @Test
    fun updateExercise_modifiesExistingRecord() = runTest {
        val id = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )

        val toUpdate = ExerciseEntity(id = id, name = "Incline Bench Press", createdAt = 1000L)
        exerciseDao.updateExercise(toUpdate)

        val updated = exerciseDao.getExerciseById(id)
        assertEquals("Incline Bench Press", updated!!.name)
    }

    @Test
    fun getExerciseById_returnsNullForNonexistentId() = runTest {
        // Verify the DAO handles missing data gracefully (returns null, not crash).
        val result = exerciseDao.getExerciseById(999L)
        assertNull(result)
    }
}