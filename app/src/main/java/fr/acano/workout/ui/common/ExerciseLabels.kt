package fr.acano.workout.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.Muscle

/*
 * Libellés des détails d'un exercice, dans la langue de l'interface. Une
 * correspondance explicite plutôt qu'une recherche de ressource par son nom :
 * le compilateur signale toute valeur oubliée.
 */

val ExerciseCategory.label: String
    @Composable get() = stringResource(labelRes)

val Muscle.label: String
    @Composable get() = stringResource(labelRes)

val Equipment.label: String
    @Composable get() = stringResource(labelRes)

val ExerciseCategory.labelRes: Int
    get() = when (this) {
        ExerciseCategory.CHEST -> R.string.category_chest
        ExerciseCategory.BACK -> R.string.category_back
        ExerciseCategory.SHOULDERS -> R.string.category_shoulders
        ExerciseCategory.TRAPS -> R.string.category_traps
        ExerciseCategory.BICEPS -> R.string.category_biceps
        ExerciseCategory.TRICEPS -> R.string.category_triceps
        ExerciseCategory.FOREARMS -> R.string.category_forearms
        ExerciseCategory.CORE -> R.string.category_core
        ExerciseCategory.LEGS -> R.string.category_legs
        ExerciseCategory.GLUTES -> R.string.category_glutes
        ExerciseCategory.CALVES -> R.string.category_calves
        ExerciseCategory.FULL_BODY -> R.string.category_full_body
        ExerciseCategory.CARDIO -> R.string.category_cardio
    }

val Muscle.labelRes: Int
    get() = when (this) {
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.UPPER_CHEST -> R.string.muscle_upper_chest
        Muscle.LOWER_CHEST -> R.string.muscle_lower_chest
        Muscle.SHOULDERS -> R.string.muscle_shoulders
        Muscle.FRONT_DELTS -> R.string.muscle_front_delts
        Muscle.SIDE_DELTS -> R.string.muscle_side_delts
        Muscle.REAR_DELTS -> R.string.muscle_rear_delts
        Muscle.ROTATOR_CUFF -> R.string.muscle_rotator_cuff
        Muscle.BACK -> R.string.muscle_back
        Muscle.LATS -> R.string.muscle_lats
        Muscle.MID_BACK -> R.string.muscle_mid_back
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.TRAPS -> R.string.muscle_traps
        Muscle.ARMS -> R.string.muscle_arms
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.BICEPS_LONG_HEAD -> R.string.muscle_biceps_long_head
        Muscle.BICEPS_SHORT_HEAD -> R.string.muscle_biceps_short_head
        Muscle.BRACHIALIS -> R.string.muscle_brachialis
        Muscle.BRACHIORADIALIS -> R.string.muscle_brachioradialis
        Muscle.TRICEPS -> R.string.muscle_triceps
        Muscle.TRICEPS_LONG_HEAD -> R.string.muscle_triceps_long_head
        Muscle.FOREARMS -> R.string.muscle_forearms
        Muscle.CORE -> R.string.muscle_core
        Muscle.ABS -> R.string.muscle_abs
        Muscle.OBLIQUES -> R.string.muscle_obliques
        Muscle.TRANSVERSE_ABDOMINIS -> R.string.muscle_transverse_abdominis
        Muscle.HIP_FLEXORS -> R.string.muscle_hip_flexors
        Muscle.LEGS -> R.string.muscle_legs
        Muscle.QUADS -> R.string.muscle_quads
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.ADDUCTORS -> R.string.muscle_adductors
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.GLUTE_MEDIUS -> R.string.muscle_glute_medius
        Muscle.CALVES -> R.string.muscle_calves
        Muscle.SOLEUS -> R.string.muscle_soleus
        Muscle.POSTERIOR_CHAIN -> R.string.muscle_posterior_chain
        Muscle.FULL_BODY -> R.string.muscle_full_body
        Muscle.CARDIO -> R.string.muscle_cardio
    }

val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.EZ_BAR -> R.string.equipment_ez_bar
        Equipment.TRAP_BAR -> R.string.equipment_trap_bar
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.PLATE -> R.string.equipment_plate
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.AB_WHEEL -> R.string.equipment_ab_wheel
        Equipment.CARDIO_MACHINE -> R.string.equipment_cardio_machine
        Equipment.SLED -> R.string.equipment_sled
        Equipment.BATTLE_ROPE -> R.string.equipment_battle_rope
        Equipment.JUMP_ROPE -> R.string.equipment_jump_rope
        Equipment.OTHER -> R.string.equipment_other
    }

/**
 * « Pectoraux · Machine » : ce qu'on cherche en parcourant le catalogue. Un
 * exercice de l'utilisateur sans détails se décrit par son type de suivi.
 */
@Composable
fun ExerciseEntity.summary(): String {
    val target = when (primaryMuscle) {
        // « Cardio · Machine de cardio » se répète : le matériel suffit.
        Muscle.CARDIO -> null
        null -> category?.label
        else -> primaryMuscle.label
    }
    val parts = listOfNotNull(target, equipment?.label)
    return if (parts.isEmpty()) kind.label else parts.joinToString("  ·  ")
}
