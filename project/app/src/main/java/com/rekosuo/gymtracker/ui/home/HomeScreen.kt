package com.rekosuo.gymtracker.ui.home

import androidx.compose.foundation.clickable
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
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.ui.components.ExerciseListItem
import com.rekosuo.gymtracker.ui.components.GroupListItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExerciseList: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onNavigateToGraph: (Long) -> Unit,
    onNavigateToCalendar: (Long) -> Unit,
    onEditExercise: (Long) -> Unit,
    onEditGroup: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Progress Tracker") },
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick access button
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToExerciseList)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_symbol_fitness_center),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "View All Exercises",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Browse and manage your exercises",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_symbol_arrow_forward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Favorite exercises section
                if (state.favoriteExercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(
                        items = state.favoriteExercises,
                        key = { "fav_${it.id}" }
                    ) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            onClick = { onExerciseClick(exercise.id) },
                            onEdit = { onEditExercise(exercise.id) },
                            onToggleFavorite = {
                                viewModel.onEvent(HomeEvent.ToggleFavoriteExercise(exercise))
                            },
                            onNavigateToGraph = { onNavigateToGraph(exercise.id) },
                            onNavigateToCalendar = { onNavigateToCalendar(exercise.id) },
                            onDelete = {
                                viewModel.onEvent(HomeEvent.DeleteExercise(exercise))
                            }
                        )
                    }
                }

                // Favorite groups section
                if (state.favoriteGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorite Groups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(
                        items = state.favoriteGroups,
                        key = { "group_${it.id}" }
                    ) { group ->
                        GroupListItem(
                            group = group,
                            onClick = { onGroupClick(group.id) },
                            onEdit = { onEditGroup(group.id) },
                            onToggleFavorite = {
                                viewModel.onEvent(HomeEvent.ToggleFavoriteGroup(group))
                            },
                            onDelete = {
                                viewModel.onEvent(HomeEvent.DeleteGroup(group))
                            }
                        )
                    }
                }

                // Empty state
                if (state.favoriteExercises.isEmpty() && state.favoriteGroups.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_symbol_fitness_center),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No exercises yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the + button to create your first exercise",
                                    style = MaterialTheme.typography.bodyMedium,
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
