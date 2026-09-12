# Candidats AWS corrigés — serveurs préservés, 12 septembre 2026

Lot 10C, Astra High. Préparation et deux nouvelles prévisualisations autorisées,
**aucune exécution**. Ce dossier remplace les candidats refusés de la
[première prévisualisation](aws-preview-results-2026-09-12.md).
Templates et tests : commit local `29d714e`, sans push.

## Correction locale

Les deux templates exigent `InstanceImageId` (`AWS::EC2::Image::Id`), sans défaut.
`ImageId` référence directement ce paramètre, sans résolution SSM dynamique.
`UbuntuAmiParameter` est conservé pour compatibilité des paramètres historiques,
mais n'est plus utilisé. Une migration d'OS demande un autre changement approuvé.

Valeurs relues sur EC2, images `available` et `arm64` :

- stack legacy : `ami-08e6ec4617b83d578` ;
- plateforme : `ami-0d2cb709b1ee2acd4`.

Le bloc `GitHubDeployRole` legacy est identique au déployé, trust policy incluse.
Le paramètre local inutilisé `SsmDeployInstanceId` est retiré ; il n'avait jamais
été appliqué au stack. Le workflow continue d'utiliser le rôle SSM du stack
distinct `fragments-staging-deploy-role`, inchangé. Préserver l'ancien rôle ne
recommande pas son usage : sa suppression/restriction reste une décision séparée.

Deux tests de garde-fou ont d'abord échoué sur le candidat précédent puis passent
sur celui-ci : AMI obligatoire sans défaut mouvant, checksum du bloc IAM historique.
Ils complètent la preuve CloudFormation réelle ci-dessous, sans la remplacer.
Aucun domaine, contrat, migration SQL ou code mobile/Studio modifié.

## Prévisualisations corrigées — pas encore approuvées pour exécution

Compte `851725375299`, région `eu-west-3`, type `UPDATE`.
Les deux change sets sont `CREATE_COMPLETE` / `AVAILABLE`.

| Stack | Ajouts | Modifications | Remplacements / suppressions | EC2/EBS/EIP |
| --- | ---: | ---: | --- | --- |
| `fragments-staging-minimal` | 29 | 9 | 0 / 0 | Aucun changement |
| `platform-staging` | 0 | 1 | 0 / 0 | Aucun changement |

ARN exacts à utiliser uniquement après accord d'application distinct :

```text
arn:aws:cloudformation:eu-west-3:851725375299:changeSet/fragments-release-preserved-20260912-minimal/9489eccb-2eb9-41c5-9e6c-21c18c088ec1
arn:aws:cloudformation:eu-west-3:851725375299:changeSet/fragments-release-preserved-20260912-platform/310b2426-b5ff-4507-a3dd-5baf3339f562
```

Templates soumis (SHA-256) :

```text
staging-minimal.yaml  d004c0f0d56b4f6b4d10beaf11dc760a39de96b6003a5f75664c6f3e4c4dd0f4
platform-staging.yaml f4cc5f7f33f74d1a0e593a223a0109b5c734c21dc1b8ac0860380c42cfde01b3
```

Les 29 ajouts sont la file Experience, huit DLQ, 18 alarmes, un topic et son
abonnement email. Les neuf modifications legacy sont les sept redrive policies,
le tag de conservation de la shared DLQ et la policy de `InstanceRole`.
La plateforme ne modifie que `PlatformRuntimeRole`.

Contrôles des réponses avec `--include-property-values` :

- zéro `Remove`, zéro remplacement `True` ou `Conditional` ;
- aucun changement `AWS::EC2::*`, OIDC ou rôle GitHub legacy ;
- liste exacte des modifications legacy et modification unique de la plateforme ;
- 13 paramètres legacy conservés ; email, seuil et AMI explicitement fournis ;
- 15 paramètres plateforme conservés ; seul ARN Experience ajouté à la liste
  Fragments existante, dans son ordre initial ; AMI explicitement fournie ;
- projection des statements IAM avant/après sur les ressources Anchor
  ECR/SQS/S3/SSM et KMS identique, actions et conditions incluses.

L'autorisation objet S3 Fragments est resserrée aux préfixes cafés/articles/
private-media/backups ; liste limitée au préfixe cafés ; métrique limitée au
namespace `Fragments/Staging`. Aucune policy du bucket partagé n'est modifiée.
La vérification porte sur ce diff IAM, pas une simulation exhaustive des droits
effectifs du compte. La recette S3/SQS déployée reste nécessaire.

## Suite et limites

Régression complète après correction : **515 tests / 209 classes**, zéro échec,
erreur ou ignoré ; `bash scripts/test-release.sh`, terminé à **14:27:33 CEST**
en **2 min 49**, rapports `target/release-verification.uWtZbL`. Les sept garde-fous
infra passent aussi en exécution ciblée. JVM locale Java 23 Valhalla, compilation
21 ; avertissements de terminaison Hikari/Surefire connus toujours présents,
sans échec de la suite. Aucune suite mobile ou Studio relancée, dépôts inchangés.
Validation AWS des templates réussie ; états EC2 et dates de mise à jour des
stacks relus après création et inchangés. Les AMI figées sont une preuve de
préservation de cette mise à jour, pas une certification de sécurité de l'OS.

L'accord suivant doit couvrir **l'exécution de ces deux change sets précis** :
files/alertes d'abord, IAM plateforme ensuite, contrôle après chaque mise à jour.
Un changement de template, paramètres ou état des stacks impose une nouvelle revue.
Le chiffrage précédent reste valable : environ 2,19 USD/mois de surveillance,
avant franchises et hors usages variables ; voir la [revue tarifaire](aws-change-review-2026-09-12.md).

La création future de l'abonnement enverra un email de confirmation à l'adresse
opérateur fournie hors Git. L'alarme éditoriale peut rester en alerte tant que son
timer n'est pas déployé. Pas de purge/redrive des messages legacy, pas de mise à
jour du bucket, pas de push ni migration SQL ou déploiement applicatif implicite.
Apple/SSM, backup frais, maintenance et recette restent des prérequis distincts.

Les anciennes prévisualisations dangereuses restent présentes, non exécutées,
et ne doivent pas être utilisées. Leur suppression par les deux ARN historiques
pourra accompagner l'accord d'application ; elle ne supprimerait aucune ressource.
Les nouveaux résultats détaillés sont conservés localement dans le répertoire
privé `/private/tmp/fragments-cfn-preserved-CZl3if`, hors Git, email inclus dans
les paramètres AWS mais absent des fichiers versionnés.
