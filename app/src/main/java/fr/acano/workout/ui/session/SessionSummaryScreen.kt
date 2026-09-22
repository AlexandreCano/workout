package fr.acano.workout.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.AppCard
import fr.acano.workout.ui.common.PrimaryActionButton
import fr.acano.workout.ui.common.SectionTitle
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.history.SessionDetailViewModel

@Composable
fun SessionSummaryScreen(
    viewModel: SessionDetailViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.finishIfNeeded() }
    BackHandler { onDone() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            Text("🎉", style = MaterialTheme.typography.displayMedium)
            Text(
                "Séance terminée",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "⭐ +1",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = buildString {
                    append(state.type?.label.orEmpty())
                    append("  ·  ")
                    append(formatDuration(state.durationMillis))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(28.dp))

            SectionTitle("Résumé", Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))

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
                        Text(
                            text = line.sets.joinToString("  /  ") { set ->
                                set.repetitions?.toString()
                                    ?: set.durationSeconds?.let { "${it}s" }
                                    ?: "—"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryActionButton(text = "TERMINER", onClick = onDone)

            Spacer(Modifier.height(32.dp))
        }
    }
}
