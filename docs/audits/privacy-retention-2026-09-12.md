# Confidentialité — conservation réelle, 12 septembre 2026

## Décision opérateur du 13 septembre — chantier en pause

La résolution complète conservation/restauration est explicitement reportée
après le prochain TestFlight. **Pause, pas clôture ni correction des constats.**
Le candidat local `5fd0fc3` n'est ni déployé ni activé. La recette conserve le
backend staging `c8dea6a`. Aucun nettoyage supplémentaire, migration LOB,
restauration, journal indépendant ou purge de transport n'est lancé pour ce build.

Les pages publiques décrivent désormais la bêta et ses limites ; leur publication
ne constitue pas une certification juridique ou une preuve d'effacement complet.
Les mentions « brouillon / pas de publication / pas de changement EAS » ci-dessous
décrivent les étapes historiques du 12 septembre, pas l'état courant du site.
Voir le [dossier du prochain TestFlight](../deployment/testflight-2026-09-13.md).

Cause du dépassement : élargissement de la rédaction légale à l'inventaire des
copies techniques, découverte du mapping PostgreSQL LOB, puis protection contre
rejeu et restauration. Ces travaux ont été trop couplés à la préparation du
build. Les durées actives n'ont pas été isolées : aucune estimation rétrospective
précise n'est inventée. La prochaine projection de charge du chantier suspendu
devra distinguer migration/rollback, anciennes copies, journal de suppression,
réapplication après restauration, logs et recette opérationnelle.

Complément au lot pages publiques / préparation TestFlight. Inspection technique,
pas certification juridique. Aucun contenu personnel en base n'a été consulté.

## Décision et changement appliqué

Après confirmation explicite de l'opérateur : expiration S3 à 30 jours des objets
sous le seul préfixe `fragments/staging/backups/postgres/`, dans
`anchor-assets-prod-851725375299`, région `eu-west-3`.

Source : `infra/aws/compose/platform/staging/fragments/backup-retention.json`.
Compte AWS contrôlé : `851725375299`. Avant écriture : absence de lifecycle
revérifiée, bucket non versionné. Arrêt prévu si une règle existait, afin de ne
pas écraser une politique tierce. Après écriture : relecture de la règle activée,
préfixe exact et `Expiration.Days=30` conformes. Aucun préfixe Anchor ou média
n'est couvert. Aucun `DeleteObject`, aucune purge SQL/DLQ, aucun redémarrage.

L'inventaire préalable commence au 7 septembre : aucun objet inventorié n'a
30 jours à la date d'application. Aucune suppression par cette intervention.
Les suppressions futures par S3 seront définitives. L'âge est celui de chaque
objet, pas celui du compte. L'expiration S3 est asynchrone : ce n'est pas une
garantie d'effacement physique à la seconde exacte du trentième jour.

Ne pas réappliquer ce JSON aveuglément : l'API remplace la configuration du
bucket entier. Toujours relire et préserver les éventuelles règles ajoutées
entre-temps. Un retour arrière doit retirer uniquement cette règle, après
comparaison ; il peut empêcher de futures expirations mais ne restaure pas
les objets déjà supprimés. Un changement ultérieur du versionnement exige
une nouvelle analyse des versions non courantes.

## Constats bloquant la finalisation de la politique

### 1. Outbox : conservation de copies personnelles

- `OutboxDomainEventPublisher.publish` sérialise l'événement de domaine complet
  dans `outbox_events.payload_json`.
- `AuthUserCreatedEvent` contient email, nom public, identifiant fournisseur,
  avatar ; `ExperienceSnapshotChangedEvent` contient le texte de l'expérience.
- `OutboxEventDispatcher` marque `SENT` sans retirer le payload.
- Les erasers métier inspectés retirent les données propres à leur contexte,
  mais ne traitent pas ces copies techniques. Aucun mécanisme général de
  minimisation/expiration n'a été trouvé dans les chemins inspectés.

Conséquence : des copies peuvent rester après la suppression métier et entrer
dans de nouvelles sauvegardes. Les 30 jours S3 ne bornent donc pas à eux seuls
la conservation après suppression du compte. Ne pas déclarer ce point corrigé.

### 2. Inbox et statuts ne sont pas des caches jetables

`InboxMessageRepository` conserve les clés de déduplication et, en échec,
`error.getMessage()`. `CommandStatusRepository` conserve notamment demandeur,
empreinte, statut et motif. Une suppression générale mettrait en danger
l'idempotence, le contrôle du demandeur et la réconciliation offline-first.
Les exceptions peuvent aussi contenir des données : prévoir leur minimisation.

### 3. Logs et restauration

Inspection runtime SSM `99fbf680-61ac-4c03-b764-29fe9ed16800` : Caddy et backend
utilisent `json-file` avec `Config={}`. Aucune rotation explicite par conteneur
ni durée maximale n'est ainsi prouvée. `describe-log-groups` retourne une liste
vide en `eu-west-3` ; ne pas annoncer une conservation CloudWatch de 30 jours.

Le script de sauvegarde exporte PostgreSQL, sans traitement des suppressions
intervenues après le point de restauration. Une procédure de restauration doit
réappliquer ces suppressions avant remise en service. Ne pas promettre cette
garantie tant qu'elle n'est pas implémentée et testée.

### 4. Médias

Configuration source : nettoyage activé par bootstrap, attente d'upload 24 h,
passage nominal toutes les 10 min. `CleanExperienceMediaObjects` supprime les
objets puis confirme en base. Ces intervalles ne sont pas une garantie de délai
en cas d'incident. Aucun lifecycle média général n'est ajouté : il risquerait
de supprimer des photos encore utilisées.

## Correctif distinct proposé — à cadrer avant implémentation

