package com.rekosuo.gymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Table for storing exercises
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
