# Déploiement staging journalisé — préparation locale, 12 septembre 2026

Lot 10B, Astra High. Aucun push, déploiement, changement SSM ou change set créé
dans cette tranche. Les consultations AWS se limitent aux ressources et à deux
paramètres non secrets du stack legacy.
Code et tests : commit local `571fa51`.

## Migration identifiée et non rejouée

`render-release-migration.sh <répertoire-release> <révision-Git-40-hex>` produit
sur stdout un bundle psql autonome. Il lit le driver `app-store-2026-09.psql` et
ses huit inclusions explicites, valide leurs chemins, refuse les fichiers
manquants/liens symboliques et les commandes de transaction/psql en tête de ligne
dans les fragments. Ce dernier garde-fou n'est pas un parseur SQL de sécurité :
les fichiers restent du code de migration à revoir, jamais du contenu utilisateur.

Le checksum SHA-256 couvre le driver et les huit fragments, noms et ordre compris.
Une révision Git différente avec les mêmes fichiers garde le même checksum.
Une modification de ces fichiers impose **une nouvelle version de migration**,
pas un remplacement du checksum déjà stocké.

Le journal technique `release_schema_history` contient :

- `version` (clé primaire), ici `app-store-2026-09` ;
- `checksum` SHA-256 ;
- `source_revision` de la première application ;
- `applied_at`.

Validation des métadonnées → transaction et verrou PostgreSQL → comparaison du
journal → DDL/backfills si version absente → insertion du reçu → commit.
Même version/checksum : aucune reprise de données. Checksum différent : erreur,
sans réécriture du reçu. Reçu et modifications partagent la même transaction.
Deux applications concurrentes aboutissent à une seule application et un saut.

PostgreSQL 15 n'a pas renvoyé le code d'échec attendu avec `\quit 3` dans le test
initial ; le refus utilise désormais une erreur SQL contrôlée avec `ON_ERROR_STOP`.
Ce chemin est couvert par le test réel de checksum incompatible.

Ce n'est pas un moteur général Flyway : cette première migration a un manifeste
explicite. Les suivantes nécessiteront leur propre version et ordre de déploiement.
Le journal ne dispense pas du contrôle de dérive du schéma ou de la recette.

## Déploiement contrôlé

Le workflow local est **manuel seulement**, sur `main`, avec l'entrée booléenne
`approve_staging_release` (false par défaut). Les déploiements sont sérialisés,
sans annulation du précédent. La suite complète Java 21 précède le packaging.
Le script hôte exige aussi `--approved-staging-release`, une image correspondant
exactement à la révision, un verrou `flock` et la topologie Fragments attendue.
Un doublon backend/base est refusé, jamais supprimé automatiquement.

1. Téléchargement de tous les fichiers à la même révision Git ; rendu du bundle.
2. Lecture SSM dans un environnement candidat temporaire privé. Le bootstrap
   accepte un répertoire de sortie ; validation Compose avant changement live.
   Les valeurs multiligne/`$`/`#`/apostrophe sont préservées par les quotes Compose.
3. Image récupérée avant interruption. Installation des scripts/unités opérateur.
4. Arrêt du seul backend Fragments résolu, donc de ses writers/consommateurs ;
   backup S3 frais exigé pendant cet arrêt. PostgreSQL et Anchor restent actifs.
5. Migration journalisée sur stdin de psql, jamais `schema.sql` sur la base existante.
6. Installation du nouvel environnement/Compose, démarrage du candidat, contrôle
   de santé, activation du timer de métriques.

| Échec | Comportement préparé |
| --- | --- |
| Accord/révision/topologie invalide ou SSM/config/image indisponible | Pas d'arrêt du backend ni d'écrasement de `.env` live |
| Backup en échec avant tentative SQL | Ancien backend redémarré seulement s'il tournait avant la tentative ; un backend déjà arrêté reste arrêté ; aucune migration |
| SQL en échec ou accusé de commit perdu | Ancien backend laissé arrêté ; inspection du journal obligatoire, aucun rollback d'image automatique |
| Configuration/démarrage échoué après migration | Reprise opérateur à partir du journal et de l'image identifiée |
| Santé candidat non verte | Candidat arrêté ; aucune ancienne image relancée automatiquement |

Les sorties SQL/configuration ne sont pas imprimées dans SSM/CI : elles peuvent
contenir des données. Les temporaires privés sont supprimés à la sortie normale
ou en erreur. Après arrêt brutal/SIGKILL de l'hôte, vérifier manuellement les
temporaires et l'état avant reprise. Aucun effacement de données métier n'est
une procédure de récupération.

