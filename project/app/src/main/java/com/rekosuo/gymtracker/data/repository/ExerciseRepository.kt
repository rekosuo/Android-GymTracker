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

    suspend fun toggleExerciseFavorite(exercise: Exercise) {
        exerciseDao.updateFavoriteStatus(exercise.id, !exercise.isFavorite)
    }

    suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.toEntity())
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

    suspend fun getGroupWithExercises(groupId: Long): GroupWithExercises? {
        val group = groupDao.getGroupById(groupId) ?: return null
        val exercises = groupDao.getOrderedExercisesForGroup(groupId)
        return GroupWithExercises(
            group = group.toDomain(),
            exercises = exercises.map { it.toDomain() }
        )
    }

    suspend fun insertGroup(group: ExerciseGroup): Long {
        return groupDao.insertGroup(group.toEntity())
    }

    suspend fun updateGroup(group: ExerciseGroup) {
        groupDao.updateGroup(group.toEntity())
    }

    suspend fun toggleGroupFavorite(group: ExerciseGroup) {
        groupDao.updateFavoriteStatus(group.id, !group.isFavorite)
    }

    suspend fun deleteGroup(group: ExerciseGroup) {
        groupDao.deleteGroup(group.toEntity())
    }

    suspend fun updateGroupExercises(groupId: Long, exerciseIds: List<Long>) {
        val crossRefs = exerciseIds.mapIndexed { index, exerciseId ->
            ExerciseGroupCrossRef(
                exerciseId = exerciseId,
                groupId = groupId,
                orderIndex = index
            )
        }
        groupDao.replaceGroupExercises(groupId, crossRefs)
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