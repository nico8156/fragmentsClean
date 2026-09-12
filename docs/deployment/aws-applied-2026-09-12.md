# AWS appliqué — files, alertes et IAM, 12 septembre 2026

**Suivi configuration :** SNS est désormais confirmé et la clé de chiffrement
SSM créée ; les trois éléments Apple restent attendus. Voir le
[passage au déploiement](staging-configuration-2026-09-12.md).

Lot 10C, Astra High. Accord explicite de l'opérateur pour exécuter uniquement les
[deux candidats corrigés](aws-preserved-preview-2026-09-12.md), sans migration SQL
ni déploiement applicatif. Templates et garde-fous : commit `29d714e`.

## Exécution réelle

Compte `851725375299`, région `eu-west-3`. Avant exécution : identité vérifiée,
diffs et paramètres identiques aux prévisualisations revues, dates de mise à jour
des stacks inchangées, santé publique Fragments `UP`. Les deux changements ont
été exécutés séquentiellement, par ARN exact, avec contrôle entre les étapes.

| Stack | Début CEST | Fin CEST | Résultat |
| --- | --- | --- | --- |
| `fragments-staging-minimal` | 14:31:57 | 14:34:34 | `UPDATE_COMPLETE` |
| `platform-staging` | 14:37:05 | 14:37:29 | `UPDATE_COMPLETE` |

Les ARN exécutés sont ceux du dossier candidat (`9489eccb-2eb9-41c5-9e6c-21c18c088ec1`
et `310b2426-b5ff-4507-a3dd-5baf3339f562`), pas ceux des premières prévisualisations.
Tokens de suivi : `fragments-approved-minimal-20260912-9489eccb` et
`fragments-approved-platform-20260912-310b2426`.

Les templates relus après application correspondent exactement aux fichiers
approuvés, hors espaces de bord du transport CLI. SHA-256 des fichiers sources :

```text
staging-minimal.yaml  d004c0f0d56b4f6b4d10beaf11dc760a39de96b6003a5f75664c6f3e4c4dd0f4
platform-staging.yaml f4cc5f7f33f74d1a0e593a223a0109b5c734c21dc1b8ac0860380c42cfde01b3
```

## Contrôles après application

- Huit files sources, dont Experience, chacune reliée à sa DLQ dédiée ;
  `maxReceiveCount=5`, visibilité 120 secondes, long poll 20 secondes vérifiés.
- Huit DLQ avec rétention 14 jours ; chiffrement SQS géré vérifié sur les seize
  files. Aucun envoi, receive, purge ou redrive de message effectué.
- Shared DLQ conservée : environ cinq messages visibles, zéro en vol au contrôle.
- Dix-huit alarmes présentes, actions activées, alarmes et retours OK dirigés
  vers `arn:aws:sns:eu-west-3:851725375299:fragments-staging-operations-alarms`.
- Policy inline réelle de `fragments-platform-staging-runtime` équivalente à la
  policy approuvée, après normalisation des valeurs uniques/lists IAM. Les
  statements concernant Anchor ECR/SQS/S3/SSM et KMS sont conservés avant/après.
- Instances, images, états, dates de lancement et identifiants des volumes
  identiques au relevé avant application. Plateforme `i-004d3e9cbca327d01` toujours
  running ; legacy `i-04984df67caf3099b` toujours stopped. Aucun remplacement.
- Endpoint public Fragments `/actuator/health` répond `UP` avant et après.
  Ce n'est pas une recette fonctionnelle complète de Fragments ou Anchor.

Les permissions réelles sont vérifiées au niveau de la policy appliquée, pas
par une simulation exhaustive de toutes les policies du compte ni un upload S3.
La recette des nouveaux parcours attend toujours le déploiement applicatif.

### Écart aperçu/exécution : événement EIP legacy

Malgré l'absence de changement EIP dans le change set corrigé, CloudFormation a
émis `UPDATE_IN_PROGRESS` puis `UPDATE_COMPLETE` sur `ElasticIp` legacy à
14:34:28–14:34:29. Le contrôle strict a arrêté la progression avant la seconde
exécution, le temps de vérifier cet écart.

L'adresse physique `15.224.134.41` et son rattachement à `i-04984df67caf3099b`
correspondent au contexte antérieur. Aucun événement de changement d'instance
ou de volume, et comparaison des états EC2 avant/après identique. L'IP plateforme
est toujours rattachée à l'hôte actif. L'identifiant d'association EIP n'avait pas
été capturé avant l'opération : aucune affirmation sur une éventuelle réassociation
transitoire. La cause interne de l'événement CloudFormation n'est pas établie.
Ne pas transformer « aucun remplacement » en « aucun événement EC2 émis ».

## Alertes à terminer / signaux attendus

Au dernier contrôle, l'abonnement email est **`PendingConfirmation`**. L'opérateur
doit confirmer le message AWS SNS reçu à l'adresse fournie. L'adresse demeure
hors Git. Aucun test de notification manuelle n'a été envoyé, et la réception
effective des futures alertes n'est pas encore prouvée.

Deux alarmes `ALARM`, seize `OK` au contrôle :

- `fragments-staging-legacy-shared-dlq-not-empty` : cinq messages historiques ;
  triage séparé, aucune purge pour faire disparaître l'alerte.
- `fragments-staging-editorial-operations-degraded` : métrique absente, car le
  nouveau timer/applicatif n'est pas encore déployé. Ne pas neutraliser l'alarme.

Les listes de change sets des deux stacks sont vides après les mises à jour :
les anciennes prévisualisations dangereuses n'y sont plus disponibles. Aucun
appel manuel de suppression de change set ou de ressource n'a été effectué.

## Ce qui reste hors de cette livraison

Aucun push Git, build/push ECR, déploiement backend/mobile/Studio, arrêt de service,
migration SQL, lecture/écriture de secret SSM, modification de bucket S3 ou test
fonctionnel métier. L'ancien backend reste en place ; disposer de la file
Experience ne signifie pas que le nouveau consommateur est déjà déployé.

Prochaine étape : confirmation SNS, configuration Apple/chiffrement via canal
opérateur sûr, puis accord de maintenance pour déploiement journalisé avec backup
frais et migration. Recette SQS/S3/SSE/offline, Java 21 ARM64 et iPhone/TestFlight
restent ouvertes. Le lot 10 et le GO App Store ne sont pas clos.

Documentation uniquement modifiée dans cette tranche. Dernière régression
backend inchangée : 515 tests verts, 14:27:33 CEST, non réexécutée après opérations
AWS ; contrôles de cette tranche effectués sur les ressources réelles ci-dessus.
Preuves détaillées locales : `/private/tmp/fragments-aws-apply-4nhK3m` (privé,
hors Git). Aucun contenu de message, ligne métier, secret ou backup téléchargé.
