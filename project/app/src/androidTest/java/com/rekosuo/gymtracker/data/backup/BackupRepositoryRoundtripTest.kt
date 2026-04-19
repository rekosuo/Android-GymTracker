package com.rekosuo.gymtracker.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.local.entity.SetEntry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Instrumented round-trip test for BackupRepository.
 *
 * Seed data is deliberately multi-entity and exercises ordering/multiplicity:
 *   - 3 exercises, at least one favorited
 *   - 2 groups, at least one favorited
 *   - 4 cross-refs covering multi-exercise-per-group and multi-group-per-exercise,
 *     with nonzero orderIndex values
 *   - 3 performances: one with return-to-previous-weight sets, one single-set,
 *     one at a different exerciseId
 *
 * A single-entity seed would pass even if ordering/FK/orderIndex were broken.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryRoundtripTest {

    private lateinit var sourceDb: GymDatabase
    private lateinit var sourceRepo: BackupRepository
    private lateinit var targetDb: GymDatabase
    private lateinit var targetRepo: BackupRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        sourceDb = Room.inMemoryDatabaseBuilder(ctx, GymDatabase::class.java)
            .allowMainThreadQueries().build()
        targetDb = Room.inMemoryDatabaseBuilder(ctx, GymDatabase::class.java)
            .allowMainThreadQueries().build()

        sourceRepo = BackupRepository(
            context = ctx,
            database = sourceDb,
            exerciseDao = sourceDb.exerciseDao(),
            groupDao = sourceDb.groupDao(),
            performanceDao = sourceDb.performanceDao(),
        )
        targetRepo = BackupRepository(
            context = ctx,
            database = targetDb,
            exerciseDao = targetDb.exerciseDao(),
            groupDao = targetDb.groupDao(),
            performanceDao = targetDb.performanceDao(),
        )
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    private suspend fun seedSourceDb(): SeedData {
        val exerciseDao = sourceDb.exerciseDao()
        val groupDao = sourceDb.groupDao()
        val performanceDao = sourceDb.performanceDao()

        val benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench", createdAt = 1000L, isFavorite = true)
        )
        val squatId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat", createdAt = 1001L, isFavorite = false)
        )
        val deadliftId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Deadlift", createdAt = 1002L, isFavorite = false)
        )

        val pushId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Push Day", createdAt = 2000L, isFavorite = true)
        )
        val fullBodyId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Full Body", createdAt = 2001L, isFavorite = false)
        )

        // Multi-exercise-per-group AND multi-group-per-exercise with nonzero orderIndex
        groupDao.insertExerciseGroupCrossRefs(
            listOf(
                ExerciseGroupCrossRef(exerciseId = benchId, groupId = pushId, orderIndex = 0),
                ExerciseGroupCrossRef(exerciseId = squatId, groupId = pushId, orderIndex = 1),
                ExerciseGroupCrossRef(exerciseId = benchId, groupId = fullBodyId, orderIndex = 0),
                ExerciseGroupCrossRef(exerciseId = deadliftId, groupId = fullBodyId, orderIndex = 1),
            )
        )

        val perfBench1 = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = benchId,
                date = 3000L,
                sets = listOf(
                    SetEntry(weight = 60f, reps = 10, order = 0),
                    SetEntry(weight = 65f, reps = 8, order = 1),
                    // Return to previous weight — meaningful for the WeightRow grouping
                    SetEntry(weight = 60f, reps = 7, order = 2),
                ),
                notes = "felt strong"
            )
        )
        val perfBench2 = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = benchId,
                date = 3100L,
                sets = listOf(SetEntry(weight = 62.5f, reps = 9, order = 0)),
                notes = ""
            )
        )
        val perfSquat = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = squatId,
                date = 3200L,
                sets = listOf(
                    SetEntry(weight = 100f, reps = 5, order = 0),
                    SetEntry(weight = 100f, reps = 5, order = 1),
                ),
                notes = "warmup skipped"
            )
        )

        return SeedData(
            benchId, squatId, deadliftId, pushId, fullBodyId,
            perfBench1, perfBench2, perfSquat
        )
    }

    private data class SeedData(
        val benchId: Long, val squatId: Long, val deadliftId: Long,
        val pushId: Long, val fullBodyId: Long,
        val perfBench1: Long, val perfBench2: Long, val perfSquat: Long,
    )

    @Test
    fun exportImport_roundTrip_preservesAllTables() = runTest {
        seedSourceDb()

        val buffer = ByteArrayOutputStream()
        sourceRepo.exportToStream(buffer).getOrThrow()

        targetRepo.importFromStream(ByteArrayInputStream(buffer.toByteArray())).getOrThrow()

        assertEquals(
            sourceDb.exerciseDao().getAllForBackup(),
            targetDb.exerciseDao().getAllForBackup()
        )
        assertEquals(
            sourceDb.groupDao().getAllGroupsForBackup(),
            targetDb.groupDao().getAllGroupsForBackup()
        )
        assertEquals(
            sourceDb.groupDao().getAllCrossRefsForBackup(),
            targetDb.groupDao().getAllCrossRefsForBackup()
        )
        assertEquals(
            sourceDb.performanceDao().getAllForBackup(),
            targetDb.performanceDao().getAllForBackup()
        )
    }

    @Test
    fun roundTrip_preservesCrossRefOrderIndex() = runTest {
        val seed = seedSourceDb()

        val buffer = ByteArrayOutputStream()
        sourceRepo.exportToStream(buffer).getOrThrow()
        targetRepo.importFromStream(ByteArrayInputStream(buffer.toByteArray())).getOrThrow()

        val imported = targetDb.groupDao().getAllCrossRefsForBackup()

        // Specifically assert the (exerciseId, groupId, orderIndex) tuples match the seed.
        val pushRefs = imported.filter { it.groupId == seed.pushId }.sortedBy { it.orderIndex }
        assertEquals(2, pushRefs.size)
        assertEquals(seed.benchId, pushRefs[0].exerciseId)
        assertEquals(0, pushRefs[0].orderIndex)
        assertEquals(seed.squatId, pushRefs[1].exerciseId)
        assertEquals(1, pushRefs[1].orderIndex)

        val fullBodyRefs = imported.filter { it.groupId == seed.fullBodyId }.sortedBy { it.orderIndex }
        assertEquals(2, fullBodyRefs.size)
        assertEquals(seed.benchId, fullBodyRefs[0].exerciseId)
        assertEquals(seed.deadliftId, fullBodyRefs[1].exerciseId)
        assertEquals(1, fullBodyRefs[1].orderIndex)
    }

    @Test
    fun roundTrip_preservesNestedSetEntryOrderAndWeights() = runTest {
        val seed = seedSourceDb()

        val buffer = ByteArrayOutputStream()
        sourceRepo.exportToStream(buffer).getOrThrow()
        targetRepo.importFromStream(ByteArrayInputStream(buffer.toByteArray())).getOrThrow()

        val benchPerf1 = targetDb.performanceDao().getPerformanceById(seed.perfBench1)!!
        // Return-to-previous-weight pattern must be preserved verbatim.
        assertEquals(listOf(0, 1, 2), benchPerf1.sets.map { it.order })
        assertEquals(listOf(60f, 65f, 60f), benchPerf1.sets.map { it.weight })
        assertEquals(listOf(10, 8, 7), benchPerf1.sets.map { it.reps })
    }

    @Test
    fun import_rejectsUnsupportedFormatVersion() = runTest {
        val bad = """
            {
              "formatVersion": 999,
              "dbSchemaVersion": $BACKUP_DB_SCHEMA_VERSION,
              "exportedAt": 0,
              "exercises": [], "groups": [],
              "groupExerciseCrossRefs": [], "performances": []
            }
        """.trimIndent().toByteArray()

        val result = targetRepo.importFromStream(ByteArrayInputStream(bad))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("format version"))
    }

    @Test
    fun import_rejectsUnsupportedDbSchemaVersion() = runTest {
        val bad = """
            {
              "formatVersion": $BACKUP_FORMAT_VERSION,
              "dbSchemaVersion": 999,
              "exportedAt": 0,
              "exercises": [], "groups": [],
              "groupExerciseCrossRefs": [], "performances": []
            }
        """.trimIndent().toByteArray()

        val result = targetRepo.importFromStream(ByteArrayInputStream(bad))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("schema"))
    }

    @Test
    fun import_malformedJson_fails() = runTest {
        val bad = "{ not valid json".toByteArray()

        val result = targetRepo.importFromStream(ByteArrayInputStream(bad))
        assertTrue(result.isFailure)
    }

    @Suppress("unused")
    private fun jsonInstance() = Json { prettyPrint = true; ignoreUnknownKeys = true }
}
