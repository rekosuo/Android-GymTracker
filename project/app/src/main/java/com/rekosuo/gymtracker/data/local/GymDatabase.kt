package com.rekosuo.gymtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rekosuo.gymtracker.data.local.dao.ExerciseDao
import com.rekosuo.gymtracker.data.local.dao.GroupDao
import com.rekosuo.gymtracker.data.local.dao.PerformanceDao
import com.rekosuo.gymtracker.data.local.entity.Converters
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity

// Central database class
@Database(
    // Define which tables / entities exist
    entities = [
        ExerciseEntity::class,
        ExerciseGroupEntity::class,
        ExerciseGroupCrossRef::class,
        PerformanceEntity::class
    ],
    version = 1,    // Increment for schema changes
    exportSchema = false
)
// Register type converters and provide access to DAOs. Room generates implementation.
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun groupDao(): GroupDao
    abstract fun performanceDao(): PerformanceDao
}
