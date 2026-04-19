package com.rekosuo.gymtracker.data.backup

import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import kotlinx.serialization.Serializable

const val BACKUP_FORMAT_VERSION = 1

// Kept in sync with GymDatabase version=. Bump both together.
const val BACKUP_DB_SCHEMA_VERSION = 3

@Serializable
data class BackupFile(
    val formatVersion: Int,
    val dbSchemaVersion: Int,
    val exportedAt: Long,
    val exercises: List<ExerciseEntity>,
    val groups: List<ExerciseGroupEntity>,
    val groupExerciseCrossRefs: List<ExerciseGroupCrossRef>,
    val performances: List<PerformanceEntity>,
)
