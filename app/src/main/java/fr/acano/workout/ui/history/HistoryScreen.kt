package fr.acano.workout.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.AppCard
import fr.acano.workout.ui.common.SectionTitle
import fr.acano.workout.ui.common.formatDayWithYear
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatWeight

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    if (sessions.isEmpty()) {
        EmptyState("Aucune séance terminée pour l'instant.")
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Historique",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(sessions, key = { it.sessionId }) { session ->
            AppCard(Modifier.clickable { onOpenSession(session.sessionId) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            session.type.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatDuration(session.durationMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatDayWithYear(session.startedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (session.exerciseNames.isNotEmpty()) {
                        Text(
                            session.exerciseNames.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

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
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
            }
            Text(
                state.type?.label.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Text(
            text = buildString {
                append(formatDayWithYear(state.startedAt))
                if (!state.isInProgress) {
                    append("  ·  ")
                    append(formatDuration(state.durationMillis))
                } else {
                    append("  ·  en cours")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )

        Spacer(Modifier.height(18.dp))

        state.lines.forEach { line ->
            AppCard(Modifier.padding(bottom = 10.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            line.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (line.weightKg != null) {
                            Text(
                                formatWeight(line.weightKg),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    SectionTitle("${line.sets.size} série(s)", Modifier.padding(top = 6.dp))
                    Text(
                        text = line.sets.joinToString("  /  ") { set ->
                            set.repetitions?.toString()
                                ?: set.durationSeconds?.let { "${it}s" }
                                ?: "—"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
