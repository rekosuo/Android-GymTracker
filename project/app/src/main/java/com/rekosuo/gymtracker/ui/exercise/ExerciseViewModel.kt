package com.rekosuo.gymtracker.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseListState(
    val exercises: List<Exercise> = emptyList(),
    val groups: List<ExerciseGroup> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ExerciseListEvent {
    data class SearchQueryChanged(val query: String) : ExerciseListEvent()
    data class DeleteExercise(val exercise: Exercise) : ExerciseListEvent()
    data class ToggleFavorite(val exercise: Exercise) : ExerciseListEvent()
    data class DeleteGroup(val group: ExerciseGroup) : ExerciseListEvent()
    data class ToggleFavoriteGroup(val group: ExerciseGroup) : ExerciseListEvent()
}

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Combine all exercises and groups with search filtering
    val state: StateFlow<ExerciseListState> = combine(
        repository.getAllExercises(),
        repository.getAllGroups(),
        searchQuery,
        _isLoading,
        _error
    ) { exercises, groups, query, loading, error ->
        val filteredExercises = if (query.isBlank()) {
            exercises
        } else {
            exercises.filter { it.name.contains(query, ignoreCase = true) }
        }

        val filteredGroups = if (query.isBlank()) {
            groups
        } else {
            groups.filter { it.name.contains(query, ignoreCase = true) }
        }
        // Combine all data into single state object
        ExerciseListState(
            exercises = filteredExercises,
            groups = filteredGroups,
            searchQuery = query,
            isLoading = loading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExerciseListState(isLoading = true)
    )

    fun onEvent(event: ExerciseListEvent) {
        when (event) {
            is ExerciseListEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query    // Simple state update
            }

            is ExerciseListEvent.DeleteExercise -> {
                viewModelScope.launch {
                    try {
                        repository.deleteExercise(event.exercise)   // Async operation
                    } catch (e: Exception) {
                        _error.value = "Failed to delete exercise: ${e.message}"
                    }
                }
            }

            is ExerciseListEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    try {
                        repository.updateExercise(
                            event.exercise.copy(isFavorite = !event.exercise.isFavorite)
                        )
                    } catch (e: Exception) {
                        _error.value = "Failed to update favorite: ${e.message}"
                    }
                }
            }

            is ExerciseListEvent.DeleteGroup -> {
                viewModelScope.launch {
                    try {
                        repository.deleteGroup(event.group)
                    } catch (e: Exception) {
                        _error.value = "Failed to delete group: ${e.message}"
                    }
                }
            }

            is ExerciseListEvent.ToggleFavoriteGroup -> {
                viewModelScope.launch {
                    try {
                        repository.updateGroup(
                            event.group.copy(isFavorite = !event.group.isFavorite)
                        )
                    } catch (e: Exception) {
                        _error.value = "Failed to update favorite: ${e.message}"
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
