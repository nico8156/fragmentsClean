# FragmentsClean — short

**FragmentsClean** est une plateforme mobile **offline-first** construite sur une architecture **event-driven**, **CQRS** et **hexagonale**, pensée pour des systèmes distribués robustes et industrialisables.

Le projet démontre une architecture **production-grade** :

* pipelines asynchrones
* outbox pattern
* projections read
* event-driven
* intégration d’un moteur natif externe (C++ CLI contractuel)

Use-case démonstrateur :

> **Vérification de tickets** via pipeline distribuée : RN outbox → Spring Boot CQRS → Kafka → moteur C++ → projections read → WebSocket ACK.

Points différenciants :

* offline-first réel
* découplage strict domaine / infra
* moteur externe isolé
* contrats inter-composants
* testabilité E2E

🎯 Objectif : montrer comment construire une plateforme moderne, modulaire, fiable et scalable — pas un simple POC, mais un **socle applicatif réel**.

👉 Démo locale reproductible, architecture documentée, contexts métiers séparés.

`v1-demo` — projet vitrine / portfolio technique

