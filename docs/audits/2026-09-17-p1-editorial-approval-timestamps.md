# P1 — Précision canonique des approbations éditoriales

Date : 2026-09-17  
Constat traité : `FR-005`  
Périmètre : backend `articleContext`  
État : implémenté et validé localement ; non fusionné, non poussé, non déployé

## Résultat

Le format signé `v1` encode l'expiration en epoch seconds. L'émission stockait
auparavant l'`Instant` original avec ses nanosecondes : un jeton valide et son
enregistrement pouvaient donc différer uniquement par leur précision puis être
rejetés.

Les nouveaux enregistrements utilisent désormais une précision canonique à la
seconde pour `createdAt` et `expiresAt`. La signature HMAC, le hash SHA-256
persisté, les identifiants saga/article/révision et le format du jeton restent
inchangés.

## Compatibilité et sécurité

Pour un enregistrement émis avant ce correctif, la validation compare
l'expiration signée à l'expiration persistée tronquée à la seconde. Cette règle
est volontairement étroite : elle ne relâche ni la signature, ni le hash, ni la
liaison aux trois identifiants.

L'expiration signée reste autoritaire. À la seconde exacte portée par le jeton,
la validation échoue même si une ancienne ligne PostgreSQL possède encore une
fraction de seconde. Un ancien enregistrement arrivé dans cette seule fraction
terminale n'est pas réutilisé lors d'une nouvelle émission.

La consommation reste un `UPDATE` conditionnel atomique. Le test PostgreSQL
prouve qu'une deuxième consommation échoue et qu'une consommation effectuée
dans une transaction de publication annulée est elle-même restaurée.

Ce lot ne change pas la rotation du secret HMAC, la durée de vie configurée ou
le transport du lien Studio. Ces sujets restent distincts du défaut de
précision `FR-005`.

## Architecture

- `articleContext` reste propriétaire de l'approbation et de sa politique ;
- aucune logique cryptographique n'entre dans le controller Studio ;
- aucun nouveau contrat inter-contextes ni événement n'est introduit ;
- aucune migration n'est nécessaire : la lecture assure la compatibilité des
  lignes existantes et toutes les nouvelles écritures sont canoniques ;
- la consommation participe toujours à la transaction courte de publication.

## Tests rouges observés

```text
./mvnw -q -Dtest=ArticleReviewApprovalTokenServiceTest test
  5 tests : 1 échec et 2 erreurs reproduisant la précision et la réémission

./scripts/backend-testcontainers -q \
  -Dtest=JdbcArticleReviewApprovalRepositoryIT test
  3 tests : 2 erreurs métier après correction d'une fixture Timestamp
```

Le premier lancement Testcontainers dans le sandbox n'avait pas accès au
socket Docker. La même commande autorisée hors sandbox a démarré PostgreSQL.
La première version de la fixture passait directement un `Instant` au driver ;
elle a été corrigée en `Timestamp` avant de qualifier le défaut applicatif.

## Preuves vertes ciblées

```text
./mvnw -q \
  -Dtest=ArticleReviewApprovalTokenServiceTest,ApproveArticlePublicationFlowTest test
  7 tests, 0 échec

./scripts/backend-testcontainers -q \
  -Dtest=JdbcArticleReviewApprovalRepositoryIT test
  3 tests, 0 échec
```

La matrice couvre :

- émission et validation sous-seconde ;
- réutilisation d'un jeton encore actif ;
- compatibilité d'une expiration historique fractionnaire ;
- refus à l'instant exact d'expiration ;
- remplacement pendant la fraction historique non signée ;
- altération de signature ;
- rejet d'une liaison persistée à une autre saga, article ou révision ;
- consommation PostgreSQL unique ;
- rollback de la consommation avec la transaction de publication ;
- publication de la révision exacte et fin de saga.

Le gate release complet, exécuté avant l'ajout du seul test de liaison ci-dessus
(aucun code de production modifié ensuite), est également vert :

```text
./scripts/test-release.sh
  582 tests, 0 échec, 0 erreur, 0 ignoré
```

## FlowAtlas

FlowAtlas Java a été interrogé avec la requête bornée
`fragments-java-http-command-request.json` et la recherche Handler
`ArticleReviewApprovalTokenService`. Le résultat contient zéro correspondance.
Ce n'est pas une preuve d'absence : cette requête modélise explicitement la
verticale HTTP ticket, et les détecteurs Java actuels savent représenter les
domain events, commandes HTTP, intégrations, projections, frontières externes
et tâches planifiées, mais pas encore une verticale
controller → ACL Studio → service cryptographique → repository.

Créer une requête en prétendant que le service HMAC implémente un
`CommandHandler` aurait produit une représentation trompeuse. L'exploration a
donc été poursuivie par les sources et les tests. Une extension utile de
FlowAtlas serait un détecteur de use cases applicatifs et de ports/adaptateurs
non événementiels, avec configuration explicite des méthodes d'émission,
validation, consommation et persistance.

## Déploiement et recette

Aucune migration n'est requise. Après déploiement backend :

1. générer une approbation pour une révision en attente ;
2. ouvrir Studio et confirmer explicitement la publication ;
3. vérifier la publication de la révision exacte ;
4. rejouer le même lien et vérifier son rejet ;
5. vérifier qu'aucun token ni secret n'apparaît dans les logs.

`FR-005` peut être fermé après fusion, déploiement et cette recette.
