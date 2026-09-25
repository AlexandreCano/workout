package fr.acano.workout.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
import fr.acano.workout.data.Settings
import fr.acano.workout.domain.Sex
import fr.acano.workout.domain.UserProfile
import fr.acano.workout.domain.WeightUnit
import fr.acano.workout.ui.common.formatNumber
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.history.BackRow
import fr.acano.workout.ui.history.ScreenTitle
import fr.acano.workout.ui.theme.WorkoutTheme

/** Réglages de l'application : l'unité de poids, et le profil qui sert à estimer les calories. */
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    val unit by settings.weightUnit.collectAsStateWithLifecycle()
    val profile by settings.profile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Avant le défilement : la zone visible rétrécit au-dessus du clavier,
            // et le champ en cours de saisie y est ramené au lieu d'être recouvert.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onBack)
        ScreenTitle(stringResource(R.string.settings_title))

        SectionHeader(stringResource(R.string.settings_weight_unit))
        val options = listOf(
            WeightUnit.KG to stringResource(R.string.settings_unit_kg),
            WeightUnit.LB to stringResource(R.string.settings_unit_lb),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = unit == option,
                    onClick = { settings.setWeightUnit(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        Text(
            text = stringResource(R.string.settings_unit_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        ProfileSection(profile, unit, onChange = settings::setProfile)
        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
    }
}

/**
 * Le profil, pour estimer les calories des exercices de l'application. Chaque
 * champ s'enregistre dès qu'il est valide ; un champ vidé est oublié.
 */
@Composable
private fun ProfileSection(profile: UserProfile, unit: WeightUnit, onChange: (UserProfile) -> Unit) {
    SectionHeader(stringResource(R.string.settings_profile))
    Text(
        text = stringResource(R.string.settings_profile_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(WorkoutTheme.spacing.lg))

    val sexes = listOf(
        Sex.MALE to stringResource(R.string.settings_sex_male),
        Sex.FEMALE to stringResource(R.string.settings_sex_female),
    )
    Text(stringResource(R.string.settings_sex), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(WorkoutTheme.spacing.xs))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        sexes.forEachIndexed { index, (sex, label) ->
            SegmentedButton(
                selected = profile.sex == sex,
                // Toucher le choix actif le retire : le champ reste facultatif.
                onClick = { onChange(profile.copy(sex = sex.takeUnless { profile.sex == it })) },
                shape = SegmentedButtonDefaults.itemShape(index, sexes.size),
            ) { Text(label, maxLines = 1) }
        }
    }
    Spacer(Modifier.height(WorkoutTheme.spacing.lg))

    // Le poids se saisit dans l'unité choisie mais reste stocké en kilos.
    val weightRange = unit.fromKg(20.0)..unit.fromKg(300.0)
    NumberField(
        label = stringResource(R.string.settings_weight_label, unit.symbol),
        initial = profile.weightKg?.let { formatNumber(unit.fromKg(it), decimals = 1) }.orEmpty(),
        decimal = true,
        key = unit,
        isValid = { it in weightRange },
        error = stringResource(
            R.string.settings_weight_error,
            formatNumber(weightRange.start, 0),
            formatNumber(weightRange.endInclusive, 0),
            unit.symbol,
        ),
        onValue = { value -> onChange(profile.copy(weightKg = value?.let(unit::toKg))) },
    )
    Spacer(Modifier.height(WorkoutTheme.spacing.md))
    NumberField(
        label = stringResource(R.string.settings_height_label),
        initial = profile.heightCm?.toString().orEmpty(),
        isValid = { it in 100.0..250.0 },
        error = stringResource(R.string.settings_height_error, 100, 250),
        onValue = { value -> onChange(profile.copy(heightCm = value?.toInt())) },
    )
    Spacer(Modifier.height(WorkoutTheme.spacing.md))
    NumberField(
        label = stringResource(R.string.settings_birth_year_label),
        initial = profile.birthYear?.toString().orEmpty(),
        isValid = { it in 1900.0..2020.0 },
        error = stringResource(R.string.settings_birth_year_error, 1900, 2020),
        onValue = { value -> onChange(profile.copy(birthYear = value?.toInt())) },
    )
}

/**
 * Champ numérique qui garde le texte tapé tel quel (virgule comprise) et ne
 * transmet qu'une valeur plausible — null quand il est vidé. Une saisie hors
 * bornes n'est pas enregistrée, et le champ dit pourquoi au lieu de l'ignorer
 * en silence.
 */
@Composable
private fun NumberField(
    label: String,
    initial: String,
    onValue: (Double?) -> Unit,
    isValid: (Double) -> Boolean,
    error: String,
    decimal: Boolean = false,
    key: Any? = null,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    val parsed = text.replace(',', '.').toDoubleOrNull()
    val invalid = text.isNotEmpty() && (parsed == null || !isValid(parsed))
    // Le message d'erreur s'affiche sous le champ : on le ramène au-dessus du clavier.
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(invalid) { if (invalid) bringIntoView.bringIntoView() }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(6)
            text = cleaned
            val value = cleaned.replace(',', '.').toDoubleOrNull()
            when {
                cleaned.isEmpty() -> onValue(null)
                value != null && isValid(value) -> onValue(value)
                // Sinon on garde la dernière valeur valide : une frappe en cours ne l'efface pas.
            }
        },
        label = { Text(label) },
        isError = invalid,
        supportingText = if (invalid) ({ Text(error) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView),
    )
}
