package fr.acano.workout.domain

/*
 * Ce qui décrit un exercice au-delà de son nom : la zone travaillée, les
 * muscles sollicités et le matériel. Les valeurs reprennent celles du
 * catalogue (`resources/catalog/exercises.json`) ; leurs libellés vivent dans
 * les ressources (voir `ExerciseLabels`).
 *
 * L'ordre de déclaration est l'ordre d'affichage.
 */

/** Grande famille d'un exercice : c'est elle qui sert de filtre dans le catalogue. */
enum class ExerciseCategory {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    LEGS,
    GLUTES,
    CALVES,
    CORE,
    TRAPS,
    FOREARMS,
    FULL_BODY,
    CARDIO,
}

enum class Muscle {
    CHEST,
    UPPER_CHEST,
    LOWER_CHEST,
    SHOULDERS,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    ROTATOR_CUFF,
    BACK,
    LATS,
    MID_BACK,
    LOWER_BACK,
    TRAPS,
    ARMS,
    BICEPS,
    BICEPS_LONG_HEAD,
    BICEPS_SHORT_HEAD,
    BRACHIALIS,
    BRACHIORADIALIS,
    TRICEPS,
    TRICEPS_LONG_HEAD,
    FOREARMS,
    CORE,
    ABS,
    OBLIQUES,
    TRANSVERSE_ABDOMINIS,
    HIP_FLEXORS,
    LEGS,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    GLUTES,
    GLUTE_MEDIUS,
    CALVES,
    SOLEUS,
    POSTERIOR_CHAIN,
    FULL_BODY,
    CARDIO,
}

enum class Equipment {
    MACHINE,
    CABLE,
    SMITH_MACHINE,
    BARBELL,
    EZ_BAR,
    TRAP_BAR,
    DUMBBELL,
    KETTLEBELL,
    PLATE,
    BODYWEIGHT,
    AB_WHEEL,
    CARDIO_MACHINE,
    SLED,
    BATTLE_ROPE,
    JUMP_ROPE,
    OTHER,
}
