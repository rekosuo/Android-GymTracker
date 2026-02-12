package com.rekosuo.gymtracker.ui.graph

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.domain.model.Performance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a single data point on the progress graph.
 * Contains all computed metrics to support multiple display modes.
 * Computed on-the-fly from Performance.sets - no database changes needed.
 */
data class GraphDataPoint(
    val performanceId: Long,
    val date: Long,
    val maxWeight: Float,
    val minWeight: Float,
    val avgWeight: Float,
    val totalSets: Int,
    val totalReps: Int
)

/**
 * Display mode for the graph.
 * Prepared for future expansion to AVERAGE and RANGE modes.
 */
enum class WeightDisplayMode {
    MAX,
    AVERAGE,
    RANGE
}

/**
 * Time range filter for the graph display.
 */
enum class TimeRange(val label: String, val months: Int?) {
    ONE_MONTH("1 mo", 1),
    THREE_MONTHS("3 mo", 3),
    ONE_YEAR("1y", 12),
    ALL("all", null)
}

/**
 * UI State for Progress Graph Screen.
 */
data class ProgressGraphState(
    val exerciseId: Long = 0,
    val exerciseName: String = "",
    val dataPoints: List<GraphDataPoint> = emptyList(),
    val filteredDataPoints: List<GraphDataPoint> = emptyList(),
    val displayMode: WeightDisplayMode = WeightDisplayMode.MAX,
    val timeRange: TimeRange = TimeRange.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Events for Progress Graph Screen.
 */
sealed class ProgressGraphEvent {
    data class SetTimeRange(val timeRange: TimeRange) : ProgressGraphEvent()
    data class SetDisplayMode(val mode: WeightDisplayMode) : ProgressGraphEvent()
}

/**
 * ViewModel for ProgressGraphScreen.
 *
 * Follows Android Architecture Components guidelines:
 * - Uses SavedStateHandle for navigation arguments (survives process death)
 * - Exposes immutable StateFlow to UI (unidirectional data flow)
 * - Transforms data in ViewModel, keeping UI logic-free
 */
@HiltViewModel
class ProgressGraphViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val performanceRepository: PerformanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L

    private val _state = MutableStateFlow(ProgressGraphState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, exerciseId = exerciseId) }

            try {
                // Load exercise name
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                if (exercise == null) {
                    _state.update { it.copy(isLoading = false, error = "Exercise not found") }
                    return@launch
                }

                _state.update { it.copy(exerciseName = exercise.name) }

                // Collect performances as Flow and transform to graph points
                performanceRepository.getPerformancesForExercise(exerciseId)
                    .collect { performances ->
                        val dataPoints = performances
                            .filter { it.sets.isNotEmpty() }
                            .map { it.toGraphDataPoint() }
                            .sortedBy { it.date }

                        _state.update { currentState ->
                            currentState.copy(
                                dataPoints = dataPoints,
                                filteredDataPoints = filterByTimeRange(
                                    dataPoints,
                                    currentState.timeRange
                                ),
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to load data: ${e.message}")
                }
            }
        }
    }

    fun onEvent(event: ProgressGraphEvent) {
        when (event) {
            is ProgressGraphEvent.SetTimeRange -> {
                _state.update { currentState ->
                    currentState.copy(
                        timeRange = event.timeRange,
                        filteredDataPoints = filterByTimeRange(
                            currentState.dataPoints,
                            event.timeRange
                        )
                    )
                }
            }

            is ProgressGraphEvent.SetDisplayMode -> {
                _state.update { it.copy(displayMode = event.mode) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Transforms a Performance into a GraphDataPoint.
     * All metrics computed on-the-fly from the sets list.
     * This keeps the data layer clean and avoids denormalization.
     */
    private fun Performance.toGraphDataPoint(): GraphDataPoint {
        val weights = sets.map { it.weight }
        return GraphDataPoint(
            performanceId = id,
            date = date,
            maxWeight = weights.maxOrNull() ?: 0f,
            minWeight = weights.minOrNull() ?: 0f,
            avgWeight = if (weights.isNotEmpty()) weights.average().toFloat() else 0f,
            totalSets = sets.size,
            totalReps = sets.sumOf { it.reps }
        )
    }

    /**
     * Filters data points by time range.
     */
    private fun filterByTimeRange(
        dataPoints: List<GraphDataPoint>,
        timeRange: TimeRange
    ): List<GraphDataPoint> {
        if (timeRange.months == null) return dataPoints

        val cutoffTime = System.currentTimeMillis() - (timeRange.months * 30L * 24 * 60 * 60 * 1000)
        return dataPoints.filter { it.date >= cutoffTime }
    }
}
