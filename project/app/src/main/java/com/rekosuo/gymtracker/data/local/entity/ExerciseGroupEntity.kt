package com.rekosuo.gymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Table for storing exercise groups
@Serializable
@Entity(tableName = "exercise_groups")
data class ExerciseGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
