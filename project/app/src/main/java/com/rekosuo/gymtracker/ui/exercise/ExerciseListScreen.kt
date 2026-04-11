package com.rekosuo.gymtracker.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.ui.components.ExerciseListItem
import com.rekosuo.gymtracker.ui.components.GroupListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onNavigateBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onNavigateToGraph: (Long) -> Unit,
    onNavigateToExerciseCalendar: (Long) -> Unit,
    onNavigateToAllCalendar: () -> Unit,
    onNavigateToGroupCalendar: (Long) -> Unit,
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
                // Menu button to open New Exercise and New Group
                actions = {
                    IconButton(onClick = onNavigateToAllCalendar) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_symbol_calendar_today),
                            contentDescription = "Calendar"
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(painter = painterResource(id = R.drawable.ic_symbol_more_vert), "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // New Exercise
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
                        // New Group
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
                                onNavigateToGroupCalendar = { onNavigateToGroupCalendar(group.id) },
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
                                onNavigateToExerciseCalendar = {
                                    onNavigateToExerciseCalendar(
                                        exercise.id
                                    )
                                },
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
                                        "No exercises or groups yet.\nTap the top menu button to create them!"
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
