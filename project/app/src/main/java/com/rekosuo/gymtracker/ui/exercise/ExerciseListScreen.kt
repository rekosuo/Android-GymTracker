package com.rekosuo.gymtracker.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.ui.components.ExerciseListItem
import com.rekosuo.gymtracker.ui.components.GroupListItem
import com.rekosuo.gymtracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onNavigateBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onNavigateToGraph: (Long) -> Unit,
    onNavigateToCalendar: (Long) -> Unit,
    onCreateGroup: () -> Unit,
    onEditGroup: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises & Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_symbol_arrow_back),
                            "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(painter = painterResource(id = R.drawable.ic_symbol_more_vert), "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Exercise") },
                            onClick = {
                                showMenu = false
                                onCreateExercise()
                            },
                            leadingIcon = {
                                Icon(painter = painterResource(id = R.drawable.ic_symbol_add), null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Group") },
                            onClick = {
                                showMenu = false
                                onCreateGroup()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_symbol_create_new_folder),
                                    null
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateExercise) {
                Icon(painter = painterResource(id = R.drawable.ic_symbol_add), "Add Exercise")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = {
                    viewModel.onEvent(ExerciseListEvent.SearchQueryChanged(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search exercises and groups...") },
                leadingIcon = {
                    Icon(painter = painterResource(id = R.drawable.ic_symbol_search), "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(ExerciseListEvent.SearchQueryChanged(""))
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

            // Error message
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                painter = painterResource(id = R.drawable.ic_symbol_close),
                                "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Content
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Groups section
                    if (state.groups.isNotEmpty()) {
                        item {
                            Text(
                                text = "Groups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = state.groups,
                            key = { "group_${it.id}" }
                        ) { group ->
                            GroupListItem(
                                group = group,
                                onClick = { onGroupClick(group.id) },
                                onEdit = { onEditGroup(group.id) },
                                onToggleFavorite = {
                                    viewModel.onEvent(ExerciseListEvent.ToggleFavoriteGroup(group))
                                },
                                onDelete = {
                                    viewModel.onEvent(ExerciseListEvent.DeleteGroup(group))
                                }
                            )
                        }
                    }

                    // Exercises section
                    if (state.exercises.isNotEmpty()) {
                        item {
                            Text(
                                text = "Exercises",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = state.exercises,
                            key = { "exercise_${it.id}" }
                        ) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onExerciseClick(exercise.id) },
                                onEdit = { onEditExercise(exercise.id) },
                                onToggleFavorite = {
                                    viewModel.onEvent(ExerciseListEvent.ToggleFavorite(exercise))
                                },
                                onNavigateToGraph = { onNavigateToGraph(exercise.id) },
                                onNavigateToCalendar = { onNavigateToCalendar(exercise.id) },
                                onDelete = {
                                    viewModel.onEvent(ExerciseListEvent.DeleteExercise(exercise))
                                }
                            )
                        }
                    }

                    // Empty state
                    if (state.exercises.isEmpty() && state.groups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.searchQuery.isEmpty()) {
                                        "No exercises or groups yet.\nTap + to create one!"
                                    } else {
                                        "No results found for \"${state.searchQuery}\""
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
