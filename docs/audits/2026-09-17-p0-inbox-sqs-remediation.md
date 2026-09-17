# P0 — Sécurité d’acquittement inbox/SQS

Date : 2026-09-17
Branche : `fix/p0-inbox-sqs-ack-safety`
Constat traité : `FR-001` de l’audit pré-release du 2026-09-17

## Problème

`InboxMessageRepository.claim()` retournait un booléen. La valeur `false`
représentait indistinctement un événement déjà traité et un événement encore
détenu par un autre worker. Le routeur retournait normalement dans les deux cas,
puis `SqsIntegrationEventConsumer` supprimait le message SQS.

Un crash du premier worker suivi d’une livraison pendant son bail pouvait donc
faire supprimer le seul message disponible avant que le bail ne devienne
récupérable.

## Correction

- Le port `InboxMessageStore` sépare `CLAIMED`, `ALREADY_PROCESSED` et `BUSY`.
- Chaque acquisition reçoit un `ownerToken` persisté dans `lease_owner`.
- `markProcessed` et `markFailed` sont conditionnés par ce jeton : un ancien
  worker ne peut plus finaliser un bail repris.
- Un doublon `ALREADY_PROCESSED` est acquitté sans rejouer le handler.
- Un message `BUSY` n’est jamais supprimé. Sa visibilité SQS est prolongée
  jusqu’à l’expiration du bail, ce qui évite aussi de consommer inutilement le
  nombre maximal de réceptions et d’alimenter la DLQ.
- Une perte de bail après l’effet métier provoque un échec de traitement et donc
  une nouvelle livraison ; l’idempotence métier reste nécessaire conformément à
  la livraison au moins une fois.

## Persistance et déploiement

La migration additive `messaging-safety-2026-09.psql` ajoute
`inbox_messages.lease_owner`. L’index unique existant sur
`(destination,event_id)` couvre déjà la finalisation conditionnelle ; aucun index
redondant n’est ajouté. La migration possède son propre reçu,
son checksum et dépend explicitement de `article-curation-2026-09`. Les anciens
manifests ne sont pas modifiés.

Le déploiement staging télécharge et rend ce quatrième manifest avant l’arrêt du
backend et l’application atomique du bundle SQL. Le schéma de bootstrap est
aligné.

## Preuves de vérification

Commande de release :

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./scripts/test-release.sh
```

Résultat : `BUILD SUCCESS`, 549 tests, 0 échec, 0 erreur, 0 ignoré. Durée :
5 min 12 s. Cette exécution couvre notamment PostgreSQL, LocalStack SQS, les
migrations depuis le schéma staging, le rejeu, la concurrence de migration, le
script réel de déploiement et les tests d’architecture.

Un test vertical supplémentaire a ensuite été ajouté et exécuté avec les tests
d’infrastructure ciblés :

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./scripts/backend-testcontainers -q \
  -Dtest=SqsInboxAcknowledgementIT,InboxClaimLeaseIT,SqsIntegrationEventConsumerLocalStackTest \
  test
```

Résultat : succès. Le test vertical relie le consommateur, le routeur, le port
d’inbox, son adaptateur JDBC et PostgreSQL ; il vérifie qu’un claim actif n’est
pas acquitté, qu’il devient récupérable après expiration et que le jeton périmé
ne peut pas finaliser le traitement.

## Limites et suite

Ce lot ferme `FR-001`. Il ne ferme pas les autres P0 du rapport : barrières
anti-résurrection après suppression de compte (`FR-002`) et procédure de
restauration/rejeu des effacements (`FR-003`). Aucun déploiement staging ni
commit n’a été réalisé dans ce lot au moment de la rédaction de ce reçu.

Les avertissements préexistants vus pendant la suite (API dépréciées,
`open-in-view`, arrêt tardif de certains threads de projection) ne sont pas
masqués et restent dans la roadmap de l’audit.
