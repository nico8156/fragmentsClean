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
│   │   ├── primary/springboot/          # controllers + SQS handlers
│   │   └── secondary/gateways/          # JPA + providers (C++/OpenAI/fakes)
│   └── configuration/
│
└── read/                                # read model (CQRS)
    ├── projections/                     # TicketStatusView + handlers
    ├── adapters/                        # REST + SQS handlers
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

* `TicketVerificationProcessManager`

➡️ Réagit à l’acceptation via SQS `ticket-verification-requested`
et exécute la vérification via un provider.

Règles de traitement :

* le provider `ticketverify` reçoit uniquement du texte OCR ;
* `imageRef` reste une référence de capture/image, utilisée par un flux OCR séparé ;
* un résultat `ok` confirme le ticket ;
* un résultat `partial` ne confirme jamais le ticket et devient un rejet métier explicite ;
* un rejet métier produit un `TicketVerificationCompletedEvent` ;
* une panne technique retryable ne produit pas d'event métier : elle remonte en exception pour laisser SQS redélivrer ;
* les erreurs techniques ne doivent pas être transformées en succès de consommation.

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

Le provider C++ est un moteur texte :

```text
ocrText UTF-8
-> ticketverify stdin
-> JSON stdout
-> TicketVerificationProvider.Result
```

Il ne lit pas l'image depuis `imageRef`. La vision native Apple ou tout autre OCR
produit le texte en amont. Aucun dump OCR local ni log de payload OCR complet ne
doit être produit par l'adapter.

### Packaging du moteur

Le backend appelle le moteur via `ProcessBuilderTicketVerificationProvider`.
Le chemin est configure par `ticketverify.binary-path` ou par
`TICKETVERIFY_BINARY_PATH`.

En developpement local, le binaire attendu est :

```text
fragmentsClean/bin/ticketverify
```

Pour le reconstruire depuis le repo moteur :

```bash
TICKETVERIFY_ENGINE_DIR=/Users/nicolasmaldiney/ticketverify-engine \
  ./scripts/build-ticketverify-engine.sh
```

En Docker/staging/prod, l'image backend ne reutilise pas ce binaire local,
qui peut etre macOS. Le `Dockerfile` reconstruit un binaire Linux depuis le
repo `nico8156/ticket_engine` au commit
`cdebb4e33cc419f5111a2a93b9a4f4f82e1b2bb5`, puis le copie vers
`/app/bin/ticketverify`.

---

### SQS

* `TicketSqsIntegrationEventHandlers`

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
* `GET /api/users/me/tickets?cursor=...&limit=...`

Les deux lectures sont privées et filtrées par l'identité authentifiée. La liste
utilise une pagination keyset et n'expose ni OCR, ni image, ni données de paiement.

---

### Projections

* `TicketVerifyAcceptedEventHandler`
* `TicketVerificationCompletedEventHandler`

➡️ Deux étapes principales :

1. on matérialise « la demande est acceptée »
2. on matérialise « la vérification est terminée »

Chaque handler publie ensuite un `ProjectionSyncEvent` uniquement après la mise
à jour de `ticket_status_projection` :

```text
ticket_status_projection updated
-> ProjectionSyncEvent(
     eventName="projection.updated",
     projection="tickets",
     scope="entity",
     entityId=ticketId,
     hints=["status", "..."]
   )
-> SSE
-> client GET /api/tickets/{ticketId}/status
```

Le frontend ne reçoit jamais `TicketVerifyAcceptedEvent` ni
`TicketVerificationCompletedEvent`.

Quand le résultat d'une vérification change, `ticketContext` publie son contrat
d'intégration stable. Le sous-module Pass de `userApplicationContext` le consomme
via sa propre inbox et met à jour sa projection locale :

```text
ticket.verification.completed / ticket.admin.updated / ticket.admin.deleted
-> app-users-events
-> inbox
-> pass_ticket_contributions
-> user_pass_projection
-> ProjectionSyncEvent(
     eventName="projection.updated",
     projection="entitlements",
     scope="user",
     entityId=userId,
     hints=["confirmedTickets"]
   )
-> SSE
-> client GET /api/users/me/entitlements
```

Le client reçoit ensuite un signal de projection, puis relit le snapshot. Le
handler ticket ne lit et ne modifie aucune table du Pass.

## Contribution au Pass

Le Pass n'appartient plus à `ticketContext`. Ce contexte fournit uniquement des
faits versionnés sur le résultat du ticket. `userApplicationContext` possède la
politique et le contrat mobile `GET /api/users/me/entitlements` :

```text
ticket integration events
-> userApplicationContext inbox
-> local ticket contribution
-> PassProgressPolicy v2
-> /api/users/me/entitlements
-> mobile entitlementWl read model
-> PassViewModel selector
-> Pass rings UI
```

Un reçu OCR strictement identique est protégé par une empreinte normalisée et une
contrainte d'unicité concurrente. Cette empreinte ne prétend pas détecter deux
photos différentes du même reçu ; les entrées sans OCR restent une limite connue.

---

### Repositories read

* `JdbcTicketStatusReadRepository`
* `JdbcTicketStatusProjectionRepository`
* `JdbcTicketHistoryReadRepository`

➡️ Le read model utilise une persistance optimisée (JDBC) pour les vues.

---

### SQS handler

* `TicketSqsIntegrationEventHandlers`

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
