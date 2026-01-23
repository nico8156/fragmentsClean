# ticketContext

> Le **ticketContext** implémente une pipeline de **vérification de tickets** conçue comme un traitement **asynchrone**, **fiable** et **industrializable**.
>
> C’est le context le plus « démonstrateur » du projet : il combine **CQRS**, **outbox**, **event-driven**, intégration d’un **moteur externe (C++ CLI)** via un contrat strict, et un read model orienté UX.

---

## 🎯 Rôle fonctionnel

Ce context gère :

* la demande de vérification d’un ticket (OCR ou imageRef)
* l’acceptation asynchrone (202)
* l’exécution de la vérification (provider)
* la production du résultat (Approved / Rejected / Failed)
* l’exposition d’un statut lisible côté UI

👉 Le ticketContext n’est pas un endpoint de parsing.
C’est une **pipeline métier distribuée**.

---

## 🧠 Pourquoi cette architecture ?

### Pourquoi asynchrone ?

Parce que la vérification :

* dépend d’IO (moteur externe, OCR, etc.)
* peut être lente
* doit gérer timeout / erreurs

➡️ On évite de bloquer HTTP.
On accepte la requête (202) et on traite en arrière-plan.

---

### Pourquoi CQRS ?

Parce que :

* le write gère l’intention + règles + orchestration
* le read doit fournir un statut clair pour l’UI (poll/WS)

➡️ CQRS permet :

* write model orienté invariants
* read model orienté affichage
* projection simple et rapide

---

### Pourquoi outbox + event-driven ?

Parce qu’on veut :

* fiabilité (événements non perdus)
* propagation vers read model
* intégration facile avec d’autres contexts
* capacité de replay

➡️ L’outbox garantit que l’événement « résultat de vérification » est durable et émis exactement comme prévu.

---

### Pourquoi un moteur externe CLI (C++) ?

Parce que le parsing / extraction :

* peut évoluer indépendamment du backend
* nécessite parfois des libs/performances natives
* doit rester isolé (crash, encoding, deps)

➡️ Le CLI contractuel apporte :

* découplage total (black box)
* reproductibilité (même entrée → même sortie)
* testabilité (stdout JSON)
* robustesse (timeouts/exit codes)

> Le wrapper Java ne contient **aucune** logique métier : il orchestre, sécurise et mappe.

---

## 🧱 Structure

```
ticketContext/
├── write/                               # write model (CQRS)
│   ├── businesslogic/
│   │   ├── models/                      # Ticket + events
│   │   ├── usecases/                    # commands + event handlers
│   │   └── gateways/                    # ports
│   ├── adapters/
│   │   ├── primary/springboot/          # controllers + kafka listener
│   │   └── secondary/gateways/          # JPA + providers (C++/OpenAI/fakes)
│   └── configuration/
│
└── read/                                # read model (CQRS)
    ├── projections/                     # TicketStatusView + handlers
    ├── adapters/                        # REST + Kafka listener
    └── repositories/                    # JDBC projection repository
```

---

## ✍️ Write side — intentions et orchestration

### Entrée API

* `WriteTicketController`
* `TicketVerifyRequestDto`

➡️ Reçoit une demande de vérification et publie une commande.

Le contrôleur ne fait pas de parsing.
Il transforme HTTP → Command.

---

### Command

* `VerifyTicketCommand`
* `VerifyTicketCommandHandler`

➡️ La commande représente l’intention : « vérifier ce ticket ».

Elle valide les prérequis et déclenche le traitement asynchrone.

---

### Events métier

* `TicketVerifyAcceptedEvent`
* `TicketVerificationCompletedEvent`

➡️ Le système parle en **faits métier**.

---

### Orchestration asynchrone

* `ProcessTicketVerificationEventHandler`

➡️ Réagit à l’acceptation (ou au message Kafka) et exécute la vérification via un provider.

---

### Ports

* `TicketRepository`
* `TicketVerificationProvider`

➡️ Le domaine dépend d’abstractions.

---

## 🔌 Adapters write

### Persistance

* `JpaTicketRepository` / `SpringTicketRepository`
* `TicketJpaEntity`

➡️ Le write model persiste l’état du ticket et son avancement.

---

### Providers de vérification

Le provider est interchangeable :

* `ProcessBuilderTicketVerificationProvider` (moteur C++ CLI)
* `OpenAiTicketVerificationProvider` (optionnel / expérimentation)
* `FakeTicketVerificationProvider` (tests)

➡️ Le domaine ne dépend d’aucun provider concret.

---

### Kafka

* `TicketVerificationRequestsKafkaListener`

➡️ Permet d’intégrer le traitement dans une pipeline event-driven.

---

## 📖 Read side — statut UX

### Objectif

Fournir une vue simple, stable, utile à l’UI.

* `status` (ex: CONFIRMED)
* `outcome` (APPROVED/REJECTED/FAILED)
* `amountCents`, `currency`
* `merchantName`
* `ticketDate`, etc.

➡️ Le read model est fait pour l’affichage, pas pour les invariants.

---

### REST

* `ReadTicketController`

Endpoint de lecture :

* `GET /api/tickets/{ticketId}/status`

---

### Projections

* `TicketVerifyAcceptedEventHandler`
* `TicketVerificationCompletedEventHandler`

➡️ Deux étapes principales :

1. on matérialise « la demande est acceptée »
2. on matérialise « la vérification est terminée »

---

### Repositories read

* `JdbcTicketStatusReadRepository`
* `JdbcTicketStatusProjectionRepository`

➡️ Le read model utilise une persistance optimisée (JDBC) pour les vues.

---

### Kafka listener

* `TicketEventsKafkaListener`

➡️ Le read model se reconstruit à partir des événements.

---

## 🧪 Testabilité

Ce context est conçu pour être testable à chaque niveau :

* domaine pur (handlers/usecases)
* providers fake
* CLI fake possible
* projection read testable
* E2E possible (docker compose + scripts)

Les invariants sont isolés du framework.

---

## 🎯 Ce que ce context démontre

* pipeline distribuée robuste
* séparation write/read
* outbox fiable
* contrat inter-composants strict (CLI)
* intégration d’un moteur externe sans fuite de logique métier
* read model orienté UX

---

## 🏁 Objectif

Le ticketContext montre comment construire un traitement “lourd” de manière :

* fiable
* testable
* découplée
* évolutive

C’est une pièce maîtresse de la vitrine technique.

---
