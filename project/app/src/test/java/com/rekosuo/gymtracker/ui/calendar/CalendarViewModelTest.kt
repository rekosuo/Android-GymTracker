package com.rekosuo.gymtracker.ui.calendar

import androidx.lifecycle.SavedStateHandle
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.local.entity.SetEntry
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var exerciseDao: FakeExerciseDao
    private lateinit var groupDao: FakeGroupDao
    private lateinit var performanceDao: FakePerformanceDao
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var performanceRepository: PerformanceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        exerciseDao = FakeExerciseDao()
        groupDao = FakeGroupDao(exerciseDao)
        performanceDao = FakePerformanceDao()
        exerciseRepository = ExerciseRepository(exerciseDao, groupDao)
        performanceRepository = PerformanceRepository(performanceDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        exerciseId: Long = 0L,
        groupId: Long = 0L
    ): CalendarViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "exerciseId" to exerciseId,
                "groupId" to groupId
            )
        )
        return CalendarViewModel(performanceRepository, exerciseRepository, savedStateHandle)
    }

    /** Convert a LocalDate to epoch millis at noon (avoids timezone edge cases). */
    private fun LocalDate.toEpochMillis(): Long {
        return this.atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // ── ALL mode ──────────────────────────────────────────────

    @Test
    fun `ALL mode - title is Calendar`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(CalendarMode.ALL, vm.state.value.mode)
        assertEquals("Calendar", vm.state.value.title)
    }

    @Test
    fun `ALL mode - highlights days with performances`() = runTest {
        val exerciseId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Squat")
        )
        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = exerciseId,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(100f, 5, 0))
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(today.dayOfMonth in vm.state.value.highlightedDays)
    }

    @Test
    fun `ALL mode - loads exercise names`() = runTest {
        val exerciseId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Bench Press")
        )
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = exerciseId,
                date = LocalDate.now().toEpochMillis()
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("Bench Press", vm.state.value.exerciseNames[exerciseId])
    }

    @Test
    fun `ALL mode - shows all exercises`() = runTest {
        val ex1 = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val ex2 = exerciseDao.insertExercise(ExerciseEntity(name = "Deadlift"))
        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex1, date = today.toEpochMillis())
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex2, date = today.toEpochMillis())
        )

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.exerciseNames.size)
        assertTrue(today.dayOfMonth in vm.state.value.highlightedDays)
    }

    @Test
    fun `ALL mode - no performances yields empty highlights`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.highlightedDays.isEmpty())
        assertTrue(vm.state.value.performancesByDate.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    // ── EXERCISE mode ─────────────────────────────────────────

    @Test
    fun `EXERCISE mode - title is exercise name`() = runTest {
        val exerciseId = exerciseDao.insertExercise(
            ExerciseEntity(name = "Overhead Press")
        )

        val vm = createViewModel(exerciseId = exerciseId)
        advanceUntilIdle()

        assertEquals(CalendarMode.EXERCISE, vm.state.value.mode)
        assertEquals("Overhead Press", vm.state.value.title)
    }

    @Test
    fun `EXERCISE mode - only shows performances for that exercise`() = runTest {
        val ex1 = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val ex2 = exerciseDao.insertExercise(ExerciseEntity(name = "Deadlift"))
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex1, date = today.toEpochMillis())
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex2, date = yesterday.toEpochMillis())
        )

        val vm = createViewModel(exerciseId = ex1)
        advanceUntilIdle()

        // Only today should be highlighted (ex1), not yesterday (ex2)
        assertTrue(today.dayOfMonth in vm.state.value.highlightedDays)
        assertFalse(yesterday.dayOfMonth in vm.state.value.highlightedDays)
    }

    @Test
    fun `EXERCISE mode - missing exercise falls back to Calendar title`() = runTest {
        val vm = createViewModel(exerciseId = 999L)
        advanceUntilIdle()

        assertEquals(CalendarMode.EXERCISE, vm.state.value.mode)
        assertEquals("Calendar", vm.state.value.title)
    }

    // ── GROUP mode ────────────────────────────────────────────

    @Test
    fun `GROUP mode - title is group name`() = runTest {
        val ex1 = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val groupId = groupDao.insertGroup(ExerciseGroupEntity(name = "Leg Day"))
        groupDao.replaceGroupExercises(
            groupId, listOf(
                ExerciseGroupCrossRef(exerciseId = ex1, groupId = groupId, orderIndex = 0)
            )
        )

        val vm = createViewModel(groupId = groupId)
        advanceUntilIdle()

        assertEquals(CalendarMode.GROUP, vm.state.value.mode)
        assertEquals("Leg Day", vm.state.value.title)
    }

    @Test
    fun `GROUP mode - only shows performances for exercises in the group`() = runTest {
        val ex1 = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val ex2 = exerciseDao.insertExercise(ExerciseEntity(name = "Bench Press"))
        val groupId = groupDao.insertGroup(ExerciseGroupEntity(name = "Legs"))
        groupDao.replaceGroupExercises(
            groupId, listOf(
                ExerciseGroupCrossRef(exerciseId = ex1, groupId = groupId, orderIndex = 0)
            )
        )

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex1, date = today.toEpochMillis())
        )
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex2, date = yesterday.toEpochMillis())
        )

        val vm = createViewModel(groupId = groupId)
        advanceUntilIdle()

        // Only ex1 (in group) should appear
        assertTrue(today.dayOfMonth in vm.state.value.highlightedDays)
        assertFalse(yesterday.dayOfMonth in vm.state.value.highlightedDays)
    }

    @Test
    fun `GROUP mode - missing group falls back to Calendar title`() = runTest {
        val vm = createViewModel(groupId = 999L)
        advanceUntilIdle()

        assertEquals(CalendarMode.GROUP, vm.state.value.mode)
        assertEquals("Calendar", vm.state.value.title)
    }

    @Test
    fun `GROUP mode - empty group yields no highlights`() = runTest {
        val groupId = groupDao.insertGroup(ExerciseGroupEntity(name = "Empty Group"))

        val vm = createViewModel(groupId = groupId)
        advanceUntilIdle()

        assertEquals("Empty Group", vm.state.value.title)
        assertTrue(vm.state.value.highlightedDays.isEmpty())
    }

    // ── Month navigation ──────────────────────────────────────

    @Test
    fun `month changed event updates currentMonth and reloads`() = runTest {
        val ex = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val lastMonth = YearMonth.now().minusMonths(1)
        val dateInLastMonth = lastMonth.atDay(15).toEpochMillis()
        performanceDao.insertPerformance(
            PerformanceEntity(exerciseId = ex, date = dateInLastMonth)
        )

        val vm = createViewModel()
        advanceUntilIdle()

        // Initially current month — no highlight for last month's data
        assertFalse(15 in vm.state.value.highlightedDays)

        vm.onEvent(CalendarScreenEvent.MonthChanged(lastMonth))
        advanceUntilIdle()

        assertEquals(lastMonth, vm.state.value.currentMonth)
        assertTrue(15 in vm.state.value.highlightedDays)
    }

    // ── Day selection & dialog ─────────────────────────────────

    @Test
    fun `selecting a highlighted day shows dialog with summaries`() = runTest {
        val exerciseId = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = exerciseId,
                date = today.toEpochMillis(),
                sets = listOf(
                    SetEntry(100f, 5, 0),
                    SetEntry(100f, 5, 1),
                    SetEntry(100f, 3, 2)
                ),
                notes = "Felt strong"
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(today.dayOfMonth))

        assertTrue(vm.state.value.showDayDialog)
        assertEquals(1, vm.state.value.selectedSummaries.size)

        val summary = vm.state.value.selectedSummaries.first()
        assertEquals("Squat", summary.exerciseName)
        assertEquals("Felt strong", summary.notes)
        assertEquals(1, summary.weightRows.size)
        assertEquals(100f, summary.weightRows[0].weight)
        assertEquals(listOf(5, 5, 3), summary.weightRows[0].sets)
    }

    @Test
    fun `selecting a day with no performances does not show dialog`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(1))

        assertFalse(vm.state.value.showDayDialog)
        assertTrue(vm.state.value.selectedSummaries.isEmpty())
    }

    @Test
    fun `dismiss dialog event hides dialog`() = runTest {
        val exerciseId = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = exerciseId,
                date = LocalDate.now().toEpochMillis()
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(LocalDate.now().dayOfMonth))
        assertTrue(vm.state.value.showDayDialog)

        vm.onEvent(CalendarScreenEvent.DismissDayDialog)
        assertFalse(vm.state.value.showDayDialog)
    }

    @Test
    fun `multiple performances on same day produce multiple summaries`() = runTest {
        val ex1 = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val ex2 = exerciseDao.insertExercise(ExerciseEntity(name = "Deadlift"))
        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = ex1,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(100f, 5, 0))
            )
        )
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = ex2,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(140f, 3, 0))
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(today.dayOfMonth))

        assertEquals(2, vm.state.value.selectedSummaries.size)
        val names = vm.state.value.selectedSummaries.map { it.exerciseName }.toSet()
        assertTrue("Squat" in names)
        assertTrue("Deadlift" in names)
    }

    @Test
    fun `EXERCISE mode - day summaries only include the filtered exercise`() = runTest {
        val squat = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val bench = exerciseDao.insertExercise(ExerciseEntity(name = "Bench Press"))
        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = squat,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(100f, 5, 0))
            )
        )
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = bench,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(80f, 8, 0))
            )
        )

        val vm = createViewModel(exerciseId = squat)
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(today.dayOfMonth))

        assertTrue(vm.state.value.showDayDialog)
        assertEquals(1, vm.state.value.selectedSummaries.size)
        assertEquals("Squat", vm.state.value.selectedSummaries[0].exerciseName)
    }

    @Test
    fun `GROUP mode - day summaries only include exercises in the group`() = runTest {
        val squat = exerciseDao.insertExercise(ExerciseEntity(name = "Squat"))
        val lunge = exerciseDao.insertExercise(ExerciseEntity(name = "Lunge"))
        val bench = exerciseDao.insertExercise(ExerciseEntity(name = "Bench Press"))
        val groupId = groupDao.insertGroup(ExerciseGroupEntity(name = "Leg Day"))
        groupDao.replaceGroupExercises(
            groupId, listOf(
                ExerciseGroupCrossRef(exerciseId = squat, groupId = groupId, orderIndex = 0),
                ExerciseGroupCrossRef(exerciseId = lunge, groupId = groupId, orderIndex = 1)
            )
        )

        val today = LocalDate.now()
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = squat,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(100f, 5, 0))
            )
        )
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = lunge,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(40f, 12, 0))
            )
        )
        performanceDao.insertPerformance(
            PerformanceEntity(
                exerciseId = bench,
                date = today.toEpochMillis(),
                sets = listOf(SetEntry(80f, 8, 0))
            )
        )

        val vm = createViewModel(groupId = groupId)
        advanceUntilIdle()

        vm.onEvent(CalendarScreenEvent.DaySelected(today.dayOfMonth))

        assertTrue(vm.state.value.showDayDialog)
        assertEquals(2, vm.state.value.selectedSummaries.size)
        val names = vm.state.value.selectedSummaries.map { it.exerciseName }.toSet()
        assertTrue("Squat" in names)
        assertTrue("Lunge" in names)
        assertFalse("Bench Press" in names)
    }

    // ── Loading state ─────────────────────────────────────────

    @Test
    fun `loading completes with isLoading false`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
    }
}