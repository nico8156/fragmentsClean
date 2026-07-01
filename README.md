![Java](https://img.shields.io/badge/java-21-blue)
![Spring Boot](https://img.shields.io/badge/springboot-3.x-green)
![SQS](https://img.shields.io/badge/aws-sqs-orange)
![Docker](https://img.shields.io/badge/docker-ready-blue)
![React Native](https://img.shields.io/badge/mobile-react--native-61dafb)
![C++](https://img.shields.io/badge/c%2B%2B-20-orange)
![Architecture](https://img.shields.io/badge/architecture-CQRS%20%2B%20Hexagonal-purple)
![Offline-first](https://img.shields.io/badge/ux-offline--first-success)

# FragmentsClean

> **FragmentsClean** est une plateforme mobile **offline-first**, orientée expérience utilisateur, construite sur une architecture **event‑driven**, **CQRS**, **hexagonale**, et pensée pour des systèmes distribués robustes.
>
> Le projet démontre une approche **production‑grade** : séparation des contextes métier, pipelines asynchrones, outbox, projections read, contrats stricts entre composants, et intégration d’un moteur natif externe.

---

## 🔗 Navigation rapide

* [Vision](#-vision)
* [Ce que le projet démontre](#-ce-que-le-projet-démontre)
* [Contexts métier](#-contexts-métier)
* [Pipeline Ticket](#-pipeline-démonstrateur-ticket-verification)
* [Démo locale](#-démo-locale-recruteur-ready)
* [Organisation du projet](#-organisation-du-projet)
* [Highlights techniques](#-highlights-techniques)
* [Choix d’architecture](#-choix-darchitecture-trade-offs)
* [Qualité & testabilité](#-qualité--testabilité)
* [Roadmap](#-roadmap-courte)
* [Pitch](#-pitch-technique-30-secondes)
* [Documentation](#-documentation-par-domaine-et-composants)

---

## 🎯 Vision

FragmentsClean est conçu comme une plateforme modulaire orientée **domain‑driven design** permettant :

* des interactions mobiles **offline‑first**
* des traitements métier **asynchrones**
* des pipelines distribuées
* une architecture testable, évolutive et industrialisable

L’objectif n’est pas un simple prototype, mais un **socle applicatif réel**, structuré comme un produit.

---

## 🧠 Ce que le projet démontre

### Architecture

* Architecture **hexagonale (ports/adapters)**
* **CQRS** (write model / read model séparés)
* **Event‑driven architecture**
* **Outbox pattern**
* **Projections read**
* **AWS SQS** comme transport cible MVP
* **WebSocket ACK**
* **Idempotence / retry / backoff**

### Plateforme

* Backend : Spring Boot
* Mobile : React Native (offline‑first)
* Engine natif : C++ (CLI contractuel)
* Communication inter‑services : événements
* Séparation stricte : domaine / application / infra

---

## 🧩 Contexts métier

Le projet est structuré en **bounded contexts** indépendants :

* **authenticationContext**
  Authentification, OAuth2, JWT, gestion des identités

* **userApplicationContext**
  User applicatif (profil, compte, données produit), distinct de l’auth

* **coffeeContext**
  Référentiel des lieux, socle spatial du produit

* **articleContext**
  Contenus éditoriaux, modèle CQRS, template architectural

* **socialContext**
  Likes, commentaires, interactions sociales, temps réel

* **ticketContext**
  Vérification de tickets, pipeline de traitement asynchrone, intégration moteur natif

Chaque context possède :

* son modèle de domaine
* ses commandes
* ses événements
* ses projections read
* ses adapters

---

## 🔁 Pipeline démonstrateur (ticket verification)

Le use‑case **Ticket Verification** sert de démonstration E2E complète :

```
Mobile App (RN)
   ↓
Outbox client
   ↓
Spring Boot (Command)
   ↓
Outbox
   ↓
SQS / local event bus dev
   ↓
Event Handler
   ↓
ProcessBuilder Provider
   ↓
Engine C++ (CLI)
   ↓
JSON contractuel
   ↓
Mapping domaine
   ↓
Event
   ↓
Projection Read
   ↓
API Query
   ↓
WebSocket ACK / Poll
```

### Points clés

* moteur natif externe **isolé** (C++ CLI)
* contrat **stdout JSON strict**
* timeout contrôlé
* exit codes
* parsing robuste
* mapping domaine propre
* aucun code métier dans le wrapper Java

---

## 🚀 Démo locale (recruteur‑ready)

### Prérequis

* Docker
* Java 21
* Maven

### Lancer la démo

```bash
docker compose up -d
./scripts/run-demo.sh
```

Dans un autre terminal :

```bash
./scripts/demo.sh
```

### Ce que la démo montre

* POST asynchrone `/api/tickets/verify`
* pipeline event‑driven
* appel moteur C++
* projection read
* polling read model
* réponse finale métier

---

## 🧭 Organisation du projet

```
fragmentsClean/
├── authenticationContext/
├── userApplicationContext/
├── coffeeContext/
├── articleContext/
├── socialContext/
├── ticketContext/
├── sharedKernel/
├── bin/
│   └── ticketverify
├── scripts/
│   ├── run-demo.sh
│   └── demo.sh
├── docker-compose.yml
├── src/
├── README.md
```

---

## ⭐ Highlights techniques

Points clés à explorer dans le code :

* `ProcessBuilderTicketVerificationProvider`

  * gestion stdin/stdout
  * timeout
  * exit codes
  * parsing JSON

* Outbox dispatcher / consumer

* CQRS command handlers

* Projections read

* Event contracts

* CLI contract moteur C++

---

## 🧠 Choix d’architecture (trade‑offs)

* CQRS pour découpler écriture / lecture
* Outbox pour garantir la fiabilité des événements
* CLI contractuel pour l’engine → découplage total
* Event‑driven pour scalabilité
* Hexagonal pour testabilité
* Offline‑first pour UX mobile

---

## 🧪 Qualité & testabilité

* Tests unitaires domaine
* Adapters fake
* CLI fake pour tests
* Testcontainers
* Architecture orientée tests
* contrats stricts

---

## 🧭 Roadmap courte

* stabilisation packaging engine C++
* versioning binaire
* schéma JSON versionné
* observabilité (traceId)
* enrichissement parsing

---

## 🎤 Pitch technique (30 secondes)

> « FragmentsClean est une plateforme mobile offline‑first construite sur une architecture event‑driven CQRS.
> Elle intègre un moteur natif C++ via un contrat CLI strict, utilise une pipeline asynchrone avec outbox, SQS comme transport cible et projections read, et démontre une architecture production-grade testable, modulaire et scalable. »

---

## 📚 Documentation par domaine et composants

La référence architecturale actuelle est :

* `AGENTS.md` : doctrine générale Fragments.
* `.agents/backend/AGENTS.md` : règles backend pour agents et développeurs.
* `.agents/backend/routing.md` : sélection de l'orchestrator avant implémentation.
* `docs/architecture/README.md` : index des décisions d'architecture.

Cette documentation remplace l'ancienne lecture "Kafka par défaut" : le transport cible MVP est désormais SQS, avec WebSocket opportuniste et `/commands/{commandId}` comme fallback canonique mobile.

### 🧩 Bounded Contexts

* 🔐 Authentication / Identity
  `src/main/java/com/nm/fragmentsclean/authenticationContext/README.md`

* 👤 User Application
  `src/main/java/com/nm/fragmentsclean/userApplicationContext/README.md`

* ☕ Coffee
  `src/main/java/com/nm/fragmentsclean/coffeeContext/README.md`

* 📰 Article
  `src/main/java/com/nm/fragmentsclean/articleContext/README.md`

* 💬 Social
  `src/main/java/com/nm/fragmentsclean/socialContext/README.md`

* 🎫 Ticket
  `src/main/java/com/nm/fragmentsclean/ticketContext/README.md`

### 🧱 Infrastructure & Architecture

* 🧠 Shared Kernel
  `src/main/java/com/nm/fragmentsclean/sharedKernel/README.md`

### ⚙️ Composants techniques

* 🧠 Moteur natif C++
  `ticketverify-engine/README.md`

* 🎬 Scripts de démonstration
  `scripts/README.md`

---

## 🏁 Statut

**v1-demo** — démo E2E stable, reproductible, vitrine technique prête pour démonstration, review et entretien.
