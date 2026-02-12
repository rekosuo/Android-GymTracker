package com.rekosuo.gymtracker.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupEditState(
    val groupName: String = "",
    val isFavorite: Boolean = false,
    val availableExercises: List<Exercise> = emptyList(),
    val selectedExerciseIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed class GroupEditEvent {
    data class NameChanged(val name: String) : GroupEditEvent()
    data class FavoriteToggled(val isFavorite: Boolean) : GroupEditEvent()
    data class ExerciseToggled(val exerciseId: Long) : GroupEditEvent()
    object SaveGroup : GroupEditEvent()
}

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

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
                _state.update { it.copy(availableExercises = exercises) }

                // Load existing group if editing
                groupId?.let { id ->
                    if (id != 0L) {
                        loadGroup(id)
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
                        selectedExerciseIds = it.exercises.map { ex -> ex.id }.toSet()
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
            is GroupEditEvent.NameChanged -> {
                _state.update { it.copy(groupName = event.name) }
            }

            is GroupEditEvent.FavoriteToggled -> {
                _state.update { it.copy(isFavorite = event.isFavorite) }
            }

            is GroupEditEvent.ExerciseToggled -> {
                _state.update { currentState ->
                    val newSelectedIds = if (event.exerciseId in currentState.selectedExerciseIds) {
                        currentState.selectedExerciseIds - event.exerciseId
                    } else {
                        currentState.selectedExerciseIds + event.exerciseId
                    }
                    currentState.copy(selectedExerciseIds = newSelectedIds)
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
                    currentState.selectedExerciseIds.toList()
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
