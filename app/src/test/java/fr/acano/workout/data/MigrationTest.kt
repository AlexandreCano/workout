package fr.acano.workout.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.db.WorkoutDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Migrations écrites à la main, testées sur les schémas réellement exportés.
 *
 * La base de départ est recréée à partir du schéma JSON de sa version, puis
 * ouverte par Room : à l'ouverture, Room joue les migrations *et* vérifie que
 * le résultat correspond exactement au schéma courant, sans quoi il lève une
 * erreur. C'est le même contrôle que `MigrationTestHelper`, sans avoir à
 * embarquer les schémas dans les assets de l'application.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var room: WorkoutDatabase? = null

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(DB)
    }

    /** Crée une base vide à la [version] donnée, exactement comme Room l'aurait créée. */
    private fun createAt(version: Int, fill: SQLiteDatabase.() -> Unit = {}) {
        val schema = JSONObject(File(SCHEMAS, "$version.json").readText()).getJSONObject("database")
        context.deleteDatabase(DB)
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB).apply { parentFile?.mkdirs() }, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
        db.fill()
        db.close()
    }

    /** Ouvre la base avec Room : migrations jouées, puis schéma validé. */
    private fun openMigrated(): SQLiteDatabaseLike {
        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, DB)
            .addMigrations(WorkoutDatabase.MIGRATION_3_4, WorkoutDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        room = database
        return SQLiteDatabaseLike(database)
    }

    @Test
    fun `la v6 recopie series cibles et repos de l exercice dans les etapes`() {
        createAt(5) {
            execSQL(
                "INSERT INTO exercise (id, name, kind, plannedSets, targetRepsMin, targetRepsMax, " +
                    "targetDurationSeconds, restSeconds, weightStepKg, imagePath, isCustom) VALUES " +
                    "('chest_press', 'Chest Press', 'WEIGHTED_REPS', 4, 8, 12, NULL, 60, 2.5, NULL, 0), " +
                    "('custom_1', 'Gainage', 'TIMED', 3, NULL, NULL, 45, 30, 5.0, '/img.png', 1)",
            )
            execSQL("INSERT INTO custom_workout (id, name, createdAt) VALUES (1, 'Full', 0)")
            // Une étape sans réglage propre (null = « celles de l'exercice »), une réglée à la main.
            execSQL(
                "INSERT INTO custom_workout_exercise (workoutId, exerciseId, position, plannedSets, " +
                    "targetRepsMin, targetRepsMax, targetDurationSeconds) VALUES " +
                    "(1, 'chest_press', 0, 4, NULL, NULL, NULL), (1, 'custom_1', 1, 3, NULL, NULL, 90)",
            )
            execSQL("INSERT INTO workout_session (id, type, startedAt, starAwarded) VALUES (1, 'CUSTOM', 0, 0)")
            execSQL(
                "INSERT INTO exercise_session (sessionId, exerciseId, position, plannedSets) " +
                    "VALUES (1, 'custom_1', 0, 3)",
            )
        }

        val db = openMigrated()

        assertEquals(
            listOf(listOf("chest_press", "8", "12", null, "60"), listOf("custom_1", null, null, "90", "30")),
            db.rows(
                "SELECT exerciseId, targetRepsMin, targetRepsMax, targetDurationSeconds, restSeconds " +
                    "FROM custom_workout_exercise ORDER BY position",
            ),
        )
        assertEquals(
            listOf(listOf("45", "30")),
            db.rows("SELECT targetDurationSeconds, restSeconds FROM exercise_session"),
        )
        assertEquals(
            listOf(listOf("Gainage", "TIMED", "5", "/img.png", "1", null)),
            db.rows("SELECT name, kind, weightStepKg, imagePath, isCustom, archivedAt FROM exercise WHERE id = 'custom_1'"),
        )
    }

    @Test
    fun `toute la chaine de migrations mene au schema courant`() {
        createAt(1)
        val db = openMigrated()
        // Ouverte et validée par Room ; la v4 y a inscrit les deux entraînements du programme.
        assertEquals(listOf(listOf("2")), db.rows("SELECT COUNT(*) FROM custom_workout"))
        assertTrue(db.rows("SELECT restSeconds FROM custom_workout_exercise").isNotEmpty())
    }

    /** Lecture brute des lignes, en texte, à travers la base ouverte par Room. */
    class SQLiteDatabaseLike(private val database: WorkoutDatabase) {
        fun rows(sql: String): List<List<String?>> =
            database.openHelper.writableDatabase.query(sql).use { c ->
                buildList {
                    while (c.moveToNext()) add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) })
                }
            }
    }

    private companion object {
        const val DB = "migration-test.db"
        val SCHEMAS = File("schemas/fr.acano.workout.data.db.WorkoutDatabase")
    }
}
