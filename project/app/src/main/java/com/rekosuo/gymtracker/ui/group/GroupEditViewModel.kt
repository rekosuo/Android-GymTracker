package com.rekosuo.gymtracker.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupEditState(
    val groupName: String = "",
    val isFavorite: Boolean = false,
    val searchQuery: String = "",
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExercises: List<Exercise> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed class GroupEditEvent {
    data class SearchQueryChanged(val query: String) : GroupEditEvent()
    data class NameChanged(val name: String) : GroupEditEvent()
    data class FavoriteToggled(val isFavorite: Boolean) : GroupEditEvent()
    data class ExerciseToggled(val exerciseId: Long) : GroupEditEvent()
    data class ExerciseMoved(val exerciseId: Long, val direction: MoveDirection) : GroupEditEvent()
    object SaveGroup : GroupEditEvent()
}

enum class MoveDirection { UP, DOWN }

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var allExercises: List<Exercise> = emptyList()

    private val groupId: Long? = savedStateHandle.get<Long>("groupId")

    private val _state = MutableStateFlow(GroupEditState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Load all available exercises - use first() to get initial value
                val exercises = repository.getAllExercises().first()
                allExercises = exercises
                _state.update { it.copy(availableExercises = exercises) }

                // Load existing group if editing
                groupId?.let { id ->
                    if (id != 0L) {
                        loadGroup(id)
                        _state.update { it.copy(isEditing = true) }
                    }
                }

                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun loadGroup(id: Long) {
        try {
            val groupWithExercises = repository.getGroupWithExercises(id)
            groupWithExercises?.let {
                _state.update { state ->
                    state.copy(
                        groupName = it.group.name,
                        isFavorite = it.group.isFavorite,
                        selectedExercises = it.exercises
                    )
                }
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(error = "Failed to load group: ${e.message}")
            }
        }
    }

    fun onEvent(event: GroupEditEvent) {
        when (event) {
            is GroupEditEvent.SearchQueryChanged -> {
                val query = event.query
                val filtered = if (query.isBlank()) allExercises
                else allExercises.filter { it.name.contains(query, ignoreCase = true) }
                _state.update { it.copy(searchQuery = query, availableExercises = filtered) }
            }

            is GroupEditEvent.NameChanged -> {
                _state.update { it.copy(groupName = event.name) }
            }

            is GroupEditEvent.FavoriteToggled -> {
                _state.update { it.copy(isFavorite = event.isFavorite) }
            }

            is GroupEditEvent.ExerciseToggled -> {
                _state.update { currentState ->
                    val isSelected =
                        currentState.selectedExercises.any { it.id == event.exerciseId }
                    val newSelected = if (isSelected) {
                        currentState.selectedExercises.filter { it.id != event.exerciseId }
                    } else {
                        val exercise = allExercises.find { it.id == event.exerciseId }
                            ?: return@update currentState
                        currentState.selectedExercises + exercise
                    }
                    currentState.copy(selectedExercises = newSelected)
                }
            }

            is GroupEditEvent.ExerciseMoved -> {
                _state.update { currentState ->
                    val list = currentState.selectedExercises.toMutableList()
                    val index = list.indexOfFirst { it.id == event.exerciseId }
                    if (index < 0) return@update currentState
                    val targetIndex = when (event.direction) {
                        MoveDirection.UP -> index - 1
                        MoveDirection.DOWN -> index + 1
                    }
                    if (targetIndex !in list.indices) return@update currentState
                    list[index] = list[targetIndex].also { list[targetIndex] = list[index] }
                    currentState.copy(selectedExercises = list)
                }
            }

            GroupEditEvent.SaveGroup -> {
                saveGroup()
            }
        }
    }

    private fun saveGroup() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.groupName.isBlank()) {
                _state.update { it.copy(error = "Group name cannot be empty") }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                val group = ExerciseGroup(
                    id = groupId ?: 0,
                    name = currentState.groupName.trim(),
                    isFavorite = currentState.isFavorite,
                    createdAt = System.currentTimeMillis()
                )

                // Check for both null AND 0L to determine if this is a new group
                val savedGroupId = if (groupId == null || groupId == 0L) {
                    repository.insertGroup(group)
                } else {
                    repository.updateGroup(group)
                    groupId
                }

                // Update exercise-group relationships
                repository.updateGroupExercises(
                    savedGroupId,
                    currentState.selectedExercises.map { it.id }
                )

                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save group: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
