package com.rekosuo.gymtracker.data.local.dao

import androidx.room.*
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseWithGroups
import kotlinx.coroutines.flow.Flow

/**
 * Interface that defines what database operations are possible.
 * Room generates implementations at compile time.
 */
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    // Get exercises that belong to a specific group
    @Query("""
        SELECT exercises.* FROM exercises
        INNER JOIN exercise_group_cross_ref ON exercises.id = exercise_group_cross_ref.exerciseId
        WHERE exercise_group_cross_ref.groupId = :groupId
        ORDER BY exercises.name ASC
    """)
    fun getExercisesByGroup(groupId: Long): Flow<List<ExerciseEntity>>

    // Get exercises that don't belong to any group
    @Query("""
        SELECT * FROM exercises
        WHERE id NOT IN (SELECT DISTINCT exerciseId FROM exercise_group_cross_ref)
        ORDER BY name ASC
    """)
    fun getUngroupedExercises(): Flow<List<ExerciseEntity>>

    // Get an exercise with all its groups
    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseWithGroups(exerciseId: Long): ExerciseWithGroups?

    // Get all exercises with their groups
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercisesWithGroups(): Flow<List<ExerciseWithGroups>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    // Many-to-many relationship operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseGroupCrossRef(crossRef: ExerciseGroupCrossRef)

    @Delete
    suspend fun deleteExerciseGroupCrossRef(crossRef: ExerciseGroupCrossRef)

    @Query("DELETE FROM exercise_group_cross_ref WHERE exerciseId = :exerciseId")
    suspend fun deleteAllGroupsForExercise(exerciseId: Long)

    @Query("DELETE FROM exercise_group_cross_ref WHERE exerciseId = :exerciseId AND groupId = :groupId")
    suspend fun removeExerciseFromGroup(exerciseId: Long, groupId: Long)
}
