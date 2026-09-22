# Prompt Claude — App Android de suivi musculation

Je veux que tu développes une application Android native de suivi de mes séances de musculation.

L'application est destinée à mon usage personnel. Je veux quelque chose de très simple, rapide à utiliser pendant une séance, moderne visuellement, et surtout pas une application de fitness générique remplie de fonctionnalités inutiles.

## Stack technique

Utilise :

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room pour stocker localement l'historique des séances et les poids
- DataStore si nécessaire pour les préférences
- Architecture simple et propre, par exemple MVVM
- Application offline-first
- Aucun compte utilisateur
- Aucun backend
- Aucune connexion Internet nécessaire pour utiliser l'application

L'application doit être facilement ouvrable et modifiable dans Android Studio.

Privilégie une architecture simple et maintenable plutôt qu'une architecture inutilement complexe.

---

# Mon programme

Je m'entraîne 2 fois par semaine.

Il y a deux séances :

1. Haut du corps
2. Bas du corps

Chaque séance commence systématiquement par :

### Échauffement

5 minutes de vélo.

Je veux que l'application affiche simplement cette étape comme première étape de la séance avec éventuellement un timer de 5 minutes.

---

# Séance Haut du corps

## 1. Chest Press

- 4 séries
- 8 à 12 répétitions
- poids configurable

## 2. Pec Deck

- 3 séries
- 10 à 15 répétitions
- poids configurable

## 3. Lat Pulldown / Tirage vertical

- 3 séries
- 8 à 12 répétitions
- poids configurable

## 4. Seated Row / Tirage horizontal

- 3 séries
- 8 à 12 répétitions
- poids configurable

Puis exercices de fin de séance :

## Planche

- 4 séries
- 1 minute chacune

## Stomach Vacuum

- entre 3 et 5 répétitions

---

# Séance Bas du corps

## 1. Leg Press / Presse à cuisses

- 4 séries
- 8 à 12 répétitions
- poids configurable

## 2. Leg Curl

- 3 séries
- 10 à 15 répétitions
- poids configurable

## 3. Leg Extension

- 3 séries
- 10 à 15 répétitions
- poids configurable

## 4. Mollets

- 3 séries
- 12 à 20 répétitions
- poids configurable

Puis :

## Planche

- 4 séries
- 1 minute chacune

## Stomach Vacuum

- entre 3 et 5 répétitions

---

# Fonctionnement pendant une séance

Je veux que l'écran principal d'entraînement soit extrêmement simple à utiliser avec une seule main.

Pour l'exercice actuel, affiche clairement :

- le nom de l'exercice
- un GIF montrant comment réaliser correctement le mouvement
- le numéro de la série actuelle, par exemple "Série 2 / 4"
- le nombre de répétitions à effectuer, par exemple "8–12 reps"
- le poids prévu
- le poids utilisé lors de ma dernière séance pour cet exercice
- éventuellement les performances des séries précédentes de la séance actuelle

Exemple :

Chest Press

Série 2 / 4

Objectif : 8–12 reps

Poids : 45 kg  
Dernière séance : 42,5 kg

