package fr.acano.workout.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import fr.acano.workout.domain.SetEffort
import fr.acano.workout.domain.WorkoutType

/**
 * Catalogue d'exercices. L'identifiant est un slug stable (« chest_press ») :
 * il sert aussi de nom de fichier pour l'animation dans `assets/exercises/`.
 *
 * Un exercice créé par l'utilisateur ([isCustom]) a un identifiant préfixé par
 * `custom_`, pour ne jamais entrer en collision avec un futur exercice du
 * programme, et son image éventuelle vit dans le stockage privé ([imagePath]).
 *
 * Un exercice ne décrit que le mouvement : séries, répétitions, durée et repos
 * sont propres à chaque entraînement (voir [CustomWorkoutExerciseEntity]). Seul
 * le pas de charge reste ici, parce qu'il dépend de la machine.
 *
 * Supprimer un exercice de l'utilisateur ne fait que l'archiver ([archivedAt]) :
 * il disparaît du catalogue, mais l'historique garde son nom.
 *
 * Catégorie, muscles et matériel viennent du catalogue pour les exercices de
 * l'application, et restent facultatifs pour ceux de l'utilisateur.
 */
@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: ExerciseKind,
    val weightStepKg: Double = 2.5,
    /** Chemin absolu de l'image choisie par l'utilisateur ; prioritaire sur l'animation fournie. */
    val imagePath: String? = null,
    @ColumnInfo(defaultValue = "0") val isCustom: Boolean = false,
    val archivedAt: Long? = null,
    val category: ExerciseCategory? = null,
    val primaryMuscle: Muscle? = null,
    @ColumnInfo(defaultValue = "") val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: Equipment? = null,
) {
    val isArchived: Boolean get() = archivedAt != null
}

/** Une séance. `endedAt == null` signifie « séance en cours » : c'est le seul marqueur de reprise. */
@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: WorkoutType,
    val startedAt: Long,
    val endedAt: Long? = null,
    val starAwarded: Boolean = false,
    /**
     * Entraînement personnalisé d'origine. Volontairement sans clé étrangère :
     * supprimer l'entraînement ne doit pas effacer les séances déjà faites.
     */
    val customWorkoutId: Long? = null,
    /** Nom de l'entraînement personnalisé au moment de la séance, pour que l'historique reste lisible. */
    val name: String? = null,
)

/** Titre affiché d'une séance : le nom choisi pour un entraînement personnalisé, sinon celui du programme. */
fun WorkoutSessionEntity.title(): String = name ?: type.label

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
    /** Cibles de l'étape, copiées de l'entraînement au démarrage ; null = sans objet pour ce type d'exercice. */
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null,
    /** Repos automatique après une série, en secondes. 0 = pas de chrono. */
    @ColumnInfo(defaultValue = "60") val restSeconds: Int = 60,
    val targetDistanceMeters: Int? = null,
    /**
     * Exercice passé (machine prise, par exemple) : ses séries restantes sont
     * abandonnées et la séance continue. Les séries déjà faites sont gardées.
     */
    @ColumnInfo(defaultValue = "0") val skipped: Boolean = false,
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
    val distanceMeters: Double? = null,
    /** Ressenti noté à la validation ; null s'il n'a pas été donné. */
    val effort: SetEffort? = null,
)

/** Un entraînement composé par l'utilisateur à partir du catalogue. */
@Entity(tableName = "custom_workout")
data class CustomWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Un exercice d'un entraînement, à sa place, avec ses séries, ses cibles et son repos. */
@Entity(
    tableName = "custom_workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = CustomWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class CustomWorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val position: Int,
    val plannedSets: Int,
    /** Cibles de l'étape ; null = sans objet (pas de reps pour un exercice chronométré, et inversement). */
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null,
    /** Repos automatique après une série, en secondes. 0 = pas de chrono. */
    @ColumnInfo(defaultValue = "60") val restSeconds: Int = 60,
    val targetDistanceMeters: Int? = null,
)
