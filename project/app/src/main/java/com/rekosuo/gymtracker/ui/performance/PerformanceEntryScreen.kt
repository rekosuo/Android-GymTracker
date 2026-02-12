package com.rekosuo.gymtracker.ui.performance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.domain.model.WeightRow
import com.rekosuo.gymtracker.ui.components.PerformanceOptionsMenu

/**
 * Performance Entry Screen - The Dynamic Matrix Grid
 *
 * This screen allows users to log their workout sets in a matrix format where:
 * - Each row represents a weight
 * - Each column within a row represents a set at that weight
 * - The chronological order of sets is preserved across rows
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceEntryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onEditExercise: () -> Unit,
    viewModel: PerformanceEntryViewModel = hiltViewModel()
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
                title = {
                    Text(
                        text = state.exerciseName.ifEmpty { "Loading..." },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_symbol_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    PerformanceOptionsMenu(
                        onNavigateToGraph = onNavigateToGraph,
                        onNavigateToCalendar = onNavigateToCalendar,
                        onEditExercise = onEditExercise,
                        onDeletePerformance = {
                            viewModel.onEvent(PerformanceEntryEvent.DeletePerformance)
                        },
                        showDeleteOption = state.performanceId != 0L
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
                                    painter = painterResource(id = R.drawable.ic_symbol_close),
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Main content - Dynamic Matrix Grid
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Weight rows
                    itemsIndexed(
                        items = state.weightRows,
                        key = { index, _ -> "row_$index" }
                    ) { rowIndex, weightRow ->
                        WeightRowItem(
                            row = weightRow,
                            onWeightChange = { weight ->
                                viewModel.onEvent(
                                    PerformanceEntryEvent.UpdateWeight(rowIndex, weight)
                                )
                            },
                            onRepChange = { repIndex, reps ->
                                viewModel.onEvent(
                                    PerformanceEntryEvent.UpdateRep(rowIndex, repIndex, reps)
                                )
                            },
                            onAddRep = {
                                viewModel.onEvent(PerformanceEntryEvent.AddRepToRow(rowIndex))
                            },
                            onDeleteRep = { repIndex ->
                                viewModel.onEvent(
                                    PerformanceEntryEvent.DeleteRep(rowIndex, repIndex)
                                )
                            },
                            onDeleteRow = {
                                viewModel.onEvent(PerformanceEntryEvent.DeleteRow(rowIndex))
                            }
                        )
                    }

                    // Add new row button
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AddButton(
                                onClick = {
                                    viewModel.onEvent(PerformanceEntryEvent.AddWeightRow)
                                },
                                size = 48.dp
                            )
                        }
                    }

                    // Notes field
                    item {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = {
                                viewModel.onEvent(PerformanceEntryEvent.UpdateNotes(it))
                            },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // Save button
                Button(
                    onClick = { viewModel.onEvent(PerformanceEntryEvent.SavePerformance) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(if (state.performanceId == 0L) "Save Performance" else "Update Performance")
                }
            }
        }
    }
}

/**
 * A single row in the dynamic matrix grid.
 *
 * Displays:
 * - Editable weight input on the left
 * - Horizontally scrollable list of rep inputs
 * - Add button to add more reps
 * - Delete row button (shown on long press or via menu)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeightRowItem(
    row: WeightRow,
    onWeightChange: (Float) -> Unit,
    onRepChange: (repIndex: Int, reps: Int) -> Unit,
    onAddRep: () -> Unit,
    onDeleteRep: (repIndex: Int) -> Unit,
    onDeleteRow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weight input with delete option
        Box {
            WeightInput(
                weight = row.weight,
                onWeightChange = onWeightChange,
                onLongClick = { showRowMenu = true }
            )

            DropdownMenu(
                expanded = showRowMenu,
                onDismissRequest = { showRowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete Row") },
                    onClick = {
                        showRowMenu = false
                        onDeleteRow()
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

        // Rep inputs
        row.reps.forEachIndexed { repIndex, reps ->
            RepInput(
                reps = reps,
                onRepsChange = { newReps -> onRepChange(repIndex, newReps) },
                onDelete = { onDeleteRep(repIndex) }
            )
        }

        // Add rep button
        AddButton(
            onClick = onAddRep,
            size = 44.dp
        )
    }
}

/**
 * Editable weight input field styled as a card.
 * Supports long-click to trigger delete row menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeightInput(
    weight: Float,
    onWeightChange: (Float) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(weight) {
        mutableStateOf(if (weight == 0f) "" else weight.toString().removeSuffix(".0"))
    }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(72.dp)
            .height(48.dp)
            .combinedClickable(
                onClick = { /* Opens keyboard via BasicTextField focus */ },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        // Only allow valid decimal numbers
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            textValue = filtered
                            val parsedValue = filtered.toFloatOrNull()
                            if (parsedValue != null) {
                                onWeightChange(parsedValue)
                            } else if (filtered.isEmpty()) {
                                onWeightChange(0f)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (textValue.isEmpty() && !isFocused) {
                                Text(
                                    text = "0",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Editable rep input field.
 */
@Composable
fun RepInput(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(reps) {
        mutableStateOf(if (reps == 0) "" else reps.toString())
    }
    var isFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = modifier
                .size(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            onClick = { /* Opens keyboard */ }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        textValue = filtered
                        val parsedValue = filtered.toIntOrNull()
                        if (parsedValue != null) {
                            onRepsChange(parsedValue)
                        } else if (filtered.isEmpty()) {
                            onRepsChange(0)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (textValue.isEmpty() && !isFocused) {
                                Text(
                                    text = "0",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Long press menu for delete
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
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
}

/**
 * Circular add button with a plus icon.
 */
@Composable
fun AddButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_symbol_add),
            contentDescription = "Add",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}