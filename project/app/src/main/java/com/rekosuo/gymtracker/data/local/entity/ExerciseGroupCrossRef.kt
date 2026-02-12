package com.rekosuo.gymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// Table that links exercises to groups in a many-to-many relationship
@Entity(
    tableName = "exercise_group_cross_ref",
    primaryKeys = ["exerciseId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("groupId")]
)
data class ExerciseGroupCrossRef(
    val exerciseId: Long,
    val groupId: Long
)
