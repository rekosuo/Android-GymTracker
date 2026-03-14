package com.rekosuo.gymtracker.ui.group

import androidx.lifecycle.SavedStateHandle
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
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
 * Unit tests for GroupEditViewModel.
 *
 * This ViewModel manages the most stateful screen in the app. The user can:
 * - Name the group and toggle favorite
 * - Search through available exercises
 * - Select/deselect exercises to include in the group
 * - Reorder selected exercises (move up/down)
 * - Save: creates/updates the group AND its exercise relationships atomically
 *
 * WHAT WE'RE TESTING:
 * - Exercise toggle: selecting adds to list, deselecting removes
 * - Exercise reordering: swap logic with boundary checks (can't move first up,
 *   can't move last down)
 * - Search filtering: filters the available list while preserving selections
 * - Save validation and the two-step save (group + cross-refs)
 * - Editing existing group loads name, favorite, and selected exercises
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var exerciseDao: FakeExerciseDao
    private lateinit var groupDao: FakeGroupDao
    private lateinit var repository: ExerciseRepository

    // IDs assigned by the fake DAO (auto-increment from 1)
    private var benchId = 0L
    private var squatId = 0L
    private var deadliftId = 0L

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)

        exerciseDao = FakeExerciseDao()
        groupDao = FakeGroupDao(exerciseDao)
        repository = ExerciseRepository(exerciseDao, groupDao)

        // Pre-populate exercises that will appear in the "available exercises" list
        benchId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press", createdAt = 1000L)
        )
        squatId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat", createdAt = 1000L)
        )
        deadliftId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Deadlift", createdAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(groupId: Long = 0L): GroupEditViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("groupId" to groupId))
        return GroupEditViewModel(repository, savedStateHandle)
    }

    // -- Initialization --

    @Test
    fun `new group - loads available exercises`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.availableExercises.size)
        assertTrue(state.selectedExercises.isEmpty())
        assertEquals("", state.groupName)
        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
    }

    // -- Name and favorite --

    @Test
    fun `NameChanged updates group name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.NameChanged("Push Day"))

        assertEquals("Push Day", viewModel.state.value.groupName)
    }

    @Test
    fun `FavoriteToggled updates favorite state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.FavoriteToggled(true))

        assertTrue(viewModel.state.value.isFavorite)
    }

    // -- Exercise selection --

    @Test
    fun `ExerciseToggled - selecting adds exercise to selected list`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))

        val selected = viewModel.state.value.selectedExercises
        assertEquals(1, selected.size)
        assertEquals("Bench Press", selected[0].name)
    }

    @Test
    fun `ExerciseToggled - deselecting removes exercise from selected list`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Select then deselect
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        assertEquals(1, viewModel.state.value.selectedExercises.size)

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        assertTrue(viewModel.state.value.selectedExercises.isEmpty())
    }

    @Test
    fun `ExerciseToggled - selecting multiple exercises preserves order`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(deadliftId))

        val selected = viewModel.state.value.selectedExercises
        assertEquals(3, selected.size)
        // Order should match selection order, not alphabetical
        assertEquals("Squat", selected[0].name)
        assertEquals("Bench Press", selected[1].name)
        assertEquals("Deadlift", selected[2].name)
    }

    @Test
    fun `ExerciseToggled - nonexistent exercise id is a no-op`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(999L))

        assertTrue(viewModel.state.value.selectedExercises.isEmpty())
    }

    // -- Exercise reordering --

    @Test
    fun `ExerciseMoved UP - swaps with exercise above`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Select three exercises: Bench, Squat, Deadlift
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(deadliftId))

        // Move Squat (index 1) up to index 0
        viewModel.onEvent(GroupEditEvent.ExerciseMoved(squatId, MoveDirection.UP))

        val selected = viewModel.state.value.selectedExercises
        assertEquals("Squat", selected[0].name)       // moved up
        assertEquals("Bench Press", selected[1].name)  // swapped down
        assertEquals("Deadlift", selected[2].name)     // unchanged
    }

    @Test
    fun `ExerciseMoved DOWN - swaps with exercise below`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(deadliftId))

        // Move Bench (index 0) down to index 1
        viewModel.onEvent(GroupEditEvent.ExerciseMoved(benchId, MoveDirection.DOWN))

        val selected = viewModel.state.value.selectedExercises
        assertEquals("Squat", selected[0].name)
        assertEquals("Bench Press", selected[1].name)
        assertEquals("Deadlift", selected[2].name)
    }

    @Test
    fun `ExerciseMoved UP at index 0 - no-op`() = runTest {
        // Moving the first element up should not crash or change anything.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))

        viewModel.onEvent(GroupEditEvent.ExerciseMoved(benchId, MoveDirection.UP))

        val selected = viewModel.state.value.selectedExercises
        assertEquals("Bench Press", selected[0].name)  // unchanged
        assertEquals("Squat", selected[1].name)
    }

    @Test
    fun `ExerciseMoved DOWN at last index - no-op`() = runTest {
        // Moving the last element down should not crash or change anything.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))

        viewModel.onEvent(GroupEditEvent.ExerciseMoved(squatId, MoveDirection.DOWN))

        val selected = viewModel.state.value.selectedExercises
        assertEquals("Bench Press", selected[0].name)
        assertEquals("Squat", selected[1].name)  // unchanged
    }

    @Test
    fun `ExerciseMoved with unknown exercise id - no-op`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))

        // Move an exercise that isn't in the selected list
        viewModel.onEvent(GroupEditEvent.ExerciseMoved(999L, MoveDirection.UP))

        assertEquals(1, viewModel.state.value.selectedExercises.size)
    }

    // -- Search filtering --

    @Test
    fun `SearchQueryChanged filters available exercises`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.SearchQueryChanged("bench"))

        val state = viewModel.state.value
        assertEquals("bench", state.searchQuery)
        assertEquals(1, state.availableExercises.size)
        assertEquals("Bench Press", state.availableExercises[0].name)
    }

    @Test
    fun `SearchQueryChanged is case insensitive`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.SearchQueryChanged("SQUAT"))

        assertEquals(1, viewModel.state.value.availableExercises.size)
        assertEquals("Squat", viewModel.state.value.availableExercises[0].name)
    }

    @Test
    fun `SearchQueryChanged with blank query shows all exercises`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Filter down
        viewModel.onEvent(GroupEditEvent.SearchQueryChanged("bench"))
        assertEquals(1, viewModel.state.value.availableExercises.size)

        // Clear filter
        viewModel.onEvent(GroupEditEvent.SearchQueryChanged(""))
        assertEquals(3, viewModel.state.value.availableExercises.size)
    }

    @Test
    fun `SearchQueryChanged does not affect selected exercises`() = runTest {
        // Searching should only filter the "available" list, not remove
        // anything from the "selected" list.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))

        // Search for something that doesn't match "Bench Press"
        viewModel.onEvent(GroupEditEvent.SearchQueryChanged("squat"))

        val state = viewModel.state.value
        assertEquals(1, state.availableExercises.size)         // filtered
        assertEquals(2, state.selectedExercises.size)          // unaffected
        assertEquals("Bench Press", state.selectedExercises[0].name)
    }

    // -- Saving --

    @Test
    fun `SaveGroup with blank name shows error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaved)
        assertEquals("Group name cannot be empty", viewModel.state.value.error)
    }

    @Test
    fun `SaveGroup creates group and saves exercise relationships`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.NameChanged("Push Day"))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))
        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)

        // Verify the group was created in the database
        val group = groupDao.getGroupById(1L)
        assertEquals("Push Day", group?.name)

        // Verify exercises were linked in the right order
        val exercises = groupDao.getOrderedExercisesForGroup(1L)
        assertEquals(2, exercises.size)
        assertEquals("Bench Press", exercises[0].name)
        assertEquals("Squat", exercises[1].name)
    }

    @Test
    fun `SaveGroup trims whitespace from name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.NameChanged("  Push Day  "))
        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()

        val group = groupDao.getGroupById(1L)
        assertEquals("Push Day", group?.name)
    }

    @Test
    fun `SaveGroup with no exercises creates group with empty exercise list`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.NameChanged("Empty Group"))
        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)
        val exercises = groupDao.getOrderedExercisesForGroup(1L)
        assertTrue(exercises.isEmpty())
    }

    @Test
    fun `clearError resets error to null`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()
        assertEquals("Group name cannot be empty", viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    // -- Editing existing group --

    @Test
    fun `editing existing group loads name and selected exercises`() = runTest {
        // Pre-populate a group with exercises in the fake database
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Leg Day", isFavorite = true, createdAt = 1000L)
        )
        groupDao.insertExerciseGroupCrossRefs(
            listOf(
                ExerciseGroupCrossRef(squatId, groupId, orderIndex = 0),
                ExerciseGroupCrossRef(deadliftId, groupId, orderIndex = 1)
            )
        )

        val viewModel = createViewModel(groupId = groupId)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Leg Day", state.groupName)
        assertTrue(state.isFavorite)
        assertTrue(state.isEditing)
        assertEquals(2, state.selectedExercises.size)
        assertEquals("Squat", state.selectedExercises[0].name)
        assertEquals("Deadlift", state.selectedExercises[1].name)
    }

    @Test
    fun `saving existing group updates instead of inserting`() = runTest {
        val groupId = groupDao.insertGroup(
            ExerciseGroupEntity(name = "Leg Day", createdAt = 1000L)
        )
        groupDao.insertExerciseGroupCrossRefs(
            listOf(ExerciseGroupCrossRef(squatId, groupId, orderIndex = 0))
        )

        val viewModel = createViewModel(groupId = groupId)
        advanceUntilIdle()

        // Change the name and exercises
        viewModel.onEvent(GroupEditEvent.NameChanged("Upper Body"))
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(squatId))    // deselect squat
        viewModel.onEvent(GroupEditEvent.ExerciseToggled(benchId))    // select bench
        viewModel.onEvent(GroupEditEvent.SaveGroup)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)

        // Same ID, updated name
        val group = groupDao.getGroupById(groupId)
        assertEquals("Upper Body", group?.name)

        // Exercises replaced: squat gone, bench added
        val exercises = groupDao.getOrderedExercisesForGroup(groupId)
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises[0].name)
    }
}