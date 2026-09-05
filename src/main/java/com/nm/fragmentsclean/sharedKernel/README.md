# sharedKernel

> Le **sharedKernel** fournit uniquement les contrats techniques transverses nécessaires aux bounded contexts : **CQRS**, **outbox/inbox**, enveloppes de messages, **Projection Sync**, et quelques abstractions DDD communes.
>
> Il n’est pas un « utilitaire fourre-tout » : c’est un **contrat d’architecture** partagé, volontairement minimal, qui permet aux bounded contexts d’évoluer sans dupliquer la plomberie.

---

## 🎯 Rôle

Le sharedKernel sert à :

* standardiser la façon de traiter **commands / queries / events**
* définir les ports du **pipeline outbox/inbox** et ses adaptateurs techniques partagés
* fournir des interfaces DDD communes (AggregateRoot, Entity, DomainEvent)
* offrir une base d’infrastructure (SQS, Projection Sync) via adapters

👉 Chaque bounded context vient « brancher » sa logique métier sur ces mécanismes.

---

## 🧠 Pourquoi un shared kernel ?

Dans une architecture multi-contexts, il y a deux extrêmes problématiques :

1. **Tout dupliquer** : chaque context réécrit sa bus/pipeline/outbox → dette + incohérences.
2. **Tout mutualiser** : on crée un "framework interne" trop riche → couplage + rigidité.

Le sharedKernel vise un **juste milieu** :

✅ mutualiser uniquement :

* ce qui est vraiment transversal
* ce qui doit être cohérent globalement

❌ éviter :

* logique métier partagée
* modèles métiers partagés
* dépendances croisées entre contexts

> Le sharedKernel partage de la *plomberie*, pas du *métier*.

---

## 🧩 Ce que contient le sharedKernel

### 1) Contrat CQRS

* `Command`, `CommandHandler`
* `Query`, `QueryHandler`
* `CommandBus`, `QueryBus`

➡️ Objectif : des interactions uniformes et testables.

---

### 2) Contrat event-driven

* `DomainEvent`
* `EventHandler`
* `EventBus`

➡️ Objectif : publier des faits métier sans dépendre de SQS/Spring.

---

### 3) Outbox pattern (fiabilité)

* `OutboxEventRepository`
* `OutboxEventSender`
* `OutboxStatus`
* `OutboxDomainEventPublisher`
* `OutboxEventDispatcher`

➡️ Objectif : garantir que les événements ne sont jamais perdus, même en cas de crash.

Le write model publie des events → l’outbox persiste → un dispatcher envoie des enveloppes stables vers SQS.

---

### 4) Enveloppes d'intégration

* `IntegrationEventEnvelope`
* `IntegrationEventEnvelope`
* ports techniques de publication et de consommation

La composition, le catalogue de types, le routage et les contrats publics versionnés appartiennent à `platform/eventing`, pas au shared kernel. Les adaptateurs SQS désérialisent ces contrats puis passent par une ACL locale avant d’appeler le contexte consommateur.

➡️ Objectif : partager la mécanique sans transformer le shared kernel en propriétaire des contrats métier.

---

### 5) Projection Sync (fraîcheur read models)

* `ProjectionSyncEvent`
* `ProjectionSyncEventRepository`
* `ProjectionSyncPublisher`
* `ProjectionSyncController`

➡️ Objectif : signaler qu’une projection est à jour, sans exposer les Domain Events au client.

Le client reçoit `projection.updated`, puis relit le read model par HTTP.

---

### 6) Abstractions DDD

* `AggregateRoot`
* `Entity`
* `DomainEventPublisher`
* `CurrentUserProvider`
* `DateTimeProvider`

➡️ Objectif : garder le domaine expressif et testable.

Exemple :

* `DeterministicDateTimeProvider` permet des tests déterministes
* `FakeCurrentUserProvider` facilite des tests sans auth

---

## 🧱 Structure

```
sharedKernel/
├── businesslogic/
│   └── models/
│       ├── command/           # Command + handler
│       ├── query/             # Query + handler
│       ├── event/             # Event bus + handlers
│       ├── gateways/          # outbox ports
│       └── ...                # DDD primitives
│
└── adapters/
    ├── primary/springboot/
    │   ├── CommandBus, QueryBus, EventBus
    │   ├── eventDispatcher/   # OutboxEventDispatcher
    │   └── configuration/     # Jackson, WS, wiring
    └── secondary/gateways/
        ├── repositories/jpa/  # OutboxEventJpaEntity, repos
        └── providers/         # outbox senders, routing
```

---

## 🔄 Comment les contexts l’utilisent

Chaque bounded context :

* implémente ses **use cases** (handlers)
* émet des **DomainEvents**
* utilise le `DomainEventPublisher` → outbox
* consomme des events via handlers SQS
* expose son read model via queries

➡️ Le sharedKernel fournit les ports et primitives techniques, `platform` compose le runtime, les contexts fournissent le sens et leurs ACL.

---

## 🧠 Pourquoi c’est stratégique dans le projet

Parce que c’est le composant qui :

* évite la duplication
* garantit la cohérence
* rend l’ensemble testable
* rend l’ensemble industrialisable

Sans sharedKernel :

* architecture moins lisible
* plus de risque de divergence entre contexts
* plus de code d’infrastructure dans les domaines

---

## ⚖️ Trade-offs assumés

* Le sharedKernel ne doit pas devenir un framework interne.
* Il reste volontairement *petit* et *stable*.
* Les contexts restent propriétaires de leur domaine.

> Le sharedKernel porte les invariants d’architecture, pas les invariants métier.

---

## 🏁 Objectif

Le sharedKernel fournit un **langage commun** pour construire :

* des contexts indépendants
* une pipeline event-driven fiable
* une expérience mobile temps réel

Tout en gardant le domaine au centre.

---

> "Les bounded contexts évoluent. Le sharedKernel garantit la grammaire."
