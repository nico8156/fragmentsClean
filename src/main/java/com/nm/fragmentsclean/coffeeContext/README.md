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

➡️ La création d’un lieu est un **fait métier structurant**.

---

### Use case

* `CreateCoffeeCommand`
* `CreateCoffeeCommandHandler`

➡️ Un seul point d’entrée métier pour la création.

---

### Ports

* `CoffeeRepository`

➡️ Le domaine dépend d’abstractions.

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

➡️ Vue orientée affichage carte / liste.

---

### Event handling

* `CoffeeCreatedEventHandler`

➡️ Synchronisation write → read par événements.

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
