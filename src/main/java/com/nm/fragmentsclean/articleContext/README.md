# articleContext

> Le **articleContext** est le bounded context dédié à la gestion des contenus éditoriaux (articles) : création, publication, projection, et exposition en lecture.
>
> Il sert de démonstrateur clair d’une architecture **CQRS + event-driven + hexagonale**, orientée lisibilité métier, découplage technique et évolutivité.

---

## 🎯 Rôle fonctionnel

Le context *article* répond à un besoin simple côté produit :

* créer des contenus
* publier des articles
* exposer des listes
* exposer des articles par identifiant fonctionnel (slug)

Mais l’objectif n’est pas seulement fonctionnel :
👉 il sert de **socle structurel** pour démontrer comment organiser un domaine métier proprement dans une architecture distribuée.

---

## 🧠 Pourquoi cette architecture ?

### Pourquoi CQRS ?

Parce que les usages sont fondamentalement différents :

* **Write** : validation métier, invariants, cohérence
* **Read** : performance, pagination, projection, formats API

➡️ Séparer write/read permet :

* modèles adaptés à chaque besoin
* pas de compromis entre métier et performance
* évolutivité indépendante

---

### Pourquoi event-driven ?

La création d’un article n’est pas un simple `save()` :

* elle produit un **fait métier** (ArticleCreated)
* ce fait peut intéresser plusieurs systèmes
* ce fait doit être traçable

➡️ L’événement devient la vérité métier, pas la base de données.

---

### Pourquoi hexagonal ?

Pour éviter :

* dépendance aux frameworks
* couplage aux bases de données
* logique métier noyée dans l’infrastructure

➡️ Le domaine dépend uniquement de ses **ports**, jamais des adapters.

---

## 🧩 Structure du context

```
articleContext/
├── write/                 # write model (CQRS)
│   ├── businesslogic/     # domaine pur
│   │   ├── models/        # entités + events
│   │   ├── usecases/      # commandes
│   │   └── gateways/      # ports
│   └── adapters/          # adapters infra
│       ├── primary/       # REST controllers
│       └── secondary/     # JPA / fake repos
│
├── read/                  # read model (CQRS)
│   ├── projections/       # vues matérialisées
│   ├── configuration/     # wiring
│   ├── adapters/          # REST + SQS
│   └── queries/           # query handlers
```

---

## ✍️ Write side — logique métier

### Modèle de domaine

* `Article`
* `ArticleId`
* `ArticleStatus`
* `ArticleCreatedEvent`

➡️ Le domaine est **expressif**, pas technique.

Pas de JPA. Pas de Spring. Pas de transport SQS dans le domaine.
Seulement des concepts métier.

---

### Use case

* `CreateArticleCommand`
* `CreateArticleCommandHandler`

➡️ Le handler orchestre :

* validation
* création d’entité
* émission d’événement
* marquage `command_status` en `APPLIED`

Pas de persistance directe : il parle à un **port** (`ArticleRepository`).

Une commande idempotente déjà appliquée doit rester observable. Si l'article
existe déjà pour le même `articleId`, le handler ne recrée pas l'effet métier,
mais marque tout de même le `commandId` comme appliqué.

---

### Ports

* `ArticleRepository` (gateway)

➡️ Le domaine dépend d’une **abstraction**, jamais d’une implémentation.

---

## 🔌 Adapters write

### Primary (entrée)

* `WriteArticleController`
* `AdminStudioArticlesController` via `adminImportContext`

➡️ Adaptation HTTP → Command

Aucune logique métier dans le controller.

Fragments Studio ne poste pas directement une projection. Il gère d'abord des
documents éditoriaux admin (`draft`, `published`, `deleted`) sous
`/api/admin/studio/articles`. Ces documents permettent de lister les articles,
reprendre un brouillon, sauvegarder sans publier, puis supprimer côté console.

Le publish reste le seul passage vers l'app mobile : le boundary admin conserve
l'`articleId` généré par le Studio, génère le `commandId`, construit
`UpsertArticleDraftCommand`, puis délègue au command bus. La publication passe
par la revue explicite de la révision et l'archivage est une transition métier.

Les images article sont uploadées en multipart via
`/api/admin/studio/articles/images`. L'adapter de stockage peut écrire en local
ou S3 et retourne une référence image utilisable dans le cover ou les blocs.
Les URI internes S3 sont résolues côté read API avant d'être envoyées au mobile.

---

### Secondary (sortie)

* `JpaArticleRepository`
* `SpringArticleRepository`
* `ArticleJpaEntity`
* `FakeArticleRepository`

➡️ Plusieurs implémentations du même port :

* JPA (prod)
* Fake (tests)

C’est la **preuve** du découplage.

---

## 📖 Read side — projections

### Pourquoi des projections ?

Parce que la lecture n’est pas un besoin métier, mais un besoin **produit/API**.

➡️ On ne lit pas le domaine, on lit des **vues matérialisées**.

---

### Projections

* `ArticleView`
* `ArticleListView`
* `ArticleBlockView`
* `AuthorView`
* `ImageRefView`

➡️ Modèles orientés API/UI, pas métier.

---

### Event handler

* `ArticleCreatedEventHandler`

➡️ Transformation :
`Event métier → Projection read`

---

### SQS

* `ArticleCreatedSqsIntegrationEventHandler`

➡️ Le read model se reconstruit uniquement à partir des événements.

Pas de couplage direct au write model.

