package com.rekosuo.gymtracker.data.repository

import com.rekosuo.gymtracker.data.local.dao.ExerciseDao
import com.rekosuo.gymtracker.data.local.dao.GroupDao
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup
import com.rekosuo.gymtracker.domain.model.GroupWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val groupDao: GroupDao
) {
    // Exercise operations
    fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFavoriteExercises(): Flow<List<Exercise>> {
        return exerciseDao.getFavoriteExercises().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getExerciseById(id: Long): Exercise? {
        return exerciseDao.getExerciseById(id)?.toDomain()
    }

    suspend fun insertExercise(exercise: Exercise): Long {
        return exerciseDao.insertExercise(exercise.toEntity())
    }

    suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateExercise(exercise.toEntity())
    }

    suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.toEntity())
    }

    fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Group operations
    fun getAllGroups(): Flow<List<ExerciseGroup>> {
        return groupDao.getAllGroups().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFavoriteGroups(): Flow<List<ExerciseGroup>> {
        return groupDao.getFavoriteGroups().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getGroupById(id: Long): ExerciseGroup? {
        return groupDao.getGroupById(id)?.toDomain()
    }

    suspend fun getGroupWithExercises(groupId: Long): GroupWithExercises? {
        return groupDao.getGroupWithExercises(groupId)?.let { relation ->
            GroupWithExercises(
                group = relation.group.toDomain(),
                exercises = relation.exercises.map { it.toDomain() }
            )
        }
    }

    fun getAllGroupsWithExercises(): Flow<List<GroupWithExercises>> {
        return groupDao.getAllGroupsWithExercises().map { relations ->
            relations.map { relation ->
                GroupWithExercises(
                    group = relation.group.toDomain(),
                    exercises = relation.exercises.map { it.toDomain() }
                )
            }
        }
    }

    suspend fun insertGroup(group: ExerciseGroup): Long {
        return groupDao.insertGroup(group.toEntity())
    }

    suspend fun updateGroup(group: ExerciseGroup) {
        groupDao.updateGroup(group.toEntity())
    }

    suspend fun deleteGroup(group: ExerciseGroup) {
        groupDao.deleteGroup(group.toEntity())
    }

    suspend fun addExerciseToGroup(exerciseId: Long, groupId: Long) {
        groupDao.insertExerciseGroupCrossRef(
            ExerciseGroupCrossRef(exerciseId = exerciseId, groupId = groupId)
        )
    }

    suspend fun removeExerciseFromGroup(exerciseId: Long, groupId: Long) {
        groupDao.deleteExerciseGroupCrossRef(
            ExerciseGroupCrossRef(exerciseId = exerciseId, groupId = groupId)
        )
    }

    suspend fun updateGroupExercises(groupId: Long, exerciseIds: List<Long>) {
        // First, get current exercises in the group
        val currentExercises =
            groupDao.getGroupWithExercises(groupId)?.exercises?.map { it.id } ?: emptyList()

        // Remove exercises that are no longer in the group
        val toRemove = currentExercises.filter { it !in exerciseIds }
        toRemove.forEach { exerciseId ->
            removeExerciseFromGroup(exerciseId, groupId)
        }

        // Add new exercises
        val toAdd = exerciseIds.filter { it !in currentExercises }
        toAdd.forEach { exerciseId ->
            addExerciseToGroup(exerciseId, groupId)
        }
    }

    // Mapper functions
    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    private fun ExerciseGroupEntity.toDomain() = ExerciseGroup(
        id = id,
        name = name,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    private fun ExerciseGroup.toEntity() = ExerciseGroupEntity(
        id = id,
        name = name,
        isFavorite = isFavorite,
        createdAt = createdAt
    )
}