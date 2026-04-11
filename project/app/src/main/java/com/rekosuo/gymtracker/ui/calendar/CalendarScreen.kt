package com.rekosuo.gymtracker.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.domain.model.PerformanceSummary
import java.time.YearMonth
import java.time.temporal.ChronoField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title,
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
                }
            )
        }
    ) { padding ->
        if (state.showDayDialog) {
            PerformanceEntriesDialog(
                summaries = state.selectedSummaries,
                onDismiss = { viewModel.onEvent(CalendarScreenEvent.DismissDayDialog) }
            )
        }

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
                YearMonthHeader(yearMonth = state.currentMonth)
                MonthArrowBar(
                    direction = ArrowDirection.UP,
                    onClick = {
                        viewModel.onEvent(
                            CalendarScreenEvent.MonthChanged(
                                state.currentMonth.minusMonths(
                                    1
                                )
                            )
                        )
                    }
                )
                CalendarGrid(
                    yearMonth = state.currentMonth,
                    highlightedDays = state.highlightedDays,
                    onDayClicked = { day ->
                        viewModel.onEvent(CalendarScreenEvent.DaySelected(day))
                    }
                )
                MonthArrowBar(
                    modifier = Modifier.padding(vertical = 10.dp),
                    direction = ArrowDirection.DOWN,
                    onClick = {
                        viewModel.onEvent(
                            CalendarScreenEvent.MonthChanged(
                                state.currentMonth.plusMonths(
                                    1
                                )
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun YearMonthHeader(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth = YearMonth.now(),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp, vertical = 20.dp)
            .border(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                width = 2.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = modifier
                .width(160.dp)
                .fillMaxWidth()
                .padding(10.dp)
                .wrapContentWidth(align = Alignment.CenterHorizontally),
            text = yearMonth.toString(),
            fontSize = 24.sp
        )
    }
}

/**
 * Main calendar grid.
 * Dynamically generates DayCells at the right positions.
 * A row represents a week. Weeks start at Monday.
 */
@Composable
fun CalendarGrid(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth = YearMonth.now(),
    highlightedDays: Set<Int> = emptySet(),
    onDayClicked: (Int) -> Unit = {}
) {
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.get(ChronoField.DAY_OF_WEEK)
    val lastDayOfMonth = yearMonth.atEndOfMonth().dayOfMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7)
    ) {
        // Make sure that the first day of the month lands on the right weekday.
        items(
            count = firstDayOfWeek - 1,
            itemContent = {
                Spacer(
                    modifier = modifier
                        .width(32.dp)
                        .height(48.dp)
                )
            }
        )

        // Generate all days for the month.
        val days = 1..lastDayOfMonth
        for (day in days)
            if (day in highlightedDays) {
                item {
                    DayCell(
                        day = day,
                        isHighlighted = true,
                        onClick = { onDayClicked(day) }
                    )
                }
            } else {
                item {
                    DayCell(
                        day = day,
                        isHighlighted = false
                    )
                }
            }

        // Fill the end of the grid with empty days to keep grid height consistent.
        val remainingEmptyDays = 42 - (firstDayOfWeek - 1 + lastDayOfMonth)
        items(
            count = remainingEmptyDays,
            itemContent = {
                Spacer(
                    modifier = modifier
                        .width(32.dp)
                        .height(48.dp)
                )
            }
        )
    }
}

/**
 * Individual day in the calendar grid.
 */
@Composable
fun DayCell(
    modifier: Modifier = Modifier,
    day: Int = 1,
    isHighlighted: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .width(32.dp)
            .height(48.dp)
    ) {
        val mod = if (isHighlighted) {
            Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(
                    width = 3.dp,
                    shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface
                )
                .clickable(onClick = onClick)
        } else {
            Modifier
                .fillMaxSize()
                .padding(2.dp)
        }
        Card(
            modifier = mod,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = day.toString(),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Display this dialog upon clicking a highlighted day.
 * Displays available performance entry summaries for that day.
 */
@Composable
fun PerformanceEntriesDialog(
    summaries: List<PerformanceSummary>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout Summary") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                summaries.forEach { summary ->
                    PerformanceEntryCard(
                        summary = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Display one performance entry as a part of a PerformanceEntriesDialog.
 */
@Composable
fun PerformanceEntryCard(
    modifier: Modifier = Modifier,
    summary: PerformanceSummary
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = summary.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            summary.weightRows.forEach { weightRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${weightRow.weight} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "x ${weightRow.sets.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (summary.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class ArrowDirection { UP, DOWN }

@Composable
fun MonthArrowBar(
    direction: ArrowDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val icon = when (direction) {
        ArrowDirection.UP -> rememberChevronUp(color)
        ArrowDirection.DOWN -> rememberChevronDown(color)
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 50.dp)
            .fillMaxWidth()
            .border(width = 3.dp, shape = CircleShape, color = color),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (direction == ArrowDirection.UP)
                "Previous month" else "Next month",
            modifier = Modifier.size(width = 88.dp, height = 24.dp)
        )
    }
}

/**
 * A wide, sharp-edged chevron pointing upward.
 * The viewportWidth/viewportHeight control the coordinate space;
 * the actual rendered size is set via Modifier.size() at the call site.
 */
@Composable
fun rememberChevronUp(color: Color): ImageVector {
    return remember(color) {
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 88.dp,
            defaultHeight = 24.dp,
            viewportWidth = 88f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(color),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(4f, 20f)    // bottom-left
                lineTo(44f, 4f)    // top-center apex
                lineTo(84f, 20f)   // bottom-right
            }
        }.build()
    }
}

/**
 * The same chevron flipped vertically (pointing downward).
 */
@Composable
fun rememberChevronDown(color: Color): ImageVector {
    return remember(color) {
        ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 88.dp,
            defaultHeight = 24.dp,
            viewportWidth = 88f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(color),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(4f, 4f)     // top-left
                lineTo(44f, 20f)   // bottom-center apex
                lineTo(84f, 4f)    // top-right
            }
        }.build()
    }
}