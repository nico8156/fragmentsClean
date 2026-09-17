# authenticationContext

> Le **authenticationContext** est le bounded context responsable de l’identité, de l’authentification et de la sécurité applicative.
>
> Il ne cherche pas à implémenter « une auth parfaite théorique », mais une **authentification pragmatique, fiable et industrialisable**, adaptée à un produit réel.

---

## 🎯 Rôle fonctionnel

Ce context couvre :

* authentification des utilisateurs
* gestion des identités
* gestion des rôles
* gestion des tokens (access / refresh)
* intégration OAuth externe (Google et Apple)
* sécurisation des endpoints
* révocation des sessions et anonymisation de l'identité lors d'une suppression

Mais surtout :
👉 il définit la **frontière de confiance** du système.

---

## 🧠 Philosophie de conception

L’authentification est traitée comme un **sous-système métier**, pas comme un simple module technique.

Elle est pensée autour de trois principes :

1. **Confiance contrôlée**
2. **Simplicité opérationnelle**
3. **Intégration produit réaliste**

---

## 🔐 Choix assumé : backend de confiance

Le système repose sur un **backend de confiance** :

* OAuth externe (Google et Apple)
* échange de tokens côté backend
* génération des JWT côté serveur

➡️ Le mobile **ne manipule jamais directement** les secrets OAuth.

Ce n’est pas un flow PKCE pur côté client, mais un modèle **broker d’authentification** :

```
Mobile → Backend → fournisseur OAuth → Backend → JWT → Mobile
```

### Pourquoi ce choix ?

Parce qu’il apporte :

* contrôle centralisé
* sécurité opérationnelle
* rotation de secrets
* auditabilité
* cohérence multi-clients
* gestion serveur des identités

👉 C’est un modèle utilisé dans de nombreuses architectures produits réelles.

Ce n’est pas une faiblesse, c’est un **choix d’architecture conscient**.

---

## 🧩 Architecture

L’authentification est organisée comme un **domaine** à part entière :

* write model → commandes
* read model → projections
* événements métier
* ports/adapters
* séparation domaine / infra

---

## ✍️ Write side — identité et sécurité

### Modèle de domaine

* `AuthUser`
* `AuthProvider`
* `AuthRole`
* `JwtClaims`
* `RefreshToken`

➡️ Concepts métier, pas concepts techniques.

---

### Events

* `AuthUserCreatedEvent`
* `AuthUserLoggedInEvent`
* `AuthenticationAccountDataErasedEvent`

➡️ L’authentification produit des **faits métier**.

---

### Use cases

* `GoogleLoginCommand`
* `AppleLoginCommand`
* `LogoutCommand`
* `RefreshTokenCommand`

➡️ Chaque action utilisateur est un **use case explicite**.

---

### Ports (gateways)

* `GoogleAuthService`
* `AppleAuthService`
* `ProviderCredentialRepository`
* `TokenService`
* `JwtClaimsFactory`
* `AuthUserRepository`
* `RefreshTokenRepository`
* `RefreshTokenHasher`

➡️ Le domaine dépend uniquement d’abstractions.

---

## 🔌 Adapters write

### Primary

* `AuthWriteController`

➡️ Adaptation HTTP → Command

---

### Secondary

* `HttpGoogleAuthService`
* `HttpAppleAuthService`
* `JwtTokenService`
* `JpaAuthUserRepository`
* `JpaRefreshTokenRepository`
* `FakeGoogleAuthService`

➡️ Implémentations interchangeables des ports.

Le domaine ne dépend pas de Google.
Le domaine ne dépend pas de JWT.

---

## 📖 Read side — statut de sécurité

Le contexte expose uniquement la lecture technique nécessaire au resource
server pour refuser immédiatement un JWT appartenant à une identité supprimée.
Le profil public `/api/users/me` appartient au `userApplicationContext`.

---

## 🔐 Sécurité applicative

* JWT signés côté backend
* Resource server Spring
* mapping rôles → authorities
* filtres de sécurité dédiés
* configuration isolée

La sécurité est **une couche**, pas une dépendance métier.

---

## 🧪 Testabilité

* services fake (`FakeGoogleAuthService`, `FakeTokenService`)
* repositories fake
* ports mockables
* logique métier isolée

➡️ Tests rapides, fiables, sans dépendances externes.

Les credentials Apple nécessaires à la révocation sont chiffrés au repos. La
suppression distante Apple précède l'effacement transactionnel local ; un échec
provider est rejoué par la consommation SQS/inbox.

### Rotation des sessions Fragments

Les refresh tokens Fragments sont opaques pour le client et ne sont jamais
stockés en clair côté serveur. PostgreSQL ne conserve que leur empreinte
SHA-256 ; leur entropie aléatoire rend cette empreinte impropre à une recherche
exhaustive réaliste.

Une rotation charge l'ancien token avec un verrou pessimiste, le révoque et
persiste son successeur dans une seule transaction. Deux requêtes concurrentes
ne peuvent donc pas créer deux descendants. Une panne avant le commit restaure
l'ancien état. Le mobile regroupe les demandes simultanées en une seule requête.

La migration `refresh-token-hardening-2026-09` supprime les anciens bearers en
clair au lieu de prolonger une compatibilité moins sûre. Son déploiement impose
une reconnexion unique aux sessions déjà ouvertes. Si la réponse d'une rotation
est perdue après commit, le client conserve temporairement sa session locale,
mais la prochaine réponse d'authentification explicite provoque une reconnexion ;
le serveur ne réémet jamais un successeur secret à partir d'un ancien token.

---

## 🧠 Positionnement architectural

Ce context joue un rôle central :

* point d’entrée du système
* source d’identité
* fournisseur de confiance
* racine de sécurité

Il protège les autres contexts.

---

## 🎯 Pourquoi ce context est structurant

Parce que toute l’architecture dépend de la **qualité de l’identité** :

* droits
* permissions
* ownership
* traçabilité
* audit
* sécurité globale

Une auth faible = système faible.

---

## 🏁 Objectif

Le authenticationContext n’est pas un module technique.

C’est un **système d’identité** intégré à l’architecture globale, conçu pour être :

* fiable
* testable
* évolutif
* industrialisable
* cohérent produit

---
