package com.rekosuo.gymtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.repository.ExerciseRepository
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val favoriteExercises: List<Exercise> = emptyList(),
    val favoriteGroups: List<ExerciseGroup> = emptyList(),
    val isLoading: Boolean = false
)

sealed class HomeEvent {
    data class ToggleFavoriteExercise(val exercise: Exercise) : HomeEvent()
    data class ToggleFavoriteGroup(val group: ExerciseGroup) : HomeEvent()
    data class DeleteExercise(val exercise: Exercise) : HomeEvent()
    data class DeleteGroup(val group: ExerciseGroup) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repository.getFavoriteExercises(),
        repository.getFavoriteGroups()
    ) { favoriteExercises, favoriteGroups ->
        HomeState(
            favoriteExercises = favoriteExercises,
            favoriteGroups = favoriteGroups,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState(isLoading = true)
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ToggleFavoriteExercise -> {
                viewModelScope.launch {
                    try {
                        repository.updateExercise(
                            event.exercise.copy(isFavorite = !event.exercise.isFavorite)
                        )
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }

            is HomeEvent.ToggleFavoriteGroup -> {
                viewModelScope.launch {
                    try {
                        repository.updateGroup(
                            event.group.copy(isFavorite = !event.group.isFavorite)
                        )
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }

            is HomeEvent.DeleteExercise -> {
                viewModelScope.launch {
                    try {
                        repository.deleteExercise(event.exercise)
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }

            is HomeEvent.DeleteGroup -> {
                viewModelScope.launch {
                    try {
                        repository.deleteGroup(event.group)
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        }
    }
}

