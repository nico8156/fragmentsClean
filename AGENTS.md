# AGENTS.md — Fragments Architecture Blueprint

Ce document sert de **blueprint de référence pour les agents IA, Codex, et les développeurs**.
Il décrit la hiérarchie fonctionnelle du projet, les responsabilités de chaque couche, et les invariants à respecter.

---

# 1) Vision globale

Le système repose sur une architecture :

* **Feature-first**
* **DDD / Hexagonale**
* **CQRS**
* **Offline-first côté front**
* **Outbox pattern front + back**
* **ACK websocket pour réconciliation UI**
* **Kafka + projections côté backend**

La règle principale :

> chaque couche a une responsabilité stricte et ne doit pas absorber celle des autres.

L’objectif est de garantir :

* lisibilité
* granularité
* testabilité
* résilience réseau
* évolutivité
* séparation write/read

---

# 2) Frontend AGENT RULES

## 2.1 Architecture fonctionnelle

Hiérarchie stricte :

```text
Component UI
   ↓
Hooks VM
   ↓
Selectors
   ↓
Redux Store
   ↓
Use Cases
   ↓
Outbox
   ↓
Gateways / adapters secondaires
   ↓
Server / WebSocket / SecureStore / Expo APIs
```

Chaque niveau a un rôle précis.

---

## 2.2 Components

Les composants :

* ne contiennent pas de logique métier
* consomment uniquement les hooks VM
* restent déclaratifs
* pilotent le rendu UI
* déclenchent uniquement des callbacks exposés par le VM

Interdit :

* accès direct au store
* appels gateway
* logique optimistic update
* gestion ACK
* logique réseau

---

## 2.3 Hooks VM

Les hooks VM sont la **frontière UI → logique applicative**.

Ils :

* branchent les selectors
* exposent un contrat stable au composant
* déclenchent les use cases
* gèrent les effets UI locaux
* assurent des valeurs par défaut sûres
* encapsulent loading / refreshing / stale logic

Exemple : `useLikesForCafe()`.

Le VM doit toujours retourner un état exploitable même si le store est vide.

---

## 2.4 Selectors

Les selectors sont responsables de :

* lire le store
* transformer la donnée
* appliquer fallback defaults
* garantir un **VM-safe output**
* éviter les `undefined` côté composant
* centraliser les dérivations

Ils ne mutent jamais le state.

---

## 2.5 Use cases

Le use case est **un orchestrateur d’intention métier**.

Il ne représente pas la donnée.

Il :

* écoute une action intentionnelle UI
* lit l’état courant
* décide de la transition
* applique l’optimistic update
* crée la commande outbox
* déclenche le process outbox

Exemple : `uiLikeToggleRequested`.

Règle :

> le use case orchestre, il ne fait pas l’infrastructure.

---

## 2.6 Outbox frontend

L’outbox est la couche de fiabilité.

Responsabilités :

* persistance locale
* retry
* exponential backoff
* jitter
* squash éventuel
* reprise offline/online
* idempotence par commandId
* rollback via undo
* polling éventuel command status

Règle :

> toute opération write critique doit pouvoir survivre à une perte réseau ou fermeture app.

---

## 2.7 ACK websocket

L’ACK websocket est la **source de vérité finale write → UI**.

Il permet :

* reconcile state
* finaliser optimistic update
* invalider si échec
* drop outbox item
* afficher feedback sync UI

Exemple :

* pending ring
* acked green ring
* failed red ring

---

## 2.8 Runtime listeners

Les listeners runtime maintiennent la cohérence avec le cycle de vie de l’app.

Sources :

* foreground/background
* connectivité
* auth session refresh
* websocket reconnect
* watchdog timers

Ils garantissent :

* reprise outbox
* refresh read side
* reconnexion WS
* resynchronisation après suspension

---

# 3) Backend AGENT RULES

## 3.1 Write flow

Hiérarchie :

