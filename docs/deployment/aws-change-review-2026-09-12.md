# Revue AWS et chiffrage — 12 septembre 2026

**Dernier résultat :** les [prévisualisations corrigées](aws-preserved-preview-2026-09-12.md)
préservent désormais les serveurs, disques, réseau et rôle GitHub legacy. Aucune
exécution ; le chiffrage de ce document reste applicable.

**Complément après autorisation :** les deux prévisualisations ont été créées,
sans exécution. Elles prévoient le remplacement des deux instances EC2 et sont
refusées ; voir les [résultats détaillés](aws-preview-results-2026-09-12.md).
Le texte ci-dessous conserve le cadrage antérieur à leur création.

Lot 10C, Astra High. Préparation uniquement : lectures AWS et validation des
templates, sans création de change set, abonnement SNS, ressource ou paramètre.
Aucun push, déploiement, arrêt de service ou SQL sur staging.

## Ce qui est confirmé

Compte `851725375299`, région de déploiement `eu-west-3`. Les templates déployés
ont été relus par `get-template` et comparés aux fichiers locaux. Les deux appels
`validate-template` AWS réussissent : IAM requis pour `staging-minimal`, IAM nommé
pour `platform-staging`. Cette validation ne prouve ni les droits d'exécution,
ni l'absence de remplacement, ni le succès d'une future mise à jour.

L'opérateur a fourni le destinataire email dans la conversation. Ne pas le
committer dans un template, fixture ou exemple ; passer sa valeur au paramètre
`OperationsAlarmEmail` lors de la préparation autorisée. Aucun email envoyé.
L'abonnement futur nécessite une confirmation SNS par son destinataire.

## Périmètre de la prévisualisation proposée

| Stack | Changements du candidat local par rapport au template déployé |
| --- | --- |
| `fragments-staging-minimal` | 1 file Experience, 8 DLQ dédiées, 18 alarmes, 1 topic SNS, 1 abonnement email ; redrive des 7 sources existantes ; tag de conservation de la DLQ legacy ; IAM runtime legacy et rôle GitHub legacy modifiés |
| `platform-staging` | IAM runtime : ARN SQS Experience ajouté par paramètre, droits objets S3 Fragments resserrés, `ListBucket` limité à `fragments/staging/coffees/*`, publication de métrique limitée à `Fragments/Staging` |

29 ajouts attendus avec email, aucun identifiant logique supprimé. Le fournisseur
OIDC conditionnel n'est pas un ajout autorisé : réutiliser l'ARN existant.
Ce comptage reste structurel, pas un résultat de change set.

Sur la plateforme, conserver **tous** les paramètres existants sauf
`FragmentsSqsQueueArns`, dont la liste actuelle de huit ARN doit être conservée
et complétée par :

```text
arn:aws:sqs:eu-west-3:851725375299:fragments-staging-experiences-events
```

Cela conserve aussi l'ARN de la DLQ historique ; aucun triage, receive, purge
ou redrive de ses messages n'est inclus. Ne pas ajouter de droit consommateur
sur les huit nouvelles DLQ par défaut. Aucun paramètre Anchor ne change.

Pour le stack legacy, conserver les paramètres existants et fournir explicitement
`OperationsAlarmEmail`, `QueueOldestMessageAlarmSeconds=300` et
`SsmDeployInstanceId=i-004d3e9cbca327d01`. Préserver :

```text
ExistingGitHubOidcProviderArn=arn:aws:iam::851725375299:oidc-provider/token.actions.githubusercontent.com
```

### Écart IAM à décider avant exécution

Le template local remplace les droits d'ouverture SSH temporaire de l'ancien
`GitHubDeployRole` par des droits de déploiement SSM sur la plateforme. Ce
changement était absent du simple comptage d'ajouts ; il n'est pas nécessaire
pour ajouter les files et alarmes.

Le workflow candidat utilise déjà **un autre rôle** :
`fragments-staging-github-ssm-deploy`, propriété vérifiée du stack distinct
`fragments-staging-deploy-role`, ressource `GitHubSsmDeployRole`. Ce stack n'est
pas à modifier dans cette tranche. Son existence et sa policy inline sont
confirmées, pas une simulation exhaustive de ses permissions effectives.

Recommandation : exclure le changement du rôle GitHub legacy du candidat final
d'exécution, en conservant son bloc déployé, puis traiter son retrait éventuel
séparément. Une prévisualisation du template local peut exposer cet écart, mais
ne vaut pas approbation de modifier ce rôle. Ne pas simplement exécuter le
template complet après avoir vérifié le nombre de nouvelles files.

### AMI : risque constaté, aucun remplacement autorisé

| Cible | État relu | Image réellement associée |
| --- | --- | --- |
| Plateforme `i-004d3e9cbca327d01` | running | `ami-0d2cb709b1ee2acd4` |
| Legacy `i-04984df67caf3099b` | stopped | `ami-08e6ec4617b83d578` |
| Paramètre public Ubuntu ARM64 courant | version 76 | `ami-0f361e5c8f5125c8e` |

