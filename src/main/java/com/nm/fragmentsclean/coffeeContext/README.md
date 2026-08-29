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

* `CoffeeArchivedEvent`
* `CoffeeCreatedEvent`
* `CoffeeDeletedEvent`
* `CoffeeOpeningHoursImportedEvent`
* `CoffeePhotosImportedEvent`

➡️ La création d’un lieu est un **fait métier structurant**.
Les enrichissements Google sont des faits métier séparés : ils ne sont pas écrits directement par l’import admin.

---

### Use case

* `ArchiveCoffeeCommand`
* `ArchiveCoffeeCommandHandler`
* `CreateCoffeeCommand`
* `CreateCoffeeCommandHandler`
* `ImportGoogleOpeningHoursForCoffee`
* `ImportGooglePhotosForCoffee`
* `CoffeeCreatedProcessManager`

➡️ Un seul point d’entrée métier pour la création.
Les enrichissements réagissent ensuite à `CoffeeCreatedEvent` via l’EventBus local
quand il est activé. En staging/production, le contrat primitif SQS `coffee.created`
déclenche `CoffeeCreatedIntegrationEnrichmentHandler`, afin que le flux reste
opérationnel avec `APP_MESSAGING_LOCAL_EVENT_BUS_ENABLED=false`.
L'action produit de retrait du catalogue est `ArchiveCoffeeCommand`. Le hard delete reste une capacité technique non exposée comme comportement produit par défaut.

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
-> CoffeeCreatedProcessManager
-> ImportGooglePhotosForCoffee
-> GooglePlacePhotosGateway
-> CoffeePhotoStorage
-> CoffeePhotosImportedEvent
-> CoffeePhotosImportedEventHandler
-> coffee_photos_projection
-> projection.updated hints:["photos"]
```

En staging/production, le point de départ effectif est le message SQS
`coffee.created`, qui déclenche séparément l’import des horaires et des photos.

Le gateway Google récupère d'abord les `photos[].name` via Place Details, puis appelle Place Photos avec `skipHttpRedirect=true` pour obtenir un `photoUri` temporaire. L'image est téléchargée immédiatement et stockée via `CoffeePhotoStorage`.

Adapter actuel :

* `LocalCoffeePhotoStorage` par défaut en local/dev
* `S3CoffeePhotoStorage` en staging lorsque `COFFEE_PHOTOS_STORAGE_BACKEND=s3`
* `COFFEE_PHOTOS_STORAGE_DIRECTORY`
* `COFFEE_PHOTOS_PUBLIC_BASE_URL`
* `COFFEE_PHOTOS_S3_BUCKET`
* `COFFEE_PHOTOS_S3_PREFIX`
* `COFFEE_PHOTOS_S3_REGION`
* `COFFEE_PHOTOS_S3_PRESIGN_TTL`

En staging, les photos Fragments réutilisent le bucket assets Anchor avec un préfixe isolé :

```text
s3://anchor-assets-prod-851725375299/fragments/staging/coffees/{coffeeId}/photos/{photoId}.jpg
```

La projection conserve cette référence stable `s3://...`. Les controllers read résolvent ensuite `photoUri` en URL signée courte à chaque GET. Le frontend ne connaît pas S3 et continue à reconstruire ses vues après `projection.updated`.

### Gestion admin des photos

Fragments Studio peut maintenant gérer les photos persistées d'un café sans écrire directement dans les projections :

```text
POST /api/admin/coffees/{coffeeId}/photos
-> AddCoffeePhotoCommand
-> CoffeePhotoStorage
-> CoffeePhotoAddedEvent
-> Outbox/SQS
-> CoffeePhotoAddedEventHandler
-> coffee_photos_projection append
-> projection.updated hints:["photos"]
```

Les messages SQS `coffee.photo_added` et `coffee.photos_imported` utilisent des
contrats d'intégration primitifs (`UUID`, chaînes, dates et listes de références)
distincts des événements de domaine. Cela garantit la désérialisation côté
consommateur et évite d'exposer les value objects du write model au transport.
La réponse HTTP `202` signifie que S3 et la projection peuvent être traités
asynchronement ; Studio doit donc rafraîchir après le signal de projection ou
une nouvelle lecture.

```text
DELETE /api/admin/coffees/{coffeeId}/photos/{photoId}
-> DeleteCoffeePhotoCommand
-> CoffeePhotoDeletedEvent
-> Outbox/SQS
-> CoffeePhotoDeletedEventHandler
-> coffee_photos_projection delete
-> projection.updated hints:["photos"]
```

La suppression retire la photo du read model Fragments. La suppression physique de l'objet S3 pourra être ajoutée derrière un port de stockage dédié si le besoin d'optimisation/coût apparaît.

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
* `CoffeeArchivedEventHandler`
* `CoffeeDeletedEventHandler`
* `CoffeePhotoAddedEventHandler`
* `CoffeePhotoDeletedEventHandler`
* `CoffeeOpeningHoursImportedEventHandler`
* `CoffeePhotosImportedEventHandler`

➡️ Synchronisation write → read par événements.
Chaque handler publie ensuite un `projection.updated` orienté read model, jamais un Domain Event vers le frontend.

#### Monotonie et atomicité

SQS ne garantit pas que deux événements métier distincts seront reçus dans
l'ordre de leur version. La projection café conserve donc un checkpoint durable
par `coffeeId` dans `coffee_projection_checkpoints` : dernière version connue,
statut de publication, tombstone de suppression et date du changement.

Règles :

* une transition de version inférieure ou égale au checkpoint est ignorée ;
* une publication ou un archivage reçu avant la création est conservé, puis
  fusionné lorsque les données descriptives arrivent ;
* une suppression garde sa tombstone afin qu'un ancien `coffee.created` ne
  puisse pas ressusciter le café ;
* aucun `projection.updated` n'est écrit pour un événement ignoré ;
* résumé, enfants supprimés et `projection_sync_events` sont écrits dans la
  même transaction locale. Un échec provoque un rollback et laisse SQS rejouer
  le message.

La suppression admin visible dans Fragments Studio archive le café. Elle ne supprime pas physiquement le write model.

```text
DELETE /api/admin/coffees/{coffeeId}
-> ArchiveCoffeeCommand
-> CoffeeArchivedEvent
-> Outbox/SQS
-> CoffeeArchivedEventHandler
-> passage de la summary projection à ARCHIVED
-> conservation des photos et horaires pour Fragments Studio
-> exclusion des lectures mobiles publishedOnly
-> projection.updated hints:["archived","summary"]
```

`DeleteCoffeeCommand` / `CoffeeDeletedEvent` reste réservé à un hard delete technique explicite. Pour le catalogue mobile, le comportement produit par défaut est l'archive.

---

### SQS

* `CoffeeSqsIntegrationEventHandlers`

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
