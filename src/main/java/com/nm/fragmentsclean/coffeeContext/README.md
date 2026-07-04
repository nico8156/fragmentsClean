# coffeeContext

> Le **coffeeContext** est le bounded context responsable de la gestion des entités « coffee shop » : création, normalisation, projection et exposition.
>
> Il représente le **cœur produit** de l’application : les lieux, les points d’intérêt, les repères physiques autour desquels s’organisent les usages.

---

## 🎯 Rôle fonctionnel

Ce context gère :

* la création de cafés
* l’identification unique des lieux
* les données descriptives
* la géolocalisation
* les horaires
* les métadonnées

Mais sa vraie responsabilité est plus profonde :

👉 fournir un **référentiel fiable** de lieux sur lequel les autres contexts s’appuient.

---

## 🧠 Philosophie de conception

Le coffeeContext n’est pas conçu comme un simple CRUD.

Il est pensé comme un **système de référence** :

* source de vérité des lieux
* normalisation des données
* stabilité des identifiants
* cohérence spatiale

Les autres contexts (social, ticket, user, etc.) **ne définissent pas les lieux** :
ils s’y rattachent.

---

## 🧩 Pourquoi cette architecture ?

### Pourquoi CQRS ?

Parce que les usages sont asymétriques :

* écriture rare
* lecture massive
* requêtes orientées UX

➡️ Séparer write/read permet :

* write model métier propre
* read model optimisé pour affichage
* projections spécialisées

---

### Pourquoi event-driven ?

Un lieu est une **entité structurante** :

* sa création impacte plusieurs usages
* ses données doivent se propager

➡️ L’événement devient le mécanisme de propagation.

---

### Pourquoi hexagonal ?

Pour éviter que :

* les APIs externes
* les bases de données
* les providers

ne contaminent le domaine.

➡️ Le domaine reste stable, l’infrastructure évolue.

---

## 🧱 Structure du context

```
coffeeContext/
├── write/
│   ├── businessLogic/
│   │   ├── models/
│   │   ├── VO/
│   │   ├── usecases/
│   │   └── gateways/
│   └── adapters/
│       ├── primary/
│       └── secondary/
│
├── read/
│   ├── projections/
│   ├── adapters/
│   ├── configuration/
│   └── queries/
```

---

## ✍️ Write side — modèle métier

### Modèle de domaine

* `Coffee`
* `OpeningHours`
* `Photo`

### Value Objects

* `CoffeeId`
* `CoffeeName`
* `Address`
* `GeoPoint`
* `GooglePlaceId`
* `PhoneNumber`
* `WebsiteUrl`
* `OpeningHours`
* `TimeWindowMinutes`
* `Tag`

➡️ Les données sont **sémantisées**, pas primitives.

Le modèle parle le langage métier.

---

### Event

* `CoffeeCreatedEvent`
* `CoffeeDeletedEvent`
* `CoffeeOpeningHoursImportedEvent`
* `CoffeePhotosImportedEvent`

➡️ La création d’un lieu est un **fait métier structurant**.
Les enrichissements Google sont des faits métier séparés : ils ne sont pas écrits directement par l’import admin.

---

### Use case

* `CreateCoffeeCommand`
* `CreateCoffeeCommandHandler`
* `DeleteCoffeeCommand`
* `DeleteCoffeeCommandHandler`
* `ImportGoogleOpeningHoursForCoffee`
* `ImportGooglePhotosForCoffee`

➡️ Un seul point d’entrée métier pour la création.
Les enrichissements réagissent ensuite à `CoffeeCreatedEvent` via outbox/SQS.

---

### Ports

* `CoffeeRepository`
* `GooglePlaceOpeningHoursGateway`
* `GooglePlacePhotosGateway`
* `CoffeePhotoStorage`

➡️ Le domaine dépend d’abstractions.

### Photos Google

Le flux photo actuel est asynchrone :

```text
CoffeeCreatedEvent
-> ImportGooglePhotosForCoffee
-> GooglePlacePhotosGateway
-> CoffeePhotoStorage
-> CoffeePhotosImportedEvent
-> CoffeePhotosImportedEventHandler
-> coffee_photos_projection
-> projection.updated hints:["photos"]
```

Le gateway Google récupère d'abord les `photos[].name` via Place Details, puis appelle Place Photos avec `skipHttpRedirect=true` pour obtenir un `photoUri` temporaire. L'image est téléchargée immédiatement et stockée via `CoffeePhotoStorage`.

Adapter actuel :

* `LocalCoffeePhotoStorage`
* `COFFEE_PHOTOS_STORAGE_DIRECTORY`
* `COFFEE_PHOTOS_PUBLIC_BASE_URL`

Cet adapter est volontairement remplaçable. La cible long terme est un adapter S3, sur le même port `CoffeePhotoStorage`, sans modifier le use case ni les projections.

---

## 🔌 Adapters write

### Primary

* `WriteCoffeeController`

➡️ Adaptation HTTP → Command

---

### Secondary

* `JpaCoffeeRepository`
* `SpringCoffeeRepository`
* `FakeCoffeeRepository`

➡️ Implémentations interchangeables.

---

## 📖 Read side — projections

### Pourquoi des projections ?

Parce que l’UI ne consomme pas le domaine.

Elle consomme des **vues optimisées** :

* simples
* rapides
* adaptées UX

---

### Projections

* `CoffeeSummaryView`
* `CoffeePhotoView`
* `CoffeeOpeningHoursView`

➡️ Vue orientée affichage carte / liste.

### Seed read model

`CoffeeReadSeedRunner` est désactivé par défaut.

Il ne doit pas alimenter staging/production : les projections doivent venir du flux normal `Domain Event -> Outbox -> SQS -> Projection`.
Pour activer explicitement le seed local ou en test :

```properties
COFFEE_READ_SEED_ENABLED=true
```

---

### Event handling

* `CoffeeCreatedEventHandler`
* `CoffeeDeletedEventHandler`
* `CoffeeOpeningHoursImportedEventHandler`
* `CoffeePhotosImportedEventHandler`

➡️ Synchronisation write → read par événements.
Chaque handler publie ensuite un `projection.updated` orienté read model, jamais un Domain Event vers le frontend.

La suppression admin d'un café suit le même chemin :

```text
DELETE /api/admin/coffees/{coffeeId}
-> DeleteCoffeeCommand
-> CoffeeDeletedEvent
-> Outbox/SQS
-> CoffeeDeletedEventHandler
-> suppression summary/photos/openingHours projections
-> projection.updated hints:["deleted","summary","photos","openingHours"]
```

---

### Kafka

* `CoffeeEventsKafkaListener`

➡️ Le read model est reconstruit depuis le flux d’événements.

---

## 🔍 Queries

* `ListCoffeesQuery`
* `ListCoffeesQueryHandler`

➡️ Lecture dédiée, découplée du write model.

---

## 🧪 Testabilité

* Fake repositories
* domaine pur
* VOs testables
* projections isolées
* handlers testables

➡️ Tests rapides et ciblés.

---

## 🧠 Positionnement système

Le coffeeContext est la **colonne vertébrale** du produit :

* structure l’espace
* stabilise les références
* ancre les usages
* supporte les autres contexts

Sans référentiel de lieux fiable, le produit n’existe pas.

---

## 🎯 Objectif

Ce context pose un socle :

* référentiel stable
* modèle riche
* découplage fort
* évolutivité
* lisibilité métier

---
