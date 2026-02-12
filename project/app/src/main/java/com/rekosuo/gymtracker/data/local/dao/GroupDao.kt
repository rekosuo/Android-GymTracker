
package com.rekosuo.gymtracker.data.local.dao

import androidx.room.*
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.GroupWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    // Basic CRUD operations
    @Query("SELECT * FROM exercise_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ExerciseGroupEntity>>

    @Query("SELECT * FROM exercise_groups WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteGroups(): Flow<List<ExerciseGroupEntity>>

    @Query("SELECT * FROM exercise_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ExerciseGroupEntity?

    @Query("SELECT * FROM exercise_groups WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGroups(query: String): Flow<List<ExerciseGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExerciseGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ExerciseGroupEntity)

    @Delete
    suspend fun deleteGroup(group: ExerciseGroupEntity)

    // Relationship queries
    @Transaction
    @Query("SELECT * FROM exercise_groups WHERE id = :groupId")
    suspend fun getGroupWithExercises(groupId: Long): GroupWithExercises?

    @Transaction
    @Query("SELECT * FROM exercise_groups ORDER BY name ASC")
    fun getAllGroupsWithExercises(): Flow<List<GroupWithExercises>>

    // Junction table operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseGroupCrossRef(crossRef: ExerciseGroupCrossRef)

    @Delete
    suspend fun deleteExerciseGroupCrossRef(crossRef: ExerciseGroupCrossRef)

    @Query("DELETE FROM exercise_group_cross_ref WHERE groupId = :groupId")
    suspend fun deleteAllExercisesFromGroup(groupId: Long)

    // Get all groups that contain a specific exercise
    @Query("""
        SELECT exercise_groups.* FROM exercise_groups
        INNER JOIN exercise_group_cross_ref ON exercise_groups.id = exercise_group_cross_ref.groupId
        WHERE exercise_group_cross_ref.exerciseId = :exerciseId
        ORDER BY exercise_groups.name ASC
    """)
    fun getGroupsForExercise(exerciseId: Long): Flow<List<ExerciseGroupEntity>>
}
