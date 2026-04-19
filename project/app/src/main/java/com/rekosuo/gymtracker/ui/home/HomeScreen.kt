package com.rekosuo.gymtracker.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.rekosuo.gymtracker.ui.settings.BackupEvent
import com.rekosuo.gymtracker.ui.settings.BackupMessage
import com.rekosuo.gymtracker.ui.settings.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExerciseList: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onNavigateToGraph: (Long) -> Unit,
    onNavigateToExerciseCalendar: (Long) -> Unit,
    onNavigateToAllCalendar: () -> Unit,
    onNavigateToGroupCalendar: (Long) -> Unit,
    onEditExercise: (Long) -> Unit,
    onEditGroup: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val backupState by backupViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) backupViewModel.onEvent(BackupEvent.ExportTo(uri))
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) backupViewModel.onEvent(BackupEvent.ImportFrom(uri))
    }

    LaunchedEffect(backupState.message) {
        val message = backupState.message ?: return@LaunchedEffect
        val text = when (message) {
            is BackupMessage.Success -> message.text
            is BackupMessage.Error -> message.text
        }
        snackbarHostState.showSnackbar(text)
        backupViewModel.onEvent(BackupEvent.DismissMessage)
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import data?") },
            text = {
                Text(
                    "Importing will replace ALL current data with the contents of " +
                        "the selected file. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gym Progress Tracker") },
                actions = {
                    IconButton(onClick = onNavigateToAllCalendar) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_symbol_calendar_today),
                            contentDescription = "Calendar"
                        )
                    }
                    HomeOverflowMenu(
                        isBusy = backupState.isBusy,
                        onExportClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                                .format(Date())
                            exportLauncher.launch("gymtracker-backup-$stamp.json")
                        },
                        onImportClick = { showImportConfirm = true },
                    )
                }
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
                            onNavigateToExerciseCalendar = { onNavigateToExerciseCalendar(exercise.id) },
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
                            onNavigateToGroupCalendar = { onNavigateToGroupCalendar(group.id) },
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
                                    text = "No favorite exercises yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Favorite exercises and groups to display them here.",
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

@Composable
private fun HomeOverflowMenu(
    isBusy: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, enabled = !isBusy) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_symbol_more_vert),
                contentDescription = "Options"
            )
        }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Export data") },
            onClick = { expanded = false; onExportClick() }
        )
        DropdownMenuItem(
            text = { Text("Import data") },
            onClick = { expanded = false; onImportClick() }
        )
    }
}
