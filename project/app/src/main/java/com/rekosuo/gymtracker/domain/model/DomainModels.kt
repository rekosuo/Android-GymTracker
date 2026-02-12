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