```text
Controller
   ↓
CommandBus
   ↓
CommandHandler
   ↓
Aggregate
   ↓
Domain Events
   ↓
Outbox DB
   ↓
Outbox Sender
   ↓
Kafka / EventBus / Socket ACK
```

---

## 3.2 Controllers

Les controllers sont volontairement **très petits**.

Ils :

* valident l’entrée HTTP
* récupèrent le principal JWT
* construisent la command
* dispatchent au CommandBus
* retournent `202 Accepted`

Ils ne doivent jamais :

* contenir la logique métier
* accéder aux repositories
* publier Kafka
* écrire le read model

---

## 3.3 Command handlers

Le command handler est l’**orchestrateur write-side**.

Responsabilités :

* charger l’agrégat
* créer si absent
* appliquer mutation métier
* persister
* enrichir avec données calculées
* publier les domain events

Exemple : `MakeLikeCommandHandler`.

---

## 3.4 Domaine

Le domaine porte :

* l’état métier
* les invariants
* les transitions
* le versioning
* les domain events

Exemple `Like` :

* `applyState()` = mutation métier pure
* `registerLikeSetEvent()` = émission événement riche

Règle :

> la mutation métier appartient au domaine, jamais au controller.

---

## 3.5 Outbox backend

Le publisher outbox persiste chaque événement dans la base **dans la même transaction que le write side**.

But :

* atomicité
* retry
* résilience
* no lost event

Le sender secondaire diffuse ensuite vers :

* Kafka
* EventBus interne
* logging
* socket ack

Le `CompositeOutboxEventSender` permet de composer plusieurs destinations.

---

## 3.6 Kafka & projections

Kafka sert à alimenter le read side.

Les listeners Kafka :

* restent fins
* routent par type d’événement
* délèguent aux projection handlers

Les projection handlers :

* appliquent les événements
* écrivent dans les tables read
* restent idempotents
* respectent le version gating

Exemple : `JdbcCommentProjectionRepository`.

---

## 3.7 Query side

Le read side expose des **query handlers dédiés**.

Responsabilités :

* pagination
* cursorisation
* enrichissement view model
* batch lookup secondaires
* contrat API lecture

Exemple : `ListCommentsQueryHandler`.

Règle :

> le read side ne réutilise pas les entités write.

---

# 4) Invariants absolus

## Front

* pas de logique métier dans les composants
* pas de réseau hors gateways
* pas de store brut dans UI
* ACK = vérité finale
* offline-first obligatoire sur writes critiques

## Back

* controller mince
* handler = orchestration
* domaine = mutation + events
* outbox transactionnelle
* Kafka pour read side
* query handlers dédiés

---

# 5) Règle Codex / Agents

Quand un agent modifie le projet :

1. respecter la hiérarchie
2. ne pas court-circuiter use case / command handler
3. ne pas injecter de logique dans UI ou controller
4. préserver outbox + ACK flow
5. préserver séparation write/read
6. préserver testabilité par couche

---

# 6) Philosophie

La complexité ajoutée est volontaire.

Elle sert à gagner :

* précision
* robustesse
* testabilité
* résilience
* vitesse d’évolution

La règle d’or :

> chaque couche a une place fonctionnelle précise.

C’est cette discipline qui garantit la maintenabilité long terme du produit.

---

# 7) Backend AGENTS.md — Deep conventions Spring / CQRS / Testcontainers

Cette section formalise les conventions d’implémentation backend afin de garder une cohérence forte entre les contexts.

## 7.1 Packaging strategy

Structure recommandée par bounded context :

```text
<context>
 ├── write
 │   ├── adapters
 │   │   ├── primary
 │   │   │   └── springboot/controllers
 │   │   ├── secondary
 │   │   │   └── repositories / gateways
 │   ├── businesslogic
 │   │   ├── models
 │   │   ├── usecases
 │   │   └── gateways
 │
 └── read
     ├── adapters
     │   ├── primary
     │   │   └── springboot / kafka
     │   └── secondary
     │       └── repositories
     ├── projections
     └── queryhandlers
```

