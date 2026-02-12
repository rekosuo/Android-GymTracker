package com.rekosuo.gymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Table for storing exercise performances
@Entity(tableName = "performances")
data class PerformanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val date: Long = System.currentTimeMillis(),
    val sets: List<SetEntry> = emptyList(), // Performance entry includes all sets performed
    val notes: String = ""
)

/**
 * Represents a single set in a workout.
 *
 * ARCHITECTURE NOTE: Each SetEntry represents ONE individual set, preserving chronological order.
 * The 'order' field ensures sets are displayed in the sequence they were performed.
 *
 * This design supports the dynamic matrix grid UI where:
 * - Consecutive sets at the same weight appear on the same row
 * - If weight changes and returns to a previous weight, a NEW row is created
 * - Example: 20kg x 10, 20kg x 10, 22kg x 7, 20kg x 8 would display as:
 *   Row 1: 20kg | 10 | 10
 *   Row 2: 22kg | 7
 *   Row 3: 20kg | 8
 */
@Serializable
data class SetEntry(
    val weight: Float,
    val reps: Int,
    val order: Int  // Chronological order of this set
)

// Type converter for Room to store List<SetEntry> as JSON
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSetEntryList(value: List<SetEntry>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toSetEntryList(value: String): List<SetEntry> {
        return json.decodeFromString(value)
    }
}
