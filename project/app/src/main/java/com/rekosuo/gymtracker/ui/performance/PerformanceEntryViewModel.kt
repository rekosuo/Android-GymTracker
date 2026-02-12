package com.rekosuo.gymtracker.ui.performance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.data.repository.PerformanceRepository
import com.rekosuo.gymtracker.domain.model.Performance
import com.rekosuo.gymtracker.domain.model.SetEntry
import com.rekosuo.gymtracker.domain.model.WeightRow
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
    // Add a new row with specified weight (starts with no reps)
    object AddWeightRow : PerformanceEntryEvent()

    // Add a rep to an existing weight row
    data class AddRepToRow(val rowIndex: Int) : PerformanceEntryEvent()

    // Update the weight value for a row
    data class UpdateWeight(val rowIndex: Int, val weight: Float) : PerformanceEntryEvent()

    // Update a specific rep value within a row
    data class UpdateRep(val rowIndex: Int, val repIndex: Int, val reps: Int) :
        PerformanceEntryEvent()

    // Delete an entire weight row
    data class DeleteRow(val rowIndex: Int) : PerformanceEntryEvent()

    // Delete a specific rep from a row
    data class DeleteRep(val rowIndex: Int, val repIndex: Int) : PerformanceEntryEvent()

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
                        val weightRows = setsToWeightRows(performance.sets)
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
                    // New performance - start with one empty weight row
                    val initialRow = WeightRow(weight = 0f, reps = emptyList(), startOrder = 0)
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
            is PerformanceEntryEvent.AddRepToRow -> addRepToRow(event.rowIndex)
            is PerformanceEntryEvent.UpdateWeight -> updateWeight(event.rowIndex, event.weight)
            is PerformanceEntryEvent.UpdateRep -> updateRep(
                event.rowIndex,
                event.repIndex,
                event.reps
            )

            is PerformanceEntryEvent.DeleteRow -> deleteRow(event.rowIndex)
            is PerformanceEntryEvent.DeleteRep -> deleteRep(event.rowIndex, event.repIndex)
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
                reps = emptyList(),
                startOrder = calculateNextOrder(currentState.weightRows)
            )
            val newRows = currentState.weightRows + newRow
            currentState.copy(
                weightRows = newRows,
                sets = weightRowsToSets(newRows)
            )
        }
    }

    /**
     * Adds a new rep entry to a specific row.
     * Default rep value is 0, user will edit it.
     */
    private fun addRepToRow(rowIndex: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    row.copy(reps = row.reps + 0)  // Add a new rep with 0 value
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = weightRowsToSets(newRows)
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
                sets = weightRowsToSets(newRows)
            )
        }
    }

    /**
     * Updates a specific rep value within a row.
     */
    private fun updateRep(rowIndex: Int, repIndex: Int, reps: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    val newReps = row.reps.toMutableList()
                    if (repIndex < newReps.size) {
                        newReps[repIndex] = reps
                    }
                    row.copy(reps = newReps)
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = weightRowsToSets(newRows)
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
                sets = weightRowsToSets(newRows)
            )
        }
    }

    /**
     * Deletes a specific rep from a row.
     * If this leaves the row with no reps, the row itself is kept (user can add reps or delete row).
     */
    private fun deleteRep(rowIndex: Int, repIndex: Int) {
        _state.update { currentState ->
            val newRows = currentState.weightRows.mapIndexed { index, row ->
                if (index == rowIndex) {
                    val newReps = row.reps.filterIndexed { i, _ -> i != repIndex }
                    row.copy(reps = newReps)
                } else {
                    row
                }
            }
            currentState.copy(
                weightRows = newRows,
                sets = weightRowsToSets(newRows)
            )
        }
    }

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    /**
     * Saves the performance to the database.
     * Only saves if there are actual sets with reps.
     */
    private fun savePerformance() {
        viewModelScope.launch {
            val currentState = _state.value

            // Filter out rows with no reps and rebuild sets
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

    // CONVERSION UTILITIES

    /**
     * Converts a flat list of SetEntries to WeightRows for UI display.
     *
     * This function groups consecutive sets with the same weight into single rows.
     * Sets are processed in order, so changing weight creates a new row even if
     * returning to a previously used weight.
     *
     * Example:
     * Input:  [SetEntry(20, 10, 0), SetEntry(20, 10, 1), SetEntry(25, 8, 2), SetEntry(20, 12, 3)]
     * Output: [WeightRow(20, [10,10], 0), WeightRow(25, [8], 2), WeightRow(20, [12], 3)]
     */
    private fun setsToWeightRows(sets: List<SetEntry>): List<WeightRow> {
        if (sets.isEmpty()) return emptyList()

        val sortedSets = sets.sortedBy { it.order }
        val rows = mutableListOf<WeightRow>()
        var currentWeight = sortedSets.first().weight
        var currentReps = mutableListOf<Int>()
        var startOrder = sortedSets.first().order

        for (set in sortedSets) {
            if (set.weight == currentWeight) {
                currentReps.add(set.reps)
            } else {
                // Weight changed - save current row and start new one
                rows.add(WeightRow(currentWeight, currentReps.toList(), startOrder))
                currentWeight = set.weight
                currentReps = mutableListOf(set.reps)
                startOrder = set.order
            }
        }

        // Add the last row
        rows.add(WeightRow(currentWeight, currentReps.toList(), startOrder))

        return rows
    }

    /**
     * Converts WeightRows back to a flat list of SetEntries.
     *
     * This flattens the row structure while preserving chronological order.
     * Order values are recalculated to be continuous (0, 1, 2, ...).
     */
    private fun weightRowsToSets(rows: List<WeightRow>): List<SetEntry> {
        val sets = mutableListOf<SetEntry>()
        var order = 0

        for (row in rows) {
            for (reps in row.reps) {
                sets.add(SetEntry(weight = row.weight, reps = reps, order = order))
                order++
            }
        }

        return sets
    }

    /**
     * Calculates the next order value based on existing rows.
     */
    private fun calculateNextOrder(rows: List<WeightRow>): Int {
        if (rows.isEmpty()) return 0
        val lastRow = rows.last()
        return lastRow.startOrder + lastRow.reps.size
    }
}
