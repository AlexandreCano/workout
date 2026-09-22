package fr.acano.workout.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.WorkoutType

/**
 * Catalogue d'exercices. L'identifiant est un slug stable (« chest_press ») :
 * il sert aussi de nom de fichier pour l'animation dans `assets/exercises/`.
 */
@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: ExerciseKind,
    val plannedSets: Int,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null,
    /** Repos automatique après une série, en secondes. 0 = pas de chrono. */
    val restSeconds: Int = 60,
    val weightStepKg: Double = 2.5,
)

/** Une séance. `endedAt == null` signifie « séance en cours » : c'est le seul marqueur de reprise. */
@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: WorkoutType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val starAwarded: Boolean = false,
)

/** Un exercice tel qu'il est planifié dans une séance donnée (ordre + poids prévu). */
@Entity(
    tableName = "exercise_session",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val position: Int,
    val plannedSets: Int,
    /** Pré-rempli avec le dernier poids utilisé ; modifiable avant ou pendant l'exercice. */
    val plannedWeightKg: Double? = null,
)

/** Le résultat réel d'une série. */
@Entity(
    tableName = "set_result",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseSessionId"), Index("exerciseId")],
)
data class SetResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseSessionId: Long,
    /** Dupliqué depuis l'exercice pour que l'historique d'un exercice soit une requête directe. */
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double? = null,
    val repetitions: Int? = null,
    val durationSeconds: Int? = null,
    val completedAt: Long,
)
