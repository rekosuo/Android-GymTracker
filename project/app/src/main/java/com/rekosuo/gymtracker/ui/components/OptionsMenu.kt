package com.rekosuo.gymtracker.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import com.rekosuo.gymtracker.R

/**
 * Unified options menu for exercises.
 *
 * Provides consistent navigation options across all screens:
 * - Toggle favorite status
 * - Navigate to progress graph
 * - Navigate to calendar view
 * - Edit the exercise
 * - Delete the exercise or performance entry
 *
 * Used in: ExerciseListScreen, HomeScreen, GroupContentsScreen
 *
 * @param exerciseName Name of the exercise (used in delete confirmation)
 * @param isFavorite Current favorite status
 * @param onToggleFavorite Toggle favorite status callback
 * @param onNavigateToGraph Navigate to progress graph
 * @param onNavigateToCalendar Navigate to calendar view
 * @param onEdit Navigate to edit exercise
 * @param onDelete Delete callback
 */

@Composable
fun ExerciseOptionsMenu(
    exerciseName: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Menu trigger button
    IconButton(onClick = { showMenu = true }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_symbol_more_vert),
            contentDescription = "Options"
        )
    }

    // Dropdown menu
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        // Favorite toggle
        DropdownMenuItem(
            text = {
                Text(if (isFavorite) "Remove from favorites" else "Add to favorites")
            },
            onClick = {
                showMenu = false
                onToggleFavorite()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        id = if (isFavorite)
                            R.drawable.ic_symbol_star
                        else
                            R.drawable.ic_symbol_star_border
                    ),
                    contentDescription = null
                )
            }
        )

        // Graph navigation
        DropdownMenuItem(
            text = { Text("Graph") },
            onClick = {
                showMenu = false
                onNavigateToGraph()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_show_chart),
                    contentDescription = null
                )
            }
        )

        // Calendar navigation
        DropdownMenuItem(
            text = { Text("Calendar") },
            onClick = {
                showMenu = false
                onNavigateToCalendar()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_calendar_today),
                    contentDescription = null
                )
            }
        )

        // Edit
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                showMenu = false
                onEdit()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_edit),
                    contentDescription = null
                )
            }
        )

        // Delete
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                showMenu = false
                showDeleteDialog = true
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_delete),
                    contentDescription = null
                )
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise") },
            text = { Text("Are you sure you want to delete \"$exerciseName\"? This will also delete all performance data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
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

/**
 * Options menu for exercise groups.
 *
 * Groups have different options than exercises (no graph/calendar).
 *
 * @param groupName Name of the group (used in delete confirmation)
 * @param isFavorite Current favorite status
 * @param onToggleFavorite Toggle favorite status callback
 * @param onEdit Navigate to edit group
 * @param onDelete Delete group callback
 */
@Composable
fun GroupOptionsMenu(
    groupName: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Menu trigger button
    IconButton(onClick = { showMenu = true }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_symbol_more_vert),
            contentDescription = "Options"
        )
    }

    // Dropdown menu
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = {
                Text(if (isFavorite) "Remove from favorites" else "Add to favorites")
            },
            onClick = {
                showMenu = false
                onToggleFavorite()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        id = if (isFavorite)
                            R.drawable.ic_symbol_star
                        else
                            R.drawable.ic_symbol_star_border
                    ),
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                showMenu = false
                onEdit()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_edit),
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                showMenu = false
                showDeleteDialog = true
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_delete),
                    contentDescription = null
                )
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete \"$groupName\"? The exercises in this group will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
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

/**
 * Options menu for the Performance Entry screen.
 *
 * Provides navigation to related views and performance-specific actions.
 *
 * Used in: PerformanceEntryScreen
 *
 * @param onNavigateToGraph Navigate to the progress graph view
 * @param onNavigateToCalendar Navigate to the calendar view
 * @param onEditExercise Navigate to edit the current exercise
 * @param onDeletePerformance Delete the current performance entry
 * @param showDeleteOption Whether to show the delete option (false for new entries)
 */
@Composable
fun PerformanceOptionsMenu(
    onNavigateToGraph: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onEditExercise: () -> Unit,
    onDeletePerformance: () -> Unit,
    showDeleteOption: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Menu trigger button
    IconButton(onClick = { showMenu = true }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_symbol_more_vert),
            contentDescription = "Options"
        )
    }

    // Dropdown menu
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Graph") },
            onClick = {
                showMenu = false
                onNavigateToGraph()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_show_chart),
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Calendar") },
            onClick = {
                showMenu = false
                onNavigateToCalendar()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_calendar_today),
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Edit Exercise") },
            onClick = {
                showMenu = false
                onEditExercise()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_edit),
                    contentDescription = null
                )
            }
        )
        if (showDeleteOption) {
            DropdownMenuItem(
                text = { Text("Delete Entry") },
                onClick = {
                    showMenu = false
                    showDeleteDialog = true
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_symbol_delete),
                        contentDescription = null
                    )
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Performance Entry") },
            text = { Text("Are you sure you want to delete this performance entry? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePerformance()
                    }
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
