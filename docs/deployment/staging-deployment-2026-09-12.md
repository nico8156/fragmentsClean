# Déploiement staging backend — 12 septembre 2026

Lot 10D, Astra High, poursuite explicitement autorisée par l'opérateur.
Périmètre : backend Fragments, configuration runtime SSM, sauvegarde fraîche,
migration journalisée et contrôles. Pas de déploiement mobile/Studio, pas de
soumission App Store, pas d'intervention sur Anchor ni de traitement des DLQ.

## Candidat et vérifications

- Révision candidate immuable : `c8dea6a977ee032cd638820538727a5b27800c0f`.
- Correction CI `6031224` : ripgrep explicite, message de prérequis, garde-fou
  rouge puis vert (4 tests ciblés).
- Correction de fixtures `3fcbe1d` : dépendances FK nettoyées avant parents,
  repositories isolés des fixtures HTTP, identifiants de commandes uniques.
  Aucun code de production ou assertion métier affaiblie.
- Suite locale complète en ordre inversé : 516 tests, 0 échec/erreur/ignoré,
  2 min 39, fin 15:22:24 CEST ; Java 23 Valhalla avec compilation release 21.
- [Pipeline manuel 34696304190](https://github.com/nico8156/fragmentsClean/actions/runs/34696304190)
  : **succès**, terminé à **15:39:38 CEST**. Suite Java 21 : **516 tests,
  0 échec, 0 erreur, 0 ignoré**, rapports frais `target/release-verification.QNjRl1`
  sur le runner, durée Maven 2 min 53. Les deux tentatives antérieures ont échoué
  avant tout déploiement.

Durées réelles CI (hors préparation/corrections et recette) :

| Étape | Début / fin CEST | Durée |
| --- | --- | --- |
| Pipeline complet | 15:23:43 / 15:39:38 | 15 min 55 |
| Vérification Java 21 | 15:23:56 / 15:26:52 | 2 min 56 |
| Build/push ARM64 | 15:27:23 / 15:38:25 | 11 min 02 |
| Déploiement SSM | 15:38:25 / 15:39:28 | 1 min 03 |

La durée SSM inclut les prérequis avant arrêt ; elle n'est pas une mesure exacte
d'indisponibilité utilisateur.

## Relevé avant bascule

- AWS `851725375299`, région `eu-west-3`, hôte `i-004d3e9cbca327d01`.
- Backend en service : `sha-18ae517ab13dc2f8c8812914eae501ab3f1dfe55`.
- Base : 46 tables publiques, `release_schema_history` absent (lecture seule).
- Santé publique globale `UP`, découverte `/api/coffees?limit=1` HTTP 200,
  `/api/users/me` sans session HTTP 401. Certains composants de l'ancien backend
  sont `DEGRADED` ; la santé globale ne constitue pas une recette complète.
- Paramètres Apple : trois SecureString version 1 ; clé AES fournisseur déjà
  créée, jamais régénérée. Aucun secret exposé dans les contrôles.

Conteneurs à préserver :

| Service | Identifiant | Démarré (UTC) |
| --- | --- | --- |
| Proxy partagé | `29f0c956fd89b85fe01db697006463c19d0a0a81669b444b4917443fa2a5f396` | 2026-08-26T15:55:18.732452556Z |
| PostgreSQL | `dffba11d1f3840c36634d8497ff5ef761cc56d68118ec6cc8c5f0a5f33967a67` | 2026-08-26T15:43:05.362122731Z |

## Contrôles de sortie obtenus

- Image exécutée : tag `sha-c8dea6a977ee032cd638820538727a5b27800c0f`, architecture
  `arm64`, digest ECR et RepoDigest de l'hôte identiques :
  `sha256:bf86ceb8c612a1457709ae7a148531216593efc3c65fb1a307eb020eae34a1df`.
- Runtime Temurin **21.0.12+8**, conteneur backend démarré à 13:39:02.396 UTC.
- Les deux conteneurs du tableau ont conservé exactement leur identifiant et
  leur date de démarrage. Aucune opération Anchor ni remplacement d'instance.
- Sauvegarde pré-migration :
  `s3://anchor-assets-prod-851725375299/fragments/staging/backups/postgres/fragments-20260912T133858Z.dump`
  et `.sha256`. Métadonnées S3 : **447 033 / 136 octets**, chiffrement **AES256**
  sur les deux objets. Service backup `success`, code 0, 13:38:58–13:39:01 UTC.
  Ce backup frais n'a pas été restauré à nouveau ; la preuve de restauration
  antérieure reste distincte de cette vérification de métadonnées.
- Journal SQL : `app-store-2026-09`, révision candidate exacte, checksum
  `59cddfd0ec258ac457f1ed1e14f162790eaef329deb19d0eeb5994fb8264f2a8`,
  appliqué à **13:39:01.561070 UTC**. **70 tables** publiques après migration.
- Santé globale `UP`, `ticketVerificationHealth=UP`, cafés HTTP 200 et profil
  anonyme HTTP 401. Ces probes ne sont pas une recette authentifiée.
- Timers backup/observabilité actifs ; publication `editorial_health_published
  status=UP value=0` observée. **17 alarmes OK, 1 ALARM** : DLQ legacy historique.
- Commandes SSM de preuve en lecture seule : `57b4a7ab-bc36-4464-8027-f1458c15f7cc`
  (image/journal/backup/timers) et `4dd8a117-7a0e-4516-8403-3522427e505d`
  (ARM64 et compteurs ci-dessous), toutes deux `Success`.

## Réserves conservées

`messagingRuntimeHealth` et `articleAuthoringHealth` étaient déjà `DEGRADED`
avant la bascule et le restent. Lecture de compteurs uniquement, sans payload :

- zéro outbox `PENDING` ou `FAILED` ;
- neuf inbox `FAILED`, reçues entre le 28 août et le 9 septembre ;
- huit sagas éditoriales anciennes : six `GENERATING`, deux `GENERATION_PENDING`,
  dernières mises à jour entre le 28 août et le 9 septembre.

Aucun retry forcé, purge ou réécriture de ces lignes et aucun receive/redrive DLQ.
Leur triage est un chantier distinct : un déploiement vert n'efface pas cette dette.
Recette Apple réelle (connexion puis suppression/révocation), médias/S3,
commandes/SSE/offline et parcours produit sur appareil restent à réaliser avec
les versions mobile/Studio correspondantes. **Pas de GO App Store implicite.**

Les avertissements de fermeture Hikari et de terminaison Surefire restent
documentés ; ne pas les considérer corrigés par le seul résultat vert.
