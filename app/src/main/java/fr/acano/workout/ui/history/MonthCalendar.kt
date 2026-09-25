package fr.acano.workout.ui.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.ui.common.locale
import fr.acano.workout.ui.common.pattern
import fr.acano.workout.ui.theme.WorkoutTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * Calendrier du mois : les jours d'entraînement portent une pastille pleine,
 * aujourd'hui un simple contour. Toucher un jour marqué filtre l'historique
 * sur ses séances ; seuls ces jours-là sont cliquables.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    weeks: List<List<LocalDate?>>,
    sessionsPerDay: Map<LocalDate, Int>,
    selectedDay: LocalDate?,
    today: LocalDate,
    canGoToPreviousMonth: Boolean,
    canGoToNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = context.locale
    // La semaine commence le lundi ; les initiales suivent la langue (« L M M J V S D », « M T W T F S S »).
    val weekDays = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.format(pattern(context, R.string.date_pattern_month_year)).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPreviousMonth, enabled = canGoToPreviousMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.history_previous_month))
            }
            IconButton(onClick = onNextMonth, enabled = canGoToNextMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.history_next_month))
            }
        }

        Row(Modifier.padding(top = WorkoutTheme.spacing.sm)) {
            weekDays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { },
                )
            }
        }

        weeks.forEach { week ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                week.forEach { day ->
                    // Des cases moins hautes que larges : le mois tient sans pousser la liste hors de l'écran.
                    Box(Modifier.weight(1f).height(CELL_HEIGHT), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                sessions = sessionsPerDay[day] ?: 0,
                                selected = day == selectedDay,
                                isToday = day == today,
                                onClick = { onDayClick(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    sessions: Int,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val trained = sessions > 0
    val colors = MaterialTheme.colorScheme
    val container = when {
        selected -> colors.primary
        trained -> colors.primaryContainer
        else -> colors.surface.copy(alpha = 0f)
    }
    val content = when {
        selected -> colors.onPrimary
        trained -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val spokenDay = day.format(pattern(LocalContext.current, R.string.date_pattern_spoken_day))
    val spokenSessions = if (sessions > 0) {
        pluralStringResource(R.plurals.history_day_sessions, sessions, spokenDay, sessions)
    } else {
        spokenDay
    }
    val todayLabel = stringResource(R.string.date_today)
    val spoken = if (isToday) "$spokenSessions, $todayLabel" else spokenSessions

    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier
            .size(CELL_HEIGHT - 4.dp)
            .clip(CircleShape)
            .then(
                if (isToday && !trained) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier,
            )
            .then(if (trained) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                if (trained) {
                    role = Role.Button
                    this.selected = selected
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (trained || isToday) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private val CELL_HEIGHT = 44.dp
