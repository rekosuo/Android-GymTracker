package com.rekosuo.gymtracker.ui.exercise

import androidx.lifecycle.SavedStateHandle
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.testutil.FakeExerciseDao
import com.rekosuo.gymtracker.testutil.FakeGroupDao
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ExerciseEditViewModel.
 *
 * These tests verify the MVI event→state flow:
 * - User types a name → state.exerciseName updates
 * - User toggles favorite → state.isFavorite updates
 * - User saves → validation runs, then insert/update is called
 * - Loading existing exercise → state is populated from repository
 *
 *  Real repository, fake DAOs — we use the actual ExerciseRepository class
 *  (not a mock) with fake DAO implementations. This tests the full chain:
 *  ViewModel → Repository → (fake) DAO, catching bugs in the repository's
 *  entity↔domain mapping too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var exerciseDao: FakeExerciseDao
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        // Replace Dispatchers.Main so viewModelScope works in tests
        Dispatchers.setMain(testDispatcher)

        exerciseDao = FakeExerciseDao()
        val groupDao = FakeGroupDao(exerciseDao)
        repository = ExerciseRepository(exerciseDao, groupDao)
    }

    @After
    fun tearDown() {
        // Always reset Main dispatcher to avoid leaking into other tests
        Dispatchers.resetMain()
    }

    /**
     * Helper to create a ViewModel with the given exerciseId in SavedStateHandle.
     * exerciseId=0 means "create new", any other value means "edit existing".
     */
    private fun createViewModel(exerciseId: Long = 0L): ExerciseEditViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to exerciseId))
        return ExerciseEditViewModel(repository, savedStateHandle)
    }

    // -- Creating a new exercise --

    @Test
    fun `new exercise - initial state is empty`() = runTest {
        val viewModel = createViewModel(exerciseId = 0L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.exerciseName)
        assertFalse(state.isFavorite)
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertNull(state.error)
    }

    @Test
    fun `NameChanged event updates exerciseName in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ExerciseEditEvent.NameChanged("Bench Press"))

        assertEquals("Bench Press", viewModel.state.value.exerciseName)
    }

    @Test
    fun `FavoriteToggled event updates isFavorite in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ExerciseEditEvent.FavoriteToggled(true))

        assertTrue(viewModel.state.value.isFavorite)
    }

    @Test
    fun `SaveExercise with valid name sets isSaved true`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ExerciseEditEvent.NameChanged("Bench Press"))
        viewModel.onEvent(ExerciseEditEvent.SaveExercise)
        advanceUntilIdle() // Let the save coroutine complete

        assertTrue(viewModel.state.value.isSaved)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `SaveExercise with blank name sets error`() = runTest {
        // Validation: the ViewModel should reject empty names without calling
        // the repository at all.
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Don't set a name — leave it blank
        viewModel.onEvent(ExerciseEditEvent.SaveExercise)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaved)
        assertEquals("Exercise name cannot be empty", viewModel.state.value.error)
    }

    @Test
    fun `SaveExercise trims whitespace from name`() = runTest {
        // The ViewModel should trim the name before saving to avoid
        // exercises named "  Bench Press  " cluttering the database.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ExerciseEditEvent.NameChanged("  Bench Press  "))
        viewModel.onEvent(ExerciseEditEvent.SaveExercise)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)

        // Verify the saved exercise has a trimmed name
        val saved = exerciseDao.getExerciseById(1L)
        assertEquals("Bench Press", saved?.name)
    }

    @Test
    fun `clearError resets error to null`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger an error
        viewModel.onEvent(ExerciseEditEvent.SaveExercise)
        advanceUntilIdle()
        assertEquals("Exercise name cannot be empty", viewModel.state.value.error)

        // Clear it
        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    // -- Editing an existing exercise --

    @Test
    fun `editing existing exercise loads its data into state`() = runTest {
        // Pre-populate the fake DAO with an exercise
        exerciseDao.insertExercise(
            ExerciseEntity(id = 5L, name = "Squat", isFavorite = true, createdAt = 1000L)
        )

        val viewModel = createViewModel(exerciseId = 5L)
        advanceUntilIdle() // Let loadExercise() complete

        val state = viewModel.state.value
        assertEquals("Squat", state.exerciseName)
        assertTrue(state.isFavorite)
        assertFalse(state.isLoading)
    }

    @Test
    fun `editing nonexistent exercise shows error`() = runTest {
        // If the exercise ID doesn't exist in the database (maybe it was deleted),
        // the ViewModel should show an error instead of a blank form.
        val viewModel = createViewModel(exerciseId = 999L)
        advanceUntilIdle()

        assertEquals("Exercise not found", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `saving existing exercise calls update not insert`() = runTest {
        // When editing (exerciseId != 0), the ViewModel should call updateExercise,
        // not insertExercise. We verify by checking the exercise still has ID 5
        // (insert would assign a new ID).
        exerciseDao.insertExercise(
            ExerciseEntity(id = 5L, name = "Squat", isFavorite = false, createdAt = 1000L)
        )

        val viewModel = createViewModel(exerciseId = 5L)
        advanceUntilIdle()

        viewModel.onEvent(ExerciseEditEvent.NameChanged("Front Squat"))
        viewModel.onEvent(ExerciseEditEvent.SaveExercise)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)

        // The exercise at ID 5 should have the updated name
        val updated = exerciseDao.getExerciseById(5L)
        assertEquals("Front Squat", updated?.name)
    }
}