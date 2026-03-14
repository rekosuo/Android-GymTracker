package com.rekosuo.gymtracker.testutil

import com.rekosuo.gymtracker.data.local.dao.GroupDao
import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake GroupDao for unit tests.
 *
 * This fake needs access to the exercise data (to resolve cross-refs),
 * so it takes a reference to FakeExerciseDao. This mirrors how the real
 * database has both tables available for JOINs.
 */
class FakeGroupDao(
    private val exerciseDao: FakeExerciseDao
) : GroupDao {

    private val groups = MutableStateFlow<List<ExerciseGroupEntity>>(emptyList())
    private val crossRefs = MutableStateFlow<List<ExerciseGroupCrossRef>>(emptyList())
    private var nextId = 1L

    override fun getAllGroups(): Flow<List<ExerciseGroupEntity>> {
        return groups.map { list -> list.sortedBy { it.name } }
    }

    override fun getFavoriteGroups(): Flow<List<ExerciseGroupEntity>> {
        return groups.map { list ->
            list.filter { it.isFavorite }.sortedBy { it.name }
        }
    }

    override suspend fun getGroupById(id: Long): ExerciseGroupEntity? {
        return groups.value.find { it.id == id }
    }

    override suspend fun insertGroup(group: ExerciseGroupEntity): Long {
        val id = if (group.id == 0L) nextId++ else group.id
        val withId = group.copy(id = id)
        groups.update { current -> current.filter { it.id != id } + withId }
        return id
    }

    override suspend fun updateGroup(group: ExerciseGroupEntity) {
        groups.update { current ->
            current.map { if (it.id == group.id) group else it }
        }
    }

    override suspend fun deleteGroup(group: ExerciseGroupEntity) {
        groups.update { current -> current.filter { it.id != group.id } }
        crossRefs.update { current -> current.filter { it.groupId != group.id } }
    }

    override suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        groups.update { current ->
            current.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
    }

    override suspend fun getOrderedExercisesForGroup(groupId: Long): List<ExerciseEntity> {
        val refs = crossRefs.value
            .filter { it.groupId == groupId }
            .sortedBy { it.orderIndex }

        return refs.mapNotNull { ref -> exerciseDao.getExerciseById(ref.exerciseId) }
    }

    override suspend fun deleteAllExercisesFromGroup(groupId: Long) {
        crossRefs.update { current -> current.filter { it.groupId != groupId } }
    }

    override suspend fun insertExerciseGroupCrossRefs(crossRefs: List<ExerciseGroupCrossRef>) {
        this.crossRefs.update { current -> current + crossRefs }
    }

    override suspend fun replaceGroupExercises(
        groupId: Long,
        crossRefs: List<ExerciseGroupCrossRef>
    ) {
        deleteAllExercisesFromGroup(groupId)
        insertExerciseGroupCrossRefs(crossRefs)
    }
}