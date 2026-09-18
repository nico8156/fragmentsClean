# Publication de la politique Sentry — 18 septembre 2026

## Objet

Publier, avant le prochain build TestFlight, une information fidèle sur les
diagnostics d'erreur transmis à Sentry par l'application mobile Fragments.

Périmètre de l'intervention : un seul fichier statique,
`/legal/confidentialite.html`. Aucun conteneur, service backend, proxy, schéma
de base de données ou secret n'a été modifié.

## Source et cible

- Branche source : `fix/p1-release-certification`.
- Commit présent au début de la publication : `6807d4b`.
- Instance : `i-004d3e9cbca327d01` (`fragments-platform-staging`, région
  `eu-west-3`).
- Fichier source :
  `infra/aws/compose/platform/staging/fragments/public-site/confidentialite.html`.
- Fichier déployé :
  `/srv/platform/data/fragments-public/confidentialite.html`.
- SHA-256 avant publication :
  `18a26bcf2d48c18fe33cea3e231447548afa5cdd16b8a051586ada555e734717`.
- SHA-256 publié :
  `11e07491234f49bbf75534a453192e9b8a4129a74b90c9ef4c683280f84ef798`.
- Sauvegarde serveur :
  `/srv/platform/data/fragments-public-release-20260913/confidentialite.before-sentry-20260918.html`.

## Contenu publié

La politique indique désormais :

- les données techniques envoyées pour diagnostiquer un crash ;
- la désactivation de l'identité utilisateur, du contenu des requêtes, des
  données additionnelles, des breadcrumbs, du tracing et du replay ;
- l'exposition technique possible de l'adresse IP au prestataire ;
- le recours à l'infrastructure européenne Sentry avec ingestion en
  Allemagne ;
- la durée de consultation de 30 jours du plan Developer ;
- l'absence d'usage publicitaire ou de profilage.

Les limites déjà publiées concernant les copies techniques, les sauvegardes et
la réapplication des effacements après restauration restent explicitement
présentes. Cette publication ne les présente pas comme résolues.

## Déploiement

Une première commande SSM de lecture seule (identifiant commençant par
`e0ec3884`) a échoué avant toute écriture : `AWS-RunShellScript` utilise
`/bin/sh`, qui ne prend pas en charge `set -o pipefail` sur cette instance. Une
seconde pré-vérification, compatible POSIX, a confirmé le hash et les
permissions du fichier existant.

La publication a ensuite été réalisée par une commande SSM atomique :

- commande : `9dd38c10-35d7-4b70-accb-5e2168a37f18` ;
- statut : `Success` ;
- code retour : `0` ;
- garde : refus de remplacer le fichier si son ancien hash diffère ;
- contrôle : vérification du hash du candidat avant remplacement ;
- remplacement : `mv` du fichier temporaire après copie de sauvegarde ;
- propriétaire et mode finaux : `root:root`, `0644`.

Aucun redémarrage ni rechargement n'était requis : Caddy sert directement le
répertoire de données monté.

## Vérifications

Commandes exécutées :

```bash
node --test scripts/test-fragments-public-site.mjs
node --test scripts/test-fragments-public-site-infra.mjs
curl --fail --silent --show-error --location \
  --dump-header /private/tmp/fragments-privacy-headers.txt \
  https://fragments-staging.anchor-event.fr/legal/confidentialite.html
```

Résultats :

- tests statiques : 5/5 réussis ;
- test d'intégration Caddy réel : 1/1 réussi ;
- réponse publique : HTTP/2 `200` ;
- page publique : date du 18 septembre 2026 et mentions Sentry attendues ;
- en-têtes présents : CSP restrictive, `Referrer-Policy: no-referrer`,
  `X-Content-Type-Options: nosniff` ;
- hash distant après publication : identique au hash source attendu.

## Retour arrière

Le retour arrière consiste à vérifier que le fichier courant porte encore le
hash publié ci-dessus, puis à restaurer atomiquement la sauvegarde. Si le hash
courant a changé, l'opération doit s'arrêter pour éviter d'écraser une
publication plus récente.