[GIF de l'exercice]

[-] 45 kg [+]

[10 reps]

[VALIDER LA SÉRIE]

Je dois pouvoir modifier très rapidement :

- le poids
- le nombre de répétitions réellement effectuées

Le poids doit accepter les décimales, par exemple :

42,5 kg

---

# Chronomètre de récupération

Lorsque je valide une série, lance automatiquement un chrono de récupération de :

1 minute.

Le timer doit être très visible.

Exemple :

REPOS

00:47

Je veux pouvoir :

- mettre le chrono en pause
- le relancer
- passer le chrono
- ajouter éventuellement +30 secondes

Lorsque le chrono arrive à zéro :

- vibration
- notification sonore légère si le téléphone n'est pas en silencieux
- affichage clair indiquant que je peux commencer la série suivante

Le téléphone ne doit pas avoir besoin de rester avec l'écran allumé pour que le timer fonctionne correctement.

Entre deux exercices différents, je n'ai pas nécessairement besoin d'un chrono automatique car le temps nécessaire pour nettoyer la machine et m'installer sur la suivante me sert de récupération.

---

# Progression des poids

Je veux pouvoir suivre facilement ma progression.

Pour chaque exercice, enregistre :

- date
- poids
- nombre de répétitions de chaque série

Exemple :

Chest Press

12 septembre
40 kg
12 / 11 / 10 / 9

18 septembre
42,5 kg
11 / 10 / 10 / 9

21 septembre
42,5 kg
12 / 12 / 11 / 10

Lors de la séance suivante, pré-remplis automatiquement le dernier poids utilisé.

Je dois pouvoir modifier ce poids avant ou pendant l'exercice.

---

# Historique

Créer un écran "Historique".

Je veux pouvoir voir :

- toutes mes séances
- date
- type : Haut du corps / Bas du corps
- durée de la séance
- exercices réalisés

En cliquant sur une séance, afficher le détail :

- exercice
- poids
- séries
- répétitions

Je veux également pouvoir ouvrir un exercice particulier et voir l'évolution de mon poids dans le temps.

Un petit graphique simple d'évolution du poids serait intéressant, mais reste secondaire par rapport au MVP.

---

# Système d'étoiles

Je veux une mécanique très simple de motivation.

Chaque séance complètement terminée rapporte :

⭐ 1 étoile

Une séance ne rapporte l'étoile qu'une seule fois.

Sur l'accueil, affiche quelque chose comme :

⭐ 17 séances réalisées

ou

17 ⭐

Je veux également voir :

- nombre total de séances
- nombre de séances Haut du corps
- nombre de séances Bas du corps

Pas besoin de niveaux, XP, badges ou gamification compliquée.

L'étoile est volontairement la seule récompense.

---

# GIF des exercices

Chaque exercice doit avoir un GIF ou une petite animation montrant le mouvement correctement.

Prévois une architecture permettant d'associer un fichier local à chaque exercice.

Par exemple :

- `assets/exercises/chest_press.gif`
- `assets/exercises/pec_deck.gif`
- `assets/exercises/lat_pulldown.gif`
- `assets/exercises/seated_row.gif`
- `assets/exercises/leg_press.gif`
- `assets/exercises/leg_curl.gif`
- `assets/exercises/leg_extension.gif`
- `assets/exercises/calf_raise.gif`
- `assets/exercises/plank.gif`
- `assets/exercises/stomach_vacuum.gif`

Je fournirai éventuellement les GIF moi-même.

Si les fichiers ne sont pas encore présents, utilise un placeholder propre sans empêcher l'application de fonctionner.

Le système doit supporter correctement les GIF animés dans Jetpack Compose.

---

# Écran d'accueil

Je voudrais quelque chose de très simple.

Par exemple :

# Workout

⭐ 17

Dernière séance :
Haut du corps
19 septembre

[BOUTON : HAUT DU CORPS]

[BOUTON : BAS DU CORPS]

Puis éventuellement :

Progression récente

Chest Press
42,5 kg ↑

Leg Press
100 kg ↑

Pas besoin d'un dashboard compliqué.

---

# Déroulement d'une séance

Lorsque je clique sur "Haut du corps" par exemple :

### Étape 1
Vélo — 5:00

Bouton :
COMMENCER

Timer 5 minutes.

Puis :

### Étape 2
Chest Press

Série 1/4

etc.

Quand toutes les séries sont faites :

Exercice suivant.

Une petite indication de progression en haut serait bien :

2 / 7

ou une barre de progression.

À la fin :

🎉 Séance terminée

⭐ +1

Durée : 47 min

Résumé :

Chest Press
45 kg
12 / 11 / 10 / 9

Pec Deck
...

Puis bouton :

TERMINER

---

# Planche

Pour la planche, adapte l'interface car ce ne sont pas des répétitions.

Je fais :

4 × 1 minute

Lorsque je commence une série :

timer de 1 minute.

À la fin :

- vibration
- série validée

Puis récupération de 1 minute avant la prochaine planche.

---

# Stomach Vacuum

Je fais entre 3 et 5 répétitions.

Il suffit d'avoir :

Stomach Vacuum

Objectif :
3–5 répétitions

avec des boutons permettant d'indiquer :

3
4
5

Puis valider.

Pas besoin de poids.

---

# Design

Je veux une interface :

- moderne
- minimaliste
- sombre de préférence
- adaptée à un Pixel 9 Pro
- gros boutons faciles à toucher pendant l'entraînement
- informations essentielles visibles immédiatement
- très peu de texte inutile
- Material 3

Je préfère une UI proche d'une application sportive moderne plutôt qu'une application professionnelle / administrative.

Supporte correctement le dark mode.

---

# Navigation

Navigation principale très simple :

Accueil | Historique | Exercices

### Accueil

Lancer une séance + étoiles + dernière séance.

### Historique

Toutes les séances et progression.

### Exercices

Liste des exercices avec :

- GIF
- nom
- dernière charge utilisée
- historique

---

# Modèle de données

Prévois au minimum les concepts suivants :

WorkoutType
- UPPER_BODY
- LOWER_BODY

Exercise

WorkoutSession

ExerciseSession

SetResult

SetResult doit pouvoir contenir notamment :

- exerciseId
- setNumber
- weight
- repetitions
- duration si exercice basé sur le temps

Stocke les données avec Room.

Fais en sorte que le programme de base soit créé automatiquement au premier lancement de l'application.

---

# Important concernant la sauvegarde

Je ne veux jamais perdre mon historique en fermant l'application.

Une séance en cours doit également être sauvegardée.

Si Android ferme l'application pendant que je suis à la salle puis que je la rouvre, je dois pouvoir reprendre ma séance exactement là où j'en étais.

Exemple :

Chest Press
Série 3/4

et non recommencer la séance depuis zéro.

---

# Fonctionnalités MVP prioritaires

Priorité 1 :

- lancer Haut du corps / Bas du corps
- suivre l'exercice actuel
- saisir poids et répétitions
- mémoriser les poids
- timer de récupération 1 minute
- timer vélo 5 minutes
- timer planche
- GIF des exercices
- historique des séances
- reprendre une séance interrompue
- étoile à chaque séance terminée

Priorité 2 :

- graphique de progression
- statistiques
- animations supplémentaires

Ne développe pas de fonctionnalités inutiles avant que le MVP soit parfaitement fonctionnel.

---

# Ce que j'attends de toi

Commence par analyser le besoin et proposer une architecture simple.

Ensuite développe réellement le projet.

Je veux que tu :

1. définisses l'arborescence du projet ;
2. crées les modèles Room ;
3. crées les DAO et repositories ;
4. crées les ViewModels ;
5. développes les écrans Compose ;
6. implémentes la navigation ;
7. implémentes les timers correctement ;
8. implémentes la sauvegarde/reprise d'une séance ;
9. ajoutes les données initiales de mon programme ;
10. gères les GIF ;
11. ajoutes les tests utiles ;
12. vérifies que le projet compile.

Ne te contente pas de me donner des exemples de code ou du pseudo-code.

Travaille directement sur le projet et produis une première version fonctionnelle complète.

Lorsque tu dois prendre une décision technique mineure, prends toi-même la décision la plus simple et raisonnable au lieu de me demander systématiquement confirmation.

À chaque étape importante, vérifie que ce qui a déjà été développé compile toujours.

Favorise :
- simplicité
- robustesse
- UX rapide en salle
- code maintenable

Évite :
- over-engineering
- abstraction prématurée
- backend inutile
- authentification
- dépendances inutiles
- fonctionnalités sociales
- programmes de fitness génériques

L'objectif est d'avoir une petite application personnelle extrêmement pratique que je puisse ouvrir à la salle, démarrer ma séance en quelques secondes et utiliser sans réfléchir.
