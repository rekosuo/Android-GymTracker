package com.rekosuo.gymtracker.ui.performance

import androidx.lifecycle.SavedStateHandle
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.testutil.FakeExerciseDao
import com.rekosuo.gymtracker.testutil.FakeGroupDao
import com.rekosuo.gymtracker.testutil.FakePerformanceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.rekosuo.gymtracker.data.local.entity.SetEntry as EntitySetEntry

/**
 * Unit tests for PerformanceEntryViewModel.
 *
 * This is the most complex ViewModel in the app. It manages:
 * - A matrix grid of weight rows and rep columns
 * - Two synchronized representations: flat sets (for storage) and weight rows (for UI)
 * - Adding/removing rows and reps
 * - Saving with validation (filter out empty reps, renumber orders)
 *
 * WHAT WE'RE TESTING:
 * - New performance starts with one empty weight row
 * - Adding rows, adding reps, updating values
 * - Deleting rows and individual reps
 * - Save validation: empty reps are filtered out, orders are renumbered
 * - Editing existing performance loads data correctly
 * - Delete performance marks as saved (navigates back)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var exerciseDao: FakeExerciseDao
    private lateinit var performanceDao: FakePerformanceDao
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var performanceRepository: PerformanceRepository

    private val testExerciseId = 1L

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)

        exerciseDao = FakeExerciseDao()
        val groupDao = FakeGroupDao(exerciseDao)
        performanceDao = FakePerformanceDao()
        exerciseRepository = ExerciseRepository(exerciseDao, groupDao)
        performanceRepository = PerformanceRepository(performanceDao)

        // Pre-create an exercise that all tests will reference
        exerciseDao.insertExercise(
            ExerciseEntity(id = testExerciseId, name = "Bench Press", createdAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        exerciseId: Long = testExerciseId,
        performanceId: Long = 0L
    ): PerformanceEntryViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "exerciseId" to exerciseId,
                "performanceId" to performanceId
            )
        )
        return PerformanceEntryViewModel(
            exerciseRepository,
            performanceRepository,
            savedStateHandle
        )
    }

    // -- New performance initialization --

    @Test
    fun `new performance starts with one empty weight row`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Bench Press", state.exerciseName)
        assertEquals(1, state.weightRows.size)
        assertEquals(0f, state.weightRows[0].weight)
        assertTrue(state.weightRows[0].reps.isEmpty())
        assertFalse(state.isLoading)
    }

    // -- Adding rows and reps --

    @Test
    fun `AddWeightRow adds a row with the same weight as the last row`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Update the first row's weight to 20kg
        viewModel.onEvent(PerformanceEntryEvent.UpdateWeight(rowIndex = 0, weight = 20f))
        // Add a second row
        viewModel.onEvent(PerformanceEntryEvent.AddWeightRow)

        val rows = viewModel.state.value.weightRows
        assertEquals(2, rows.size)
        assertEquals(20f, rows[1].weight) // inherited from last row
        assertTrue(rows[1].reps.isEmpty())
    }

    @Test
    fun `AddRepToRow adds a rep with value 0`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex = 0))

        val reps = viewModel.state.value.weightRows[0].reps
        assertEquals(1, reps.size)
        assertEquals(0, reps[0]) // default value for user to edit
    }

    @Test
    fun `adding multiple reps builds up the row`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex = 0))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex = 0))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex = 0))

        assertEquals(3, viewModel.state.value.weightRows[0].reps.size)
    }

    // -- Updating values --

    @Test
    fun `UpdateWeight changes weight for a specific row`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.UpdateWeight(rowIndex = 0, weight = 60f))

        assertEquals(60f, viewModel.state.value.weightRows[0].weight)
    }

    @Test
    fun `UpdateRep changes a specific rep value`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Add a rep and then update it
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex = 0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(rowIndex = 0, repIndex = 0, reps = 12))

        assertEquals(12, viewModel.state.value.weightRows[0].reps[0])
    }

    @Test
    fun `weightRows and sets stay in sync after mutations`() = runTest {
        // This is the key invariant: every time we modify weightRows,
        // the flat sets list must be recalculated to match.
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Build a workout: 20kg x 10, 20kg x 8
        viewModel.onEvent(PerformanceEntryEvent.UpdateWeight(0, 20f))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 0, 10))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 1, 8))

        val state = viewModel.state.value
        assertEquals(2, state.sets.size)
        assertEquals(20f, state.sets[0].weight)
        assertEquals(10, state.sets[0].reps)
        assertEquals(0, state.sets[0].order)
        assertEquals(20f, state.sets[1].weight)
        assertEquals(8, state.sets[1].reps)
        assertEquals(1, state.sets[1].order)
    }

    // -- Deleting --

    @Test
    fun `DeleteRow removes the row and recalculates sets`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Create two rows
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 0, 10))
        viewModel.onEvent(PerformanceEntryEvent.AddWeightRow)
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(1))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(1, 0, 8))

        // Delete the first row
        viewModel.onEvent(PerformanceEntryEvent.DeleteRow(rowIndex = 0))

        val state = viewModel.state.value
        assertEquals(1, state.weightRows.size)
        assertEquals(1, state.sets.size)
        assertEquals(0, state.sets[0].order) // order recalculated from 0
    }

    @Test
    fun `DeleteRep removes one rep from a row`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Add two reps
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 0, 10))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 1, 8))

        // Delete the first rep
        viewModel.onEvent(PerformanceEntryEvent.DeleteRep(rowIndex = 0, repIndex = 0))

        val reps = viewModel.state.value.weightRows[0].reps
        assertEquals(1, reps.size)
        assertEquals(8, reps[0]) // the second rep remains
    }

    // -- Notes --

    @Test
    fun `UpdateNotes changes the notes field`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.UpdateNotes("Felt strong today"))

        assertEquals("Felt strong today", viewModel.state.value.notes)
    }

    // -- Saving --

    @Test
    fun `SavePerformance filters out zero-rep sets`() = runTest {
        // The ViewModel should only save sets where reps > 0.
        // This prevents saving placeholder/empty sets.
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Row with one real rep (10) and one empty rep (0)
        viewModel.onEvent(PerformanceEntryEvent.UpdateWeight(0, 20f))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 0, 10))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        // Don't update second rep — it stays at 0

        viewModel.onEvent(PerformanceEntryEvent.SavePerformance)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)

        // Verify only the non-zero rep was saved
        val saved = performanceDao.getLatestPerformance(testExerciseId)
        assertEquals(1, saved!!.sets.size)
        assertEquals(10, saved.sets[0].reps)
    }

    @Test
    fun `SavePerformance with only zero reps shows error`() = runTest {
        // If ALL reps are 0, there's nothing to save.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        // Rep stays at 0

        viewModel.onEvent(PerformanceEntryEvent.SavePerformance)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaved)
        assertEquals("Add at least one set with reps", viewModel.state.value.error)
    }

    @Test
    fun `SavePerformance renumbers set orders to be continuous`() = runTest {
        // After filtering out zero-rep sets, the remaining sets must have
        // continuous order values (0, 1, 2, ...) with no gaps.
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Create setup where filtering will leave gaps
        viewModel.onEvent(PerformanceEntryEvent.UpdateWeight(0, 20f))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(0, 0, 10))
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(0))
        // Leave second rep at 0 (will be filtered)
        viewModel.onEvent(PerformanceEntryEvent.AddWeightRow)
        viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(1))
        viewModel.onEvent(PerformanceEntryEvent.UpdateRep(1, 0, 8))

        viewModel.onEvent(PerformanceEntryEvent.SavePerformance)
        advanceUntilIdle()

        val saved = performanceDao.getLatestPerformance(testExerciseId)
        assertEquals(2, saved!!.sets.size)
        assertEquals(0, saved.sets[0].order)  // renumbered
        assertEquals(1, saved.sets[1].order)  // continuous
    }

    // -- Editing existing performance --

    @Test
    fun `editing existing performance loads sets and rows`() = runTest {
        // Pre-populate a performance in the fake DAO
        val perfId = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = testExerciseId,
                date = 1000L,
                sets = listOf(
                    EntitySetEntry(weight = 20f, reps = 10, order = 0),
                    EntitySetEntry(weight = 20f, reps = 10, order = 1),
                    EntitySetEntry(weight = 25f, reps = 8, order = 2)
                ),
                notes = "Good session"
            )
        )

        val viewModel = createViewModel(performanceId = perfId)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Good session", state.notes)
        assertEquals(3, state.sets.size)
        // Sets should be grouped into 2 weight rows: [20kg x 10, 10] and [25kg x 8]
        assertEquals(2, state.weightRows.size)
        assertEquals(20f, state.weightRows[0].weight)
        assertEquals(listOf(10, 10), state.weightRows[0].reps)
        assertEquals(25f, state.weightRows[1].weight)
        assertEquals(listOf(8), state.weightRows[1].reps)
    }

    // -- Deleting performance --

    @Test
    fun `DeletePerformance on new performance just navigates back`() = runTest {
        // If performanceId is 0 (new, unsaved), delete should just set isSaved=true
        // to trigger navigation back. No database call needed.
        val viewModel = createViewModel(performanceId = 0L)
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.DeletePerformance)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)
    }

    @Test
    fun `DeletePerformance removes from database and navigates back`() = runTest {
        val perfId = performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = testExerciseId,
                date = 1000L,
                sets = listOf(EntitySetEntry(20f, 10, 0))
            )
        )

        val viewModel = createViewModel(performanceId = perfId)
        advanceUntilIdle()

        viewModel.onEvent(PerformanceEntryEvent.DeletePerformance)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)
        // Verify it's actually gone from the database
        val deleted = performanceDao.getPerformanceById(perfId)
        assertEquals(null, deleted)
    }
}