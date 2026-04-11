package com.rekosuo.gymtracker.ui.performance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.domain.model.Performance
import com.rekosuo.gymtracker.domain.model.SetEntry
import com.rekosuo.gymtracker.domain.model.WeightRow
import com.rekosuo.gymtracker.domain.model.nextStartOrder
import com.rekosuo.gymtracker.domain.model.toSets
import com.rekosuo.gymtracker.domain.model.toWeightRows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Performance Entry Screen.
 *
 * The state is organized to support both the flat SetEntry list (for persistence)
 * and the WeightRow list (for UI display in the dynamic matrix grid).
 */
data class PerformanceEntryState(
    val exerciseId: Long = 0,
    val exerciseName: String = "",
    val performanceId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",

    // The flat list of all sets in chronological order (source of truth)
    val sets: List<SetEntry> = emptyList(),

    // Derived from sets - organized into rows for the matrix grid display
    val weightRows: List<WeightRow> = emptyList(),

    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

/**
 * Events that can occur on the Performance Entry Screen.
 *
 * The event system separates UI actions from business logic
 */
sealed class PerformanceEntryEvent {
    // Add a new row with specified weight (starts with one empty set)
    object AddWeightRow : PerformanceEntryEvent()

    // Add a rep to an existing weight row
    data class AddSetToRow(val rowIndex: Int) : PerformanceEntryEvent()

    // Update the weight value for a row
    data class UpdateWeight(val rowIndex: Int, val weight: Float) : PerformanceEntryEvent()

    // Update a specific rep value within a row
    data class UpdateSet(val rowIndex: Int, val setIndex: Int, val reps: Int) :
        PerformanceEntryEvent()

    // Delete an entire weight row
    data class DeleteRow(val rowIndex: Int) : PerformanceEntryEvent()

    // Delete a specific rep from a row
    data class DeleteSet(val rowIndex: Int, val setIndex: Int) : PerformanceEntryEvent()

    // Update notes
    data class UpdateNotes(val notes: String) : PerformanceEntryEvent()

    // Save the performance
    object SavePerformance : PerformanceEntryEvent()

    // Delete the entire performance
    object DeletePerformance : PerformanceEntryEvent()
}

/**
 * ViewModel for the Performance Entry Screen.
 *
 * This ViewModel manages the complex state transformation between:
 * - The flat SetEntry list (chronological, stored in database)
 * - The WeightRow list (grouped by consecutive weight, for UI display)
 *
 * Design:
 *
 * The `sets` list is the Single Source of Truth
 * - All modifications happen to `sets` list
 * - `weightRows` is always derived from `sets` via `setsToWeightRows()`
 *
 * Order preservation
 * - Each SetEntry has an `order` field
 * - Chronological integrity between edits is insured by recalculating order
 *
 * Row-based grouping in UI
 * - Consecutive sets at the same weight form a single WeightRow
 * - Weight changes create new rows
 * - Example: 20kg, 20kg, 25kg, 20kg -> 3 rows, not 2
 */
@HiltViewModel
class PerformanceEntryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val performanceRepository: PerformanceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val performanceId: Long = savedStateHandle.get<Long>("performanceId") ?: 0L

    private val _state = MutableStateFlow(PerformanceEntryState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, exerciseId = exerciseId) }

            try {
                // Load exercise details
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                if (exercise == null) {
                    _state.update { it.copy(isLoading = false, error = "Exercise not found") }
                    return@launch
                }

                _state.update { it.copy(exerciseName = exercise.name) }

                // Load existing performance if editing, otherwise create new
                if (performanceId != 0L) {
                    val performance = performanceRepository.getPerformanceById(performanceId)
                    if (performance != null) {
                        val weightRows = performance.sets.toWeightRows()
                        _state.update {
                            it.copy(
                                performanceId = performance.id,
                                date = performance.date,
                                notes = performance.notes,
                                sets = performance.sets,
                                weightRows = weightRows,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isLoading = false, error = "Performance not found")
                        }
                    }
                } else {
                    // New performance - start with one empty weight row with one rep
                    val initialRow = WeightRow(weight = 0f, sets = listOf(0), startOrder = 0)
                    _state.update {
                        it.copy(
                            weightRows = listOf(initialRow),
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

    fun onEvent(event: PerformanceEntryEvent) {
        when (event) {
            is PerformanceEntryEvent.AddWeightRow -> addWeightRow()
            is PerformanceEntryEvent.AddSetToRow -> addSetToRow(event.rowIndex)
            is PerformanceEntryEvent.UpdateWeight -> updateWeight(event.rowIndex, event.weight)
            is PerformanceEntryEvent.UpdateSet -> updateSet(
                event.rowIndex,
                event.setIndex,
                event.reps
            )

            is PerformanceEntryEvent.DeleteRow -> deleteRow(event.rowIndex)
            is PerformanceEntryEvent.DeleteSet -> deleteSet(event.rowIndex, event.setIndex)
            is PerformanceEntryEvent.UpdateNotes -> updateNotes(event.notes)
            is PerformanceEntryEvent.SavePerformance -> savePerformance()
            is PerformanceEntryEvent.DeletePerformance -> deletePerformance()
        }
    }

    /**
     * Adds a new weight row at the end of the matrix.
     * The new row starts with the same weight as the last row (or 0 if no rows exist).
     */
    private fun addWeightRow() {
        _state.update { currentState ->
            val lastWeight = currentState.weightRows.lastOrNull()?.weight ?: 0f
            val newRow = WeightRow(
                weight = lastWeight,
                sets = listOf(0),
                startOrder = currentState.weightRows.nextStartOrder()
            )
            val newRows = currentState.weightRows + newRow
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    /**
     * Adds a new set entry to a specific row.
     * Default rep value is 0, user will edit it.
     */
    private fun addSetToRow(rowIndex: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    row.copy(sets = row.sets + 0)  // Add a new set with 0 reps
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    /**
     * Updates the weight for a row.
     *
     * This does NOT merge rows even if they now have the same weight,
     * preserving chronological order.
     */
    private fun updateWeight(rowIndex: Int, weight: Float) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    row.copy(weight = weight)
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    /**
     * Updates a specific set value within a row.
     */
    private fun updateSet(rowIndex: Int, setIndex: Int, reps: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    val newSets = row.sets.toMutableList()
                    if (setIndex < newSets.size) {
                        newSets[setIndex] = reps
                    }
                    row.copy(sets = newSets)
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    /**
     * Deletes an entire weight row.
     */
    private fun deleteRow(rowIndex: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.filterIndexed { index, _ -> index != rowIndex }
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    /**
     * Deletes a specific set from a row.
     * If this leaves the row with no sets, the row itself is kept (user can add sets or delete row).
     */
    private fun deleteSet(rowIndex: Int, setIndex: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    val newSets = row.sets.filterIndexed { i, _ -> i != setIndex }
                    row.copy(sets = newSets)
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = newRows.toSets()
            )
        }
    }

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    /**
     * Saves the performance to the database.
     * Only saves if there are actual sets with sets.
     */
    private fun savePerformance() {
        viewModelScope.launch {
            val currentState = _state.value

            // Filter out rows with no sets and rebuild sets
            val validSets = currentState.sets.filter { it.reps > 0 }

            if (validSets.isEmpty()) {
                _state.update { it.copy(error = "Add at least one set with reps") }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                // Renumber sets to ensure continuous ordering
                val renumberedSets = validSets.mapIndexed { index, set ->
                    set.copy(order = index)
                }

                val performance = Performance(
                    id = if (currentState.performanceId != 0L) currentState.performanceId else 0,
                    exerciseId = currentState.exerciseId,
                    date = currentState.date,
                    sets = renumberedSets,
                    notes = currentState.notes
                )

                if (currentState.performanceId == 0L) {
                    performanceRepository.insertPerformance(performance)
                } else {
                    performanceRepository.updatePerformance(performance)
                }

                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    /**
     * Deletes the entire performance record.
     */
    private fun deletePerformance() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.performanceId == 0L) {
                // Nothing to delete, just navigate back
                _state.update { it.copy(isSaved = true) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                val performance = Performance(
                    id = currentState.performanceId,
                    exerciseId = currentState.exerciseId,
                    date = currentState.date,
                    sets = currentState.sets,
                    notes = currentState.notes
                )
                performanceRepository.deletePerformance(performance)
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

}
