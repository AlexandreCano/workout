package fr.acano.workout.domain

/**
 * État d'une étape de séance, réduit à ce qui suffit pour calculer la progression.
 * Volontairement sans dépendance Android ni Room : c'est la logique la plus critique
 * de l'application (reprise de séance), elle doit être testable en unit test pur.
 */
data class StepState(
    val plannedSets: Int,
    val completedSets: Int,
    /** Étape passée : terminée, quel que soit le nombre de séries faites. */
    val isSkipped: Boolean = false,
) {
    val isComplete: Boolean get() = isSkipped || completedSets >= plannedSets
}

data class SessionProgress(
    /** Index de l'étape en cours, ou `null` si la séance est terminée. */
    val currentStepIndex: Int?,
    /** Numéro de la série à réaliser (1-based) dans l'étape en cours. */
    val currentSetNumber: Int,
    val completedSteps: Int,
    val totalSteps: Int,
) {
    val isFinished: Boolean get() = currentStepIndex == null && totalSteps > 0
}

object WorkoutProgression {

    /**
     * L'étape courante est la première étape incomplète, et la série courante est
     * simplement « nombre de séries déjà validées + 1 ». Toute la reprise de séance
     * découle de ce calcul : rien n'est stocké, tout est dérivé des séries en base.
     */
    fun compute(steps: List<StepState>): SessionProgress {
        val index = steps.indexOfFirst { !it.isComplete }.takeIf { it >= 0 }
        return SessionProgress(
            currentStepIndex = index,
            currentSetNumber = index?.let { steps[it].completedSets + 1 } ?: 0,
            completedSteps = steps.count { it.isComplete },
            totalSteps = steps.size,
        )
    }
}
