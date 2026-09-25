package fr.acano.workout.data

import android.content.Context
import androidx.core.content.edit
import fr.acano.workout.domain.Sex
import fr.acano.workout.domain.UserProfile
import fr.acano.workout.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Préférences de l'application. Une poignée de valeurs simples : des
 * `SharedPreferences` suffisent, sans ajouter DataStore pour autant.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _weightUnit = MutableStateFlow(
        prefs.getString(KEY_WEIGHT_UNIT, null)
            ?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
            ?: WeightUnit.KG,
    )
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit { putString(KEY_WEIGHT_UNIT, unit.name) }
        _weightUnit.value = unit
    }

    private val _profile = MutableStateFlow(readProfile())

    /** Profil pour l'estimation des calories. Le poids est toujours stocké en kilos. */
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun setProfile(profile: UserProfile) {
        prefs.edit {
            putOrRemove(KEY_SEX, profile.sex?.name)
            if (profile.weightKg != null) putFloat(KEY_WEIGHT_KG, profile.weightKg.toFloat()) else remove(KEY_WEIGHT_KG)
            if (profile.heightCm != null) putInt(KEY_HEIGHT_CM, profile.heightCm) else remove(KEY_HEIGHT_CM)
            if (profile.birthYear != null) putInt(KEY_BIRTH_YEAR, profile.birthYear) else remove(KEY_BIRTH_YEAR)
        }
        _profile.value = profile
    }

    private fun readProfile() = UserProfile(
        sex = prefs.getString(KEY_SEX, null)?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
        weightKg = prefs.takeIf { it.contains(KEY_WEIGHT_KG) }?.getFloat(KEY_WEIGHT_KG, 0f)?.toDouble(),
        heightCm = prefs.takeIf { it.contains(KEY_HEIGHT_CM) }?.getInt(KEY_HEIGHT_CM, 0),
        birthYear = prefs.takeIf { it.contains(KEY_BIRTH_YEAR) }?.getInt(KEY_BIRTH_YEAR, 0),
    )

    private fun android.content.SharedPreferences.Editor.putOrRemove(key: String, value: String?) {
        if (value != null) putString(key, value) else remove(key)
    }

    private companion object {
        const val KEY_WEIGHT_UNIT = "weight_unit"
        const val KEY_SEX = "profile_sex"
        const val KEY_WEIGHT_KG = "profile_weight_kg"
        const val KEY_HEIGHT_CM = "profile_height_cm"
        const val KEY_BIRTH_YEAR = "profile_birth_year"
    }
}