Règle :

> on sépare physiquement write/read pour rendre CQRS évident dans le code.

---

## 7.2 Spring wiring rules

Spring doit rester une couche de câblage.

Utiliser Spring pour :

* controllers
* listeners Kafka
* repositories JPA/JDBC
* beans de configuration
* transaction boundaries
* injection des handlers

Éviter :

* annotations Spring dans le domaine si possible
* logique métier dans les `@Configuration`
* dépendance du domaine à Spring

---

## 7.3 Command bus conventions

Le `CommandBus` est la porte d’entrée du write side.

Convention :

* 1 command = 1 intention métier
* 1 handler = 1 command
* résolution explicite du handler
* pas de logique transverse dans le bus

Le bus route uniquement.

Le handler orchestre.

---

## 7.4 Aggregate conventions

Chaque aggregate doit :

* protéger ses invariants
* encapsuler son état
* exposer des mutations explicites
* versionner les transitions
* enregistrer les domain events

Pattern recommandé :

```text
load/create aggregate
→ mutate
→ save
→ publish domain events
→ clear events
```

---

## 7.5 Outbox transactionnelle

Invariant absolu :

> write DB state + outbox event doivent vivre dans la même transaction.

À préserver :

* `@Transactional` sur handler / service write
* persistance outbox synchrone
* sender asynchrone séparé
* retry count
* statuts PENDING / SENT / FAILED

---

## 7.6 Kafka conventions

Kafka ne transporte pas du métier implicite.

Chaque message doit avoir :

* payload JSON stable
* event type header
* aggregate type header
* aggregate id header
* outbox id
* stream key déterministe

Clé recommandée :

```text
user:<userId>
coffee:<coffeeId>
social:<targetId>
```

But :

* ordering local
* replay fiable
* partitionnement stable

---

## 7.7 Projection rules

Les projections sont :

* append/update orientées lecture
* idempotentes
* version-gated
* optimisées produit

Règle importante :

> la projection n’essaie jamais de reconstruire le domaine write.

Elle construit uniquement le modèle utile à la lecture.

---

## 7.8 Query handlers

Les query handlers doivent :

* lire les projections
* gérer pagination / cursor
* enrichir si besoin
* batcher les lookups secondaires
* retourner des DTOs orientés produit

Ils ne doivent pas :

* republier des événements
* muter le write side
* appeler les aggregates

---

## 7.9 Socket ACK rules

Le socket ACK est le pendant backend du flow optimistic frontend.

Responsabilités :

* notifier succès final
* renvoyer version serveur
* count final
* timestamp serveur
* commandId de corrélation

Règle :

> l’ACK doit toujours pouvoir réconcilier exactement l’optimistic state frontend.

---

## 7.10 Tests strategy

La stratégie de test attendue :

### Domaine

* tests unitaires purs
* invariants
* versioning
* émission events

### Use cases / handlers

* orchestration
* transactions
* publication outbox

### Infrastructure

* JPA repositories
* JDBC projections
* Kafka listeners
* socket gateways

### End-to-end

Toujours privilégier **Testcontainers** pour :

* PostgreSQL réel
* Kafka réel
* projections read
* validation end-to-end outbox → Kafka → read side

Règle :

> un flux critique doit être testé de l’HTTP jusqu’à la projection finale.

---

## 7.11 Anti-patterns interdits

À éviter absolument :

* logique métier dans controllers
* query directe sur tables write pour servir le read
* listener Kafka avec logique métier complexe
* outbox sender connaissant des règles produit
* projection couplée au domaine write
* DTO HTTP réutilisé comme event Kafka

---

## 7.12 Golden rule

Le backend doit rester lisible comme un pipeline :

```text
HTTP
→ CommandBus
→ Handler
→ Aggregate
→ Outbox
→ Kafka / Socket
→ Projection
→ Query
```

Si une modification casse cette lecture, elle doit être revue.

