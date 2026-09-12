# Prévisualisations AWS — refus d'exécution, 12 septembre 2026

Lot 10C, Astra High. L'opérateur a explicitement autorisé la création des deux
change sets, **sans exécution**. Les deux sont créés avec succès et consultés
avec `--include-property-values`. Verdict : **NO GO sur ces deux candidats**.

## Objets créés — ne pas exécuter

Compte `851725375299`, région `eu-west-3` ; type `UPDATE`.

| Stack | Change set | État observé | Résultat |
| --- | --- | --- | --- |
| `fragments-staging-minimal` | `fragments-release-preview-20260912-minimal` | `CREATE_COMPLETE` / `AVAILABLE` | 29 ajouts, 14 modifications, dont 2 remplacements |
| `platform-staging` | `fragments-release-preview-20260912-platform` | `CREATE_COMPLETE` / `AVAILABLE` | 3 modifications, dont 2 remplacements |

ARN exacts :

```text
arn:aws:cloudformation:eu-west-3:851725375299:changeSet/fragments-release-preview-20260912-minimal/2441d9b2-c3a3-4537-ad65-4610aa4e00b9
arn:aws:cloudformation:eu-west-3:851725375299:changeSet/fragments-release-preview-20260912-platform/0b53e7b1-1caa-45b3-b69f-17d43608a8b6
```

`AVAILABLE` signifie techniquement exécutable dans AWS, **pas approuvé**. Ils
restent présents pour revue et ne doivent jamais être utilisés comme candidats
de déploiement. Leur retrait éventuel doit cibler exactement ces ARN ; il ne
supprimerait que les prévisualisations, pas des ressources de staging.

Templates soumis depuis le commit `94eef51`, inchangés pendant cette tranche :

| Fichier | SHA-256 |
| --- | --- |
| `infra/aws/cloudformation/staging-minimal.yaml` | `4aee3cfbdeb186e7d6da62afa39bf3086a1f9bd62e6c985d0c9b5ecce4b24f94` |
| `infra/aws/cloudformation/platform-staging.yaml` | `6170e7e6c4730cc56185ecbb60f4f60fbbb0084e26b5caaf6a09ed1c92e2a23f` |

## Cause prouvée des remplacements

| Ressource | Propriété | Avant | Après proposé | Replacement |
| --- | --- | --- | --- | --- |
| `StagingInstance` | `ImageId` | `ami-08e6ec4617b83d578` | `ami-0f361e5c8f5125c8e` | `True` |
| `PlatformInstance` | `ImageId` | `ami-0d2cb709b1ee2acd4` | `ami-0f361e5c8f5125c8e` | `True` |
| `DataVolumeAttachment` | `InstanceId` | Ancienne instance legacy | Nouvelle instance, connue après application | `True` |
| `PlatformElasticIpAssociation` | `InstanceId` | Hôte partagé actuel | Nouvelle instance, connue après application | `True` |

CloudFormation classe les deux changements `ImageId` comme modifications directes
statiques exigeant une recréation. Le nom du paramètre Ubuntu a pourtant été
conservé avec `UsePreviousValue` : cela ne conserve pas sa valeur résolue.
Les templates `Processed` ont aussi été lus ; dans ces réponses, ils conservent
l'expression SSM. La preuve des AMI effectives vient des `BeforeValue` et
`AfterValue` des détails de propriétés, pas d'une interprétation du YAML seul.

Le stack legacy prévoit aussi des modifications sans remplacement de `DataVolume`
(zone référencée depuis l'instance) et `ElasticIp` (instance associée). Pas de
suppression explicite de ressource, mais les remplacements suffisent à refuser.
L'hôte plateforme porte Anchor : aucun risque de remplacement n'est acceptable
dans ce lot limité au messaging et à la surveillance Fragments.

## Autres changements vérifiés

- Les 29 ajouts correspondent à 9 files (Experience + 8 DLQ), 18 alarmes, un
  topic et un abonnement email. Aucun nouveau fournisseur OIDC n'est annoncé.
- Sept files sources changent de redrive policy sans remplacement ; la shared
  DLQ reçoit seulement un tag. Aucun appel receive/purge/redrive effectué.
- `GitHubDeployRole` legacy serait modifié sans remplacement : retrait des droits
  SSH temporaires, ajout SSM sur l'hôte partagé et lecture du stack plateforme.
  Ce changement inutile pour le workflow actuel reste à exclure du candidat final.
- `InstanceRole` legacy reçoit Experience et les droits S3/métrique prévus.
- `PlatformRuntimeRole` reçoit uniquement les ajustements IAM Fragments décrits
  dans la revue précédente. Les ressources Anchor ECR/SQS/S3/SSM et les droits
  KMS existants sont conservés dans le diff de policy inspecté. Cela ne rend pas
  le change set plateforme sûr : le remplacement EC2 l'invalide indépendamment.

Contrôle structurel des paramètres réussi contre une nouvelle lecture des stacks :
13 paramètres legacy conservés, trois nouveaux paramètres fournis ; sur la
plateforme, 15 paramètres conservés et la liste Fragments SQS augmentée du seul
ARN Experience (8 vers 9 ARN). Aucun paramètre Anchor changé. L'email est dans
les paramètres de prévisualisation AWS et les fichiers temporaires privés, pas Git.

## État préservé et suite proposée

Après création/inspection, les deux stacks restent `UPDATE_COMPLETE`, avec dates
de dernière mise à jour inchangées (4 juillet et 26 août). L'instance plateforme
est toujours `running` avec son AMI existante ; l'instance legacy reste `stopped`.
Aucun `execute-change-set`, `update-stack`, déploiement, SQL, SSM secret ou
abonnement SNS exécuté. Seuls les deux objets de prévisualisation AWS ont été créés.

Prochaine tranche proposée, à valider :

1. Préparer des candidats locaux qui figent explicitement les AMI actuellement
   utilisées, sans migration d'OS ni changement de compute ; préserver le bloc
   déployé de `GitHubDeployRole` legacy.
2. Tester les garde-fous de préparation, puis créer deux nouvelles prévisualisations
   non exécutées. Exiger zéro modification EC2/EBS/EIP, zéro remplacement,
   zéro suppression, zéro changement OIDC/Anchor/rôle GitHub legacy.
3. Archiver les preuves et retirer les deux prévisualisations dangereuses par ARN
   avec accord, puis demander l'autorisation d'exécuter uniquement les candidats
   corrigés après revue de leur diff réel. Aucun accord d'exécution acquis ici.

Documentation uniquement modifiée. Les contrôles de cette tranche portent sur
les réponses CloudFormation, les paramètres et les métadonnées EC2 ; aucune suite
métier n'a été réexécutée. La dernière preuve backend reste 513 tests verts.
Les réponses détaillées et paramètres sont dans le répertoire privé local
`/private/tmp/fragments-cfn-preview-zYsqni`, à ne pas committer.
