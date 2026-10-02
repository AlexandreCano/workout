package fr.acano.workout.data.db

import androidx.room.TypeConverter
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import fr.acano.workout.domain.SetEffort
import fr.acano.workout.domain.WorkoutType

/**
 * Les détails d'un exercice (catégorie, muscles, matériel) sont lus avec
 * tolérance : une valeur inconnue — écrite par une version plus récente, par
 * exemple — devient « non renseignée » au lieu de faire planter la lecture.
 */
class Converters {
    @TypeConverter fun fromWorkoutType(value: WorkoutType): String = value.name
    @TypeConverter fun toWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)

    @TypeConverter fun fromExerciseKind(value: ExerciseKind): String = value.name
    @TypeConverter fun toExerciseKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)

    @TypeConverter fun fromCategory(value: ExerciseCategory?): String? = value?.name
    @TypeConverter fun toCategory(value: String?): ExerciseCategory? = value?.let { enumValueOrNull<ExerciseCategory>(it) }

    @TypeConverter fun fromMuscle(value: Muscle?): String? = value?.name
    @TypeConverter fun toMuscle(value: String?): Muscle? = value?.let { enumValueOrNull<Muscle>(it) }

    @TypeConverter fun fromEquipment(value: Equipment?): String? = value?.name
    @TypeConverter fun toEquipment(value: String?): Equipment? = value?.let { enumValueOrNull<Equipment>(it) }

    @TypeConverter fun fromEffort(value: SetEffort?): String? = value?.name
    @TypeConverter fun toEffort(value: String?): SetEffort? = value?.let { enumValueOrNull<SetEffort>(it) }

    /** Une liste de muscles tient dans une colonne : « TRICEPS,FRONT_DELTS ». */
    @TypeConverter fun fromMuscles(value: List<Muscle>): String = value.joinToString(",") { it.name }
    @TypeConverter fun toMuscles(value: String): List<Muscle> =
        value.split(',').filter { it.isNotBlank() }.mapNotNull { enumValueOrNull<Muscle>(it.trim()) }

    private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
