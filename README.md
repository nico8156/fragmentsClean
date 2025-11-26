
# Fragments – Backend (Write & Read – Social Context)

Ce backend implémente progressivement le domaine *Social* de l’application **Fragments**, permettant aux utilisateurs d’interagir autour des cafés (likes, commentaires, découverte).

> ✅ Objectif actuel : construire une architecture robuste et testée autour du like et du commentaire, en appliquant DDD, Hexagonal Architecture, CQRS et Outbox Pattern.

---

## 🏛️ Architecture

Le backend suit :

* **DDD (Domain-Driven Design)**
* **Hexagonal / Ports & Adapters**
* **CQRS léger**
* **Event-driven architecture interne**
* **Outbox Pattern** pour la diffusion d’événements

### Vue simplifiée

```
HTTP
 ↓
Controller (primary adapter)
 ↓
CommandBus / QueryBus
 ↓
Use Case / Handler
 ↓
Domain Model (Aggregate)
 ↓
Domain Events
 ↓
Outbox (JPA)
 ↓
Projections (JPA read models)
 ↓
QueryBus
 ↓
Read Controllers
```

---

## ✅ Write Side (command)

Implémenté :

* CommandBus générique (registration automatique des handlers)
* Aggregate `Like`
* Use case `MakeLikeCommandHandler`
* Validation métier
* Enregistrement d’événements domaine
* Outbox persistante via JPA

### Stockage

```
Database (PostgreSQL via Testcontainers)
└── outbox_events
└── likes
└── comments (fake pour l’instant)
```

---

## 📤 Outbox Pattern

Lors d’une commande valide :

1. L’aggregate produit un événement domaine
2. Celui-ci est persisté en `outbox_events`
3. Un dispatcher (work in progress) lira l’outbox
4. Diffusion vers :

    * WebSocket
    * logs
    * futurs services externes

> but : **fiabilité / résilience / idempotence**

---

## ✅ Read Side (query)

Implémenté :

* QueryBus générique
* Projection JPA pour le statut de like
* Query handler :

```
GetLikeStatusQuery
↓
GetLikeStatusQueryHandler
↓
LikeProjectionRepository (JPA)
```

* REST endpoint :

```
GET /api/social/likes/{targetId}/status
```

### Contrat exposé (utilisé par le front)

```json
{
  "count": number,
  "me": boolean,
  "version": number,
  "serverTime": string
}
```

---

## ✅ Tests

Le projet dispose désormais d’une boucle complète testée :

### Unitaires

* logique métier du Like (aggregate)

### Intégration

* JPA repositories
* Outbox persistence

### End-to-End (E2E)

* HTTP → write → outbox → projection → read → HTTP

Basés sur :

* Spring Boot Test
* MockMvc
* Testcontainers (PostgreSQL)

---

## 🔥 Milestone atteint

✅ Boucle CQRS complète :

```
write command
→ domain
→ outbox
→ projection
→ read query
→ REST response
```

✅ Architecture stable
✅ Contrat front respecté
✅ Tests E2E réalistes

---

## 🚧 Prochaines étapes

* Traitement asynchrone de l’outbox

    * dispatcher périodique
    * WebSocket push
* read models supplémentaires (commentaires)
* auth / users context
* hardening (idempotence, retry, DLQ)

---

## 🧩 Technologies

* Java 21
* Spring Boot
* Spring Data JPA
* Testcontainers
* PostgreSQL
* WebSocket (à venir)
* Maven 

---

