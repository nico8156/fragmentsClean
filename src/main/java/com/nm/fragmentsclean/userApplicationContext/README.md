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

Ce context est souvent alimenté par des événements venant du `authenticationContext`.

---

## 🔁 Event-driven : user app synchronisé depuis auth

Le `authenticationContext` émet un événement du type :

* `AuthUserCreatedEvent`

Le `userApplicationContext` réagit :

* `AuthUserCreatedEventHandler`

➡️ Il matérialise un `AppUser` côté application.

> L’auth ne “possède” pas le user applicatif. Il lui donne une identité de départ.

---

## 🧩 Architecture

### Modèle métier

* `AppUser`
* `AppUserCreatedEvent`
* `AppUserProfileUpdatedEvent`

### Use case

* `AuthUserCreatedEventHandler`

### Port

* `AppUserRepository`

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
