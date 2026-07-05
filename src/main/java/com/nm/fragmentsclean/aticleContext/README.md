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

Pas de persistance directe : il parle à un **port** (`ArticleRepository`).

---

### Ports

* `ArticleRepository` (gateway)

➡️ Le domaine dépend d’une **abstraction**, jamais d’une implémentation.

---

## 🔌 Adapters write

### Primary (entrée)

* `WriteArticleController`

➡️ Adaptation HTTP → Command

Aucune logique métier dans le controller.

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
