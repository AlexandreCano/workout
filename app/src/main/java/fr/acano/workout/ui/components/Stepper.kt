package fr.acano.workout.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.acano.workout.R

/** Une ligne « libellé   −  valeur  + ». Le libellé est répété dans les descriptions pour TalkBack. */
@Composable
fun StepperRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Sur une ligne : « Distance » coupé en « Distanc / e » se lit mal.
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDecrement, enabled = canDecrement) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.comp_stepper_decrease, label))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            // Assez large pour « 1 min 30 » sans retour à la ligne, en laissant
            // au libellé la place de « Distance » dans une boîte de dialogue.
            modifier = Modifier.width(100.dp),
        )
        IconButton(onClick = onIncrement, enabled = canIncrement) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.comp_stepper_increase, label))
        }
    }
}

/** « 45 s », « 1 min 30 », « 5 min » : lisible d'un coup d'œil dans un compteur étroit. */
fun durationLabel(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0 -> "$rest s"
        rest == 0 -> "$minutes min"
        else -> "$minutes min %02d".format(rest)
    }
}

/** Pas fin sur les durées courtes (planche, repos), à la minute au-delà de 2 min (vélo). */
fun nextDuration(seconds: Int): Int = if (seconds < 120) seconds + 15 else seconds + 60

fun previousDuration(seconds: Int): Int = if (seconds <= 120) seconds - 15 else seconds - 60