Les deux templates utilisent une référence SSM non versionnée. Conserver le
nom du paramètre avec `UsePreviousValue` n'est pas une preuve de conservation
de sa valeur résolue. AWS demande d'inspecter le template traité du change set
pour vérifier la version qui sera utilisée :
[références dynamiques SSM](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/dynamic-references-ssm.html).

Ne pas affirmer qu'un remplacement aura nécessairement lieu : il n'y a pas
encore de change set. Mais refuser tout changement EC2/EBS, remplacement
`True` ou `Conditional`, et toute résolution AMI ambiguë avant application.
Si nécessaire, préparer un candidat avec version/AMI existante explicitement
figée, le valider et refaire la prévisualisation ; aucune relance de l'hôte
legacy ni migration d'OS dans ce chantier.

## Surcoût estimé — Paris, USD hors taxes

Tarifs obtenus le 12 septembre avec AWS Price List Query (`get-products`, endpoint
`us-east-1`, filtre **regionCode=eu-west-3**). Les pages officielles expliquent
les unités/franchises ; le catalogue régional fournit les montants ci-dessous.
Estimation **avant franchises, crédits, conversion monétaire**, au premier palier.
Ce n'est pas le montant total de la facture AWS existante.

| Poste ajouté / utilisé | Tarif Paris | Hypothèse et montant |
| --- | --- | --- |
| 18 alarmes standard, une métrique chacune | 0,10 USD / alarme-mois | 1,80 USD / mois |
| Métrique `EditorialOperationsDegraded`, sans dimensions variables | 0,30 USD / métrique-mois | 0,30 USD / mois |
| `PutMetricData` toutes les 5 minutes | 0,01 USD / 1 000 appels | 8 640 appels sur 30 jours : 0,0864 USD |
| SQS standard | 0,40 USD / million d'unités de requête | Dépend des envois, lectures, suppressions, retries et tailles |
| SNS API standard | 0,50 USD / million d'appels au-delà de la franchise | Dépend des transitions d'alarme |
| SNS email | 2 USD / 100 000 notifications au-delà de la franchise | 0,00002 USD / notification facturée |

Socle alarmes + métrique + émission régulière : **environ 2,19 USD/mois**, avant
franchises. Exemple seulement : un consommateur Experience effectuant en continu
un long poll vide de 20 secondes ferait 129 600 lectures en 30 jours, soit environ
0,052 USD supplémentaires avant franchise. Ce n'est pas une mesure du poller réel
ni un plafond : activité, concurrence et retries changent le volume.

Les nouvelles DLQ ne portent pas de forfait par file. Aucun nouvel EC2, EBS, ALB,
NAT Gateway, Redis ou Kafka prévu. SQS utilise le chiffrement géré SQS, sans
nouvelle clé KMS cliente. Les coûts d'images S3 (stockage, requêtes, transfert),
de logs supplémentaires, de backup et le socle AWS existant ne sont pas inclus
dans ce surcoût de surveillance/messaging. La consommation globale des franchises
par Anchor et les autres ressources n'a pas été auditée : ne pas promettre zéro.

Sources : [CloudWatch](https://aws.amazon.com/cloudwatch/pricing/),
[SQS](https://aws.amazon.com/sqs/pricing/), [SNS](https://aws.amazon.com/sns/pricing/).
Références catalogue CloudWatch : `UXRHCNMUCGV73M7Q` (alarme),
`9UKGFPRV59MVNQ9B` (métrique), `3WG4PACR975S7XR4` (API), publication
`2026-09-11T13:44:05Z`. SNS SMTP Paris : SKU `7Z67M746J4JYPC9V`.

## Prochaine autorisation et critères de sortie

1. Autoriser la **création de deux change sets UPDATE non exécutés** sur les
   stacks identifiés. Cela crée des objets de prévisualisation AWS, pas les files,
   alarmes ou abonnements. Aucun `execute-change-set` ou `update-stack` implicite.
2. Lire les détails par propriété et les templates traités. Exclure le changement
   du rôle GitHub legacy du candidat final ; préserver compute, réseau, OIDC,
   droits Anchor et données. Reprévisualiser tout candidat corrigé.
3. Présenter les ARN/checksums et le diff réellement calculé, puis obtenir
   l'accord distinct d'application des ressources et droits précisément listés.
4. Confirmer SNS, tester l'alerte avec accord ; fournir Apple/chiffrement via un
   canal opérateur sûr. L'alarme éditoriale peut signaler des données manquantes
   tant que le nouveau timer n'est pas déployé : ce signal est attendu et ne doit
   pas être neutralisé pour afficher du vert.
5. Push et déploiement applicatif restent des étapes séparées, avec fenêtre de
   maintenance, prérequis SSM, backup frais et migration journalisée.

Preuves de cette tranche : lectures de métadonnées/catalogues et validation AWS
des deux templates. Documentation seulement modifiée ; aucune suite métier
réexécutée. La dernière régression backend reste **513 tests verts** et ne prouve
pas la recette AWS. FlowAtlas n'est pas utilisé pour ce contrôle : il ne décrit
ni les ressources déployées, ni les résolutions SSM, ni la tarification AWS.
