package com.rekosuo.gymtracker.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditScreen(
    exerciseId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navigate back automatically when save completes
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "New Exercise" else "Edit Exercise") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_symbol_arrow_back),
                            "Back"
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_symbol_check),
                                "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Exercise name field
            OutlinedTextField(
                value = state.exerciseName,
                onValueChange = {
                    viewModel.onEvent(ExerciseEditEvent.NameChanged(it))
                },
                label = { Text("Exercise Name") },
                placeholder = { Text("e.g., Bench Press") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true
            )

            // Favorite toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add to Favorites",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Show on home screen for quick access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isFavorite,
                        onCheckedChange = {
                            viewModel.onEvent(ExerciseEditEvent.FavoriteToggled(it))
                        },
                        enabled = !state.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.onEvent(ExerciseEditEvent.SaveExercise) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (exerciseId == null) "Create Exercise" else "Save Changes")
            }
        }
    }
}
