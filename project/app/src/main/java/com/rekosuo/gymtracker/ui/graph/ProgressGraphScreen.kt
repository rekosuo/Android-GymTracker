package com.rekosuo.gymtracker.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Progress Graph Screen - Displays exercise performance over time.
 *
 * Uses Jetpack Compose Canvas for custom chart rendering, following
 * Android's recommendation to use Canvas for custom graphics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressGraphScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPerformanceEntry: (Long, Long) -> Unit,
    viewModel: ProgressGraphViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.exerciseName.ifEmpty { "Progress" },
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

                // Time range selector
                TimeRangeSelector(
                    selectedRange = state.timeRange,
                    onRangeSelected = { range ->
                        viewModel.onEvent(ProgressGraphEvent.SetTimeRange(range))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Graph area
                if (state.filteredDataPoints.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_symbol_show_chart),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No data yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Log some performances to see your progress",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Stats summary
                    StatsSummary(
                        dataPoints = state.filteredDataPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Line chart
                    LineChart(
                        dataPoints = state.filteredDataPoints,
                        displayMode = state.displayMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Add performance button
                Button(
                    onClick = { onNavigateToPerformanceEntry(state.exerciseId, 0L) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Add Performance")
                }
            }
        }
    }
}

/**
 * Time range selector using segmented buttons pattern.
 */
@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Summary statistics card showing key metrics.
 */
@Composable
fun StatsSummary(
    dataPoints: List<GraphDataPoint>,
    modifier: Modifier = Modifier
) {
    val maxWeight = dataPoints.maxOfOrNull { it.maxWeight } ?: 0f
    val latestWeight = dataPoints.lastOrNull()?.maxWeight ?: 0f
    val sessionCount = dataPoints.size

    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Best",
                value = "${maxWeight.formatWeight()} kg"
            )
            StatItem(
                label = "Latest",
                value = "${latestWeight.formatWeight()} kg"
            )
            StatItem(
                label = "Sessions",
                value = sessionCount.toString()
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Custom line chart using Compose Canvas.
 *
 * Canvas is the recommended approach for custom graphics in Compose,
 * providing direct access to drawing primitives with hardware acceleration.
 */
@Composable
fun LineChart(
    dataPoints: List<GraphDataPoint>,
    displayMode: WeightDisplayMode,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    Card(
        modifier = modifier
    ) {
        if (dataPoints.size < 2) {
            // Single point - show as centered value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${dataPoints.firstOrNull()?.maxWeight?.formatWeight() ?: 0} kg",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = "Only one session recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val paddingLeft = 50.dp.toPx()
                val paddingBottom = 40.dp.toPx()
                val paddingTop = 20.dp.toPx()
                val paddingRight = 20.dp.toPx()

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                // Get weight values based on display mode
                val weights = when (displayMode) {
                    WeightDisplayMode.MAX -> dataPoints.map { it.maxWeight }
                    WeightDisplayMode.AVERAGE -> dataPoints.map { it.avgWeight }
                    WeightDisplayMode.RANGE -> dataPoints.map { it.maxWeight }
                }

                val minWeight = (weights.minOrNull() ?: 0f) * 0.9f
                val maxWeight = (weights.maxOrNull() ?: 100f) * 1.1f
                val weightRange = maxWeight - minWeight

                val minDate = dataPoints.minOf { it.date }
                val maxDate = dataPoints.maxOf { it.date }
                val dateRange = (maxDate - minDate).coerceAtLeast(1)

                // Draw grid lines
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val y = paddingTop + (chartHeight * i / gridLineCount)
                    drawLine(
                        color = surfaceVariant,
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Y-axis labels
                    val weightValue = maxWeight - (weightRange * i / gridLineCount)
                    drawContext.canvas.nativeCanvas.drawText(
                        weightValue.formatWeight(),
                        paddingLeft - 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Draw the line path
                val path = Path()
                val points = mutableListOf<Offset>()

                dataPoints.forEachIndexed { index, point ->
                    val weight = when (displayMode) {
                        WeightDisplayMode.MAX -> point.maxWeight
                        WeightDisplayMode.AVERAGE -> point.avgWeight
                        WeightDisplayMode.RANGE -> point.maxWeight
                    }

                    val x = paddingLeft + (chartWidth * (point.date - minDate) / dateRange)
                    val y =
                        paddingTop + chartHeight - (chartHeight * (weight - minWeight) / weightRange)

                    points.add(Offset(x, y))

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Draw the line
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw data points
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                }

                // Draw X-axis date labels
                val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                val labelIndices = when {
                    dataPoints.size <= 4 -> dataPoints.indices.toList()
                    else -> listOf(0, dataPoints.size / 2, dataPoints.size - 1)
                }

                labelIndices.forEach { index ->
                    val point = dataPoints[index]
                    val x = paddingLeft + (chartWidth * (point.date - minDate) / dateRange)

                    drawContext.canvas.nativeCanvas.drawText(
                        dateFormat.format(Date(point.date)),
                        x,
                        size.height - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

/**
 * Formats weight value for display, removing unnecessary decimals.
 */
private fun Float.formatWeight(): String {
    return if (this == this.toLong().toFloat()) {
        this.toLong().toString()
    } else {
        "%.1f".format(this)
    }
}