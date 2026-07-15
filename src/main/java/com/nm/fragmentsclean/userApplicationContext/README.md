# userApplicationContext

> Le **userApplicationContext** gère l’utilisateur **dans l’application** : profil, identité applicative, données publiques et informations de compte.
>
> Il est volontairement **distinct** du `authenticationContext`.

---

## 🎯 Pourquoi séparer `authenticationContext` et `userApplicationContext` ?

Parce qu’il existe deux notions différentes de « user » :

### 1) L’identité (authenticationContext)

* authentifier un humain
* vérifier un provider (Google)
* gérer les tokens (JWT, refresh)
* sécuriser les accès

➡️ C’est une **problématique de sécurité et de confiance**.

### 2) Le user applicatif (userApplicationContext)

* profil dans le produit
* informations publiques
* préférences
* données de compte
* cafés enregistrés par l’utilisateur (`saved coffees`)

➡️ C’est une **problématique produit**.

> L’auth est *une fondation*, le user app est *un concept métier*.

---

## 🧠 Bénéfices de cette séparation

* on évite de mélanger sécurité et produit
* on peut faire évoluer le profil sans toucher à l’auth
* on peut changer de provider d’auth sans casser le modèle utilisateur
* on prépare le multi-provider / multi-clients
* on découple le domaine « identité » du domaine « usage »

➡️ Cette séparation simplifie l’architecture à long terme.

---

## 🎯 Rôle fonctionnel

Le userApplicationContext assure :

* la création d’un `AppUser` côté application
* la synchronisation depuis l’identité (auth user)
* la gestion des événements de profil

Ce context est souvent alimenté par un contrat d’intégration public venant du
`authenticationContext`.

---

## 🔁 Event-driven : user app synchronisé depuis auth

Le `authenticationContext` décide et persiste son Domain Event interne, puis le
pipeline outbox/SQS expose un contrat public :

* `auth.user.created`
* `AuthUserCreatedIntegrationEvent`

Le `userApplicationContext` réagit :

* `AuthUserCreatedEventHandler`

➡️ Il matérialise un `AppUser` côté application.

> L’auth ne “possède” pas le user applicatif. Il lui donne une identité de départ.
> Le `userApplicationContext` ne désérialise pas les Domain Events internes
> du `authenticationContext`.

---

## 🧩 Architecture

### Modèle métier

* `AppUser`
* `SavedCoffee`
* `AppUserCreatedEvent`
* `AppUserProfileUpdatedEvent`
* `SavedCoffeeSetEvent`

### Use case

* `AuthUserCreatedEventHandler`
* `SetSavedCoffeeCommandHandler`

### Port

* `AppUserRepository`
* `SavedCoffeeRepository`

## Saved coffees

Un `SavedCoffee` représente une préférence privée utilisateur : “je veux retrouver
ce café plus tard”. Il ne remplace pas le like.

Différences avec `socialContext` :

* le like est un signal social attaché à une cible et exposé en compteur ;
* le saved coffee est une préférence personnelle attachée à un `AppUser` ;
* il ne participe pas aux entitlements ;
* il doit être disponible cross-device et offline-first côté mobile.

Le flux mobile attendu est :

```text
tap enregistrer
-> commande SetSavedCoffee
-> outbox backend
-> SavedCoffeeSetEvent
-> user_saved_coffees_projection
-> projection.updated savedCoffees/user/{userId}
-> mobile GET /api/users/me/saved-coffees
```

Le read model `savedCoffees` maintient aussi une petite projection locale des
cafés alimentée par les événements stables `coffee.created`, `coffee.archived`
et `coffee.deleted`. Le read side ne lit donc pas les tables du `coffeeContext`.

---

## 🔌 Adapters

### SQS handler (entrée)

* `AuthUserCreatedSqsIntegrationEventHandler`

➡️ Le context consomme les événements d’identité.

---

### Repository JPA (sortie)

* `JpaAppUserRepository`
* `SpringAppUserRepository`
* `AppUserJpaEntity`

➡️ Persistant uniquement le user applicatif.

---

## 🧱 Structure

```
userApplicationContext/
├── write/
│   ├── businesslogic/
│   │   ├── models/
│   │   ├── usecases/
│   │   └── gateways/
│   └── adapters/
│       ├── primary/springboot/sqs/
│       └── secondary/gateways/repositories/jpa/
└── read/
    ├── projections/
    └── configuration/
```

---

## 🧪 Testabilité

* domaine pur (models/usecases)
* port `AppUserRepository` mockable
* handler SQS testable

---

## 🎯 Objectif

Le userApplicationContext existe pour garantir que :

* l’identité reste une fondation de sécurité
* le user applicatif reste un concept produit

Et que ces deux mondes peuvent évoluer séparément.

---

> "Un user authentifié n’est pas encore un user produit. Le user produit est matérialisé par l’application."
