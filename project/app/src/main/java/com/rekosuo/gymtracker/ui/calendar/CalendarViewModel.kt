package com.rekosuo.gymtracker.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.domain.model.Performance
import com.rekosuo.gymtracker.domain.model.PerformanceSummary
import com.rekosuo.gymtracker.domain.model.toWeightRows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Calendar filters highlighted days based on whether the user
 * enters through a general menu, an exercise or a group.
 */
enum class CalendarMode {
    ALL,
    EXERCISE,
    GROUP
}

/**
 * UI state for the Calendar Screen.
 */
data class CalendarState(
    val mode: CalendarMode = CalendarMode.ALL,
    val title: String = "", // Exercise/group name or "Calendar"
    val currentMonth: YearMonth = YearMonth.now(),
    val highlightedDays: Set<Int> = emptySet(), // day-of-month values for current month
    val performancesByDate: Map<LocalDate, List<Performance>> = emptyMap(),
    val exerciseNames: Map<Long, String> = emptyMap(),
    val selectedSummaries: List<PerformanceSummary> = emptyList(),
    val showDayDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Events for the Calendar Screen.
 */
sealed class CalendarScreenEvent {
    data class MonthChanged(val month: YearMonth) : CalendarScreenEvent()
    data class DaySelected(val day: Int) : CalendarScreenEvent()
    object DismissDayDialog : CalendarScreenEvent()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val groupId: Long = savedStateHandle.get<Long>("groupId") ?: 0L

    private var groupExerciseIds: List<Long> = emptyList()

    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            initModeAndTitle()
            loadPerformances()
        }
    }

    private suspend fun initModeAndTitle() {
        when {
            exerciseId != 0L -> {
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                _state.update {
                    it.copy(
                        mode = CalendarMode.EXERCISE,
                        title = exercise?.name ?: "Calendar"
                    )
                }
            }
            groupId != 0L -> {
                val groupWithExercises = exerciseRepository.getGroupWithExercises(groupId)
                groupExerciseIds = groupWithExercises?.exercises?.map { it.id } ?: emptyList()
                _state.update {
                    it.copy(
                        mode = CalendarMode.GROUP,
                        title = groupWithExercises?.group?.name ?: "Calendar"
                    )
                }
            }
            else -> {
                _state.update { it.copy(title = "Calendar") }
            }
        }
    }

    fun onEvent(event: CalendarScreenEvent) {
        when (event) {
            is CalendarScreenEvent.MonthChanged -> {
                _state.update { currentState ->
                    currentState.copy(
                        currentMonth = event.month
                    )
                }
                viewModelScope.launch {
                    loadPerformances()
                }
            }

            is CalendarScreenEvent.DaySelected -> getPerformanceSummaries(event.day)

            is CalendarScreenEvent.DismissDayDialog -> {
                _state.update { it.copy(showDayDialog = false) }
            }
        }
    }

    private fun getMonthStartEnd(): Pair<Long, Long> {
        val month = _state.value.currentMonth.atEndOfMonth()

        val startOfMonth = month.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val endOfMonth = month.with(TemporalAdjusters.lastDayOfMonth())
            .atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return Pair(startOfMonth, endOfMonth)
    }

    private suspend fun loadPerformances() {

        val monthStartEnd = getMonthStartEnd()

        _state.update { it.copy(isLoading = true) }

        try {
            val zoneId = ZoneId.systemDefault()
            val performances = when (_state.value.mode) {
                CalendarMode.ALL -> performanceRepository.getAllExercisePerformancesByDateRange(
                    monthStartEnd.first, monthStartEnd.second
                ).first()

                CalendarMode.EXERCISE -> performanceRepository.getExercisePerformancesByDateRange(
                    exerciseId, monthStartEnd.first, monthStartEnd.second
                ).first()

                CalendarMode.GROUP -> {
                    if (groupExerciseIds.isEmpty()) emptyList()
                    else performanceRepository.getMultipleExercisePerformancesByDateRange(
                        groupExerciseIds, monthStartEnd.first, monthStartEnd.second
                    ).first()
                }
            }
            val groupedByDay = performances.groupBy { performance ->
                Instant.ofEpochMilli(performance.date)
                    .atZone(zoneId).toLocalDate()
            }

            val daysWithPerformances = groupedByDay.keys.map { localDate -> localDate.dayOfMonth }
                .toSet()

            val exerciseIds: Set<Long> = performances.mapTo(HashSet()) { it.exerciseId }

            val exerciseNames = exerciseRepository.getExerciseNamesByIds(exerciseIds)

            _state.update {
                it.copy(
                    performancesByDate = groupedByDay,
                    highlightedDays = daysWithPerformances,
                    exerciseNames = exerciseNames,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to load performances: ${e.message}"
                )
            }
        }
    }

    private fun getPerformanceSummaries(day: Int) {
        val date = _state.value.currentMonth.atDay(day)

        val performances = _state.value.performancesByDate[date]

        if (performances != null) {
            val summaries = performances.map {
                PerformanceSummary(
                    exerciseName = _state.value.exerciseNames[it.exerciseId] ?: "Unknown",
                    notes = it.notes,
                    weightRows = it.sets.toWeightRows()
                )
            }
            _state.update {
                it.copy(
                    selectedSummaries = summaries,
                    showDayDialog = true
                )
            }
        }
    }
}