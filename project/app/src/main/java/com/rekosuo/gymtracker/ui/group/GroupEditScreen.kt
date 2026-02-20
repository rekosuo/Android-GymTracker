package com.rekosuo.gymtracker.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    groupId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: GroupEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navigate back when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId == null) "New Group" else "Edit Group") },
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
        ) {
            // Error message
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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

            // Group name field
            OutlinedTextField(
                value = state.groupName,
                onValueChange = {
                    viewModel.onEvent(GroupEditEvent.NameChanged(it))
                },
                label = { Text("Group Name") },
                placeholder = { Text("e.g., Chest Day") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !state.isLoading,
                singleLine = true
            )

            // Favorite toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                            viewModel.onEvent(GroupEditEvent.FavoriteToggled(it))
                        },
                        enabled = !state.isLoading
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = {
                    viewModel.onEvent(GroupEditEvent.SearchQueryChanged(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search exercises...") },
                leadingIcon = {
                    Icon(painter = painterResource(id = R.drawable.ic_symbol_search), "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(GroupEditEvent.SearchQueryChanged(""))
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_symbol_clear),
                                "Clear"
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Exercise selection section
            Text(
                text = "Select Exercises",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (state.availableExercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No exercises available.\nCreate exercises first!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.availableExercises,
                            key = { it.id }
                        ) { exercise ->
                            ExerciseCheckItem(
                                exercise = exercise,
                                isSelected = exercise.id in state.selectedExerciseIds,
                                onToggle = {
                                    viewModel.onEvent(GroupEditEvent.ExerciseToggled(exercise.id))
                                }
                            )
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = { viewModel.onEvent(GroupEditEvent.SaveGroup) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (groupId == null) "Create Group" else "Save Changes")
            }
        }
    }
}

@Composable
fun ExerciseCheckItem(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(
                    id = if (isSelected)
                        R.drawable.ic_symbol_check_circle
                    else
                        R.drawable.ic_symbol_radio_button_unchecked
                ),
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