Le nouveau workflow n'a **pas été poussé** : le workflow distant historique
existe encore. Aucun push n'est autorisé implicitement par ce changement local.
Un déclenchement manuel reste conditionné aux prérequis infra/Apple et à une
fenêtre de maintenance explicitement validée.

## Preuves et limites

Régression du 12 septembre à **13:21:32 CEST** : **513 tests / 209 classes**,
zéro échec, erreur ou ignoré, en 2 min 45. Rapports isolés :
`target/release-verification.3MRx3v`. La JVM locale reste Java 23 (compilation 21) ;
la fermeture tardive Surefire/Hikari précédemment documentée reste observée.
Shell et syntaxe YAML vérifiés. Mobile et Studio inchangés, non réexécutés.

Checksum du manifeste testé :
`59cddfd0ec258ac457f1ed1e14f162790eaef329deb19d0eeb5994fb8264f2a8`.
La révision réelle sera celle du commit de code, pas la révision synthétique
utilisée par les tests.

- `StagingReleaseUpgradeIT` : cinq tests PostgreSQL 15 réels, comprenant atomicité,
  conservation, saut effectif des backfills, checksum incompatible et concurrence.
- `ReleaseManifestTest` : déterminisme, modification d'un fragment, refus de
  transaction interne et de sortie du répertoire du manifeste.
- `DeploymentSafetyIT` : le vrai script tourne dans un conteneur avec des fakes
  aux frontières AWS/Docker/systemd ; SSM manquant, backup en panne, SQL incertain,
  parcours réussi, refus avant appels externes. Pas de faux service métier.
  La reprise conserve aussi l'état arrêté d'un ancien backend : un échec de
  backup ne doit pas le réveiller après une tentative antérieure incertaine.
- La sérialisation du PEM et des caractères sensibles est vérifiée par Compose
  puis inspection d'un conteneur **non démarré, sans réseau**, avec valeurs
  synthétiques exclusivement. Ce conteneur est supprimé par le test.
- Garde-fous statiques : workflow manuel et backup avant SQL, sans usage live du
  bootstrap DDL. Ils complètent les tests exécutables, sans les remplacer.

Le [drill sur backup réel](app-store-schema-upgrade.md) reste la preuve du
candidat `184e2ba` (69 tables). Les huit fragments métier sont inchangés ici ;
l'enveloppe journalisée ajoute une table technique (70 sur ce point de départ).
Elle est éprouvée sur le DDL réel avec données synthétiques, pas présentée comme
une nouvelle restauration du backup réel. Aucun transfert de backup répété.
La recette réelle de Java 21/ARM64, IAM, SQS, S3, Apple et du mobile reste ouverte.

## Proposition AWS à valider — pas un change set exécuté

Comparaison en lecture seule : 17 ressources déployées dans
`fragments-staging-minimal`, 47 déclarées dans le template local, sans suppression
d'identifiant logique. Les conditions expliquent les différences suivantes :

| Ajout proposé | Quantité |
| --- | ---: |
| File source Experience | 1 |
| DLQ dédiées aux huit destinations | 8 |
| Alarmes (8 DLQ, 8 âge, legacy DLQ, métrique éditoriale) | 18 |
| Topic SNS opérateur | 1 |
| Abonnement email, conditionnel et à confirmer | 1 |

Le trentième identifiant logique absent du stack est `GitHubOidcProvider`,
conditionnel : **ne pas le créer**. Préserver
`ExistingGitHubOidcProviderArn=arn:aws:iam::851725375299:oidc-provider/token.actions.githubusercontent.com`.
Le paramètre AMI legacy pointe vers la version courante Ubuntu ARM64 : une
éventuelle résolution nouvelle doit être contrôlée dans le futur change set.

Modifications prévues : redrive des sept files existantes vers leurs DLQ propres,
IAM SQS Experience, S3 limité aux préfixes Fragments, permission métrique limitée
à `Fragments/Staging`. Sur `platform-staging`, ajouter explicitement l'ARN
Experience au paramètre `FragmentsSqsQueueArns`, conserver tous les ARN existants
et les paramètres Anchor.

Critères impératifs avant exécution : aucun remplacement EC2/EBS, aucune
modification de ressources/droits Anchor, aucun nouveau fournisseur OIDC,
aucune purge/redrive automatique de la legacy DLQ (environ cinq messages au
préflight). Pas de changement global du bucket partagé ni de réseau supplémentaire.

Les 29 ajouts avec email (28 sans) sont un **diff structurel**, pas une prévision
CloudFormation garantie. Le change set devra confirmer les conditions et les
modifications des ressources existantes. Coût récurrent des alarmes/métriques et
usage SQS/SNS à chiffrer sur les tarifs applicables avant accord ; aucun montant
inventé ni engagement de dépense ici. L'adresse d'alerte est demandée à l'opérateur.
