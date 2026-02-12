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

data class GroupContentsState(
    val group: ExerciseGroup? = null,
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class GroupContentsEvent {
    data class ToggleFavoriteExercise(val exercise: Exercise) : GroupContentsEvent()
    data class DeleteExercise(val exercise: Exercise) : GroupContentsEvent()
}

@HiltViewModel
class GroupContentsViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: Long = savedStateHandle.get<Long>("groupId") ?: 0L

    private val _state = MutableStateFlow(GroupContentsState())
    val state = _state.asStateFlow()

    init {
        loadGroupContents()
    }

    private fun loadGroupContents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Load group with exercises
                val groupWithExercises = repository.getGroupWithExercises(groupId)
                if (groupWithExercises != null) {
                    _state.update {
                        it.copy(
                            group = groupWithExercises.group,
                            exercises = groupWithExercises.exercises,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Group not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load group: ${e.message}"
                    )
                }
            }
        }
    }

    fun onEvent(event: GroupContentsEvent) {
        when (event) {
            is GroupContentsEvent.ToggleFavoriteExercise -> {
                viewModelScope.launch {
                    try {
                        repository.updateExercise(
                            event.exercise.copy(isFavorite = !event.exercise.isFavorite)
                        )
                        loadGroupContents() // Refresh to show updated state
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                error = "Failed to update favorite: ${e.message}"
                            )
                        }
                    }
                }
            }

            is GroupContentsEvent.DeleteExercise -> {
                viewModelScope.launch {
                    try {
                        repository.deleteExercise(event.exercise)
                        loadGroupContents() // Refresh to show updated state
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                error = "Failed to delete exercise: ${e.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update {
            it.copy(
                error = null
            )
        }
    }
}