package com.rekosuo.gymtracker.domain.model

/**
 * Domain model for an Exercise
 * This is what the UI layer works with
 */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model for an Exercise Group
 */
data class ExerciseGroup(
    val id: Long = 0,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model representing a group with its exercises
 * Used for displaying group contents
 */
data class GroupWithExercises(
    val group: ExerciseGroup,
    val exercises: List<Exercise>
)

/**
 * Domain model for a single set entry
 * Represents one set of an exercise (weight and reps)
 */
data class SetEntry(
    val weight: Float,
    val reps: Int,
    val order: Int = 0
)

/**
 * Domain model for a Performance record
 * Represents one workout session for an exercise
 */
data class Performance(
    val id: Long = 0,
    val exerciseId: Long,
    val date: Long = System.currentTimeMillis(),
    val sets: List<SetEntry> = emptyList(),
    val notes: String = ""
)

/**
 * Represents a row in the dynamic matrix grid UI.
 *
 * A WeightRow groups consecutive sets at the same weight for display purposes.
 * This is a UI-layer model used only for rendering the matrix grid,
 * derived from the underlying SetEntry list.
 *
 * Example transformation:
 * SetEntries: [20kg x 10 (order=0), 20kg x 10 (order=1), 22kg x 7 (order=2), 20kg x 8 (order=3)]
 *
 * Becomes WeightRows:
 * - WeightRow(weight=20, reps=[10, 10], startOrder=0)
 * - WeightRow(weight=22, reps=[7], startOrder=2)
 * - WeightRow(weight=20, reps=[8], startOrder=3)
 */
data class WeightRow(
    val weight: Float,
    val reps: List<Int>,      // List of reps for consecutive sets at this weight
    val startOrder: Int       // Order of the first set in this row (for editing/deleting)
)

/**
 * A single data point on the progress graph.
 * Contains all computed metrics to support multiple display modes.
 * Computed on-the-fly from Performance.sets.
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

// ── SetEntry / WeightRow conversions ──

/**
 * Converts a flat list of SetEntries to WeightRows.
 *
 * Groups consecutive sets with the same weight into single rows.
 * Sets are processed in order, so changing weight creates a new row even if
 * returning to a previously used weight.
 *
 * Example:
 * Input:  [SetEntry(20, 10, 0), SetEntry(20, 10, 1), SetEntry(25, 8, 2), SetEntry(20, 12, 3)]
 * Output: [WeightRow(20, [10,10], 0), WeightRow(25, [8], 2), WeightRow(20, [12], 3)]
 */
fun List<SetEntry>.toWeightRows(): List<WeightRow> {
    if (isEmpty()) return emptyList()

    val sortedSets = sortedBy { it.order }
    val rows = mutableListOf<WeightRow>()
    var currentWeight = sortedSets.first().weight
    var currentReps = mutableListOf<Int>()
    var startOrder = sortedSets.first().order

    for (set in sortedSets) {
        if (set.weight == currentWeight) {
            currentReps.add(set.reps)
        } else {
            rows.add(WeightRow(currentWeight, currentReps.toList(), startOrder))
            currentWeight = set.weight
            currentReps = mutableListOf(set.reps)
            startOrder = set.order
        }
    }

    rows.add(WeightRow(currentWeight, currentReps.toList(), startOrder))
    return rows
}

/**
 * Converts WeightRows back to a flat list of SetEntries.
 * Order values are recalculated to be continuous (0, 1, 2, ...).
 */
fun List<WeightRow>.toSets(): List<SetEntry> {
    val sets = mutableListOf<SetEntry>()
    var order = 0
    for (row in this) {
        for (reps in row.reps) {
            sets.add(SetEntry(weight = row.weight, reps = reps, order = order))
            order++
        }
    }
    return sets
}

/**
 * Calculates the next order value for appending a new WeightRow.
 */
fun List<WeightRow>.nextStartOrder(): Int {
    if (isEmpty()) return 0
    val lastRow = last()
    return lastRow.startOrder + lastRow.reps.size
}

// ── Performance → GraphDataPoint ──

/**
 * Transforms a Performance into a GraphDataPoint.
 * All metrics computed on-the-fly from the sets list.
 */
fun Performance.toGraphDataPoint(): GraphDataPoint {
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

// ── Graph filtering ──

/**
 * Filters graph data points to only include those within the given number of months.
 * Returns all points if [months] is null.
 */
fun List<GraphDataPoint>.filterByTime(months: Int?): List<GraphDataPoint> {
    if (months == null) return this
    val cutoffTime = System.currentTimeMillis() - (months * 30L * 24 * 60 * 60 * 1000)
    return filter { it.date >= cutoffTime }
}
