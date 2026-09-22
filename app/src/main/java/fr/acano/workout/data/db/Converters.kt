package fr.acano.workout.data.db

import androidx.room.TypeConverter
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.WorkoutType

class Converters {
    @TypeConverter fun fromWorkoutType(value: WorkoutType): String = value.name
    @TypeConverter fun toWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)

    @TypeConverter fun fromExerciseKind(value: ExerciseKind): String = value.name
    @TypeConverter fun toExerciseKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)
}
