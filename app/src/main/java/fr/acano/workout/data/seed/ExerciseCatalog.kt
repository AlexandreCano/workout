package fr.acano.workout.data.seed

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Les exercices fournis avec l'application, lus depuis
 * `resources/catalog/exercises.json`.
 *
 * Le fichier est une ressource Java plutôt qu'un asset : il se lit sans
 * `Context`, donc aussi bien depuis le code que depuis les tests JVM. Il est
 * analysé une seule fois, à la première utilisation ; une valeur inconnue
 * (catégorie, muscle…) fait échouer la lecture, et un test le vérifie.
 *
 * Pour ajouter un exercice : une entrée dans le JSON, et son animation dans
 * `assets/exercises/<id>.webp`. Un identifiant publié ne change plus, sauf à
 * l'inscrire dans [renamedIds] : les séries enregistrées y font référence.
 */
object ExerciseCatalog {

    /**
     * Identifiants des premières versions devenus des exercices du catalogue :
     * au démarrage, séries, séances et entraînements passent sous le nouvel
     * identifiant et l'ancien exercice disparaît.
     */
    val renamedIds: Map<String, String> = mapOf(
        "leg_curl" to "lying_leg_curl",
        "calf_raise" to "standing_calf_raise",
    )

    val entries: List<CatalogExercise> by lazy { parse(read()) }

    private val byId: Map<String, CatalogExercise> by lazy { entries.associateBy { it.id } }

    private val positions: Map<String, Int> by lazy { entries.withIndex().associate { it.value.id to it.index } }

    operator fun get(id: String): CatalogExercise? = byId[id]

    /**
     * Position dans le catalogue, pour afficher les exercices dans l'ordre du
     * fichier — regroupés par famille. Un exercice inconnu (de l'utilisateur)
     * passe après.
     */
    fun indexOf(id: String): Int = positions[id] ?: Int.MAX_VALUE

    private val json = Json { ignoreUnknownKeys = true }

    internal fun parse(text: String): List<CatalogExercise> = json.decodeFromString<CatalogFile>(text).exercises

    private fun read(): String =
        requireNotNull(ExerciseCatalog::class.java.getResourceAsStream(RESOURCE)) { "$RESOURCE introuvable" }
            .bufferedReader()
            .use { it.readText() }

    private const val RESOURCE = "/catalog/exercises.json"
}

@Serializable
internal data class CatalogFile(val exercises: List<CatalogExercise>)

@Serializable
data class CatalogExercise(
    val id: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    val category: ExerciseCategory,
    @SerialName("primary_muscle") val primaryMuscle: Muscle,
    @SerialName("secondary_muscles") val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: Equipment,
    @SerialName("tracking_type") val trackingType: TrackingType,
) {
    val kind: ExerciseKind get() = trackingType.kind

    /** Le nom dans la langue de l'interface : français, ou anglais pour toute autre langue. */
    fun name(language: String): String = if (language == "fr") nameFr else nameEn

    /**
     * L'exercice tel qu'il est inscrit en base. Le nom est provisoire : il est
     * remplacé au démarrage par celui de la langue du téléphone.
     */
    fun toEntity(): ExerciseEntity = ExerciseEntity(
        id = id,
        name = nameFr,
        kind = kind,
        weightStepKg = weightStepKg,
        category = category,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
    )

    /**
     * Le pas de charge par défaut. La presse et le traîneau se chargent de
     * disques lourds : 2,5 kg n'y aurait aucun sens.
     */
    private val weightStepKg: Double
        get() = when {
            id == "leg_press" -> 5.0
            equipment == Equipment.SLED -> 10.0
            else -> 2.5
        }
}

/** Le mode de suivi tel que l'écrit le catalogue, traduit en [ExerciseKind]. */
@Serializable
enum class TrackingType(val kind: ExerciseKind) {
    WEIGHT_REPS(ExerciseKind.WEIGHTED_REPS),
    REPS_ONLY(ExerciseKind.REPS_ONLY),
    DURATION(ExerciseKind.TIMED),
    WEIGHT_DURATION(ExerciseKind.WEIGHTED_TIMED),
    DISTANCE(ExerciseKind.DISTANCE),
    WEIGHT_DISTANCE(ExerciseKind.WEIGHTED_DISTANCE),
    DURATION_DISTANCE(ExerciseKind.TIMED_DISTANCE),
}
