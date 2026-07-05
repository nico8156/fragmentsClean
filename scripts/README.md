# Scripts

Ces scripts permettent de lancer une **démo E2E locale** du projet.

Ils sont pensés pour une démonstration rapide, reproductible et lisible (recruteur / reviewer).

---

## scripts/run-demo.sh

Démarre l’environnement de démo :

```bash
docker compose up -d
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

➡️ Lance :

* PostgreSQL
* Kafka
* Zookeeper
* Backend Spring Boot (profil demo)

---

## scripts/demo.sh

Lance une démo fonctionnelle complète :

```bash
./scripts/demo.sh
```

### Ce que le script fait

1. Génère un `ticketId` et un `commandId`
2. Envoie une requête `POST /api/tickets/verify`
3. Attend la prise en charge asynchrone
4. Poll le read-model `GET /api/tickets/{id}/status`
5. Affiche le résultat final

---

## Objectif

Ces scripts servent uniquement à la **vitrine projet** :

* démonstration rapide
* reproductibilité
* onboarding
* revue technique

Ils ne sont pas des outils de production.

---

## Tests backend

Les scripts suivants donnent des points d'entrée stables pour les suites Maven :

```bash
./scripts/test-unit.sh
./scripts/test-integration.sh
./scripts/test-all.sh
./scripts/testcontainers-check.sh
```

`test-unit.sh` lance les tests rapides en excluant les classes `*IT`, `*IntegrationTest` et `*LocalStackTest`.

`test-integration.sh` vérifie Docker puis passe par `scripts/backend-testcontainers`, qui configure `DOCKER_HOST` et `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` pour Testcontainers.

`test-all.sh` chaîne unit puis integration.

`testcontainers-check.sh` valide uniquement l'accès Docker/Testcontainers attendu.
