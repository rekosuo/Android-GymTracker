package com.rekosuo.gymtracker.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rekosuo.gymtracker.R
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
                CalendarGrid(yearMonth = state.currentMonth)
            }
        }
    }
}

@Composable
fun MonthArrowBar(

) {

}

@Composable
fun YearMonthHeader(

) {

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
    // onDayClicked: () -> Unit
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
                        .height(42.dp)
                )
            }
        )

        // Generate all days for the month.
        val days = 1..lastDayOfMonth
        for (day in days) {
            item {
                DayCell(day = day)
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
                        .height(42.dp)
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
    isHighlighted: Boolean = false
    // onClick: () -> Unit
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

@Composable
fun DayEntriesDialog(

) {

}

@Composable
fun DayEntryCard(

) {

}