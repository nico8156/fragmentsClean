# socialContext

> Le **socialContext** gère les interactions sociales autour des entités du produit : **likes** et **commentaires**.
>
> Au-delà des features (like/comment), il sert de démonstrateur de **fiabilité** et de **temps réel** : CQRS, événements, projections, idempotence et ACK WebSocket.

---

## 🎯 Rôle fonctionnel

Ce context couvre :

* poser / retirer un like
* créer / modifier / supprimer un commentaire
* exposer des résumés (count)
* exposer le statut utilisateur (`me`) : « est-ce que *moi* j’ai liké ? »
* exposer des listes paginées de commentaires

👉 C’est le « feedback loop » de l’app : rapide, fréquent, et très visible côté UX.

---

## 🧠 Pourquoi ce context est important

Les interactions sociales sont :

* **très fréquentes**
* **sensibles aux latences réseau**
* **souvent exécutées en mobilité**

Donc l’enjeu n’est pas seulement *fonctionnel*.

➡️ L’enjeu est **l’expérience utilisateur** :

* un like doit “répondre” immédiatement
* un commentaire doit être fiable
* une suppression doit être cohérente
* tout doit rester correct malgré : offline, retry, duplications, latences

---

## 🧩 Pourquoi cette architecture ?

### Pourquoi CQRS ?

Parce que :

* le write impose des invariants (ownership, règles, idempotence)
* le read doit être optimisé UI (count, `me`, pagination)

➡️ CQRS permet :

* write model orienté règles
* read model orienté rendu
* évolutions indépendantes

---

### Pourquoi event-driven + projections ?

Les vues sociales attendues par l’UI ne sont pas juste des lectures de tables.
Elles sont dérivées :

* `count`
* `me`
* `version`
* vues agrégées par cible (post/café/article…)

➡️ Les événements sont le mécanisme de propagation et de reconstruction des read models.

---

### Pourquoi outbox + ACK WebSocket ?

Likes/commentaires sont sujets à :

* retry
* duplications
* latence
* offline

➡️ L’outbox garantit :

* fiabilité (pas d’événement perdu)
* replays possibles

➡️ Le WebSocket ACK garantit :

* feedback immédiat côté client
* confirmation serveur (sans polling lourd)

> Le temps réel est traité comme une sortie du pipeline outbox, pas comme un `send()` direct depuis un handler.

---

## 🧱 Structure

```
socialContext/
├── write/                         # write model (CQRS)
│   ├── businesslogic/             # domaine pur
│   │   ├── models/                # Like, Comment, events
│   │   ├── usecases/              # commands handlers
│   │   └── gateways/              # ports (repositories)
│   └── adapters/
│       ├── primary/springboot/    # controllers
│       └── secondary/             # JPA + fakes
│
└── read/                          # read model (CQRS)
    ├── projections/               # vues + handlers d’événements
    ├── projectors/                # projectors transverses (users)
    ├── adapters/                  # REST + Kafka listeners
    └── configuration/             # wiring
```

---

## ✍️ Write side — commandes et invariants

### Modèle métier

* `Like`
* `Comment`
* `ModerationStatus`

### Events métier

* `LikeSetEvent`
* `CommentCreatedEvent`
* `CommentUpdatedEvent`
* `CommentDeletedEvent`

➡️ Les modifications sociales produisent des **faits métier**.

---

### Use cases

* `MakeLikeCommand` / `MakeLikeCommandHandler`
* `CreateCommentCommand` / `CreateCommentCommandHandler`
* `UpdateCommentCommand` / `UpdateCommentCommandHandler`
* `DeleteCommentCommand` / `DeleteCommentCommandHandler`

➡️ Chaque intention UI correspond à un use case explicite.

---

### Ports

* `LikeRepository`
* `CommentRepository`

➡️ Le domaine dépend d’abstractions.

---

### Adapters

**Primary (entrée)**

* `WriteLikeController`
* `WriteCommentController`

➡️ Adaptation HTTP → Command, sans logique métier.

**Secondary (sortie)**

* JPA : `JpaLikeRepository`, `JpaCommentRepository`, `entities/*`
* Fakes : `FakeLikeRepository`, `FakeCommentRepository`

➡️ Les fakes permettent des tests rapides et déterministes.

---

## 📖 Read side — vues orientées UX

### Ce que l’UI veut réellement

L’UI a besoin de réponses comme :

* "combien ?" (`count`)
* "moi ?" (`me`)
* "quoi ?" (liste/pagination)

➡️ D’où des projections dédiées :

* `LikeSummaryView`
* `LikeStatusView`
* `CommentsListView`, `CommentItemView`, `CommentView`

Les likes sont lus depuis `social_likes_projection`.
Le read side ne lit pas le repository JPA write `likes`.

---

### Queries

* `GetLikeSummaryQuery` / handler
* `GetLikeStatusQueryHandler`
* `ListCommentsQuery` / handler

---

### Projection par événements

* `LikeSetEventHandler`
* `CommentCreatedEventHandler`
* `CommentUpdatedEventHandler`
* `CommentDeletedEventHandler`

➡️ Le read model est reconstruit en réaction aux events, pas en lisant le write model.

Pour les likes :

```text
LikeSetEvent
-> social_likes_projection
-> ProjectionSyncEvent(projection="likes", scope="target", entityId=targetId)
-> SSE
-> client GET /api/social/targets/{targetId}/likes
-> snapshot reducer
```

Le frontend ne reçoit pas `LikeSetEvent`. Il reçoit seulement la fraîcheur de la
projection puis relit le snapshot.

---

### Kafka listeners

* `SocialEventsKafkaListener`
* `AppUsersEventsKafkaListener`

➡️ Le socialContext consomme :

* ses propres événements
* et des événements « users » nécessaires pour projeter des infos publiques (ex: auteur)

---

## 🔁 Consistance, idempotence, versioning

Ce context est pensé pour être robuste à :

* double clic like
* retry mobile
* offline + resync
* concurrency

Les vues read intègrent généralement des informations utiles à l’UI :

* `count`
* `me`
* `version`
* `serverTime` (si exposé)

➡️ Ce sont des outils de **convergence** client/serveur.

---

## 🧪 Testabilité

* domaine pur (models/usecases)
* ports mockables
* fakes de repositories
* projections testables
* listeners Kafka testables

➡️ Tests unitaires rapides + E2E possibles.

---

## 🎯 Objectif

Le socialContext montre comment implémenter des interactions à haute fréquence :

* instantanées côté UX
* fiables côté backend
* cohérentes en distribué

Il démontre un pattern réutilisable pour d’autres interactions utilisateur.

---

> "Likes et commentaires ne sont pas des features simples : ce sont des features qui testent la robustesse d’un système."
