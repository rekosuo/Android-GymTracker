package com.rekosuo.gymtracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
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
 * Instrumented tests for GroupDao.
 *
 * GroupDao is the most complex DAO because it manages:
 * 1. The exercise_groups table (basic CRUD)
 * 2. The exercise_group_cross_ref junction table (many-to-many relationships)
 * 3. A @Transaction method (replaceGroupExercises) that atomically swaps
 *    all exercises in a group
 *
 * WHAT WE'RE TESTING:
 * - Basic group CRUD operations
 * - The many-to-many relationship via the junction table
 * - That exercises within a group come back in orderIndex order
 * - That the atomic replaceGroupExercises transaction works correctly
 * - That CASCADE delete removes cross-references when a group is deleted
 */
@RunWith(AndroidJUnit4::class)
class GroupDaoTest {

    private lateinit var database: GymDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymDatabase::class.java
        ).allowMainThreadQueries().build()

        groupDao = database.groupDao()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // -- Basic CRUD --

    @Test
    fun insertAndRetrieveGroup() = runTest {
        val group = ExerciseGroupEntity(name = "Push Day", createdAt = 1000L)
        val id = groupDao.insertGroup(group)

        val retrieved = groupDao.getGroupById(id)
        assertNotNull(retrieved)
        assertEquals("Push Day", retrieved!!.name)
    }

    @Test
    fun getAllGroups_sortedByName() = runTest {
        groupDao.insertGroup(ExerciseGroupEntity(name = "Pull Day", createdAt = 1000L))
        groupDao.insertGroup(ExerciseGroupEntity(name = "Arms", createdAt = 1000L))
        groupDao.insertGroup(ExerciseGroupEntity(name = "Legs", createdAt = 1000L))

        val groups = groupDao.getAllGroups().first()

        assertEquals(3, groups.size)
        assertEquals("Arms", groups[0].name)
        assertEquals("Legs", groups[1].name)
        assertEquals("Pull Day", groups[2].name)
    }

    @Test
    fun getFavoriteGroups_onlyReturnsFavorites() = runTest {
        groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", isFavorite = true, createdAt = 1000L)
        )
        groupDao.insertGroup(
            ExerciseGroupEntity(name = "Pull Day", isFavorite = false, createdAt = 1000L)
        )

        val favorites = groupDao.getFavoriteGroups().first()

        assertEquals(1, favorites.size)
        assertEquals("Push Day", favorites[0].name)
    }

    // -- Many-to-many relationship tests --

    @Test
    fun getOrderedExercisesForGroup_returnsInOrderIndexOrder() = runTest {
        // This tests the JOIN query that connects exercises to groups.
        // The key thing: exercises must come back sorted by orderIndex,
        // which represents the user's custom ordering within the group.

        // Set up: create exercises
        val benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
        val flyId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Chest Fly", createdAt = 1000L)
        )
        val dipId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Dips", createdAt = 1000L)
        )

        // Create a group
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", createdAt = 1000L)
        )

        // Link exercises to group with specific ordering:
        // Dips first (orderIndex=0), Bench second (1), Fly third (2)
        groupDao.insertExerciseGroupCrossRefs(
            listOf(
                ExerciseGroupCrossRef(exerciseId = dipId, groupId = groupId, orderIndex = 0),
                ExerciseGroupCrossRef(exerciseId = benchId, groupId = groupId, orderIndex = 1),
                ExerciseGroupCrossRef(exerciseId = flyId, groupId = groupId, orderIndex = 2)
            )
        )

        val exercises = groupDao.getOrderedExercisesForGroup(groupId)

        assertEquals(3, exercises.size)
        assertEquals("Dips", exercises[0].name)          // orderIndex 0
        assertEquals("Bench Press", exercises[1].name)   // orderIndex 1
        assertEquals("Chest Fly", exercises[2].name)     // orderIndex 2
    }

    @Test
    fun replaceGroupExercises_atomicallySwapsExercises() = runTest {
        // replaceGroupExercises is a @Transaction that deletes all existing
        // cross-refs and inserts new ones. We verify the old exercises are
        // gone and the new ones are in place.

        val benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
        val flyId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Chest Fly", createdAt = 1000L)
        )
        val squatId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat", createdAt = 1000L)
        )

        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", createdAt = 1000L)
        )

        // Initially: Bench + Fly
        groupDao.replaceGroupExercises(
            groupId,
            listOf(
                ExerciseGroupCrossRef(benchId, groupId, 0),
                ExerciseGroupCrossRef(flyId, groupId, 1)
            )
        )
        assertEquals(2, groupDao.getOrderedExercisesForGroup(groupId).size)

        // Replace with: Squat + Bench (new set, different order)
        groupDao.replaceGroupExercises(
            groupId,
            listOf(
                ExerciseGroupCrossRef(squatId, groupId, 0),
                ExerciseGroupCrossRef(benchId, groupId, 1)
            )
        )

        val exercises = groupDao.getOrderedExercisesForGroup(groupId)
        assertEquals(2, exercises.size)
        assertEquals("Squat", exercises[0].name)       // new first exercise
        assertEquals("Bench Press", exercises[1].name) // reordered
        // Fly is gone — it was replaced
    }

    @Test
    fun deleteGroup_cascadeDeletesCrossRefs() = runTest {
        // When a group is deleted, its cross-references should be automatically
        // removed by the CASCADE foreign key constraint. Without this, we'd have
        // orphaned cross-ref rows pointing to a deleted group.

        val benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", createdAt = 1000L)
        )
        groupDao.insertExerciseGroupCrossRefs(
            listOf(ExerciseGroupCrossRef(benchId, groupId, 0))
        )

        // Verify exercise is in group
        assertEquals(1, groupDao.getOrderedExercisesForGroup(groupId).size)

        // Delete the group
        val group = groupDao.getGroupById(groupId)!!
        groupDao.deleteGroup(group)

        // Group is gone
        assertNull(groupDao.getGroupById(groupId))
        // Cross-refs should be cascade-deleted too
        assertTrue(groupDao.getOrderedExercisesForGroup(groupId).isEmpty())
    }

    @Test
    fun deleteExercise_cascadeDeletesCrossRefs() = runTest {
        // The other direction: deleting an exercise should also remove its
        // cross-references from all groups it belongs to.

        val benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
        val flyId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Chest Fly", createdAt = 1000L)
        )
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", createdAt = 1000L)
        )
        groupDao.insertExerciseGroupCrossRefs(
            listOf(
                ExerciseGroupCrossRef(benchId, groupId, 0),
                ExerciseGroupCrossRef(flyId, groupId, 1)
            )
        )

        // Delete Bench Press
        val bench = exerciseDao.getExerciseById(benchId)!!
        exerciseDao.deleteExercise(bench)

        // Group should now only contain Chest Fly
        val remaining = groupDao.getOrderedExercisesForGroup(groupId)
        assertEquals(1, remaining.size)
        assertEquals("Chest Fly", remaining[0].name)
    }

    @Test
    fun getOrderedExercisesForGroup_emptyGroupReturnsEmpty() = runTest {
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Empty Group", createdAt = 1000L)
        )

        val exercises = groupDao.getOrderedExercisesForGroup(groupId)
        assertTrue(exercises.isEmpty())
    }
}