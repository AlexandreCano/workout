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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.formatDayWithYear
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.components.LeadingBadge
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Historique.
 *
 * Une liste de lignes séparées par un filet, pas une pile de cartes : sur un
 * écran de consultation, chaque carte ajoute une frontière que l'œil doit
 * franchir. L'étoile en tête de ligne rappelle qu'une séance terminée compte.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    if (sessions.isEmpty()) {
        EmptyState(
            title = "Aucune séance",
            message = "Les séances terminées apparaîtront ici, avec leur détail.",
        )
        return
    }

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
            ScreenTitle("Historique")
            SectionHeader(if (sessions.size <= 1) "1 séance" else "${sessions.size} séances")
        }
        items(sessions, key = { it.sessionId }) { session ->
            SessionRow(session, onClick = { onOpenSession(session.sessionId) })
            RowDivider()
        }
    }
}

@Composable
private fun SessionRow(session: SessionSummaryRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = WorkoutTheme.spacing.lg),
    ) {
        LeadingBadge(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.padding(horizontal = WorkoutTheme.spacing.sm))

        Column(Modifier.weight(1f)) {
            Text(session.type.label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDayWithYear(session.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDuration(session.durationMillis),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${session.exerciseNames.size} exercices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Détail d'une séance : un bloc par exercice, la charge en évidence et les
 * répétitions série par série sur une seule ligne — « 12 · 12 · 11 · 10 ».
 */
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onBack)

        Text(
            text = state.type?.label.orEmpty(),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.xs))
        Text(
            text = buildString {
                append(formatDayWithYear(state.startedAt))
                append("  ·  ")
                append(
                    if (state.isInProgress) "en cours" else formatDuration(state.durationMillis),
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        state.lines.forEachIndexed { index, line ->
            if (index > 0) RowDivider()
            Column(Modifier.padding(vertical = WorkoutTheme.spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = line.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (line.weightKg != null) {
                        Text(
                            text = formatWeight(line.weightKg),
                            style = WorkoutTheme.emphasis.metricSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(WorkoutTheme.spacing.xs))
                Text(
                    text = line.sets.joinToString("  ·  ") { set ->
                        set.repetitions?.toString()
                            ?: set.durationSeconds?.let { "${it}s" }
                            ?: "—"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
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
