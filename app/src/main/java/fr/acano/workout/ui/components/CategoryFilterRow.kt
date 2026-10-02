package fr.acano.workout.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.ui.common.label
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Filtre par famille, en pastilles défilantes : « Tous », « Pectoraux »,
 * « Dos »… Toucher la famille déjà choisie revient à « Tous ».
 */
@Composable
fun CategoryFilterRow(
    categories: List<ExerciseCategory>,
    selected: ExerciseCategory?,
    onSelect: (ExerciseCategory?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.exercises_filter_all)) },
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category.takeUnless { it == selected }) },
                label = { Text(category.label) },
            )
        }
    }
}
