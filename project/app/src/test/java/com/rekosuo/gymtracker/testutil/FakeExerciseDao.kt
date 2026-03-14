package com.rekosuo.gymtracker.testutil

import com.rekosuo.gymtracker.data.local.dao.ExerciseDao
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A fake (in-memory) implementation of ExerciseDao for use in unit tests.
 */
class FakeExerciseDao : ExerciseDao {

    // MutableStateFlow acts as our "database" — it holds the current state
    // and emits updates to any Flow collectors, just like Room does.
    private val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllExercises(): Flow<List<ExerciseEntity>> {
        return exercises.map { list -> list.sortedBy { it.name } }
    }

    override fun getFavoriteExercises(): Flow<List<ExerciseEntity>> {
        return exercises.map { list ->
            list.filter { it.isFavorite }.sortedBy { it.name }
        }
    }

    override suspend fun getExerciseById(id: Long): ExerciseEntity? {
        return exercises.value.find { it.id == id }
    }

    override suspend fun insertExercise(exercise: ExerciseEntity): Long {
        val id = if (exercise.id == 0L) nextId++ else exercise.id
        val withId = exercise.copy(id = id)
        exercises.update { current ->
            // REPLACE behavior: remove existing with same ID, add new
            current.filter { it.id != id } + withId
        }
        return id
    }

    override suspend fun updateExercise(exercise: ExerciseEntity) {
        exercises.update { current ->
            current.map { if (it.id == exercise.id) exercise else it }
        }
    }

    override suspend fun deleteExercise(exercise: ExerciseEntity) {
        exercises.update { current -> current.filter { it.id != exercise.id } }
    }

    override suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        exercises.update { current ->
            current.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
    }
}