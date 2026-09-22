package fr.acano.workout.di

import android.content.Context
import fr.acano.workout.data.ExerciseMedia
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.repository.WorkoutRepository

/**
 * Injection de dépendances manuelle. Pour une application mono-module de cette taille,
 * un conteneur explicite coûte quelques lignes et évite un processeur d'annotations
 * supplémentaire (et son temps de build).
 */
class AppContainer(context: Context) {

    private val database: WorkoutDatabase by lazy { WorkoutDatabase.build(context) }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(database.exerciseDao(), database.workoutDao())
    }

    val exerciseMedia: ExerciseMedia by lazy { ExerciseMedia(context) }
}
