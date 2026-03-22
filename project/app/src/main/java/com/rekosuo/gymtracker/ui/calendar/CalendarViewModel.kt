package com.rekosuo.gymtracker.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.domain.model.Performance
import com.rekosuo.gymtracker.domain.model.PerformanceSummary
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
    data class DaySelected(val day: LocalDate) : CalendarScreenEvent()
    object DismissDayDialog : CalendarScreenEvent()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: PerformanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val groupId: Long = savedStateHandle.get<Long>("groupId") ?: 0L


    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    init {
        getMode()
        loadData()
    }

    private fun getMode() {
        if (exerciseId != 0L) {
            _state.update { it.copy(mode = CalendarMode.EXERCISE) }
        } else if (groupId != 0L) {
            _state.update { it.copy(mode = CalendarMode.GROUP) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            loadPerformances()
        }
    }

    private suspend fun loadPerformances() {

        val month = _state.value.currentMonth.atEndOfMonth()

        val startOfMonth = month.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val endOfMonth = month.with(TemporalAdjusters.lastDayOfMonth())
            .atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        _state.update { it.copy(isLoading = true) }

        try {
            val zoneId = ZoneId.systemDefault()
            val performances =
                repository.getAllExercisePerformancesByDateRange(startOfMonth, endOfMonth).first()
            val groupedByDay = performances.groupBy { performance ->
                Instant.ofEpochMilli(performance.date)
                    .atZone(zoneId).toLocalDate()
            }

            val days = groupedByDay.keys.map { localDate -> localDate.dayOfMonth }
                .toSet()

            _state.update {
                it.copy(
                    performancesByDate = groupedByDay,
                    highlightedDays = days,
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

            is CalendarScreenEvent.DaySelected -> {

            }

            is CalendarScreenEvent.DismissDayDialog -> {

            }
        }
    }
}