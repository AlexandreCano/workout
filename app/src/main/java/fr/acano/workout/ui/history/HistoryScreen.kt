package fr.acano.workout.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatKcal
import fr.acano.workout.ui.common.formatLongDate
import fr.acano.workout.ui.common.formatVolume
import fr.acano.workout.ui.common.pattern
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.theme.WorkoutTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Historique.
 *
 * En tête, le calendrier du mois : les jours d'entraînement d'un coup d'œil,
 * et un raccourci pour ne lister que les séances d'un jour. Dessous, une
 * liste de lignes séparées par un filet, pas une pile de cartes : sur un
 * écran de consultation, chaque carte ajoute une frontière que l'œil doit
 * franchir. L'étoile en tête de ligne rappelle qu'une séance terminée compte.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) return

    LazyColumn(
        contentPadding = PaddingValues(
            start = WorkoutTheme.spacing.xl,
            end = WorkoutTheme.spacing.xl,
            top = WorkoutTheme.spacing.xl,
            bottom = WorkoutTheme.spacing.xxxl,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenTitle(stringResource(R.string.tab_history))
            MonthCalendar(
                month = state.month,
                weeks = state.weeks,
                sessionsPerDay = state.sessionsPerDay,
                selectedDay = state.selectedDay,
                today = LocalDate.now(),
                canGoToPreviousMonth = state.canGoToPreviousMonth,
                canGoToNextMonth = state.canGoToNextMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onDayClick = viewModel::toggleDay,
            )
            Text(
                text = when (state.sessionsInMonth) {
                    0 -> stringResource(R.string.history_no_sessions_this_month)
                    else -> pluralStringResource(R.plurals.history_sessions_this_month, state.sessionsInMonth, state.sessionsInMonth)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = WorkoutTheme.spacing.md),
            )
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            val selected = state.selectedDay
            if (state.totalSessions == 0) {
                // Le calendrier reste affiché même vide : il montre au moins où l'on en est.
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (selected != null) {
                // Motif long sans majuscule initiale : « Séances du mardi 22 septembre », « Sessions on Tuesday, September 22 ».
                val dayPattern = if (selected.year == LocalDate.now().year) R.string.date_pattern_long else R.string.date_pattern_long_year
                SectionHeader(
                    text = stringResource(R.string.history_sessions_on_day, selected.format(pattern(LocalContext.current, dayPattern))),
                    trailing = {
                        TextButton(onClick = viewModel::clearSelection) { Text(stringResource(R.string.history_show_all)) }
                    },
                )
            } else {
                // Le nombre de séances du mois est déjà sous le calendrier : pas de doublon ici.
                SectionHeader(stringResource(R.string.history_all_sessions))
            }
        }
        items(state.sessions, key = { it.sessionId }) { session ->
            SessionRow(session, onClick = { onOpenSession(session.sessionId) })
            RowDivider()
        }
    }
}

/**
 * Une séance dans la liste : la date en bloc à gauche — c'est par elle qu'on
 * cherche une séance —, l'entraînement et sa durée au centre, la charge totale
 * soulevée à droite.
 */
@Composable
private fun SessionRow(session: SessionSummaryRow, onClick: () -> Unit) {
    val day = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val monthFormatter = pattern(LocalContext.current, R.string.date_pattern_month_abbrev)
    val count = session.exerciseNames.size
    val exercisesLabel = pluralStringResource(R.plurals.history_exercise_count, count, count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = WorkoutTheme.spacing.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(56.dp).clearAndSetSemantics { },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = day.format(monthFormatter).trimEnd('.'),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(WorkoutTheme.spacing.lg))

        Column(Modifier.weight(1f)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(formatDuration(session.durationMillis))
                    append("  ·  ")
                    append(exercisesLabel)
                    session.kcal?.let { append("  ·  ${formatKcal(it)}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (session.volumeKg > 0) {
            Text(
                text = formatVolume(session.volumeKg, LocalWeightUnit.current),
                style = WorkoutTheme.emphasis.metricSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Détail d'une séance : un bloc par exercice, et pour chaque série la charge
 * et les répétitions réellement faites, une ligne par série — « 42,5 kg × 11 ».
 */
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inProgress = stringResource(R.string.history_in_progress)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onBack)

        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.xs))
        Text(
            text = buildString {
                append(formatLongDate(state.startedAt))
                append("  ·  ")
                append(
                    if (state.isInProgress) inProgress else formatDuration(state.durationMillis),
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.totalSets > 0) {
            Text(
                text = if (state.volumeKg > 0) {
                    pluralStringResource(
                        R.plurals.history_set_count_lifted,
                        state.totalSets,
                        state.totalSets,
                        formatVolume(state.volumeKg, LocalWeightUnit.current),
                    )
                } else {
                    pluralStringResource(R.plurals.history_set_count, state.totalSets, state.totalSets)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CaloriesLine(state.totalKcal, state.needsProfileForCalories)

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        state.lines.forEachIndexed { index, line ->
            if (index > 0) RowDivider()
            ExerciseSetsBreakdown(line)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxxl))
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier.padding(bottom = WorkoutTheme.spacing.xl),
    )
}

@Composable
fun BackRow(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = WorkoutTheme.spacing.sm, bottom = WorkoutTheme.spacing.md),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(end = WorkoutTheme.spacing.xs),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }
}

@Composable
fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WorkoutTheme.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/**
 * Les calories estimées d'une séance, ou une invitation à renseigner son poids
 * quand c'est lui qui manque. Rien du tout si la séance ne contient que des
 * exercices créés par l'utilisateur, faute de référence pour les estimer.
 */
@Composable
fun CaloriesLine(totalKcal: Double?, needsProfile: Boolean) {
    when {
        totalKcal != null -> Text(
            text = stringResource(R.string.history_kcal_burned, formatKcal(totalKcal)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        needsProfile -> Text(
            text = stringResource(R.string.history_kcal_needs_weight),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
