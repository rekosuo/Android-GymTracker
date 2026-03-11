
package com.rekosuo.gymtracker.data.local.dao

import androidx.room.*
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM exercise_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ExerciseGroupEntity>>

    @Query("SELECT * FROM exercise_groups WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteGroups(): Flow<List<ExerciseGroupEntity>>

    @Query("SELECT * FROM exercise_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ExerciseGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExerciseGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ExerciseGroupEntity)

    @Delete
    suspend fun deleteGroup(group: ExerciseGroupEntity)

    @Query("UPDATE exercise_groups SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("""
        SELECT exercises.* FROM exercises
        INNER JOIN exercise_group_cross_ref ON exercises.id = exercise_group_cross_ref.exerciseId
        WHERE exercise_group_cross_ref.groupId = :groupId
        ORDER BY exercise_group_cross_ref.orderIndex ASC
    """)
    suspend fun getOrderedExercisesForGroup(groupId: Long): List<ExerciseEntity>

    @Query("DELETE FROM exercise_group_cross_ref WHERE groupId = :groupId")
    suspend fun deleteAllExercisesFromGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseGroupCrossRefs(crossRefs: List<ExerciseGroupCrossRef>)

    @Transaction
    suspend fun replaceGroupExercises(groupId: Long, crossRefs: List<ExerciseGroupCrossRef>) {
        deleteAllExercisesFromGroup(groupId)
        insertExerciseGroupCrossRefs(crossRefs)
    }
}