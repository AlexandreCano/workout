package fr.acano.workout.data.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Inscrit les séances du programme comme des entraînements ordinaires.
 *
 * Écrit en SQL brut parce qu'il tourne à deux moments où les DAO ne sont pas
 * disponibles : à la création de la base (nouvelle installation) et dans la
 * migration 3 → 4 (base existante). Une fois insérés, ces entraînements se
 * modifient ou se suppriment comme les autres ; rien ne les recrée ensuite.
 */
object ProgramSeed {

    /**
     * [withRest] suit le schéma de la base visée : la colonne de repos des étapes
     * n'existe qu'à partir de la v6. La migration 3 → 4 travaille sur une base
     * v4 et doit donc s'en passer ; c'est la migration 5 → 6 qui la remplit
     * ensuite depuis l'exercice.
     */
    fun insertInto(db: SupportSQLiteDatabase, withRest: Boolean) {
        Program.types.forEachIndexed { index, type ->
            val workoutId = db.insert(
                "custom_workout",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", type.label)
                    // Dates « à l'origine » : ils restent en tête de liste, dans l'ordre du programme.
                    put("createdAt", index.toLong())
                },
            )
            Program.planFor(type).forEachIndexed { position, step ->
                db.insert(
                    "custom_workout_exercise",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("workoutId", workoutId)
                        put("exerciseId", step.exerciseId)
                        put("position", position)
                        put("plannedSets", step.plannedSets)
                        put("targetRepsMin", step.targetRepsMin)
                        put("targetRepsMax", step.targetRepsMax)
                        put("targetDurationSeconds", step.targetDurationSeconds)
                        if (withRest) put("restSeconds", step.restSeconds)
                    },
                )
            }
            // Rattache les séances déjà faites : statistiques et historique restent continus.
            db.execSQL(
                "UPDATE workout_session SET customWorkoutId = ?, name = ? " +
                    "WHERE type = ? AND customWorkoutId IS NULL",
                arrayOf<Any>(workoutId, type.label, type.name),
            )
        }
    }
}
