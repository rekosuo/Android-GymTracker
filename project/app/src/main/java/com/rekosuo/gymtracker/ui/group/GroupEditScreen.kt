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
    onNavigateBack: () -> Unit,
    viewModel: GroupEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back when saved or deleted
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Group" else "New Group") },
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
            // Error message — kept outside the scroll area so it's always visible
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

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val selectedIds = state.selectedExercises.map { it.id }.toSet()
                val unselectedExercises = state.availableExercises.filter {
                    it.id !in selectedIds
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group name field
                    item {
                        OutlinedTextField(
                            value = state.groupName,
                            onValueChange = {
                                viewModel.onEvent(GroupEditEvent.NameChanged(it))
                            },
                            label = { Text("Group Name") },
                            placeholder = { Text("e.g., Chest Day") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                            singleLine = true
                        )
                    }

                    // Favorites toggle (compact — no subtitle)
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add to Favorites",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Switch(
                                    checked = state.isFavorite,
                                    onCheckedChange = {
                                        viewModel.onEvent(GroupEditEvent.FavoriteToggled(it))
                                    },
                                    enabled = !state.isLoading
                                )
                            }
                        }
                    }

                    // Selected exercises section
                    if (state.selectedExercises.isNotEmpty()) {
                        item {
                            Text(
                                text = "Selected Exercises",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(
                            items = state.selectedExercises,
                            key = { "selected_${it.id}" }
                        ) { exercise ->
                            SelectedExerciseItem(
                                exercise = exercise,
                                isFirst = exercise.id == state.selectedExercises.first().id,
                                isLast = exercise.id == state.selectedExercises.last().id,
                                onMoveUp = {
                                    viewModel.onEvent(
                                        GroupEditEvent.ExerciseMoved(exercise.id, MoveDirection.UP)
                                    )
                                },
                                onMoveDown = {
                                    viewModel.onEvent(
                                        GroupEditEvent.ExerciseMoved(exercise.id, MoveDirection.DOWN)
                                    )
                                },
                                onRemove = {
                                    viewModel.onEvent(GroupEditEvent.ExerciseToggled(exercise.id))
                                }
                            )
                        }
                    }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                viewModel.onEvent(GroupEditEvent.SearchQueryChanged(it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (state.selectedExercises.isNotEmpty()) 8.dp else 0.dp),
                            placeholder = { Text("Search exercises...") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_symbol_search),
                                    "Search"
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.onEvent(
                                                GroupEditEvent.SearchQueryChanged("")
                                            )
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = R.drawable.ic_symbol_clear
                                            ),
                                            "Clear"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }

                    // No exercises note, when the user has not created exercises
                    if (unselectedExercises.isEmpty() && state.selectedExercises.isEmpty()) {
                        item {
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
                        }
                    } else {
                        items(
                            items = unselectedExercises,
                            key = { "available_${it.id}" }
                        ) { exercise ->
                            ExerciseCheckItem(
                                exercise = exercise,
                                isSelected = false,
                                onToggle = {
                                    viewModel.onEvent(
                                        GroupEditEvent.ExerciseToggled(exercise.id)
                                    )
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
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (state.isEditing) "Save Changes" else "Create Group")
            }

            // Delete button — only shown when editing an existing group
            if (state.isEditing) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_symbol_delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Group")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete \"${state.groupName}\"? The exercises in this group will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onEvent(GroupEditEvent.DeleteGroup)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SelectedExerciseItem(
    exercise: Exercise,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_arrow_upward),
                    contentDescription = "Move up"
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_arrow_downward),
                    contentDescription = "Move down"
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_close),
                    contentDescription = "Remove"
                )
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