---

## 🔍 Queries

* `GetArticleBySlugQuery`
* `ListArticlesQuery`
* handlers associés

➡️ Modèle lecture dédié, indépendant du write model.

---

## 🧪 Testabilité

Pourquoi cette structure facilite les tests :

* Fake repositories
* Domaine sans framework
* Use cases isolés
* Projections testables
* Adapters remplaçables

➡️ Tests rapides, fiables, ciblés.

---

## 🧠 Philosophie

Ce context illustre :

* séparation stricte des responsabilités
* code métier lisible
* dépendances orientées vers le domaine
* architecture qui **explique le métier** avant la technique

> Le code décrit le métier, l’infrastructure s’adapte autour.

---

## 🎯 Pourquoi ce context est important dans le projet

Parce qu’il sert de **modèle de référence** :

* structure des dossiers
* organisation CQRS
* ports/adapters
* event-driven
* projections

➡️ Les autres contexts s’alignent sur cette grammaire.

---

## 🏁 Objectif

Le articleContext n’est pas un simple CRUD.
C’est un **template architectural** pour le reste du système.

Il démontre comment construire un domaine :

* propre
* évolutif
* testable
* distribué
* maintenable

---

## Directions artistiques des illustrations Studio (2026-10-07)

Dans Studio, l’onglet **Assistance** propose une humeur pour chaque nouvelle
génération : `ORIGINAL`, `INTIMATE`, `LIVELY`, `CONTEMPLATIVE` ou `BOLD`.
`ORIGINAL` reste le défaut et conserve exactement le prompt historique.
Les autres directions modifient palette, lumière et composition, avec une
priorité explicite sur les indications visuelles du brief généré. Le choix est
manuel ; aucune rotation automatique ni re-génération des images existantes
n’est introduite.

Itération `BEHAVIOUR`, routes commande Studio, adaptateur externe et lecture.
`articleContext` possède `ArticleArtDirection` et valide les valeurs avant toute
écriture. `adminImportContext` transmet une chaîne primitive par son ACL de
commande. La saga conserve le choix dans `article_authoring_sagas.art_direction`
et l’expose dans son snapshot de consultation. Le worker utilise le choix de
la saga réclamée pour la couverture et toutes les sections, même après une
reprise. Le contrat événementiel SQS reste inchangé ; les appels OpenAI restent
hors transaction. La traduction en consignes visuelles appartient à
`OpenAiArticleImageGenerationProvider`.

Avant le déploiement backend, appliquer le manifeste additif
`src/main/resources/db/release/article-art-direction-2026-10.psql` avec le
renderer de release existant. Il inclut
`2026-10-07-article-art-direction.sql`. La colonne a pour défaut `ORIGINAL` ; les
anciens appels, générations et messages restent compatibles. Déployer ensuite
le backend puis le Studio. Aucun déploiement n’a été effectué pendant cette
itération.

Les exemples moteurs sont : conservation exacte du prompt original, palettes
et compositions différentes pour les quatre alternatives, transmission de
l’intention Studio, rejet d’une valeur inconnue avant écriture, cohérence
couverture/sections et maintien du choix après reconstitution et reprise.
La découverte principale est que les anciens briefs imposent aussi un style
chaud : la nouvelle direction doit donc être explicitement prioritaire sur
leur couleur et leur style, tout en conservant leur sujet.

Vérifications : 76 tests backend sélectionnés passent, dont
`ArticleArtDirectionPersistenceIT`, les contrats MockMvc/OpenAPI, les tests de
reprise, `ReleaseManifestTest` et `BoundedContextArchitectureTest`. Le contexte
article Studio passe ses 87 tests ; compilation TypeScript/Vite de production
et synchronisation du contrat généré passent.

Checkpoint de mutation manuel `EXECUTED` : retirer la direction du listener
Studio fait échouer `articleArtDirectionIntent.test.ts` ; retirer la direction
de la requête image fait échouer
`ArticleGeneratedMediaServiceTest.sharesTheSelectedDirectionAcrossCoverAndSections`.
Forcer l’adaptateur à utiliser le prompt historique pour toutes les directions
fait échouer les quatre exemples de palette/composition et l’exemple de priorité
sur un brief chaud dans `OpenAiArticleGenerationProviderTest`. Les trois mutants
sont `KILLED`, sans survivant dans ce périmètre et sans erreur de setup ou de
compilation. Les fichiers originaux sont restaurés et les suites repassent.
Les logs et patchs exacts de cette session sont dans `/tmp/art-front-mutation.log`,
`/tmp/art-mutation-media.log` et `/tmp/art-mutation-prompt.log` ; les mutations
exactes sont dans `/tmp/art-front-mutation.py`, `/tmp/art-mutation.py` et
`/tmp/art-mutation-prompt.py`. Baselines : `npm test -- tests/articleStudioContext`
et la commande Maven ci-dessous (tests pertinents inclus dans les 76 tests
backend). Chaque mutant est
exécuté séparément et restauré par `finally`. La vérification finale de ces
15 tests backend restaurés est consignée dans `/tmp/art-restored.log` ; la suite
Studio complète restaurée reste verte. Aucun `PIN` supplémentaire n’a été
nécessaire ; aucun score de campagne n’est revendiqué.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn -q -o \
  -Dtest=OpenAiArticleGenerationProviderTest,ArticleGeneratedMediaServiceTest,ArticleArtDirectionTest test
```
