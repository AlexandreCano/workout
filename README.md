# Workout

Application Android personnelle de suivi de séances de musculation.
100 % hors ligne, sans compte, sans backend, sans réseau.

## Ouvrir le projet

Android Studio **Narwhal ou plus récent** (le projet utilise AGP 9.4.1).
`File > Open` sur la racine du dépôt, puis Sync Gradle.

En ligne de commande :

```bash
./gradlew assembleDebug        # APK de debug
./gradlew testDebugUnitTest    # 36 tests unitaires
./gradlew lintDebug            # analyse statique
./gradlew installDebug         # installer sur un appareil branché
```

Le SDK est lu depuis `local.properties` (`sdk.dir`), non versionné.

## Stack

| | |
|---|---|
| Langage | Kotlin 2.3.21 (Kotlin intégré à AGP) |
| UI | Jetpack Compose, Material 3, thème sombre |
| Navigation | Navigation Compose, routes type-safe |
| Stockage | Room 2.8.5 (KSP 2.3.12) |
| Images | Coil 3 + `coil-gif` (GIF et WebP animés) |
| Build | AGP 9.4.1, Gradle 9.7.1, compileSdk 37, minSdk 29 |
| Injection | conteneur manuel (`di/AppContainer`), pas de Hilt |

## Design system

`ui/theme/` centralise les tokens, `ui/components/` les composants réutilisables.
Aucun écran ne contient de couleur, de forme ni d'espacement en dur.

| Fichier | Contenu |
|---|---|
| `theme/Color.kt` | Les deux `ColorScheme` Material 3 complets, dérivés d'une teinte vert-jaune |
| `theme/Type.kt` | Échelle typographique + styles `emphasis` (chiffres tabulaires) |
| `theme/Shape.kt` | Échelle de formes, décalée d'un cran vers le haut |
| `theme/Spacing.kt` | Système 8dp (`xs` → `xxxl`) et tailles de cible tactile |
| `theme/Motion.kt` | Ressorts Material 3 Expressive |

Composants : `WorkoutPrimaryButton`, `WeightSelector`, `RepsSelector`, `SetProgress`,
`StepBar`, `WorkoutTimer`, `ExerciseHeader`, `StatCard`, `ListRow`, `ExerciseImage`.

`ui/components/Previews.kt` affiche l'ensemble en thème clair et sombre :
c'est le moyen le plus rapide de vérifier une modification du design system
dans Android Studio, sans lancer l'application.

### Limites de Material 3 1.4.0

`MaterialExpressiveTheme`, `MotionScheme` et les styles typographiques
« emphasized » existent dans la bibliothèque mais y sont encore `internal`.
Le thème utilise donc `MaterialTheme`, et `theme/Motion.kt` redéfinit le schéma
de mouvement Expressive avec les valeurs de la spécification — à remplacer par
`MaterialTheme.motionScheme` dès que l'API sera publique.

## Architecture

MVVM, un seul module.

```
fr.acano.workout
├── domain/            modèles + WorkoutProgression (Kotlin pur, testable seul)
├── data/
│   ├── db/            entités Room, DAO, relations
│   ├── seed/          Program : le programme d'entraînement, en dur
│   ├── repository/    WorkoutRepository : la seule porte d'entrée des données
│   └── ExerciseMedia  résolution des animations dans assets/
├── timer/             RestTimerService (service de premier plan) + état partagé
├── di/                AppContainer, fabrique de ViewModels
└── ui/                theme, nav, home, session, history, exercises, common
```

### Trois décisions qui structurent le tout

**La progression n'est jamais stockée, elle est dérivée.**
L'exercice courant est le premier dont toutes les séries ne sont pas validées, et la
série courante est « séries déjà enregistrées + 1 ». Une séance en cours est simplement
une `WorkoutSession` dont `endedAt` est `null`. La reprise après fermeture de
l'application n'est donc pas une fonctionnalité séparée : c'est la conséquence
directe du modèle, et elle ne peut pas se désynchroniser de l'historique.

**Le chronomètre s'appuie sur une échéance absolue, pas sur une accumulation.**
`RestTimerService` retient `elapsedRealtime() + durée` et recalcule le restant à chaque
tick : aucune dérive, et le retour au premier plan est exact. C'est un service de
premier plan, donc Android ne gèle pas le process et la vibration part bien à zéro,
écran éteint.

**Le programme vit dans le code, l'historique vit en base.**
`data/seed/Program.kt` décrit les deux séances. Le catalogue est copié en base au
premier lancement pour que les séries enregistrées pointent vers des exercices stables ;
il n'y a pas de table « modèle de séance ».

## Animations des exercices

Dépose les fichiers dans `app/src/main/assets/exercises/`, nommés d'après l'identifiant
de l'exercice : `chest_press.gif`, `leg_press.webp`, `plank.gif`…
Les extensions `.gif`, `.webp`, `.png`, `.jpg` sont acceptées — **le WebP animé est
recommandé**, 3 à 5 fois plus léger qu'un GIF à qualité égale.

Identifiants attendus :
`bike_warmup`, `chest_press`, `pec_deck`, `lat_pulldown`, `seated_row`,
`leg_press`, `leg_curl`, `leg_extension`, `calf_raise`, `plank`, `stomach_vacuum`.

Tant qu'un fichier est absent, un visuel de remplacement s'affiche : rien ne casse.

## Le programme

Les deux séances comptent 7 étapes et commencent par 5 minutes de vélo.

| Haut du corps | Bas du corps |
|---|---|
| Vélo 5 min | Vélo 5 min |
| Chest Press — 4 × 8–12 | Presse à cuisses — 4 × 8–12 |
| Pec Deck — 3 × 10–15 | Leg Curl — 3 × 10–15 |
| Tirage vertical — 3 × 8–12 | Leg Extension — 3 × 10–15 |
| Tirage horizontal — 3 × 8–12 | Mollets — 3 × 12–20 |
| Planche — 4 × 1 min | Planche — 4 × 1 min |
| Stomach Vacuum — 3 à 5 | Stomach Vacuum — 3 à 5 |

Récupération automatique d'une minute entre deux séries d'un même exercice.
Aucun chrono entre deux exercices : le changement de machine sert de récupération.

### Réorganiser la suite

Pendant une séance, « Réorganiser la suite » ouvre un écran où les exercices
restants se déplacent librement au glisser-déposer, par leur poignée. Le premier
de la liste est celui qu'on fait maintenant — machine occupée, salle bondée, ou
simple envie de changer l'ordre du jour.

Seuls les exercices **restants** sont réordonnés : ceux déjà terminés gardent leur
place en tête, et un exercice repris conserve les séries déjà enregistrées. L'ordre
modifié ne vaut que pour la séance en cours — la séance suivante repart de l'ordre
défini dans `Program.kt`.

Le glissé étant inutilisable avec TalkBack, chaque ligne expose aussi deux actions
d'accessibilité « Monter » et « Descendre ».

Modifier le programme = modifier `Program.kt`. Les exercices ajoutés sont insérés
au démarrage suivant ; l'historique existant n'est pas touché.
