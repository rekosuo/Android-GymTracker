package com.rekosuo.gymtracker.ui.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseEditState(
    val exerciseName: String = "",
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed class ExerciseEditEvent {
    data class NameChanged(val name: String) : ExerciseEditEvent()
    data class FavoriteToggled(val isFavorite: Boolean) : ExerciseEditEvent()
    object SaveExercise : ExerciseEditEvent()
}

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle // Contains navigation arguments
) : ViewModel() {
    // Extract the exerciseId from navigation
    private val exerciseId: Long? = savedStateHandle.get<Long>("exerciseId")

    private val _state = MutableStateFlow(ExerciseEditState())
    val state = _state.asStateFlow()

    init {
        // Use the exerciseId to load data if editing existing exercise
        exerciseId?.let { id ->
            if (id != 0L) {
                loadExercise(id)
            } else {
                // For new exercises, set loading to false immediately
                _state.update { it.copy(isLoading = false) }
            }
        } ?: run {
            // For new exercises, set loading to false immediately
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadExercise(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val exercise = repository.getExerciseById(id)
                if (exercise != null) {
                    _state.update {
                        it.copy(
                            exerciseName = exercise.name,
                            isFavorite = exercise.isFavorite,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Exercise not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load exercise: ${e.message}"
                    )
                }
            }
        }
    }

    fun onEvent(event: ExerciseEditEvent) {
        when (event) {
            is ExerciseEditEvent.NameChanged -> {
                _state.update { it.copy(exerciseName = event.name) }
            }

            is ExerciseEditEvent.FavoriteToggled -> {
                _state.update { it.copy(isFavorite = event.isFavorite) }
            }

            ExerciseEditEvent.SaveExercise -> {
                saveExercise()
            }
        }
    }

    private fun saveExercise() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.exerciseName.isBlank()) {
                _state.update { it.copy(error = "Exercise name cannot be empty") }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                val exercise = Exercise(
                    id = exerciseId ?: 0,
                    name = currentState.exerciseName.trim(),
                    isFavorite = currentState.isFavorite,
                    createdAt = System.currentTimeMillis()
                )

                // Check for both null AND 0L to determine if this is a new exercise
                if (exerciseId == null || exerciseId == 0L) {
                    repository.insertExercise(exercise)
                } else {
                    repository.updateExercise(exercise)
                }

                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save exercise: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
