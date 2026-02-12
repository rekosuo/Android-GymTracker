package com.rekosuo.gymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Represents an Exercise with all its associated Groups
 */
data class ExerciseWithGroups(
    @Embedded val exercise: ExerciseEntity, // Attributes of ExerciseEntity are embedded directly into ExerciseWithGroups
    @Relation(
        parentColumn = "id", // ExerciseEntity's id
        entityColumn = "id", // ExerciseGroupEntity's id
        associateBy = Junction(
            value = ExerciseGroupCrossRef::class, // Which table acts as junction
            parentColumn = "exerciseId",
            entityColumn = "groupId"
        )
    )
    val groups: List<ExerciseGroupEntity> // Room fills list with groups connected to exercise
)

/**
 * Represents a Group with all its associated Exercises
 */
data class GroupWithExercises(
    @Embedded val group: ExerciseGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExerciseGroupCrossRef::class,
            parentColumn = "groupId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<ExerciseEntity>
)
