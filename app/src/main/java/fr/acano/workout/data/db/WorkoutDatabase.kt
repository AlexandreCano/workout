package fr.acano.workout.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.acano.workout.data.db.dao.CustomWorkoutDao
import fr.acano.workout.data.db.dao.ExerciseDao
import fr.acano.workout.data.db.dao.WorkoutDao
import fr.acano.workout.data.db.entity.CustomWorkoutEntity
import fr.acano.workout.data.db.entity.CustomWorkoutExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
import fr.acano.workout.data.seed.ProgramSeed

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSessionEntity::class,
        SetResultEntity::class,
        CustomWorkoutEntity::class,
        CustomWorkoutExerciseEntity::class,
    ],
    version = 7,
    exportSchema = true,
    // v2 : entraînements personnalisés. Deux tables et deux colonnes nullables
    // ajoutées, rien de supprimé : Room sait générer la migration seul.
    // v3 : cibles (reps, durée) personnalisables par exercice, colonnes nullables.
    // v4 : schéma inchangé, les séances du programme deviennent des entraînements
    // (données seulement, d'où une migration manuelle : voir MIGRATION_3_4).
    // v5 : exercices créés par l'utilisateur (image, marqueur), colonnes ajoutées.
    // v6 : séries, cibles et repos quittent l'exercice pour les étapes
    // d'entraînement et de séance ; archivage des exercices. Migration manuelle,
    // les valeurs devant être recopiées avant de disparaître : voir MIGRATION_5_6.
    // v7 : catalogue complet — catégorie, muscles et matériel des exercices,
    // distance des séries et des cibles. Colonnes ajoutées seulement.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun customWorkoutDao(): CustomWorkoutDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = ProgramSeed.insertInto(db, withRest = false)
        }

        /**
         * Séries, cibles et repos passent de l'exercice aux étapes. Chaque étape
         * reçoit d'abord les valeurs que l'exercice lui fournissait jusqu'ici
         * (sans écraser ce qui avait été réglé pour elle), puis la table des
         * exercices est reconstruite sans ces colonnes : SQLite ne sait pas
         * supprimer une colonne proprement, on recrée donc la table.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("custom_workout_exercise", "exercise_session").forEach { table ->
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `restSeconds` INTEGER NOT NULL DEFAULT 60")
                    db.execSQL(
                        """
                        UPDATE `$table` SET
                            targetRepsMin = COALESCE(targetRepsMin,
                                (SELECT e.targetRepsMin FROM exercise e WHERE e.id = `$table`.exerciseId)),
                            targetRepsMax = COALESCE(targetRepsMax,
                                (SELECT e.targetRepsMax FROM exercise e WHERE e.id = `$table`.exerciseId)),
                            targetDurationSeconds = COALESCE(targetDurationSeconds,
                                (SELECT e.targetDurationSeconds FROM exercise e WHERE e.id = `$table`.exerciseId)),
                            restSeconds = COALESCE(
                                (SELECT e.restSeconds FROM exercise e WHERE e.id = `$table`.exerciseId), 60)
                        """.trimIndent(),
                    )
                }
                db.execSQL(
                    "CREATE TABLE `exercise_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `weightStepKg` REAL NOT NULL, `imagePath` TEXT, " +
                        "`isCustom` INTEGER NOT NULL DEFAULT 0, `archivedAt` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `exercise_new` (id, name, kind, weightStepKg, imagePath, isCustom, archivedAt) " +
                        "SELECT id, name, kind, weightStepKg, imagePath, isCustom, NULL FROM `exercise`",
                )
                db.execSQL("DROP TABLE `exercise`")
                db.execSQL("ALTER TABLE `exercise_new` RENAME TO `exercise`")
            }
        }

        /** Partagé avec les tests, pour qu'une base en mémoire démarre comme une vraie installation. */
        val callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = ProgramSeed.insertInto(db, withRest = true)

            override fun onOpen(db: SupportSQLiteDatabase) {
                // Les ON DELETE CASCADE ne s'appliquent que si les FK sont actives.
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        fun build(context: Context): WorkoutDatabase =
            Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_5_6)
                .addCallback(callback)
                .build()
    }
}
