package fr.acano.workout.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.acano.workout.data.db.dao.ExerciseDao
import fr.acano.workout.data.db.dao.WorkoutDao
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSessionEntity::class,
        SetResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun build(context: Context): WorkoutDatabase =
            Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout.db")
                .addCallback(
                    object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            // Les ON DELETE CASCADE ne s'appliquent que si les FK sont actives.
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    },
                )
                .build()
    }
}
