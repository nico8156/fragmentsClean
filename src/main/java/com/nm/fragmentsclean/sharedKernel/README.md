# sharedKernel

> Le **sharedKernel** fournit les briques transverses nécessaires pour faire vivre l’architecture : **CQRS**, **event-driven**, **outbox**, **websocket ACK**, et des abstractions DDD communes.
>
> Il n’est pas un « utilitaire fourre-tout » : c’est un **contrat d’architecture** partagé, volontairement minimal, qui permet aux bounded contexts d’évoluer sans dupliquer la plomberie.

---

## 🎯 Rôle

Le sharedKernel sert à :

* standardiser la façon de traiter **commands / queries / events**
* centraliser le **pipeline outbox** (fiabilité des événements)
* fournir des interfaces DDD communes (AggregateRoot, Entity, DomainEvent)
* offrir une base d’infrastructure (Kafka, WebSocket) via adapters

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

➡️ Objectif : publier des faits métier sans dépendre de Kafka/Spring.

---

### 3) Outbox pattern (fiabilité)

* `OutboxEventRepository`
* `OutboxEventSender`
* `OutboxStatus`
* `OutboxDomainEventPublisher`
* `OutboxEventDispatcher`

➡️ Objectif : garantir que les événements ne sont jamais perdus, même en cas de crash.

Le write model publie des events → l’outbox persiste → un dispatcher envoie (Kafka / WS / logs).

---

### 4) Routing des événements

* `DomainEventRouter`
* `DefaultDomainEventRouter`
* `RoutingOutboxEventSender`

➡️ Objectif : centraliser la décision « où envoyer quel event ».

Exemple :

* certains events vont à Kafka
* certains events vont aussi à WebSocket (ACK)
* certains events sont loggés

---

### 5) WebSocket ACK (temps réel)

* `WebSocketConfig`
* `JwtStompChannelInterceptor`
* `WsAckEnvelope`
* `WebSocketOutboxEventSender`

➡️ Objectif : un retour utilisateur immédiat et fiable.

Même logique que Kafka : on n’envoie pas directement depuis le domaine.
On passe par l’outbox.

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
* consomme des events via listeners Kafka/WS
* expose son read model via queries

➡️ Le sharedKernel fournit la mécanique, les contexts fournissent le sens.

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

