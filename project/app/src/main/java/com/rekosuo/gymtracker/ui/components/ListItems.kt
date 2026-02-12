package com.rekosuo.gymtracker.ui.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rekosuo.gymtracker.R
import com.rekosuo.gymtracker.domain.model.Exercise
import com.rekosuo.gymtracker.domain.model.ExerciseGroup

/**
 * Reusable Exercise list item component
 * Used in ExerciseListScreen, HomeScreen, and GroupContentsScreen
 * @param exercise The exercise to display
 * @param onClick Called when the item is tapped (navigate to performance entry)
 * @param onToggleFavorite Toggle favorite status
 * @param onNavigateToGraph Navigate to progress graph for this exercise
 * @param onNavigateToCalendar Navigate to calendar view for this exercise
 * @param onEdit Navigate to edit this exercise
 * @param onDelete Delete this exercise
 */
@Composable
fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (exercise.isFavorite) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_symbol_star),
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            ExerciseOptionsMenu(
                exerciseName = exercise.name,
                isFavorite = exercise.isFavorite,
                onToggleFavorite = onToggleFavorite,
                onNavigateToGraph = onNavigateToGraph,
                onNavigateToCalendar = onNavigateToCalendar,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

/**
 * Reusable Group list item component
 * Used in ExerciseListScreen and HomeScreen
 */
@Composable
fun GroupListItem(
    group: ExerciseGroup,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_symbol_folder),
                    contentDescription = "Group",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (group.isFavorite) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_symbol_star),
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(start = 8.dp)
                    )
                }
            }

            GroupOptionsMenu(
                groupName = group.name,
                isFavorite = group.isFavorite,
                onToggleFavorite = onToggleFavorite,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}