**Autorisé ensuite par l'opérateur**, avec documentation systématique.
Première tranche implémentée et vérifiée localement : **533 tests serveur dans
213 classes, aucun échec/erreur/skip**. Aucun déploiement ni activation.
La suite complète a également révélé l'ancien mapping `@Lob` PostgreSQL :
les références sont converties en JSON texte par la migration candidate, avec
conservation des OID historiques pour audit ; ces anciens objets sont exclus
de la minimisation automatique et ne sont pas déclarés effacés.

[règles, frontières et preuves](../architecture/technical-data-retention.md).
Ce suivi ne clôt pas les constats ci-dessus ; les observations runtime restent
valables tant que le correctif n'est pas déployé et activé.

Deuxième tranche locale : marqueurs de suppression par contexte, verrouillage
transactionnel contre les réécritures concurrentes, projections protégées et
arrêt terminal des anciennes demandes de vérification Ticket. Le test vertical
rejoue des événements personnels avec de nouveaux identifiants : il ne repose
pas uniquement sur la déduplication inbox. Vérification complète : **537 tests,
0 échec, 0 erreur, 0 ignoré**, rapports `target/release-verification.cS6I60`,
script release terminé avec succès. Les 6 contrôles Node passent également.
Le message Surefire de fin forcée du processus de tests après 30 secondes,
déjà présent dans la tranche précédente, reste signalé dans le document technique.
Aucun déploiement de cette tranche.

Ne pas en déduire que la restauration est réglée : les marqueurs des suppressions
antérieures au déploiement nécessitaient encore une reprise dans cette deuxième
tranche ; elle est ajoutée par la troisième tranche ci-dessous. Restaurer une
ancienne base ne récupère toujours pas les suppressions plus récentes. Restent les OID
historiques, les copies en transport/DLQ et la durée des logs. Le statut de
publication des pages et de préparation EAS demeure inchangé.

### Troisième tranche locale — historique et restauration technique

- Reprise des confirmations par contexte dans les marqueurs, sans modifier les
  processus métier ni fabriquer des ACKs. Premier déploiement bloqué si des
  suppressions historiques restent en cours ; rollback testé de la migration.
- Script de restauration temporaire durci : `.env` non exécuté, artefact et
  conteneur strictement ciblés, checksum vérifié, échec de nettoyage visible.
  Test avec vrai dump/restore PostgreSQL et données synthétiques ; cinq tests
  shell supplémentaires. L'essai ne vaut pas autorisation de reprise du service.
- Inventaire staging SSM `b627a2b3-95ea-46e1-84d4-799987a83408` réussi, uniquement
  des compteurs : 159 lignes outbox, 159 objets référencés sans références
  invalides/manquantes/partagées, 464 objets au total dont **305 non attribuables
  à l'outbox par cet inventaire**. Aucun objet supprimé, aucun contenu remonté.
- Logs d'échec SQS réduits à des codes/types/corrélations ; les exceptions brutes
  peuvent contenir des données privées. La durée des logs n'est pas encore bornée.
- [Procédure et prérequis de reprise](../deployment/privacy-recovery-gate.md) :
  journal indépendant à livrer et réapplication via les propriétaires avant
  toute réouverture des consommateurs/API. Rien ne redémarre automatiquement.

Une première suite complète à 541 tests est verte (12 septembre, 22:44:13,
5m38). La vérification finale, garde de migration et logs SQS inclus, passe à
**543 tests dans 217 classes, aucun échec/erreur/ignoré** (22:49:03, 4m05),
rapports `target/release-verification.MuVQm1`. L'arrêt forcé du processus de
tests après délai Spring reste documenté, sans masquer le diagnostic.
Les 11 contrôles Node passent. Checkpoint local sur
`feat/privacy-recovery-hardening`, pages/Caddy conservés à part. Aucun déploiement,
purge, restauration staging, activation de nettoyage ni changement EAS.

1. Cartographier les copies personnelles et leurs propriétaires techniques,
   y compris transport/DLQ, erreurs, sauvegardes et stockage local mobile.
2. Définir minimisation des payloads terminés et traitement d'une suppression
   de compte, sans altérer les événements encore nécessaires aux consommateurs.
   `SENT` prouve un envoi, pas la consommation par toutes les destinations.
3. Conserver les reçus minimaux indispensables à l'idempotence, au contrôle
   d'accès et aux statuts. Ne pas supprimer les lignes inbox pour rejouer.
4. Borner les logs Fragments et prévoir une prise en compte des suppressions
   lors de restauration. Ne pas changer le proxy partagé pour ce seul lot
   sans analyser l'impact sur les autres services.
5. Tests fake-first des règles/horloge ; infra PostgreSQL ; doublons et
   redelivery ; suppression puis replay/restauration ; statuts hors ligne ;
   revue des frontières DDD/hexa. Vérification verticale avant déploiement.

Ce correctif backend ne doit pas être déguisé en édition HTML ni exécuté par
une purge SQL ad hoc. Les pages légales restent des brouillons ; aucune URL
n'est raccordée à EAS et aucune page n'est publiée dans cette intervention.

## Références consultées

- [CNIL — information et critères de conservation](https://www.cnil.fr/fr/conformite-rgpd-information-des-personnes-et-transparence)
- [CNIL — durées de conservation](https://www.cnil.fr/fr/passer-laction/les-durees-de-conservation-des-donnees)
- [AWS — expiration S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-expire-general-considerations.html)
- [AWS — protection des données et DPA](https://aws.amazon.com/compliance/data-protection/)

Validation : test de configuration `node --test scripts/test-fragments-backup-retention.mjs`
vert et relecture AWS. Pas de test de suppression avec de vraies sauvegardes.
