# Fragments — audit pré-release du 17 septembre 2026

## Résumé exécutif

**Verdict : NO-GO pour une ouverture publique App Store en l’état.** La poursuite d’une recette TestFlight contrôlée est envisageable avec données de test, mais ne vaut pas validation de production. Ce verdict est technique ; ce document n’est ni un avis juridique ni une garantie d’acceptation Apple.

Le produit a de vraies fondations : monolithe modulaire, commandes durables, séparation erreurs techniques/rejets métier, projections, interfaces de stockage, tests fake-first et intégration PostgreSQL/SQS. Les suites exécutées donnent **544 tests backend, 353 tests mobile et 160 tests Studio réussis**, plus **8 tests de configuration mobile réussis**. Le moteur C++ compile ; **CTest out-of-source passe 21/22**, puis **le même binaire de tests passe 22/22 (56 assertions)** lancé depuis le répertoire attendu par sa fixture. Le défaut de portabilité du test reste non corrigé.

Ces succès ne couvrent pas des défauts de production importants :

1. Une redelivery SQS pendant un bail inbox peut supprimer définitivement un message jamais traité (FR-001).
2. Des événements retardés peuvent recréer des données après effacement ; la rétention technique et la restauration post-effacement ne sont pas garanties (FR-002/003).
3. Le SSE diffuse des métadonnées privées à des comptes tiers (FR-004).
4. La rotation refresh et la déconnexion ont des incohérences de sécurité/résilience (FR-006/007/024).
5. Une sonde reproduit le rejet d’un jeton HMAC valide émis à un instant sous-seconde (FR-005).
6. Le staging répond UP, tout en conservant deux composants DEGRADED ; la DLQ historique contient trois messages (FR-015).

**Préparation App Store : fonctionnellement avancée, qualification non achevée. Préparation production : insuffisante sur les reprises, l’effacement et les gates opérationnels.** Aucun score arbitraire en pourcentage.

**État de cette livraison : rapport initial Phase A livré ; audit exhaustif non clos au sens d’une certification de chaque flux sur appareil, incident et charge réels.** Toutes les catégories du cadrage sont examinées ci-dessous ; les vérifications non exécutées sont explicitement laissées ouvertes. Aucune correction applicative, migration distante, publication, suppression de message, push ou déploiement effectué.

Rapport distinct de l’instrument d’exploration : [évaluation FlowAtlas](2026-09-17-flowatlas-usage-report.md).

## 0. Traçabilité, méthode et périmètre

### Révisions et état initial

| Projet | Branche | Commit audité | Working tree initial |
|---|---|---|---|
| Backend fragmentsClean | main | 17125041c53075d369c9594012c9cea34dc91976 | Propre |
| Mobile fragmentsCleanFront | main | f72396ec68075986fc36bb05907e091d5a475263 | Propre |
| Studio fragments-admin | main | 4851f89c998fcc51e7691e100cbc38936c2d07d5 | Propre |
| ticketverify-engine | main | cdebb4e33cc419f5111a2a93b9a4f4f82e1b2bb5 | Modification préexistante scripts/smoke_contract.sh, conservée |
| FlowAtlas | main | 0d362df634512bf7ee7790f7b146af0fd1f06d21 | Propre ; détails dans rapport séparé |

Racines : /Users/nicolasmaldiney/{fragmentsClean,fragmentsCleanFront,fragments-admin,ticketverify-engine,FlowAtlas}. Studio n’est pas une sous-application du dépôt backend : son application est fragments-admin/fragments-studio.

Instructions lues avant analyse : AGENTS.md de ces dépôts lorsqu’existant ; backend .agents/backend/AGENTS.md, routage et six orchestrateurs ; mobile .agents/mobile/AGENTS.md, routage et quatre orchestrateurs ; Studio .agents/studio/AGENTS.md, routage et six orchestrateurs, conventions de commits, alignement client, gaps, readiness et slice coffee-import ; skill FlowAtlas .codex/skills/architecture-exploration/SKILL.md. Aucun sous-agent utilisé.

La phase initiale d’inventaire a été sans écriture. Les analyses générant des artefacts ont utilisé des copies sous **/tmp/fragments-audit-20260917-5eDWJg**, afin de préserver les sources. Les seules nouvelles écritures persistantes de cette livraison sont ce rapport et le rapport FlowAtlas. Le dossier demandé docs/audit était absent au checkout observé ; docs/audits existait : il a été conservé sans modification.

### Dépôts voisins, outils et exclusions motivées

- fragments-BackEnd (main f7dd6f2cb25d3534b65e50743b8c2a678f1e6075) et bean-coffee-app (main 7b031df7e0343b4bef7ec1fbf42912e5bb7bf9af) : dépôts legacy repérés, working trees modifiés, non touchés. Pas identifiés comme entrées de build/runtime de la plateforme actuelle ; audit interne profond non effectué. La confirmation définitive de leur absence de déploiement résiduel reste à faire.
- coffeeAppDDD, cleanfront et clean-front : répertoires voisins examinés pour localisation ; pas de racine Git active identifiée à ces chemins. Pas considérés comme versions courantes par simple ressemblance de nom.
- Anchor : hors périmètre métier Fragments, mais **dans le périmètre des risques partagés** EC2/IAM/S3/réseau. Son application n’a pas été auditée.
- ticketverify-engine : inclus parce que le Dockerfile backend construit un exécutable depuis le dépôt Git ticket_engine à un SHA fixé. Le nom de ce remote diffère du répertoire local ; le SHA correspond au checkout inventorié.
- Dépendances, Pods, node_modules, target, dist, caches : pas ignorés comme risques, mais exclus des lignes de code propriétaire et non relus intégralement. Les lockfiles sont comptés séparément comme texte.
- Pages publiques/légales : configuration et documentation recherchées ; **sources déployées et contenu actuel des pages non retrouvés/attestés dans ces trois dépôts**. Ne pas conclure à leur absence en ligne.
- AWS observé : compte configuré, région eu-west-3, staging. Aucun autre compte/région/environnement production n’est certifié par cette analyse.

### Niveaux de preuve

- **Exécuté** : résultat de commande/test observé dans cette session.
- **Statique confirmé** : branches/SQL/contrats lus, scénario dérivé ; pas nécessairement exploité en runtime.
- **Risque / interprétation** : conséquence plausible explicitement décrite.
- **Non vérifié** : ne constitue ni succès ni faille avérée.

Les numéros de lignes visent les commits ci-dessus. Les chemins backend commençant par src/ sont relatifs à fragmentsClean ; les chemins front absolus désignent leurs dépôts respectifs. Les résultats historiques de docs/deployment sont distingués des mesures réalisées aujourd’hui.

## Registre des constats

| ID | Sévérité | Priorité | Constat |
|---|---|---|---|
| FR-001 | BLOCKER | P0 | Un bail inbox actif est acquitté comme un message déjà traité |
| FR-002 | CRITICAL | P0 | Effacement des comptes non protégé contre les événements antérieurs retardés |
| FR-003 | BLOCKER | P0 | Conservation technique et restauration : aucune preuve d’effacement complet rejouable |
| FR-004 | HIGH | P1 | Le SSE utilisateur diffuse des identifiants privés sans filtrage serveur |
| FR-005 | HIGH | P1 | Jetons HMAC valides rejetés à cause de la précision temporelle |
| FR-006 | HIGH | P1 | Rotation refresh non atomique et non protégée contre la concurrence |
| FR-007 | HIGH | P1 | Déconnexion mobile sans Authorization ; Studio ne révoque pas côté serveur |
| FR-008 | HIGH | P1 | Dispatcher outbox : réseau dans transaction, dix tentatives sans backoff ni claim multi-instance |
| FR-009 | MEDIUM | P2 | Les routes d’événement inconnues sont silencieusement considérées traitées |
| FR-010 | MEDIUM | P2 | SSE par polling individuel et curseur de séquence non assimilable à l’ordre de commit |
| FR-011 | HIGH | P1 | Bootstrap articles directement dans la projection, actif sans garde de profil |
| FR-012 | HIGH | P1 | RSS opérateur : destination réseau peu contrainte, corps et durée non bornés |
| FR-013 | HIGH | P1 | La limite de temps du processus ticket ne couvre pas son écriture d’entrée |
| FR-014 | MEDIUM | P2 | Suppression des commandes locales inconnues ou sans gateway |
| FR-015 | HIGH | P1 | Santé globale UP alors que deux sous-systèmes restent dégradés |
| FR-016 | HIGH | P1 | Pas de quality gate PR backend/mobile équivalent au gate de déploiement |
| FR-017 | MEDIUM | P2 | Bootstrap, upgrade et reconstruction ne sont pas la même garantie |
| FR-018 | HIGH | P1 | Supply chain : alertes npm et mises à jour Java non qualifiées complètement |
| FR-019 | MEDIUM | P2 | Test moteur réel non portable ; tests C++ retirés de la construction Docker |
| FR-020 | MEDIUM | P2 | Concentration de responsabilités et fichiers minifiés dans le code métier |
| FR-021 | MEDIUM | P2 | Les tests d’architecture tolèrent des imports internes au-delà du contrat ACL primitif |
| FR-022 | MEDIUM | P2 | Isolation opérationnelle limitée : instance et rôle AWS partagés avec Anchor |
| FR-023 | HIGH | P1 | Recette App Store et crash reporting natif non attestés par cette passe |
| FR-024 | HIGH | P1 | Secrets : contrôles utiles mais historique non certifié ; refresh en clair dans la base |

BLOCKER signifie que je déconseille explicitement la release tant que la condition n’est pas levée. CRITICAL qualifie l’impact ; P0/P1 décrit l’ordre d’intervention. Les P1 doivent également être traités ou faire l’objet d’une décision de périmètre documentée avant ouverture à des utilisateurs réels. Une fonctionnalité éditoriale interne désactivée peut être différée ; un défaut de conservation de données utilisateurs ne peut pas être rendu acceptable par un simple changement de libellé.

### FR-001 — Un bail inbox actif est acquitté comme un message déjà traité

- **Module / catégorie :** Backend · SQS/inbox — Bug de fiabilité / perte de message.
- **Sévérité / priorité :** BLOCKER / P0.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/secondary/gateways/repositories/jdbc/InboxMessageRepository.java:21,29-63 ; src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/sqs/SqsIntegrationEventRouter.java:37-46 ; SqsIntegrationEventConsumer.java:176-205 (même dossier). Tests InboxClaimLeaseIT.java:33-47 et SqsIntegrationEventRouterTest.java:30 vérifiés.
- **Comportement observé :** claim=false signifie aussi bien PROCESSED que RECEIVED sous bail actif. Le routeur retourne normalement dans les deux cas ; le consommateur supprime alors le message. Le bail inbox est de 5 minutes ; SQS est configuré à 120 secondes sur la file observée et le client peut imposer sa propre visibilité.
- **Risque et scénario :** Worker A réserve puis meurt avant l’effet métier. Une redelivery avant expiration du bail retourne false et est définitivement supprimée par B. La reprise après 5 minutes n’a plus de message à traiter. Déduction directe des branches exécutables ; crash réel non injecté.
- **Recommandation :** Retour typé ALREADY_PROCESSED / BUSY / CLAIMED ; BUSY ne doit pas acquitter. Ajouter propriétaire/fencing au bail et conditions de finalisation. Ne pas se contenter d’augmenter la visibilité.
- **Effort :** M. **Dépendances :** Aucune.
- **Test de résolution :** PostgreSQL + LocalStack : crash après claim avant effet, redelivery pendant/après bail, un effet finalement appliqué ; échec DeleteMessage puis redelivery ; deux workers et bail expiré.

### FR-002 — Effacement des comptes non protégé contre les événements antérieurs retardés

- **Module / catégorie :** Backend · ticket/projections/suppression — Intégrité / confidentialité.
- **Sévérité / priorité :** CRITICAL / P0.
- **Preuves :** src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/secondary/gateways/repositories/jdbc/JdbcTicketAccountDataEraser.java:15-20 ; src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/secondary/repositories/JdbcTicketStatusProjectionRepository.java:28-59 ; src/main/java/com/nm/fragmentsclean/platform/eventing/IntegrationEventDestinationResolver.java:14-19.
- **Comportement observé :** L’effaceur retire les projections et tickets ; l’application ultérieure d’un VerifyAccepted peut réinsérer userId et OCR dans une projection désormais vide. Le garde de version de ligne ne remplace pas un marqueur de suppression de compte. Les destinations de suppression et les messages antérieurs peuvent être livrés dans un ordre différent.
- **Risque et scénario :** Suppression traitée, puis message ancien sortant de DLQ ou retardé : résurrection de données personnelles. Scénario établi par SQL et routage, non rejoué sur des données réelles.
- **Recommandation :** Barrière d’effacement locale à chaque BC, persistée avant nettoyage, empêchant toute recréation par événement antérieur. Examiner tous les handlers utilisateur/ticket/social/expérience ; prouver aussi l’absence de résurrection d’agrégat.
- **Effort :** L. **Dépendances :** FR-001 ; politique FR-003.
- **Test de résolution :** Supprimer un compte synthétique puis redélivrer chaque type d’événement antérieur, y compris dans le désordre et deux fois : aucune donnée personnelle restaurée.

### FR-003 — Conservation technique et restauration : aucune preuve d’effacement complet rejouable

> Remédiation Phase B, 2026-09-17 : journal S3 indépendant create-only,
> rétention Object Lock, purge des copies techniques locales et rejeu obligatoire
> dans le drill de restauration ont été implémentés sur
> `fix/p0-inbox-sqs-ack-safety`. Voir
> `docs/audits/2026-09-17-p0-account-erasure-restoration.md`. Le constat reste
> opérationnellement ouvert jusqu'au déploiement AWS/backend et à une preuve
> datée sur un vrai backup ; cette note ne réécrit pas le diagnostic initial.

- **Module / catégorie :** Backend · S3 · exploitation — Confidentialité / récupérabilité.
- **Sévérité / priorité :** BLOCKER / P0.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/secondary/gateways/repositories/jpa/entities/OutboxEventJpaEntity.java:9 ; src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/secondary/gateways/repositories/jdbc/JdbcUserAccountDataEraser.java:15-21 ; infra/aws/compose/platform/staging/fragments/restore-postgres-drill.sh:51-67 ; docs/deployment/article-curation-staging-2026-09-15.md:7.
- **Comportement observé :** Les effaceurs ciblent les données métier, pas une politique complète de payloads outbox, traces, backups et références S3. Recherche de purge outbox/inbox dans les sources sans mécanisme de rétention trouvé. Le drill fourni vérifie un restore SQL et un nombre de tables >0, pas une remise en service désinfectée. Le chantier suspendu n’est pas dans main. AWS : backups PostgreSQL expirent à 30 jours ; cela ne traite pas les messages techniques ni le rejeu d’une sauvegarde.
- **Risque et scénario :** Un ancien backup remet un compte supprimé en circulation ; un payload OCR subsiste après effacement métier ; un média remplacé est absent de S3 alors que le backup le référence. Ces trois cas sont distincts, respectivement confidentialité, conservation technique, cohérence UX.
- **Recommandation :** Décider durées et catégories, appliquer purge/anonymisation mesurée et conservation minimale des marqueurs ; journal d’effacement indépendant de la sauvegarde restaurée ; procédure restore isolée → réapplication effacements → réconciliation S3 → contrôles → réouverture. Pas d’obligation technique de conserver éternellement des tombstones : durée fondée sur toutes les copies encore restaurables.
- **Effort :** XL. **Dépendances :** FR-002 ; inventaire des sauvegardes et validation des durées.
- **Test de résolution :** Restaurer une copie synthétique antérieure à suppression et remplacement d’avatar ; vérifier comptes, projections, payloads et objets ; aucune donnée effacée accessible après remise en service. Produire preuve datée de drill et délai réel d’effacement.

### FR-004 — Le SSE utilisateur diffuse des identifiants privés sans filtrage serveur

> **Remédiation engagée le 2026-09-17 :** audience persistée `PUBLIC | USER |
> ADMIN`, destinataire privé, filtrage serveur par principal et garde-fous
> Redux, ainsi que route administrateur explicite par défaut dans Studio, sont documentés dans
> `docs/audits/2026-09-17-p1-projection-sync-privacy.md`. Le diagnostic ci-dessous
> reste la photographie initiale ; sa clôture dépend de la suite de vérification
> et du déploiement de la migration/backend.

- **Module / catégorie :** Backend + mobile · synchronisation — Autorisation / confidentialité / amplification.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/projectionSync/ProjectionSyncController.java:19-21 ; ProjectionSyncDispatcher.java:35,87-92 ; src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/secondary/gateways/repositories/jdbc/JdbcProjectionSyncRepository.java:53-75 ; /Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/projectionSyncWl/usecases/projectionSyncListenerFactory.ts:198-225.
- **Comportement observé :** Les deux routes public-authentifié et admin utilisent le même dispatcher sans principal ; la lecture porte sur tous les événements après un offset. Le mobile recharge tickets/entitlements pour les entityId reçus, sans vérifier le propriétaire à cet endroit. Le filtrage existe pour expériences utilisateur et blocages, pas uniformément.
- **Risque et scénario :** Un utilisateur connecté observe des UUID et moments d’activité de tiers ; chaque notification privée provoque des GET inutiles chez d’autres utilisateurs. Ce n’est pas une preuve d’accès aux contenus des tickets : leurs autorisations HTTP sont une autre barrière.
- **Recommandation :** Port de lecture SSE conscient de l’audience (public, utilisateur, admin), filtrage avant émission ; propagation de l’audience dans le contrat de projection ; filtrage client défensif et coalescence des refresh.
- **Effort :** M. **Dépendances :** Schéma sync et contrats versionnés ; FR-010.
- **Test de résolution :** Deux utilisateurs + un administrateur : chacun ne reçoit que ses métadonnées privées ; cafés/articles publics restent synchronisés ; pas de GET privé pour un autre compte.

### FR-005 — Jetons HMAC valides rejetés à cause de la précision temporelle

- **Module / catégorie :** Backend · approbation éditoriale — Bug reproduit.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/articleContext/write/businesslogic/processManagers/ArticleReviewApprovalTokenService.java:39-42,73-83,99-104 ; src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/secondary/gateways/repositories/JdbcArticleReviewApprovalRepository.java:60-71.
- **Comportement observé :** expiresAt sauvegardé conserve la fraction de seconde ; le token transporte epochSecond ; validate compare deux Instant exactement. Sonde compilée contre les classes réelles : horodatage entier VALID, 2026-09-17T08:00:00.123456Z REJECTED: Approval token is invalid. Le fake ne modifie pas la précision : ce défaut précède même l’aller-retour JDBC.
- **Risque et scénario :** Les approbations émises avec une heure réelle sous-seconde échouent malgré signature et durée correctes ; workflow humain bloqué. Aucun jeton réel n’a été affiché ni consommé.
- **Recommandation :** Normaliser à une précision canonique commune avant stockage/signature, définir compatibilité des tokens déjà émis ; ne pas assouplir la signature ni supprimer les vérifications de liaison.
- **Effort :** S. **Dépendances :** Aucune ; vérifier compatibilité des anciens liens.
- **Test de résolution :** Tests temps sous-seconde + PostgreSQL, expiration exacte, mauvaise révision/saga, double consommation et rollback de publication.

### FR-006 — Rotation refresh non atomique et non protégée contre la concurrence

- **Module / catégorie :** Backend · authentification — Sécurité / disponibilité session.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/authenticationContext/write/businesslogic/usecases/RefreshTokenCommandHandler.java:39-65 ; src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/secondary/gateways/repositories/jpa/SpringRefreshTokenRepository.java:10-13 ; src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:115-123 ; src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/CommandBus.java:69-76.
- **Comportement observé :** Lecture de l’ancien refresh, révocation sauvegardée, puis génération séparée. Pas de transaction englobante ni verrou/compare-and-set sur cette lecture ; le chemin dispatchWithResult n’utilise pas le DurableCommandExecutor. Pas de garantie atomique de consommation unique démontrée.
- **Risque et scénario :** Deux requêtes lisent non révoqué et émettent deux descendants ; un échec après révocation laisse la session sans successeur utilisable. Une réponse de rotation perdue est aussi un problème distinct à traiter côté protocole.
- **Recommandation :** Consommation atomique dans une transaction avec émission du successeur, famille de jetons et politique explicite de replay ; client single-flight ; stratégie bornée de retry d’une réponse perdue sans permettre un replay illimité.
- **Effort :** M. **Dépendances :** FR-007 ; sécurité de stockage refresh.
- **Test de résolution :** Test concurrent réel PostgreSQL : un résultat autorisé ; panne entre révocation et émission rollback ; réponse perdue et retry conformes à la politique.

### FR-007 — Déconnexion mobile sans Authorization ; Studio ne révoque pas côté serveur

- **Module / catégorie :** Mobile + Studio + backend · session — Bug de sécurité fonctionnelle.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** /Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/gateways/auth/authServerGateway.ts:87-99 ; src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/configuration/AuthSecurityConfiguration.java:61-96 ; AuthWriteController.java:126-130 (dossier controllers) ; /Users/nicolasmaldiney/fragments-admin/fragments-studio/src/authContext/write/model/studioAuthSession.ts:55.
- **Comportement observé :** Le mobile POST /auth/logout sans bearer et ignore le statut HTTP ; ce POST n’est pas public dans la chaîne Spring. Studio logout() efface uniquement la mémoire locale. Le succès visuel de déconnexion ne démontre donc pas la révocation du refresh.
- **Risque et scénario :** Un refresh compromis continue à fonctionner après déconnexion ; une session locale semble correctement terminée alors que le serveur ne l’a pas invalidée. Constat contractuel, pas tentative avec un token utilisateur.
- **Recommandation :** Définir un contrat de révocation sécurisé et l’appeler dans les deux clients ; vérifier les retours. Logout offline : effacement local immédiat, mais politique honnête de révocation distante différée/expiration, sans conserver un secret inutilement.
- **Effort :** S/M. **Dépendances :** FR-006.
- **Test de résolution :** Test vertical avec véritable filtre Spring : logout puis refresh refusé ; déconnexion avec access expiré ; offline et indisponibilité backend.

### FR-008 — Dispatcher outbox : réseau dans transaction, dix tentatives sans backoff ni claim multi-instance

- **Module / catégorie :** Backend · outbox — Risque de production.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/eventDispatcher/OutboxEventDispatcher.java:18,39-77 ; ScheduledOutboxEventDispatcher.java:21 ; src/main/java/com/nm/fragmentsclean/platform/eventing/StableEnvelopeOutboxEventSender.java:37-48.
- **Comportement observé :** Une transaction englobe la sélection des 50 PENDING et les envois externes. Pas de claim/lease par lot ; retries bornés à 10, tick par défaut 500 ms, sans nextAttemptAt. Une panne rapide persistante peut épuiser les tentatives en quelques secondes (hors latence réseau). Deux instances peuvent envoyer le même lot.
- **Risque et scénario :** Panne SQS transitoire → événements FAILED nécessitant reprise opérateur ; connections DB détenues pendant I/O ; doublons au scale-out. L’outbox durable conserve les lignes : ne pas appeler cela perte physique automatique.
- **Recommandation :** Claims courts, envoi hors transaction, finalisation conditionnelle, backoff exponentiel avec jitter, circuit de reprise et visibilité des FAILED. Garder l’at-least-once et corriger FR-001 avant scale-out.
- **Effort :** M/L. **Dépendances :** FR-001 ; runbook FR-015.
- **Test de résolution :** SQS indisponible puis rétabli ; crash après send avant mark ; deux dispatchers ; pool DB limité ; aucune commande perdue et latence bornée.

### FR-009 — Les routes d’événement inconnues sont silencieusement considérées traitées

- **Module / catégorie :** Backend · contrat événements — Compatibilité / observabilité.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/sqs/SqsIntegrationEventRouter.java:44-62 ; src/test/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/sqs/SqsIntegrationEventRouterTest.java:60.
- **Comportement observé :** Absence de handler → debug puis markProcessed, puis delete SQS. Test existant valide ce choix. Certaines destinations de fanout peuvent intentionnellement ignorer certains types : toute absence n’est donc pas un bug.
- **Risque et scénario :** En déploiement mixte, une nouvelle route réellement nécessaire peut être acquittée par une ancienne instance et ne jamais rejouée.
- **Recommandation :** Catalogue explicite des ignores autorisés ; une route inconnue/non compatible sort du succès normal (quarantaine/DLQ). Versionner le déploiement des consommateurs avant producteurs.
- **Effort :** S/M. **Dépendances :** FR-001 ; catalogue de contrats.
- **Test de résolution :** Test événement volontairement ignoré vs nouveau type obligatoire ; compatibilité N/N-1, aucun acquittement silencieux du second.

### FR-010 — SSE par polling individuel et curseur de séquence non assimilable à l’ordre de commit

- **Module / catégorie :** Backend + mobile · fraîcheur — Scalabilité / concurrence.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/projectionSync/ProjectionSyncDispatcher.java:51-58,87-92 ; ProjectionSyncProperties.java:7-11 ; src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/secondary/gateways/repositories/jdbc/JdbcProjectionSyncRepository.java:29-39,64-75 ; src/main/resources/schema.sql:348.
- **Comportement observé :** Deux tâches planifiées par connexion ; un SELECT par seconde par flux par défaut. Lecture id>curseur puis avancement au dernier id. Une séquence SQL attribue les id avant commit, pas dans l’ordre des commits.
- **Risque et scénario :** T1 réserve n et tarde, T2 réserve n+1 et commit ; le lecteur passe n+1 avant commit T1 : notification n manquée. À charge, C connexions ≈ C requêtes de polling/s hors snapshots. Risque prouvé conceptuellement, interleaving non reproduit ici.
- **Recommandation :** Filtrer FR-004, coalescer par instance/audience, définir un watermark/replay tolérant les commits inversés et une resynchronisation snapshot au reconnect. Ne pas introduire Redis sans mesure.
- **Effort :** M/L. **Dépendances :** FR-004 ; tests charge.
- **Test de résolution :** Deux transactions avec commits inversés ; reconnexion et refresh sans perte durable ; charge 100/500/1000 connexions et mesure DB/SSE.

### FR-011 — Bootstrap articles directement dans la projection, actif sans garde de profil

- **Module / catégorie :** Backend · articles — Dette architecturale avec effet produit.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/articleContext/read/adapters/secondary/bootstrap/ArticleReadSeedRunner.java:21,35-48,98 ; src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/secondary/gateways/repositories/JdbcArticleRevisionMaterializer.java:15-18.
- **Comportement observé :** Si la projection est vide, le runner insère le JSON seed sans commande ni agrégat/revision auteur. Pas de condition de profil sur ce composant. Cela peut expliquer une différence catalogue éditorial/projection sur une base fraîche ; ce n’est pas une preuve que le problème TestFlight passé est encore présent.
- **Risque et scénario :** Bootstrap ou reconstruction d’une projection vide réintroduit du contenu qui n’a pas de lifecycle write-side correspondant ; une action Studio ne peut pas être présumée équivalente à ces lignes.
- **Recommandation :** Bootstrap métier explicite via commandes/outil d’import versionné, seulement quand demandé ; reconstruction depuis sources de vérité, jamais depuis fixtures implicites. Plan de migration des lignes seed existantes sans suppression surprise.
- **Effort :** M. **Dépendances :** Décision sur les seeds historiques ; contrats Studio/mobile.
- **Test de résolution :** Base fraîche et reconstruction après vidage contrôlé en test : mêmes articles gérables côté Studio, pas de contenu de démonstration introduit implicitement.

### FR-012 — RSS opérateur : destination réseau peu contrainte, corps et durée non bornés

- **Module / catégorie :** Backend · editorialIntelligenceContext — SSRF conditionnelle / déni de service.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/editorialIntelligenceContext/write/adapters/secondary/gateways/rss/RssEditorialSourceDiscoveryAdapter.java:46-63,75-96 ; src/main/java/com/nm/fragmentsclean/editorialIntelligenceContext/configuration/EditorialIntelligenceConfiguration.java:26-28.
- **Comportement observé :** Seul le schéma HTTP(S) est validé ; connectTimeout=10s ne borne pas lecture du corps. DOM construit depuis InputStream sans limite de taille ni timeout de requête. Le parseur bloque bien DTD/entités externes : pas de XXE avérée.
- **Risque et scénario :** Un endpoint RSS interne configuré par un opérateur ou une source compromise peut lire le réseau interne ou garder un worker bloqué avec un flux infini. Privilèges admin requis pour configurer la source ; IMDSv2 constaté réduit une voie d’exfiltration mais n’est pas une protection SSRF générale.
- **Recommandation :** Politique de destinations autorisées, refus réseaux privés/link-local avec contrôle DNS/redirect, timeout global lecture, limite octets/items et parsing borné ; tester aussi l’adaptateur YouTube.
- **Effort :** M. **Dépendances :** Politique de sources éditoriales.
- **Test de résolution :** Serveur local de test : slow body, body excessif, redirect vers privé, DNS rebinding simulé, XML hostile ; libération des ressources et échec retraçable.

### FR-013 — La limite de temps du processus ticket ne couvre pas son écriture d’entrée

- **Module / catégorie :** Backend + moteur C++ — Résilience / ressources.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/secondary/gateways/ticketEngine/ProcessBuilderTicketVerificationProvider.java:58-85,130-134.
- **Comportement observé :** L’écriture stdin précède le démarrage des collectors et waitFor(timeout). Si le sous-processus ne lit plus stdin, cette écriture peut bloquer avant la limite ; join() n’a pas de limite. Le finally ne détruit pas systématiquement le processus.
- **Risque et scénario :** Processus défaillant ou bloqué sur une grande entrée → worker JVM immobilisé malgré timeout configuré ; bail expire et un second travail peut partir. Le moteur normal text-only peut ne pas produire ce cas, mais l’adaptateur doit borner la frontière.
- **Recommandation :** Délai global englobant start/write/read/wait, entrées/sorties bornées, cleanup et interruption garantis, pas de commande shell construite depuis OCR.
- **Effort :** M. **Dépendances :** FR-001 ; workers ticket.
- **Test de résolution :** Binaire de test ne lisant pas stdin, sortie infinie et arrêt brutal ; nombre de threads/processus revient à son niveau initial.

### FR-014 — Suppression des commandes locales inconnues ou sans gateway

- **Module / catégorie :** Mobile · outbox — Dette de résilience / upgrade.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** /Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/outboxWl/processOutbox.ts:165-175,201-207.
- **Comportement observé :** Branches gateway manquante / kind inconnu retirent la commande durable. Ce ne sont pas des REJECTED métier. La configuration nominale testée peut ne jamais les rencontrer ; le risque vise réhydratation après évolution/downgrade et défaut de wiring.
- **Risque et scénario :** L’utilisateur conserve l’impression d’avoir demandé une mutation qui n’atteindra jamais le serveur après mise à jour.
- **Recommandation :** Quarantaine explicite, versionner le stockage/les commandes et migrer à la réhydratation ; aucune disparition sur incident technique.
- **Effort :** S/M. **Dépendances :** Versions de schéma outbox.
- **Test de résolution :** Stockage N-1 chargé en N et kind inconnu : conservation et diagnostic ; gateway rétablie : reprise.

### FR-015 — Santé globale UP alors que deux sous-systèmes restent dégradés

- **Module / catégorie :** Backend · AWS · release — Observabilité / exploitation.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/platform/observability/MessagingRuntimeHealthIndicator.java:39-52 ; src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/secondary/observability/ArticleAuthoringHealthIndicator.java:32-47 ; .github/workflows/deploy-staging-backend.yml:163-187 ; endpoint staging /actuator/health lu le 17/09.
- **Comportement observé :** HTTP de santé consulté : UP global, articleAuthoringHealth=DEGRADED, messagingRuntimeHealth=DEGRADED. 19 alarmes AWS lues, une ALARM legacy-shared-dlq-not-empty ; trois messages dans cette DLQ. Le contrôle curl réussi ne valide pas ces composants ni la propagation des commandes.
- **Risque et scénario :** Déploiement déclaré sain alors qu’une génération ou projection est bloquée ; dette ancienne et nouvelle panne sont indiscernables sans triage.
- **Recommandation :** Qualifier les lignes/messages sans les purger, owner et échéance ; gate post-déploiement par composants et transaction synthétique avec preuve de projection. Liveness doit rester indépendante d’une panne de dépendance récupérable.
- **Effort :** M. **Dépendances :** FR-001/008 ; accès d’investigation dédié.
- **Test de résolution :** Panne simulée : alarme au bon owner, commande reliée à saga/outbox/inbox, recette read-after-write ; aucun faux succès de release.

### FR-016 — Pas de quality gate PR backend/mobile équivalent au gate de déploiement

- **Module / catégorie :** CI/CD · backend/mobile/Studio — Dette de test / release.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** .github/workflows/deploy-staging-backend.yml:3-10,45-50 ; mobile eas.json:16-18 et absence de workflow GitHub suivi ; Studio .github/workflows/studio-ci.yml:3-9,35 et studio-deploy.yml (audit dépendances absent de ce dernier).
- **Comportement observé :** Backend complet vérifié au déploiement manuel, mais aucun workflow PR dans l’inventaire. Mobile EAS production auto-incrémente sans gate tests versionné ici. Studio a CI tests/build/contrat/audit ; npm audit high renvoie aujourd’hui un résultat non vert. Les règles de protection distantes n’ont pas été contrôlées.
- **Risque et scénario :** Régression fusionnée avant exécution ; publication depuis état local différent ; audit sécurité contourné par push direct main Studio.
- **Recommandation :** Workflows PR réutilisés pour promotion, statuts requis, matrice Java21/Node épinglée, tests front/back/contrat, scan secret/SCA ; artefact exact testé promu. Gérer une exception CVE seulement avec reachability/échéance documentées.
- **Effort :** M. **Dépendances :** FR-018 ; permissions dépôts.
- **Test de résolution :** PR de test volontairement rouge bloquée ; mêmes commits/contrats/artefacts dans reçu de release.

### FR-017 — Bootstrap, upgrade et reconstruction ne sont pas la même garantie

- **Module / catégorie :** DB · migrations — Maintenabilité / exploitation.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** src/main/resources/schema.sql:1 ; src/main/resources/db/release/*.psql ; infra/aws/compose/platform/staging/fragments/render-release-migration.sh:8-32 ; docs/deployment/journaled-staging-deployment.md ; pom.xml.
- **Comportement observé :** Pas de Flyway dans le POM ni db/migration. Il existe en revanche un driver psql transactionnel, journal/checksum et tests d’upgrade : ne pas les présenter comme absents. Liste de drivers/fichiers codée dans le renderer ; schema.sql et fragments répètent du DDL. Pas de preuve exhaustive de parité de toutes les bases/environnements aujourd’hui.
- **Risque et scénario :** Un nouveau module apparaît dans bootstrap mais pas upgrade ; ancienne application relancée après migration incompatible.
- **Recommandation :** Conserver le dispositif testé ou migrer progressivement vers un outil standard, mais rendre explicite le manifeste complet et tester ancien schéma→N→relaunch N-1 compatible. Pas de repair ni réécriture d’une migration appliquée.
- **Effort :** M. **Dépendances :** Inventaire live schéma en lecture seule restant.
- **Test de résolution :** CI base neuve + fixture ancienne + réapplication + checksum divergent + rollback DDL sur erreur ; parité contraintes/index.

### FR-018 — Supply chain : alertes npm et mises à jour Java non qualifiées complètement

- **Module / catégorie :** Mobile/Studio/backend/engine — Risque supply chain, pas exploit démontré.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** mobile package-lock.json ; Studio fragments-studio/package-lock.json ; backend pom.xml:10,95,116-146 ; Dockerfile:1-37. Résultats npm audit/outdated et versions Maven de cette session, annexes.
- **Comportement observé :** Mobile : 43 packages signalés (20 high,21 moderate,2 low). Studio : 2 high, chaîne js-yaml/outillage. 58/11 entrées outdated mobile/Studio. JVM vulnérabilités et image Docker non scannées avec base CVE ; plugin versions n’est pas un scanner CVE. Plusieurs alertes sont transitives/build, pas des attaques mobiles atteignables démontrées.
- **Risque et scénario :** Builder ou parseur exposé à une entrée hostile ; risque de patch tardif. Inversement une mise à jour Expo majeure automatique peut casser le natif.
- **Recommandation :** Triage reachability runtime/build, corrections compatibles lockées d’abord ; scan SBOM Java/image avec base datée ; upgrade Spring/Expo planifié avec tests natifs. Épingler actions/images par digest et réviser périodiquement.
- **Effort :** M/L. **Dépendances :** FR-016 ; rebuild natif.
- **Test de résolution :** Audit après patch avec exceptions justifiées ; tests complets ; archive iOS installée ; Docker vulnérabilités runtime et provenance.

### FR-019 — Test moteur réel non portable ; tests C++ retirés de la construction Docker

- **Module / catégorie :** ticketverify-engine · Docker — Dette de test / reproductibilité.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** /Users/nicolasmaldiney/ticketverify-engine/tests/test_engine_real_receipt.cpp:7-17,31 ; tests/CmakeLists.txt:26 ; backend Dockerfile:14.
- **Comportement observé :** ctest externe : 21/22 passent, fixture non trouvée par ../../tests/fixtures. Fixture bien versionnée : ce n’est pas une donnée manquante du dépôt. Le loader ne vérifie pas l’ouverture. Docker supprime les instructions de tests CMake pour construire le binaire.
- **Risque et scénario :** Le ticket engine livré peut régresser sans que les 544 tests Java l’attrapent ; faux échec lié au CWD masque un vrai signal.
- **Recommandation :** Chemin fixture basé sur source dir explicite, erreur de lecture claire, CI moteur séparée avec contrat JSON du binaire exact épinglé.
- **Effort :** S. **Dépendances :** FR-016 ; release moteur.
- **Test de résolution :** ctest fonctionne depuis build out-of-source et CI ARM64 ; contrat stdin/stdout/exits compatible avec adapter Java.

### FR-020 — Concentration de responsabilités et fichiers minifiés dans le code métier

- **Module / catégorie :** Backend + mobile + Studio — Maintenabilité.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** src/main/java/com/nm/fragmentsclean/platform/eventing/IntegrationEventPayloadMapper.java:48 (374 lignes,45 décisions) ; src/main/java/com/nm/fragmentsclean/userApplicationContext/write/businesslogic/usecases/ConfirmAvatarCommandHandler.java:5 ; /Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/outboxWl/commandHandlers/outboxCommandHandlers.ts:214 ; /Users/nicolasmaldiney/fragments-admin/fragments-studio/src/articleStudioContext/ui/components/ArticleEditorPanel.tsx:65.
- **Comportement observé :** Le mapper central change souvent ; plusieurs fichiers métier de 3 à 5 lignes contiennent une classe entière. Les gros handlers outbox et composants UI concentrent validation, orchestration et rendu. Ce n’est pas un motif de réécriture du produit.
- **Risque et scénario :** Une modification de contrat touche un hotspot transversal ; revue et diagnostic difficiles, métriques de lignes trompeuses pour les fichiers compressés.
- **Recommandation :** Formatage séparé sans changement sémantique, contrats typés par BC dans composition platform, extraction des transitions testables des composants ; séparer les commits comportement/style.
- **Effort :** M. **Dépendances :** Après P0/P1, tests existants préservés.
- **Test de résolution :** Tests inchangés verts avant/après extraction ; diff lisible ; règles d’import maintenues.

### FR-021 — Les tests d’architecture tolèrent des imports internes au-delà du contrat ACL primitif

- **Module / catégorie :** Backend · frontières BC — Dette architecturale.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** src/test/java/com/nm/fragmentsclean/architecture/BoundedContextArchitectureTest.java:199-223 ; src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/secondary/gateways/article/CommandBusArticleAuthoringPort.java:5-6.
- **Comportement observé :** Exception de chemin admin→article permissive ; l’adaptateur importe modèles/VO internes. Les tests d’architecture passent, mais ne prouvent pas toute la doctrine fournie. Le nouveau editorialIntelligenceContext n’est pas listé dans la doctrine initiale.
- **Risque et scénario :** Extension future de l’exception permet couplage silencieux aux invariants/données d’un autre BC.
- **Recommandation :** ACL explicite primitive et catalogue d’exceptions nommées/temporaires ; tests de frontières précis, read/write/pureté ; aligner la cartographie de doctrine sans affaiblir ownership.
- **Effort :** M. **Dépendances :** Contrats article/admin ; FR-020.
- **Test de résolution :** Test négatif qui introduit un import d’agrégat, un accès direct de controller et un accès SQL inter-BC : tous refusés.

### FR-022 — Isolation opérationnelle limitée : instance et rôle AWS partagés avec Anchor

- **Module / catégorie :** AWS · IAM/compute/backups — Risque de production accepté seulement explicitement.
- **Sévérité / priorité :** MEDIUM / P2.
- **Preuves :** infra/aws/cloudformation/platform-staging.yaml:131-164 ; infra/aws/compose/platform/staging/fragments/docker-compose.yml:1-48 ; observations EC2 du 17/09.
- **Comportement observé :** Un t4g.medium héberge la plateforme ; rôle runtime autorise des préfixes S3/SSM Anchor et Fragments, y compris DeleteObject sur les backups Fragments. EBS chiffrés 30/80 Go, IMDSv2 requis, SG n’expose pas PostgreSQL. Pas de preuve multi-instance/HA.
- **Risque et scénario :** Compromission runtime ou saturation d’un produit affecte l’autre ; le même rôle peut altérer les sauvegardes qui servent à récupérer. Shared host reste un choix MVP possible, pas une isolation forte.
- **Recommandation :** Réduire permissions par workload et dissocier write/read/delete backup, monitoring capacité/crédits et restauration ; isoler la DB/compute lorsque risque et charge le justifient. Pas de Kubernetes requis.
- **Effort :** M/L. **Dépendances :** FR-003 ; budget/disponibilité cible.
- **Test de résolution :** IAM simulation/compte de test : app ne supprime pas backup hors rôle autorisé ; restauration hors hôte ; charge avec service voisin.

### FR-023 — Recette App Store et crash reporting natif non attestés par cette passe

- **Module / catégorie :** Mobile · distribution/exploitation — Dette de validation.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** /Users/nicolasmaldiney/fragmentsCleanFront/docs/deployment/app-store-staging-runbook.md ; docs/deployment/testflight-app-store-release.md:120,151,226 ; /Users/nicolasmaldiney/fragmentsCleanFront/eas.json ; /Users/nicolasmaldiney/fragmentsCleanFront/app.config.js:37-40,96-127.
- **Comportement observé :** Tests JS verts et liens/config requis ne prouvent ni l’archive signée, ni les manifests agrégés Pods, ni le login Apple réel, ni VoiceOver. Aucun nouveau build/install/TestFlight ni crash symboliqué exécuté dans cette phase. Pas de dossier first-party PrivacyInfo.xcprivacy suivi trouvé, mais cela ne prouve pas son absence de l’IPA.
- **Risque et scénario :** Rejet Apple ou bug uniquement natif ; incident utilisateur non diagnostiqué. Les captures et succès passés du fil ne sont pas une recette du commit actuel.
- **Recommandation :** Figer tuple commits/backend/config/buildId ; inspecter IPA/entitlements/manifests, tester Google/Apple, suppression, UGC, médias, petits écrans/accessibilité/offline et rapport crash symboliqué. Vérifier URLs légales réelles et déclarations App Privacy avec les données collectées.
- **Effort :** M. **Dépendances :** FR-001 à 007 ; archive signée et appareil.
- **Test de résolution :** Matrice signée de recette réelle, compte/démo revue sans 2FA bloquante, suppression effective et suivi modération ; preuve de symbolication.

### FR-024 — Secrets : contrôles utiles mais historique non certifié ; refresh en clair dans la base

- **Module / catégorie :** Authentification · CI · données — Sécurité / dette d’audit.
- **Sévérité / priorité :** HIGH / P1.
- **Preuves :** src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/secondary/gateways/repositories/jpa/SpringRefreshTokenRepository.java:11 ; src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/secondary/gateways/TokenGateway/JwtTokenService.java:83-90 ; src/main/resources/schema.sql:701-711 ; scripts/validate-no-committed-secrets.sh ; /Users/nicolasmaldiney/fragments-admin/fragments-studio/src/config/validateBuildEnv.ts.
- **Comportement observé :** Recherche historique limitée à empreintes de clés privées/AKIA/ASIA : un hit de code PEM dans HttpAppleAuthService (pas une clé privée démontrée). Aucun secret réel publié dans ce rapport. Les refresh sont des valeurs retrouvables par token, non des empreintes ; une copie DB compromise peut les réutiliser. Les garde-fous de bundle Studio refusent correctement les anciens tokens d’environnement.
- **Risque et scénario :** Backup/lecture DB non autorisée → usurpation avec refresh encore valide ; absence de hit regex n’est pas absence de secret dans toutes branches/artefacts/logs.
- **Recommandation :** Hasher les refresh, rotation compatibilité et tests ; scanner historique avec outil spécialisé à sortie expurgée, vérifier révocation des secrets réellement exposés et accès backup. Ne pas réécrire l’historique avant inventaire/coordination.
- **Effort :** M. **Dépendances :** FR-003/006/007 ; IAM FR-022.
- **Test de résolution :** Un dump de table ne fournit aucun bearer réutilisable ; scan sans fuite dans logs ; ancien refresh migré/invalidé selon politique.


## 1. Cartographie factuelle

### Applications, couches et ownership

| Élément | Entrée / responsabilité | Stockage / dépendances |
|---|---|---|
| Mobile Expo/RN | app/routes/_layout.tsx → RootNavigator, AppBootstrap, Redux listeners et view models | SecureStore, stockage outbox/médias local ; HTTP, SSE, ACK opportunistes |
| Backend Spring | FragmentsCleanApplication ; controllers, CommandBus, query handlers, @Scheduled, SQS | PostgreSQL ; S3 ; SQS ; OAuth Google/Apple ; providers éditoriaux |
| Studio React/Vite | index.html → application React, authContext, Redux/use cases par capacité | tokens en mémoire, PKCE en sessionStorage, journal d’opérations localStorage ; API admin |
| ticketverify C++20 | CLI stdin texte → JSON v1 / exit codes | processus local lancé par Java ; pas un service HTTP |
| AWS | Caddy/Compose sur EC2, PostgreSQL15, SQS, ECR, S3/CloudFront, SES, SSM, CloudWatch/SNS | déploiement GitHub OIDC→SSM ; sauvegarde timer→S3 |
| FlowAtlas | MCP find/context, adaptateurs Redux et slices Java explicites | Instrument d’analyse ; pas dépendance runtime Fragments |

| BC ou module backend | Ownership constaté | Risques / particularités |
|---|---|---|
| authenticationContext | OAuth, JWT, refresh, identité, accès admin, révocation fournisseur | FR-006/007/024 |
| userApplicationContext | profil/avatars, favoris, Pass/entitlements, processus d’effacement | contributions tickets/expériences ; FR-002/003 |
| coffeeContext | agrégat café, photos/horaires, catalogue/read models, enrichissement/import | projections JDBC, références locales consommateurs |
| articleContext | articles/révisions structurées, génération, validation, publication, archive, rang à la une | saga, HMAC, images ; FR-005/011 |
| socialContext | likes/commentaires, signalements/blocages, projections sociales | UGC et propagation profil |
| ticketContext | soumission, fingerprint, job de vérification, état/projection ticket | processus local C++ ; inbox et lease |
| experienceContext | expérience sans ticket obligatoire, publication/modération, médias et références | ticket conserve une valeur probante pour certains niveaux ; FR-002/003 |
| adminImportContext | adaptateurs Studio, ACL authoring/import, commandes opérateur | ne doit pas devenir propriétaire des articles ; exception FR-021 |
| **editorialIntelligenceContext** | sources RSS/YouTube, signaux/candidats, briefs, consultation et calendrier | BC effectivement présent mais absent de la liste initiale ; SSRF/timeout FR-012 |
| sharedKernel | commandes durables, outbox/inbox, SSE, stockage technique, rate limit | plusieurs responsabilités techniques centrales |
| platform | contrats/enveloppes/routage, wiring et observabilité inter-contextes | mapper central, pas domaine métier propriétaire |

Le backend comporte **un module Maven racine**, pas un microservice par BC. Studio possède aussi existingCoffeesContext, articleStudioContext, moderationContext, adminAccessContext, studioOperationContext, editorialSourcesContext, topicCandidatesContext et editorialCalendarContext. Le mobile ajoute notamment projectionSyncWl aux contextes de la doctrine initiale.

### Échanges et destinations

Flux nominal : mutation mobile → listener → optimistic reducer → outbox locale → HTTP command → receipt PENDING/APPLIED/REJECTED → outbox backend dans transaction métier → enveloppe stable → destination SQS → claim inbox → handler/projection → projection.updated SSE → action de récupération du snapshot. ACK socket seulement opportuniste. Une receipt APPLIED prouve l’application de la commande, pas nécessairement l’arrivée de toutes les projections inter-BC.

Destinations identifiées dans IntegrationEventDestinationResolver et AWS : **domain-events, auth-users-events, app-users-events, coffees-events, articles-events, experiences-events, ticket-events, ticket-verification-requested**, avec une DLQ par destination ; DLQ partagée legacy conservée séparément.

| Producteur / fait | Destinations importantes | Consommation |
|---|---|---|
| TicketVerifyAccepted | ticket-events + ticket-verification-requested | projection analyzing + création/avancement job |
| Autres faits Ticket | ticket-events + app-users-events | projection terminale et contributions Pass |
| Coffee created/archive/delete | coffees-events + app-users-events + experiences-events | fiche, favoris/références, disponibilité expérience |
| Coffee publication | coffees-events + experiences-events | visibilité du catalogue/références |
| AppUser created/profile updated | app-users-events + experiences-events | profil et références auteur ; social à confirmer sur l’ensemble du fanout |
| ExperienceLifecycleChanged | app-users-events | progression non assimilée à ticket vérifié |
| Autres Experience / media / report | experiences-events | expériences, photos, modération |
| Article / ArticleAuthoringSaga | articles-events | projections et orchestration éditoriale |
| Comment/Like | domain-events | projections sociales |
| AppUserDeletionRequested | app-users/auth-users/domain/ticket/experiences | nettoyage par BC et acknowledgements d’effacement |

L’inbox est indexée **destination+eventId**, non nom de handler individualisé. Actuellement le routeur impose un handler par couple destination/type. Ajouter plusieurs consommateurs logiques sur une même destination exige de rediscuter cette granularité ; ce n’est pas automatiquement une implémentation de la règle « un inbox par handler ».

### Base et migrations

PostgreSQL système unique. Schéma public : familles auth_users/identities/refresh_tokens/auth_provider_credentials/admin_user_access ; app_users/account_deletion_processes/favoris/Pass ; coffees/photos/opening_hours et projections/checkpoints ; articles/revisions/sections/paragraphs/images/tags/authoring ; tickets/jobs/entitlements ; social/UGC ; expériences/médias ; technical outbox_events/inbox_messages/command_status/projection_sync_events ; editorial sources/signals/schedules.

155 occurrences CREATE TABLE trouvées **toutes sources SQL et fixtures incluses** ne signifient pas 155 tables de production. Le schéma déployé courant n’a pas été dumpé aujourd’hui. Les scripts release comportent 10 fragments SQL et 3 drivers psql. L’historique de déploiement décrit une ancienne base 46 tables et un upgrade 69 tables ; ces chiffres historiques ne sont pas présentés comme comptage live du 17 septembre.

Writes métier JPA dominant, lectures JDBC, mais matérialisation de révisions et plusieurs magasins éditoriaux utilisent JDBC côté write. Évaluer invariants/transaction de chaque exception plutôt que décréter « JDBC interdit ». Les commandes et projections doivent rester séparées même dans une même transaction locale.

### Points d’entrée et tâches

Comptage lexical : **120 annotations Mapping** (dont RequestMapping de classe : ce n’est pas 120 routes distinctes), **8 annotations @Scheduled**. Inventaire précis des annotations en annexe ; il n’équivaut pas à une extraction exhaustive du mapping Spring au runtime. Les beans SQS sont également des points d’entrée asynchrones.

HTTP : /auth/google/mobile, /auth/apple/mobile, /auth/google/studio/config, /auth/google/studio, /auth/refresh, /auth/logout, /auth/me ; /commands/{id} ; /api/coffees et assets ; /api/articles et assets ; tickets/verify/read/history ; profil/suppression/avatar ; expériences/médias/modération ; commentaires/likes/blocages ; admin cafés/import/authoring/approval/calendar/access/operations ; SSE /api/sync/events et /api/admin/sync/events.

Tâches : dispatcher outbox, worker vérification tickets, nettoyage avatars et médias expérience, consultations de sources, analyse signaux, calendrier éditorial ; la génération a ses consommateurs/workers. Timers système : backup PostgreSQL, métriques santé éditoriale. Les schedules locaux se multiplient avec les instances ; le claim est indispensable là où un effet doit être unique.

### Flux critiques : revue et couverture réelle

| Flux | Chemin confirmé / cas nominal | Erreurs, doublons, courses et diagnostic |
|---|---|---|
| Auth mobile | gateway OAuth natif/PKCE → endpoint → identité → JWT/refresh → SecureStore | tests Google PKCE/Apple web boundary ; Apple réel non rejoué ; refresh/logout FR-006/007 ; logs auth sans publier tokens |
| Profil / avatar | profileUpdateRequested/AvatarAttach → outbox ; upload pending → normalisation → confirm commande → événement profil | ancien avatar marqué DELETION_PENDING ; même ownership par appUserId pour Apple/Google ; pas deux domaines avatar ; reprise S3/DB, expiration URL et courses de remplacement à tester davantage |
| Café découverte | catalogue/nearby/détail → gateway mapper → Redux → Map/Home ; fraîcheur SSE | catalogue distinct du proximité ; tests contrat/cache ; GPS réel/panoramique/carousel non manipulés ; pas conclusion basée sur anciennes captures |
| Like/comment | intent → optimistic → enqueue → gateway → receipt/ACK → reconcile ; snapshots publics filtrés UGC | déduplication/rollback couverts dans suites ; blocked users, privé et global SSE FR-004 ; reporting/traitement opérateur à recetter |
| Ticket | intent → UUID stable/OCR → HTTP202 → fingerprint/agrégat → outbox → SQS → job leased → C++ → completion → projection | duplicate fingerprint, lease, erreurs retryables présents ; FR-001/013 ; imageRef seul accepté à l’entrée mais provider actuel exige OCR (contrat text-only explicite) |
| Entitlements | faits tickets/expériences → contributions locales Pass → snapshot → Redux | ne pas confondre expérience sans preuve et niveau exigeant ticket ; tester replay après suppression, retrait/recalcul et ordres inversés |
| SSE→snapshot | notification projection.updated ; pas de domaine complet envoyé au mobile | coalescence café et reconnexion présentes ; filtrage, amplification et curseurs FR-004/010 ; refresh manuel Home réellement dans le code |
| Import Studio | recherche/preview Google Places → use case admin → ACL/commande Coffee → outbox/projection | boutons/idempotence et journal d’opérations ; preview n’est pas import ; pas nouvel import live effectué ; fournisseur indisponible et concurrence multi-opérateurs à valider |
| Article éditorial | brief → saga lease génération → validation/review → publication → projection ; draft/archived exclus lecture publique | owner articleContext ; revue vs génération indépendante ; anciennes seeds FR-011 ; HMAC FR-005 ; no-autopublish déclaré par workflow humain |
| HMAC | lien saga/article/révision/expiry/hash → POST d’approbation → consommation conditionnelle → commandes | bonne comparaison constante et consommation SQL atomique ; sous-secondes échouent ; liens en query/path visibles dans historique/logs avant retrait ; pas GET mutant trouvé sur cette action |
| Curation Home | publié+featuredRank ordonné, 5 max ; sans rang fallback un article ; bibliothèque des publiés | selectHomeHeroArticles n’invente pas 5 cartes ; rangs clairsemés acceptés ; archive/retrait nettoient rang ; changement Studio auto-dispatch sur sélection, pas second bouton requis |
| Effacement | commande → process manager → effaceurs BC → ACKs ; nettoyage objets différé | FR-001/002/003 ; suppression applicative ≠ suppression instantanée de tous objets/backups |
| Sources/calendrier | RSS/YouTube → signaux → candidats retenus → brief ; schedule→commande→réconciliation | nouveau flux découvert et inspecté ; connectTimeout seul insuffisant ; tous les interleavings du calendrier non exercés |

« À lire ensuite » utilise le catalogue remonté, pas une liste de titres codée dans Home. Le transport et les selectors doivent rester l’unique chemin ; les fixtures dans assets/data sont aussi utilisées par des fakes de tests, donc ne prouvent pas une donnée de production factice. L’affichage « un article » peut être le fallback prévu si aucun featuredRank n’arrive ; vérifier payload/révision réellement déployée avant d’accuser le carousel.

## 2. Mesures quantitatives

### Méthode et limites de mesure

Fichiers = git ls-files, texte UTF-8 sans NUL ; LOC physiques y compris docs/config/lockfiles. SLOC **approximatives** : lignes non vides hors commentaires de ligne/blocs détectés ; pas un lexer parfait (commentaires inline, littéraux et code minifié limitent ce chiffre). Extensions code : Java, TS/TSX, JS, C/C++, shell, SQL, Swift/Kotlin/Gradle. Tests reconnus par chemins test/tests/__tests__ ou .test/.spec ; certains fakes sous app/main sont donc comptés production bien qu’outils de test.

Fonctions TS/JS et méthodes Java mesurées par AST ; décisions = 1 + branches/boucles/catch/case/expressions logiques. Proxy de complexité explicite, **pas certification McCabe comparable entre outils** ; lambdas/méthodes imbriquées Java peuvent contribuer au parent. Pas de mesure C++ équivalente. Les classes Java incluent les types imbriqués/records, pas seulement les fichiers.

| Projet | Fichiers suivis | Fichiers texte | LOC texte | SLOC production/outillage | SLOC tests | Ratio prod/tests |
|---|---:|---:|---:|---:|---:|---:|
| backend | 1442 | 1434 | 85945 | 34359 | 19013 | 1.81 |
| mobile | 598 | 553 | 64505 | 26919 | 8519 | 3.16 |
| studio | 158 | 158 | 15804 | 6948 | 2807 | 2.48 |
| engine | 32 | 32 | 2167 | 1201 | 188 | 6.39 |

Nombre AST Java production : **582 classes, 478 records, 153 interfaces, 47 enums ; 2936 méthodes avec corps**. Mobile : **1823 fonctions JS/TS**, 81 fichiers TSX, 28 fichiers nommés useX (proxy hooks, pas décompte sémantique). Studio : **777 fonctions**, 27 fichiers TSX ; aucun fichier useX détecté, pas preuve d’absence de hooks. « Service » n’est pas une catégorie mesurable fiable par suffixe dans cette architecture : les ports/use cases/beans sont cartographiés plutôt que sommés arbitrairement.

Dépendances déclarées JS : mobile **51 runtime +13 dev**, Studio **4 runtime +11 dev**. Backend package.json a une dépendance d’outillage JS : à distinguer des dépendances Maven/BOM. Pas de nombre artificiel de dépendances Java transitives « obsolètes » : liste précise des mises à jour mesurées en annexe.

### Concentrations et hotspots

| Fichier | Taille / mesure | Fréquence Git depuis 17/08 | Lecture |
|---|---|---:|---|
| backend schema.sql | 1199 LOC | 32 commits touchant le fichier | DDL central, parité bootstrap/upgrade sensible |
| IntegrationEventPayloadMapper.java | 747 LOC ; méthode toPublicPayloadJson 374 lignes /45 décisions | 19 | hotspot réel de contrats transversaux |
| JdbcCoffeeProjectionRepository.java | 533 LOC | 8 | nombreuses responsabilités projection/lecture à détailler |
| mobile outboxCommandHandlers.ts | 522 LOC ; bloc ligne214 158 lignes/37 décisions | 8 | orchestration et erreurs critiques, refactor après tests |
| mobile HomeScreen.tsx | 305 LOC | 9 | fréquence élevée, pas énorme en soi |
| mobile projectionSyncListenerFactory.ts | 344 LOC | 7 | pont de fraîcheur, risque d’amplification |
| Studio ArticleEditorPanel.tsx | 454 LOC ; fonction 309 lignes/32 décisions | voir annexe métriques | rendu + actions multiples |
| Studio ExistingCoffeeDetailsPanel.tsx | 423 LOC ; fonction 301 lignes/54 décisions | 9 | JSX contribue aux décisions, pas 54 règles métier |
| Studio contrat généré studio-api-v1.ts | 1041 LOC | 11 | généré : pas cible de refactoring manuel |

Fichiers mobile photo.ts (1669 lignes) et openingHours.ts (1300) sont des données/fixtures : leur volume ne signifie pas composant hypertrophié. Plusieurs classes avatar/experience minifiées contiennent beaucoup d’opérations sur une ligne : le nombre de lignes sous-estime leur coût de maintenance.

### Duplication et cycles

Détection conservatrice de blocs normalisés de 12 lignes (pas jscpd complet) : backend au moins 7 groupes, notamment controllers d’assets article/café, propriétés stockage article/café et DDL média répété dans schema/release. Aucun groupe détecté avec cette heuristique mobile/Studio : **cela ne signifie pas zéro duplication**. Upload avatar/expérience et formulaires reproduisent des séquences à comparer sémantiquement avant extraction.

Un SCC syntaxique de 6 fichiers outbox.type/commandFor* est détecté en mobile. Inspection : imports principalement employés en positions de type (`typeof commandKinds` inclus) ; **ne pas qualifier cela de cycle runtime avéré**. Passer en import type clarifierait l’intention. Aucun cycle résolu dans les imports statiques Studio analysés ; exports/barrels/dynamic import non exhaustifs. Backend : pas d’analyse exhaustive du graphe de classes/injection ; tests de frontières exécutés, mais ce n’est pas une preuve de zéro cycle Spring.

### Bundles

Studio build production vérifié : **JS 303.64 kB (gzip 88.06), CSS 13.84 kB (gzip 3.39), HTML 0.40 kB** ; 95 modules transformés, 767 ms de phase Vite sur cette machine, hors dépendances/tsc. Build a d’abord refusé des variables de bearer locales ; relance avec ces variables vides réussie, verify:dist vérifie 3 fichiers.

**Poids mobile IPA/JS/Hermes non mesuré** : aucun nouveau build EAS ou export natif créé. Les tailles d’anciennes URL IPA de conversation ne sont pas des mesures du commit audité.

## 3. Code mort et hygiène

| Candidat | Classification | Vérification / décision |
|---|---|---|
| Home WelcomeMessage.tsx, Categories.tsx | suppression probable à confirmer | recherche nom dans app/tests ne trouve pas consommateur UI ; ne pas supprimer sans graphe exports/routes/storybooks complet |
| imports reducer outbox de tests, alias any inutilisé | suppression certaine locale | tsc noUnused et ESLint concordent ; aucune suppression réalisée |
| DetailsNavBar onRefresh | contrat/prop inutilisé à clarifier | tsc noUnused : réception sans lecture ; peut correspondre à UX incomplète, pas forcément supprimer la fonctionnalité |
| fakeCfPhoto/fakeOpeningHours + assets/data | conservés intentionnellement pour fakes | usages confirmés dans gateways fake ; pas de nettoyage global de fixtures |
| legacy admin_studio_articles | compatibilité de migration | fixture historique et documentation la préservent ; ne pas drop sans inventaire live |
| seed articles | actif, **pas mort** | runner Spring sans profil ; FR-011 |
| façade event bus locale / logging sender | compatibilité local/test | prod désactive bus local et active SQS ; ne pas supprimer sur absence d’usage mobile |
| branche finale provider ticket après exit==0/2/!=0 | branche inatteignable probable | ProcessBuilderTicketVerificationProvider.java:119-129 ; couverture logique des entiers, relecture recommandée |
| anciens dépôts et scripts demo | non qualifiés morts | aucun usage deploy courant trouvé mais possèdent environnements/habitudes possibles |

Recherche TODO/FIXME/HACK/@Disabled/test.skip : deux TODO backend trouvés dans tests sociaux, pas de test ignoré par la suite release. Zero TODO ne mesure pas la dette : les constats distribués majeurs n’ont pas de TODO.

Contrôle supplémentaire TypeScript `--noUnusedLocals --noUnusedParameters` échoue sur **12 diagnostics** ; le tsconfig nominal passe. Plusieurs paramètres de callback sont volontairement non utilisés. Pas d’outil de référence complet type Knip/JVM reachability installé/exécuté ; endpoints sans consommateur externe et beans réflexifs non déclarés morts par simple grep.

## 4. Bilan architectural

### À préserver

- Monolithe modulaire adapté au stade produit ; aucune raison démontrée de répartir ces BC en microservices.
- DurableCommandExecutor : registration d’une receipt, verrou, fingerprint/requester, transaction métier, persistance séparée du REJECTED ; APPLIED même sans nouvel événement. Les vieux P0 de statut ne sont pas arbitrairement déclarés ouverts : ce mécanisme existe et ses tests passent.
- GetCommandStatusQueryHandler appelle findForRequester : contrôle d’objet effectif dans le chemin de lecture des receipts ; ACK WebSocket non source de vérité.
- Invariants ticket/profil/expérience dans modèles/use cases ; ports de stockage, normalisation d’images hors agrégat ; expériences sans ticket cohérentes avec la décision produit.
- SQS, enveloppes versionnées, projections locales, tests de replay/lease constituent une bonne direction. FR-001 illustre la différence entre l’intention d’idempotence et sa réalisation exacte.
- Home est une composition de lecture ; aucun homeContext métier artificiel nécessaire. FeaturedRank est un attribut de publication, pas un cinquième statut métier indépendant.
- Archive article est une transition et une projection, non une suppression physique demandée au controller.

### À corriger sans réécriture

Priorité aux frontières de transaction, à l’ack et à l’effacement, non au renommage des packages. L’exception admin→article et la matérialisation JDBC sont des dettes à encadrer, pas prétexte à introduire dix interfaces sans invariant. Extraire le mapper platform par contrats propriétaires pour réduire la cascade de changements. Documenter le nouveau contexte éditorial et ses événements/états.

Horizontalité non prouvée : jobs génération/ticket possèdent lease/version/owner (bonne base), mais inbox/outbox, polling SSE, rate limiting en mémoire et stockage local restent des limites. Le RateLimiter déclare lui-même « Single-node » ; il ne protège pas les endpoints d’auth anonymes, et multiplier les instances multiplie le quota.

## 5. Bilan des tests

| Vérification exécutée | Résultat exact | Portée réelle |
|---|---|---|
| backend bash scripts/test-release.sh | 544 tests,0 failure,0 error,0 skipped ; BUILD SUCCESS ; environ5m20 | unitaires, infra PostgreSQL/LocalStack, HTTP, architecture et verticales réunis |
| mobile Jest --runInBand --watchman=false | 93 suites,353 tests réussis ; 37.989s | JS/TS, fakes/adapters et assertions de guardrails |
| mobile tsc --noEmit | sans diagnostic | contrat de compilation, pas comportement natif |
| mobile ESLint app tests | 0 erreur,16 warnings | import/first, array-type et unused tests ;12 fixables |
| mobile tests config Node | 8 réussis | contraintes publiques de config production |
| mobile tsc noUnused supplémentaire | 12 diagnostics, échec attendu de l’analyse stricte | candidats d’hygiène, pas régression du tsconfig nominal |
| Studio Vitest --maxWorkers=1 | 34 fichiers,160 tests réussis ;52.17s | unit/use cases/adapters/UI jsdom |
| Studio contract:check | contrat généré courant | OpenAPI v1 backend local vs TS, pas tout endpoint backend |
| Studio build + verify:dist | réussi après neutralisation anciens bearer locaux | sortie production non publiée |
| engine cmake build | réussi | compilation C++ native machine locale |
| engine ctest |21/22 ; un échec fixture/CWD | pas bug parser démontré |
| engine binaire de tests, CWD corrigé sans changement de source |22/22,56 assertions | confirme le défaut de chemin de fixture ; CTest externe reste non portable |
| scripts shell backend : bash -n |20 scripts,0 échec | syntaxe seulement ; aucun script de déploiement exécuté |
| sonde HMAC indépendante | entier valide ; sous-seconde rejetée | défaut de validation reproduit avec classes réelles |

Java local **23-valhalla**, compilation target21 ; ce n’est pas la même JVM que Java21 du workflow. Certaines infra tests utilisent PostgreSQL13.1 et les tests release upgrade PostgreSQL15 ; prod documentée PostgreSQL15. Node local22.16.0, Studio CI24. Docker/Testcontainers démarrés uniquement pour tests locaux. Les dépendances installées/caches ont été réutilisés ; **build clean checkout avec npm ci de tous dépôts non reconstitué intégralement**.

### Qualité, pas seulement quantité

- Avertissements backend effectivement vus : API dépréciée dans S3ArticleImageStorageConfiguration (DefaultCredentialsProvider.create aux lignes57/66, remplacement précis à confirmer par -Xlint:deprecation), API de test DefaultCoffeePhotoUriResolver, dialecte Hibernate explicitement redondant, open-in-view activé par défaut, chargement dynamique ByteBuddy sur JVM23. Open-in-view mérite une configuration explicite et un test de lecture hors session (FR-017/021), pas seulement suppression du warning. Le daemon Docker local indique un profil seccomp non standard : propriété de l’environnement de test, pas preuve du profil EC2. Les warnings n’ont pas été masqués par un changement de configuration.
- Vite prévient que l’import de validateBuildEnv sans extension ne sera pas compatible avec son futur configLoader native par défaut ; le build courant passe. Dette locale à traiter dans FR-018/020, pas BLOCKER App Store.
- Tests à vraies frontières HTTP et DB utiles ; fakes business préférables aux mocks omniprésents, déjà largement présents.
- InboxClaimLeaseIT vérifie que le claim actif est refusé ; routeur/consumer vérifient chacun leur branche. Leur composition perd pourtant le message : contre-exemple concret à « beaucoup de tests = système correct ».
- AuthRefreshIT valide le nominal, pas deux consommations simultanées ni réponse perdue.
- Des guardrails mobiles testent le texte source (`toContain` d’accessibility props) : ils ne prouvent pas VoiceOver ni l’absence de clipping/clavier.
- Java/image tests ne remplacent pas un binaire natif absent du dev build ; les incidents Apple/ImageManipulator historiques justifient une recette sur IPA exact.
- Aucun résultat JaCoCo/Istanbul consolidé par BC généré aujourd’hui ; aucun pourcentage annoncé. Ajouter temporairement/ultérieurement JaCoCo au verify et Jest --coverage/Vitest --coverage avec provider compatible ; collecter prod/test séparément et conserver exclusions explicites.
- Aucune nouvelle campagne de charge, chaos, DNS hostile réel, penetration test, Xcode/Maestro, VoiceOver, batterie/mémoire ou rollback production exécutée.

### Pyramide minimale de sortie

1. Domaine pur : invariants de profil/expérience/publication/ticket/niveaux, horloges frontières et commandes répétées.
2. Use cases fakes : no-op APPLIED, rejet durable, conflit commandId, demandeur, workflow média et états saga.
3. Infra réelle : PostgreSQL concurrent, transaction outbox, inbox redelivery/crash/delete-failure ; S3 test pour read borné/normalisation/suppression ; migrations neuve/ancienne.
4. Verticales : Google/Apple boundaries→session→profil ; expérience/media→snapshot ; ticket→entitlement ; import/article→lecture mobile ; deux comptes + UGC/suppression.
5. Appareil : auth Apple native, photo, retour background, expiration URL, clavier, petit écran, dynamic type, reduced motion/transparency, hors ligne, double tap, changement de compte.
6. Exploitation : restauration et réapplication effacements, rollback image compatible, DLQ triagée, un crash symboliqué, alertes reçues.

## 6. Bilan sécurité

### Contrôles réellement vus

JWT resource-server, issuer configuré, accès actif au compte ; secrets requis en profil prod (le fallback test de classe ne prouve pas secret faible en prod). Google échange code+verifier et valide redirect attendue ; Apple valide issuer/audience et cohérence du sujet du code échangé. Cela ne remplace pas la recette fournisseur réelle.

Studio : accès admin séparé, PKCE aléatoire Web Crypto + state, access/refresh en mémoire ; seuls state/verifier passent en sessionStorage. Aucun dangerouslySetInnerHTML trouvé dans src Studio. React échappe le texte mais URLs/assets et providers restent à valider. CSRF désactivé avec sessions stateless/bearer : pas automatiquement une faille ; une future auth cookie imposerait de reconsidérer ce choix.

S3 privé : JPEG/PNG vérifiés par signature, dimensions bornées avant décodage, reconstruction JPEG sans métadonnées fournies, lecture réseau bornée puis abort, clés construites côté backend, AES256, presigned GET. Avatar512x512, entrée8MB/40MP ; potentiel pic mémoire de plusieurs images40MP en parallèle à mesurer. Ancienne photo **demandée en suppression**, pas supprimée atomiquement au remplacement ; nettoyage planifié et délai réel restent à attester. URL privée courte n’est pas une politique d’effacement.

HMAC : signature HmacSHA256, hash SHA256 stocké, comparaison constante, liaison saga/article/révision/expiration, consommation SQL conditionnelle. Défaut temps FR-005 ; pas de preuve complète de rotation/révocation de clé. Le jeton reste dans query string Studio jusqu’à soumission (ArticleEditorPanel.tsx:91-103) ; le retirer dès capture et appliquer Referrer-Policy, redaction logs, contexte de confirmation humain. Un GET n’a pas été trouvé comme action de publication.

### Lacunes et absence de certification

Secrets : recherche historique par regex clés privées et AKIA/ASIA, limitée ; hit de code PEM à de7ece1/HttpAppleAuthService, pas secret démontré. Pas de gitleaks/trivy/semgrep installé ; aucune analyse complète des blobs supprimés, artefacts EAS, logs CloudWatch/SSM ou des anciens dépôts. Ne pas en déduire « zéro secret ». Refresh récupérable FR-024.

Pas de fuzzing systématique de tous controllers, mass assignment, injection SQL, IDOR ou URL/SSRF testé ; lectures échantillonnées montrent largement paramètres JDBC. Le RSS est une surface concrète restant ouverte. Les quotas de ReleaseRequestRateLimiter visent ticket/médias/UGC authentifiés, pas tous appels auth/lecture ; ils restent par instance.

Cadre Apple : signalement, filtrage UGC, blocage et contact doivent être utilisables et suivis, pas seulement présents dans un menu. L’application permettant des comptes doit offrir l’initiation de leur suppression et traiter leurs données associées. Références consultées : [App Review Guidelines, notamment1.2/5.1.1](https://developer.apple.com/app-store/review/guidelines/), [suppression de compte](https://developer.apple.com/support/offering-account-deletion-in-your-app/). L’analyse des obligations de conservation et des exceptions légales doit être validée par le responsable de traitement, pas improvisée en code.

## 7. Observabilité et exploitation

### État AWS effectivement consulté

- EC2 plateforme t4g.medium running ; IMDSv2 required ; stockage EBS gp3 30Go+80Go chiffré, data DeleteOnTermination=false.
- SG entrant :80/443 publics,22 limité à un /32 ; pas5432 public dans ce SG ; sortie tous protocoles. NACL/host firewall/IAM effectif complet non certifiés.
- Buckets observés : anchor-assets-prod-851725375299 et fragments-studio-staging-851725375299. Les deux bloquent les quatre modes d’accès public et sont chiffrés AES256.
- Bucket assets : versioning non activé dans la réponse, lifecycle backup PostgreSQL30jours ; aucun lifecycle média générique observé. Cela n’annule pas les jobs de cleanup applicatifs.
- Bucket Studio : versioning Enabled, **NoSuchLifecycleConfiguration** ; accumulation de versions/releases à borner après politique rollback.
- ticket-events : visibilité120s, retention4jours, longpoll20s, maxReceiveCount5, SSE-SQS activé, profondeur0 à cet instant.
- DLQ legacy :3 messages, rétention14jours. Pas de réception/lecture de contenu ; état ponctuel pas analyse de cause.
-19 alarmes lues :18 OK, une ALARM historique ; réception humaine des alertes/email non retestée.
- Health public : db/liveness/readiness/ticket UP ; deux composants DEGRADED déjà détaillés. Version binaire live vs commit local non attestée par nouvel inspect SSM aujourd’hui.

### Diagnostiquer un incident

| Flux | Signaux existants | Manque décisif |
|---|---|---|
| Commande mobile | commandId/outboxId, receipt canonique, télémétrie outbox | corrélation centralisée du device à projection, crash JS/natif symboliqué |
| SQS/projections | eventId/destination, métriques pending/failed/latence, DLQ | différencier BUSY/DONE, alerter âge réel par consumer, reprise sûre |
| Ticket | jobId/trace provider, health, lease | deadline globale processus et scenario crash ; lien opérateur lisible |
| Article | sagaId/revisionId, generation attempts, health | diagnostic HMAC sans token, expiration/reprise active, alertes des sagas bloquées |
| Médias/effacement | statuts PENDING/DELETION_PENDING, jobs | âge maximal de cleanup, objets orphelins et preuve effacement multi-stockage |
| Studio/import | journal d’opérations, command status | corrélation erreur opérateur/provider, reprise sans double effet |
| Backup | timer/script, checksum, objet S3 | RPO/RTO mesurés et restore fonctionnel post-effacement |

Socle obligatoire avant ouverture : reçu par release, dashboards âge/profondeur DLQ/outbox et latence projections, alertes owner+runbook, erreurs auth/media structurées sans PII, provenance image/contrat, drill restore et rollback, test crash mobile symboliqué. Ne pas arrêter liveness pour une DLQ ancienne ; gate promotion peut en revanche refuser une nouvelle dégradation non expliquée.

Socle ultérieur : traces échantillonnées OpenTelemetry si utilité démontrée, budget cardinalité, métriques métier taux ticketvalide/publication, SLO et coût par1000actions. Logs actuels ne doivent pas devenir entrepôt de payloads OCR ou tokens.

## 8. CI/CD et reproductibilité

Backend : workflow manuel main avec confirmation explicite, suite complète avant image, OIDC sans clé longue GitHub, SHA tag, scripts de déploiement récupérés à Git SHA, migration journalisée ; bonnes protections. Le Dockerfile reconstruit et dépend de tags d’images/actions non épinglés digest ; le tag SHA n’est pas à lui seul immutabilité du registre. Pas d’audit vulnérabilités image ni SBOM/signature vérifiés.

Studio : workflows PR, main deploy et rollback, npm ci, contrat généré contrôlé contre backend checkout **non épinglé au commit de contrat dans le workflow**, tests/build/verify, releases S3 versionnées et manifeste current/previous, invalidation CloudFront puis smoke HTML/manifest. Risque build non reproductible si backend main avance entre vérifications ; promotion doit fixer tuple de commits. Audit npm high présent en CI PR, absent du job de déploiement main.

Mobile : EAS avec profils development/preview/production, version remote/autoIncrement ; publication et env requièrent encore discipline opérateur. Un dossier ios suivi signifie que config Expo et projet natif doivent être alignés ; vider Metro n’ajoute pas de module natif manquant. Pas de build ou soumission lancé dans cet audit.

### Pipeline cible incrémentale

PR : checkout locké → lint/typecheck/format check → unit/infra/architecture/contrats → scan secrets/SCA → package/artifacts. Main : mêmes gates, image digest+SBOM+SHA, migrations dry-run/copie, release notes. Staging : accord environnement → backup vérifié → upgrade journalisé → image exacte → smoke lecture+commande/snapshot+health détaillé. Production : promotion du même artefact, pas rebuild, environnement dédié, plan de rollback et compatibilité N-1. Mobile : commit/tag+contrat/env publics+buildId, tests archive, TestFlight, validation humaine, soumission documentée.

Aucune obligation de GitOps/Kubernetes ici. Un runbook court, reproductible et vérifié vaut mieux qu’un orchestrateur supplémentaire non maîtrisé.

## 9. Performance, résilience et montée en charge

**Aucune capacité chiffrée n’est prouvée par un benchmark.** Les paliers suivants sont des hypothèses d’investigation, pas des promesses. Utilisateurs inscrits ≠ utilisateurs simultanés.

| Population | Hypothèse de sessions actives à mesurer | Risque plausible et décision |
|---|---|---|
|100 |10–30 | une panne inbox/auth suffit déjà ; corriger intégrité, vérifier mémoire normalisation et restore |
|1000 |50–200 | polling SSE50–200 SELECT/s de base + snapshots, images et quotas ; mesurer CPU/crédits DB/threadpool |
|10000 |500–2000 |500–2000 fluxSSE, broadcast global et refresh amplifiés ; filtrage/coalescence avant extension |
|100000 |5000–20000 | modèle parconnexion et hôteDB partagé probablement inadaptés ; capacity plan, pagination/CDN/DB dédiée et fanout mesurés |

Les chiffres simultanés sont des **scénarios de test proposés**, pas estimations d’usage produit. Pas de Redis/Kafka/MSK/Kubernetes recommandé par défaut. Multi-instance nécessite FR-001/008, vérification fencing workers, quotas partagés ou edge, assets sans dépendance locale implicite, connections budgetées. SSE peut rester dans le monolithe avec un mécanisme de distribution plus sobre.

Campagne proposée en staging de charge isolé, non exécutée :70%lectures café/article,15%social/expérience,10%snapshot/refresh,5%tickets ;100 puis500sessions SSE, bursts reconnexion,20uploads simultanés bornés, arrêt worker après claim, SQS indisponible5minutes. Critères proposés à convenir :0perte/double effet métier,0fuite intercompte, HTTP lecture p95<500ms/p99<1500ms horsréseauclient, acceptation commande p95<1s horsupload, projection p95<3s nominal, récupération backlog sans intervention après outage bornée, mémoire stable après30min et aucun pool durablement saturé. Mesurer délai provider séparément du HTTP202.

Tests EXPLAIN ANALYZE sur tailles réalistes, N+1 profil/photos/articles et pagination stable restent à faire. Ne pas expliquer un écran lent uniquement par « React Native » : chargeimages, ETag/URLpresignée, transitions Redux et endpoint sont à mesurer ensemble.

## 10. Préparation App Store et exploitation publique

| Élément | Verdict de preuve |
|---|---|
| Bouton Apple / entitlement natif | code présent historiquement, **IPA courant non contrôlé** |
| Google PKCE / Apple backend | tests boundary passés ; fournisseurs réels non reconnectés |
| Permissions photos/localisation/caméra | config lue, textes et refus à recetter sur archive |
| Privacy manifest | pas conclu depuis seule absence de fichier firstparty ; inspecter archive et dépendances |
| UGC signalement/blocage | capacités code présentes ; délai de traitement et responsable à attester |
| Suppression compte | initiation présente ; FR-002/003 bloquent la garantie de bout en bout |
| Liens confidentialité/conditions/support | config impose valeurs ; URLs déployées/contenu actuel non attestés ici |
| Compte/démo de revue + ticket | pas validé dans App Store Connect par cette passe |
| Accessibilité et UX | tests JS/guardrails ; pas validation VoiceOver/dynamic type réelle |
| Crash reporting / symbolication | non prouvés sur archive ; gate de recette |
| Rollback mobile | retour binaire via nouvelle distribution ; OTA limitée compatibilité native, pas substitution au build |

La checklist Apple évolue : vérifier aussi SDK/Xcode requis et règles de connexion à la date de soumission, sans conclure uniquement sur version Expo. Références : [Guidelines](https://developer.apple.com/app-store/review/guidelines/), [manifestes de confidentialité](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files). Ce tableau est une matrice de preuves, pas une déclaration que tous les points inconnus sont des violations.

## 11. Commandes exécutées, erreurs et reproductibilité

Répertoire temporaire utilisé : /tmp/fragments-audit-20260917-5eDWJg. Les logs y restent consultables localement, non commités ; les résultats essentiels sont recopiés dans ce rapport. Aucune donnée métier distante téléchargée. Copies rsync des dépôts sans .git/target/dist/nativebuild ; mobile node_modules lié pour les tests ; Studio dépendances copiées après refus Vite du lien.

Commandes principales (répéter dans une copie isolée, jamais lancer les scripts de deploy dans un audit) :

```bash
git status --short
git rev-parse HEAD
git branch --show-current
rg --files --hidden -g AGENTS.md -g '!node_modules' -g '!.git'
git ls-files -z
git log --since=2026-08-17 --pretty=format: --name-only
bash scripts/test-release.sh
# dans la copie mobile
./node_modules/.bin/jest --runInBand --watchman=false --cacheDirectory /tmp/fragments-audit-20260917-5eDWJg/jest-cache
./node_modules/.bin/tsc --noEmit
./node_modules/.bin/eslint app tests
./node_modules/.bin/tsc --noEmit --noUnusedLocals --noUnusedParameters
npm audit --json
npm outdated --json
# dans la copie Studio/fragments-studio
npm test -- --maxWorkers=1
npm run contract:check -- /Users/nicolasmaldiney/fragmentsClean/contracts/studio-api/v1/openapi.json
VITE_ADMIN_IMPORT_BEARER_TOKEN='' VITE_PROJECTION_SYNC_BEARER_TOKEN='' VITE_STUDIO_AUTH_MODE=oauth VITE_STUDIO_API_BASE_URL=https://fragments-staging.anchor-event.fr npm run build
npm run verify:dist
npm audit --json
npm outdated --json
# dans la copie backend : information de versions, aucune mise à jour
./mvnw -B org.codehaus.mojo:versions-maven-plugin:2.18.0:display-dependency-updates -DprocessDependencyManagement=false -DallowMajorUpdates=false
cmake -S /tmp/fragments-audit-20260917-5eDWJg/engine -B /tmp/fragments-audit-20260917-5eDWJg/engine-build
cmake --build /tmp/fragments-audit-20260917-5eDWJg/engine-build
ctest --test-dir /tmp/fragments-audit-20260917-5eDWJg/engine-build --output-on-failure
curl -fsS --max-time 20 https://fragments-staging.anchor-event.fr/actuator/health
```

L’argument VITE_STUDIO_API_BASE_URL de la relance n’est pas le nom utilisé par le workflow Studio, qui utilise VITE_FRAGMENTS_BACKEND_URL : ce build démontre la compilation et le garde-fou, **pas la parfaite reproduction de l’environnement CI/deploy**. Aucun token n’a été injecté au bundle par cette relance.

AWS lecture seule : sts get-caller-identity ; cloudformation describe-stacks ; sqs list-queues/get-queue-attributes (All pour ticket-events, profondeur/rétention pour shared-dlq) ; cloudwatch describe-alarms ; ec2 describe-instances/describe-volumes/describe-security-groups ; s3api list-buckets/get-public-access-block/get-bucket-encryption/get-bucket-versioning/get-bucket-lifecycle-configuration. Région eu-west-3, instance plateforme i-004d3e9cbca327d01. Pas de ReceiveMessage, redrive, write config ou SSM d’exécution distante dans cette passe de finition.

Historique limité : `git log --all --format='%h' --name-only -G 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}'`, exclusions src/test/docs pour backend et tests/docs mobile. N’affiche pas les secrets ; pas couverture complète des formats.

### Échecs non masqués

1. Jest initial : Watchman refusé dans l’environnement ; relance --watchman=false réussie.
2. Vitest initial : EPERM sur cache .vite-temp via node_modules lié ; copie des dépendances dans le temp et relance réussie.
3. Studio build initial : rejet de VITE_ADMIN_IMPORT_BEARER_TOKEN et VITE_PROJECTION_SYNC_BEARER_TOKEN présents dans configuration locale copiée ; **garde-fou réussi**, build volontairement refusé. Relance vide passée.
4. CTest : test18 real_receipt échoue (fixture_size=0) ;21/22, source non corrigée. Relance du même binaire depuis /tmp/fragments-audit-20260917-5eDWJg/engine/build/tests :22/22,56 assertions ; confirme la dépendance au CWD.
5. npm audit : nonvert (43/2 packages) ; npm outdated nonzero indique mises à jour, pas échec d’installation.
6. TypeScript noUnused additionnel :12 diagnostics ; nominal tsc passe.
7. Maven versions premier essai : EPERM écriture cache ~/.m2 ; relance avec autorisation cache/réseau réussie.
8. AWS ec2/s3 premier essai : endpoint inaccessible depuis sandbox ; relance autorisée read-only réussie. S3 Studio lifecycle : NoSuchLifecycleConfiguration, état réel « absent », pas réseau.
9. FlowAtlas context projectionUpdated inexistant ; find puis projection.updated réussi. Deux find sans résultat. Détails rapport dédié.
10. Recherche de chemins absents (application.yml, db/migration, deploy-staging.yml, quelques noms de classes/dossiers) et globs sans match : outils ont échoué, chemins réels retrouvés via rg ; pas défaut applicatif par eux-mêmes.
11. Tentative de charger les métriques JSON dans le contexte avec sortie trop longue : troncature puis parse JSON refusé ; reprise avec champs d’imports exclus. Données métriques disque non altérées.
12. Outils cloc/scc/jscpd/gitleaks/trivy/semgrep/maestro non disponibles dans PATH vérifié ; méthodes de substitution et limites précisées.

## 12. Limites et vérifications restant ouvertes

Ce rapport ne prétend pas relire sémantiquement chaque ligne des trois applications ni exécuter tous les scénarios d’incident demandés. Il couvre inventaire global, suites automatisées disponibles et approfondissement des frontières critiques. Restent :

- audit complet IAM effectif/policies resource-based/KMS/CloudFront/SSM secrets et tous comptes AWS ; pas de contenu S3 utilisateur ni messages DLQ lus ;
- schéma/data/index/statistiques live complets, EXPLAIN et charge ; backup courant réellement restauré et réouvert ;
- validation des pages légales publiées, privacy labels, rôle UE/trader, compte revue et configuration App Store Connect ;
- IPA exact, versions natives embarquées, symbolication, accessibilité, UX GPS/images/mémoire/batterie et réseau réel ;
- scan CVE Java/image/C++ exhaustif et secret scanning de tout historique/artefacts ;
- couverture instrumentée, graphe de classes/DI complet, exports dynamiques et code mort certain ;
- matrice exhaustive des process managers (génération, calendrier, enrichissement), événements tardifs/replay/multi-instance et autorisation de chaque endpoint ;
- reconstitution de tous builds depuis checkout totalement propre et sans dépendances préinstallées ;
- correspondance du binaire staging courant avec le SHA backend de cette analyse et du Studio public avec son SHA.

Ces lacunes sont des **tâches de qualification**, pas une autorisation implicite d’ouvrir au public. Aucun chiffre de disponibilité, coût AWS mensuel ou capacité100000utilisateurs n’est inventé.

## 13. Roadmap de remédiation soumise à validation

Tailles : XS≤½j, S≈½–1j, M≈1–3j, L≈3–5j, XL>5j ; indicatives, pas engagement calendaire. Efforts incluent tests ciblés mais les accès/opérations/recette peuvent décaler. Réestimer après chaque lot : prévu, réel, écart, projection restante. Aucun lot démarré.

| Ordre / PR | Niveau | Résultat attendu et périmètre | Références | Dépendances | Taille | Régression / vérification |
|---|---|---|---|---|---|---|
|1 fix(messaging): distinguish inbox busy from processed |P0| ack sûr et fencing inbox, Java/SQL/SQS |FR-001|aucune|M|élevée ; Testcontainers crash/redelivery/deletefailure|
|2 fix(privacy): reject late events after erasure |P0| barrières locales par BC et handlers|FR-002|1|L|élevée ; matrice replay tous types + comptes supprimés|
|3 feat(privacy): bounded retention and restore erasure |P0| inventaire/durées, purge technique, journal minimal, restore isolé|FR-003|2, décision rétention|XL|très élevée ; backup antérieur + S3 + contrôle aucune résurrection|
|4 fix(sync): scope private projection notifications |P1| audience serveur et guards clients, contrats|FR-004|1, migration sync|M|élevée ; tests2comptes/admin + snapshots|
|5 fix(auth): atomic refresh and token hashing |P1| atomicité, rotation, stockage sûr|FR-006/024|politique reprise session|M/L|élevée ; concurrence DB et compatibilité ancien refresh|
|6 fix(auth): revoke sessions on logout |P1| mobile/Studio/HTTP cohérents|FR-007|5|S/M|moyenne ; vertical filtresSpring + offline|
|7 fix(editorial): canonical approval timestamps |P1| token HMAC compatible précision et ancienneté|FR-005|aucune ; peut être petit lot indépendant|S|moyenne ; sonde + ITSQL + doublepublication|
|8 fix(outbox): durable retry and bounded dispatch |P1| claims, backoff, reprise, pas networktx longue|FR-008|1|M/L|élevée ;2dispatchers + outageSQS|
|9 fix(io): bound rss and ticket subprocess |P1| deadlines/bytes/networks externes|FR-012/013|providers testables|M/L|moyenne ; fakeshostiles, processusbloqué|
|10 fix(article): explicit business bootstrap |P1| supprimer bootstrap projection implicite, plan des seeds|FR-011|inventaire contenu existant|M|élevée ; fraîchebase + reconstruction + Studio|
|11 ci(release): enforce gates and qualify dependencies |P1| PR/main/gates, SCA daté et exceptions|FR-016/018|lockfiles et choix upgrades|M/L|moyenne ; gates rouges bloqueurs, archive native|
|12 ops(release): incident baseline and signed-off recipe |P1| triage historique, dashboards, rollback, pages/IPA/recette|FR-015/023|P0 +5/6/11|M/L|faible code,fortops ; preuvealerte+restore+crash|
|13 refactor(architecture): narrow ACL and format hotspots |P2| frontières vérifiables et code lisible|FR-020/021|tests P1|M|moyenne ; refactoring sans comportement séparé|
|14 test(release): portable engine and migration parity |P2| C++CI, bootstrap/upgrade/rollback, donnéesfixtures|FR-017/019|11|M|moyenne ; checkoutpropre ARM64/Java21|
|15 fix(outbox): quarantine incompatible client commands |P2| migration stockage mobile et diagnostic|FR-014|contrat command versions|S/M|moyenne ; réhydratation N-1/N|
|16 ops(messaging): catalog unknown routes and recoveries |P2| ignores explicites vs incompatibles, runbook|FR-009/015|1/8|M|moyenne ; rollingN/N-1|
|17 perf(sync): measure then reduce fanout |P3| polling/cursor/charge, sans nouveau broker par réflexe|FR-010|4|M/L|élevée ; commit inversé + charge/soak|
|18 ops(isolation): workload/backup IAM and capacity |P3*| séparer blast radius quand nécessaire, budgetsconnexions|FR-022|3,mesurescharge|L|ops élevée ; IAM/restore/load|

*Le retrait du droit destructif inutile sur les backups doit être anticipé en P1 si les mêmes credentials sont accessibles à l’application ; l’isolement complet compute peut attendre les mesures.*

Chaque PR contient : intention utilisateur/opérateur, preuve du bug avant correction, tests unit/infra/verticales, migrations/compatibilité si nécessaire, contrat et runbook, résultat exact des commandes, rollback et limites. Commits conseillés par PR : **test reproduisant → changement minimal → documentation/recette**, sans mélanger formatage et sémantique. Front et back étant des dépôts distincts, utiliser un reçu associant leurs SHAs et l’ordre de promotion ; ne pas simuler un commit atomique multi-repo.

Pas d’estimation honnête « tout en quatre jours » pour cette liste et sa qualification. Un petit lot HMAC ou logout peut être rapide ; restauration et incident distribué demandent leurs preuves. Le périmètre peut être réduit en désactivant des fonctionnalités éditoriales non essentielles, pas en ignorant intégrité/confidentialité.


## Annexe A — Répartition mesurée par module et langage

Attribution par chemin ; les tests backend *ContextTest sont regroupés avec leur BC et les chemins mobile contextWL/contextWl sont normalisés. Les tests transversaux restent dans tests/architecture/app. Une cellule tests=0 ne signifie pas absence de couverture indirecte par une verticale. Fichiers binaires inclus dans total suivi, exclus des sous-totaux texte ci-dessous.

### backend

| Module / zone | Fichiers texte | LOC | SLOC prod/outils | SLOC tests | Prod/tests |
|---|---:|---:|---:|---:|---:|
| .agents | 8 | 506 | 0 | 0 | — |
| .classpath | 1 | 57 | 0 | 0 | — |
| .dockerignore | 1 | 17 | 0 | 0 | — |
| .gitattributes | 1 | 2 | 0 | 0 | — |
| .github | 1 | 187 | 0 | 0 | — |
| .gitignore | 1 | 41 | 0 | 0 | — |
| .mvn | 1 | 19 | 0 | 0 | — |
| AGENTS.md | 1 | 343 | 0 | 0 | — |
| Dockerfile | 1 | 37 | 0 | 0 | — |
| README.md | 1 | 321 | 0 | 0 | — |
| README.short.md | 1 | 30 | 0 | 0 | — |
| contracts | 1 | 236 | 0 | 0 | — |
| docker-compose.yml | 1 | 48 | 0 | 0 | — |
| docs | 61 | 11442 | 0 | 0 | — |
| infra | 28 | 2453 | 448 | 0 | — |
| mvnw | 1 | 259 | 0 | 0 | — |
| mvnw.cmd | 1 | 149 | 0 | 0 | — |
| package-lock.json | 1 | 777 | 0 | 0 | — |
| package.json | 1 | 5 | 0 | 0 | — |
| pom.xml | 1 | 200 | 0 | 0 | — |
| runPostgresqlDockerForDev.sh | 1 | 3 | 3 | 0 | — |
| scripts | 13 | 453 | 174 | 0 | — |
| FragmentsCleanApplication.java | 1 | 15 | 11 | 0 | — |
| adminImportContext | 133 | 5259 | 2982 | 1533 | 1.95 |
| articleContext | 209 | 9934 | 5782 | 2498 | 2.31 |
| authenticationContext | 74 | 4085 | 2222 | 943 | 2.36 |
| coffeeContext | 172 | 10448 | 5399 | 2991 | 1.81 |
| editorialIntelligenceContext | 82 | 2781 | 1574 | 793 | 1.98 |
| experienceContext | 100 | 2294 | 1595 | 389 | 4.10 |
| platform | 39 | 1709 | 1380 | 137 | 10.07 |
| sharedKernel | 131 | 6961 | 3077 | 2546 | 1.21 |
| socialContext | 127 | 6509 | 3171 | 2019 | 1.57 |
| ticketContext | 90 | 5531 | 2580 | 1567 | 1.65 |
| userApplicationContext | 113 | 5327 | 2621 | 1763 | 1.49 |
| src | 25 | 6333 | 1340 | 808 | 1.66 |
| TestContainers.java | 1 | 24 | 0 | 19 | 0.00 |
| architecture | 9 | 1150 | 0 | 1007 | 0.00 |

Langages/extensions (LOC physiques, pas SLOC) : .md: 14891 ; (none): 477 ; .yml: 370 ; .properties: 203 ; .json: 4408 ; .yaml: 1488 ; .example: 195 ; .runtime-deploy: 7 ; .sh: 924 ; .service: 24 ; .timer: 21 ; .cmd: 149 ; .xml: 200 ; .java: 59880 ; .sql: 2545 ; .psql: 163.

### mobile

| Module / zone | Fichiers texte | LOC | SLOC prod/outils | SLOC tests | Prod/tests |
|---|---:|---:|---:|---:|---:|
| .agents | 6 | 262 | 0 | 0 | — |
| .gitignore | 1 | 48 | 0 | 0 | — |
| .idea | 7 | 56 | 0 | 0 | — |
| .maestro | 2 | 43 | 0 | 0 | — |
| .vscode | 1 | 7 | 0 | 0 | — |
| AGENTS.md | 1 | 244 | 0 | 0 | — |
| README.md | 1 | 85 | 0 | 0 | — |
| android | 22 | 940 | 223 | 0 | — |
| app.config.js | 1 | 130 | 123 | 0 | — |
| app | 110 | 6772 | 5512 | 0 | — |
| articles | 2 | 310 | 297 | 0 | — |
| auth | 2 | 218 | 191 | 0 | — |
| cafes | 16 | 1924 | 1704 | 0 | — |
| experiences | 2 | 227 | 213 | 0 | — |
| home | 6 | 738 | 669 | 0 | — |
| map | 36 | 3316 | 2960 | 0 | — |
| onboarding | 2 | 179 | 164 | 0 | — |
| pass | 6 | 831 | 748 | 0 | — |
| profile | 9 | 1356 | 1204 | 0 | — |
| scan | 3 | 439 | 379 | 0 | — |
| search | 1 | 307 | 279 | 0 | — |
| appWl | 24 | 2151 | 837 | 1011 | 0.83 |
| articleWl | 10 | 566 | 357 | 118 | 3.03 |
| cfPhotosWl | 13 | 263 | 104 | 109 | 0.95 |
| coffeeWl | 11 | 922 | 517 | 277 | 1.87 |
| commentWl | 22 | 2006 | 811 | 720 | 1.13 |
| entitlementWl | 10 | 413 | 151 | 180 | 0.84 |
| experienceWl | 8 | 361 | 224 | 100 | 2.24 |
| likeWl | 14 | 1154 | 469 | 420 | 1.12 |
| locationWl | 9 | 581 | 289 | 177 | 1.63 |
| openingHoursWl | 9 | 204 | 119 | 35 | 3.40 |
| outboxWl | 40 | 4529 | 1891 | 1676 | 1.13 |
| projectionSyncWl | 9 | 1201 | 404 | 644 | 0.63 |
| savedCoffeeWl | 11 | 649 | 351 | 193 | 1.82 |
| ticketWl | 13 | 1222 | 634 | 417 | 1.52 |
| userWl | 16 | 1694 | 966 | 492 | 1.96 |
| assets | 5 | 3797 | 3711 | 0 | — |
| babel.config.cjs | 1 | 16 | 14 | 0 | — |
| docs | 15 | 1530 | 0 | 0 | — |
| eas.json | 1 | 25 | 0 | 0 | — |
| eslint.config.js | 1 | 10 | 8 | 0 | — |
| ios | 16 | 923 | 50 | 0 | — |
| jest.config.cjs | 1 | 29 | 29 | 0 | — |
| package-lock.json | 1 | 18970 | 0 | 0 | — |
| package.json | 1 | 97 | 0 | 0 | — |
| plugins | 1 | 36 | 31 | 0 | — |
| scripts | 5 | 433 | 286 | 74 | 3.86 |
| tests | 48 | 2271 | 0 | 1876 | 0.00 |
| tsconfig.json | 1 | 20 | 0 | 0 | — |

Langages/extensions (LOC physiques, pas SLOC) : .md: 3239 ; (none): 413 ; .iml: 9 ; .xml: 166 ; .yaml: 43 ; .json: 19187 ; .gradle: 245 ; .pro: 14 ; .kt: 121 ; .properties: 72 ; .bat: 94 ; .js: 452 ; .tsx: 10026 ; .ts: 29316 ; .mmd: 141 ; .cjs: 101 ; .env: 11 ; .pbxproj: 436 ; .xcscheme: 88 ; .swift: 70 ; .h: 3 ; .entitlements: 10 ; .plist: 101 ; .storyboard: 46 ; .mjs: 101.

### studio

| Module / zone | Fichiers texte | LOC | SLOC prod/outils | SLOC tests | Prod/tests |
|---|---:|---:|---:|---:|---:|
| .agents | 13 | 604 | 0 | 0 | — |
| .github | 3 | 190 | 0 | 0 | — |
| .gitignore | 1 | 3 | 0 | 0 | — |
| AGENTS.md | 1 | 182 | 0 | 0 | — |
| docs | 3 | 154 | 0 | 0 | — |
| fragments-studio | 47 | 6263 | 70 | 2807 | 0.02 |
| adminAccessContext | 3 | 79 | 76 | 0 | — |
| adminImportContext | 14 | 1437 | 1314 | 0 | — |
| app | 1 | 35 | 32 | 0 | — |
| articleStudioContext | 12 | 1876 | 1769 | 0 | — |
| authContext | 5 | 183 | 159 | 0 | — |
| config | 1 | 49 | 41 | 0 | — |
| editorialCalendarContext | 4 | 125 | 116 | 0 | — |
| editorialSourcesContext | 7 | 152 | 141 | 0 | — |
| existingCoffeesContext | 11 | 1661 | 1512 | 0 | — |
| main.tsx | 1 | 34 | 32 | 0 | — |
| moderationContext | 6 | 126 | 123 | 0 | — |
| projectionSyncContext | 7 | 364 | 320 | 0 | — |
| shared | 3 | 1141 | 1052 | 0 | — |
| studioOperationContext | 6 | 62 | 58 | 0 | — |
| styles.css | 1 | 943 | 0 | 0 | — |
| ticketAdminContext | 3 | 80 | 76 | 0 | — |
| topicCandidatesContext | 4 | 42 | 41 | 0 | — |
| vite-env.d.ts | 1 | 19 | 16 | 0 | — |

Langages/extensions (LOC physiques, pas SLOC) : .md: 1094 ; .yml: 190 ; (none): 10 ; .example: 13 ; .html: 12 ; .json: 2799 ; .mjs: 62 ; .tsx: 2601 ; .ts: 8080 ; .css: 943.

### engine

| Module / zone | Fichiers texte | LOC | SLOC prod/outils | SLOC tests | Prod/tests |
|---|---:|---:|---:|---:|---:|
| .gitignore | 1 | 34 | 0 | 0 | — |
| CMakeLists.txt | 1 | 39 | 0 | 0 | — |
| README.md | 1 | 157 | 0 | 0 | — |
| include | 9 | 203 | 123 | 0 | — |
| scripts | 3 | 389 | 290 | 0 | — |
| src | 9 | 1005 | 788 | 0 | — |
| tests | 8 | 340 | 0 | 188 | 0.00 |

Langages/extensions (LOC physiques, pas SLOC) : (none): 34 ; .txt: 132 ; .md: 157 ; .hpp: 203 ; .sh: 389 ; .cpp: 1252.

## Annexe B — Dépendances obsolètes, instantané du 17/09

« Latest » et « wanted » proviennent du registre interrogé par npm, non d’une recommandation d’upgrade. Écosystème Expo à mettre à jour de façon cohérente ; une ligne outdated n’est pas une CVE. Lockfiles conservés inchangés.

### Mobile

| Package | Installé | Wanted | Latest |
|---|---|---|---|
| @babel/core | 7.28.5 | 7.29.7 | 7.29.7 |
| @expo/vector-icons | 15.0.3 | 15.1.1 | 15.1.1 |
| @gorhom/bottom-sheet | 5.2.6 | 5.2.14 | 5.2.14 |
| @react-native-community/netinfo | 11.4.1 | 11.5.2 | 12.0.1 |
| @react-navigation/bottom-tabs | 7.5.0 | 7.19.1 | 7.19.1 |
| @react-navigation/elements | 2.7.0 | 2.9.42 | 2.9.42 |
| @react-navigation/native | 7.1.18 | 7.4.1 | 7.4.1 |
| @reduxjs/toolkit | 2.9.2 | 2.12.0 | 2.12.0 |
| @types/jest | 29.5.14 | 29.5.14 | 30.0.0 |
| @types/react | 19.1.17 | 19.1.17 | 19.3.0 |
| babel-jest | 30.2.0 | 30.5.1 | 30.5.1 |
| babel-plugin-module-resolver | 5.0.2 | 5.0.3 | 5.0.3 |
| babel-preset-expo | 54.0.12 | 54.0.12 | 57.0.12 |
| eslint | 9.38.0 | 9.39.5 | 10.10.0 |
| eslint-config-expo | 10.0.0 | 10.0.0 | 57.0.2 |
| expo | 54.0.37 | 54.0.37 | 57.0.23 |
| expo-apple-authentication | 8.0.8 | 8.0.8 | 57.0.2 |
| expo-auth-session | 7.0.11 | 7.0.11 | 57.0.12 |
| expo-blur | 15.0.8 | 15.0.8 | 57.0.3 |
| expo-constants | 18.0.14 | 18.0.14 | 57.0.18 |
| expo-crypto | 15.0.9 | 15.0.9 | 57.0.3 |
| expo-dev-client | 6.0.21 | 6.0.21 | 57.0.19 |
| expo-file-system | 19.0.24 | 19.0.24 | 57.0.7 |
| expo-font | 14.0.12 | 14.0.12 | 57.0.4 |
| expo-haptics | 15.0.8 | 15.0.8 | 57.0.3 |
| expo-image | 3.0.11 | 3.0.11 | 57.0.5 |
| expo-image-manipulator | 14.0.8 | 14.0.8 | 57.0.18 |
| expo-image-picker | 17.0.11 | 17.0.11 | 57.0.18 |
| expo-linking | 8.0.12 | 8.0.12 | 57.0.10 |
| expo-location | 19.0.8 | 19.0.8 | 57.0.18 |
| expo-router | 6.0.24 | 6.0.24 | 57.0.21 |
| expo-secure-store | 15.0.8 | 15.0.8 | 57.0.4 |
| expo-splash-screen | 31.0.13 | 31.0.13 | 57.0.9 |
| expo-status-bar | 3.0.9 | 3.0.9 | 57.0.1 |
| expo-symbols | 1.0.8 | 1.0.8 | 57.0.3 |
| expo-system-ui | 6.0.9 | 6.0.9 | 57.0.4 |
| expo-web-browser | 15.0.11 | 15.0.11 | 57.0.3 |
| jest | 29.7.0 | 29.7.0 | 30.5.1 |
| jest-expo | 54.0.18 | 54.0.18 | 57.0.5 |
| metro-config | 0.83.3 | 0.83.8 | 0.87.1 |
| react | 19.1.0 | 19.1.0 | 19.3.0 |
| react-dom | 19.1.0 | 19.1.0 | 19.3.0 |
| react-native | 0.81.5 | 0.81.5 | 0.87.1 |
| react-native-clusterer | 5.0.1 | 5.0.2 | 5.0.2 |
| react-native-gesture-handler | 2.28.0 | 2.28.0 | 3.3.0 |
| react-native-get-random-values | 1.11.0 | 1.11.0 | 2.0.0 |
| react-native-maps | 1.20.1 | 1.20.1 | 1.29.2 |
| react-native-mmkv-storage | 12.0.0 | 12.0.1 | 12.0.1 |
| react-native-nitro-modules | 0.30.2 | 0.30.2 | 0.37.1 |
| react-native-reanimated | 4.1.3 | 4.1.7 | 4.6.0 |
| react-native-safe-area-context | 5.6.1 | 5.6.2 | 5.10.0 |
| react-native-screens | 4.16.0 | 4.16.0 | 4.28.0 |
| react-native-svg | 15.12.1 | 15.12.1 | 15.15.5 |
| react-native-webview | 13.15.0 | 13.15.0 | 14.0.1 |
| react-native-worklets | 0.5.1 | 0.5.1 | 0.12.2 |
| react-redux | 9.2.0 | 9.3.0 | 9.3.0 |
| typescript | 5.9.3 | 5.9.3 | 7.0.2 |
| uuid | 13.0.0 | 13.0.2 | 14.0.2 |

### Studio

| Package | Installé | Wanted | Latest |
|---|---|---|---|
| @testing-library/jest-dom | 6.9.1 | 6.9.1 | 7.0.1 |
| @testing-library/react | 16.3.2 | 16.3.3 | 16.3.3 |
| @testing-library/user-event | 14.6.1 | 14.6.7 | 14.6.7 |
| @types/react | 18.3.31 | 18.3.31 | 19.3.0 |
| @types/react-dom | 18.3.7 | 18.3.7 | 19.3.0 |
| jsdom | 24.1.3 | 24.1.3 | 29.1.1 |
| react | 18.3.1 | 18.3.1 | 19.3.0 |
| react-dom | 18.3.1 | 18.3.1 | 19.3.0 |
| typescript | 5.9.3 | 5.9.3 | 7.0.2 |
| vite | 8.2.2 | 8.3.0 | 8.3.0 |
| vitest | 5.0.0 | 5.0.1 | 5.0.1 |

### Backend Maven : mises à jour mineures/patch proposées par le plugin

La restriction allowMajorUpdates=false était active. Le BOM Spring reste l’unité d’upgrade ; ne pas épingler arbitrairement des composants Spring incompatibles. Pas de preuve CVE du runtime tirée de cette liste.

```text
[INFO] The following dependencies in Dependencies have newer versions:
[INFO]   com.fasterxml.jackson.datatype:jackson-datatype-jsr310 ...
[INFO]                                                         2.17.1 -> 2.22.2
[INFO]   com.openai:openai-java ............................... 4.0.0 -> 4.63.3
[INFO]   org.postgresql:postgresql .......................... 42.7.3 -> 42.7.13
[INFO]   org.projectlombok:lombok .......................... 1.18.32 -> 1.18.48
[INFO]   org.springframework.boot:spring-boot-starter-actuator ...
[INFO]                                                          3.3.0 -> 3.5.16
[INFO]   org.springframework.boot:spring-boot-starter-data-jpa ...
[INFO]                                                          3.3.0 -> 3.5.16
[INFO]   org.springframework.boot:spring-boot-starter-oauth2-resource-server ...
[INFO]                                                          3.3.0 -> 3.5.16
[INFO]   org.springframework.boot:spring-boot-starter-security ...
[INFO]                                                          3.3.0 -> 3.5.16
[INFO]   org.springframework.boot:spring-boot-starter-test .... 3.3.0 -> 3.5.16
[INFO]   org.springframework.boot:spring-boot-starter-web ..... 3.3.0 -> 3.5.16
[INFO]   org.springframework.security:spring-security-oauth2-jose ...
[INFO]                                                          6.3.0 -> 6.5.11
[INFO]   org.springframework.security:spring-security-test .... 6.3.0 -> 6.5.11
[INFO]   org.testcontainers:localstack ....................... 1.20.4 -> 1.21.4
[INFO]   org.testcontainers:postgresql ....................... 1.20.4 -> 1.21.4
[INFO]   software.amazon.awssdk:s3 ......................... 2.31.78 -> 2.54.20
[INFO]   software.amazon.awssdk:ses ........................ 2.31.78 -> 2.54.20
[INFO]   software.amazon.awssdk:sqs ........................ 2.31.78 -> 2.54.20
[INFO]
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-maven-plugin/maven-metadata.xml
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-maven-plugin/maven-metadata.xml (10 kB at 198 kB/s)
[INFO] The following dependencies in pluginManagement of plugins have newer versions:
```

### Avis npm : qualification requise

Les 43/2 comptent des packages affectés, pas des vulnérabilités exploitables indépendantes. Le Studio remonte notamment la chaîne js-yaml ([avis GHSA](https://github.com/advisories/GHSA-2883-xcg3-v3hh)), principalement outillage ici. Mobile présente notamment image-size, xmldom, ws, uuid et des dépendances Metro/Expo ; classifier parsers build vs code embarqué. Les appels nanoid observés ne passent pas une taille négative/hostile : ne pas déclarer une exploitation parce que le scanner signale ce package. Aucune bibliothèque déclarée « abandonnée » sans vérification de maintenance primaire ; cette enquête de maintenance reste incomplète.

## Annexe C — Inventaire lexical des entrées backend

Chaque ligne conserve le fichier et l’annotation inspectable ; les RequestMapping de classe doivent être combinés avec ceux des méthodes. Routes construites implicitement par Spring/Security/Actuator et handlers événements ne sont pas représentés exhaustivement par ce seul tableau.

```text
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminAccessController.java:26 @RequestMapping("/api/admin/access/users")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminAccessController.java:43 @GetMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminAccessController.java:48 @PostMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminAccessController.java:57 @DeleteMapping("/{userId}")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminCoffeeOperationJournalController.java:22 @GetMapping("/api/admin/coffees/{coffeeId}/operations")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminCommandStatusController.java:20 @RequestMapping("/api/admin/commands")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminCommandStatusController.java:33 @GetMapping("/{commandId}")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialCalendarController.java:14 @RequestMapping("/api/admin/editorial/calendar")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialCalendarController.java:22 @GetMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialCalendarController.java:27 @PostMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialCalendarController.java:33 @PostMapping("/{scheduleId}/cancel")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:13 @RequestMapping("/api/admin/editorial/sources")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:17 @GetMapping public SourceListResponse list(){return new SourceListResponse(catalog.listSources());}
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:18 @PostMapping public ResponseEntity<SourceAcceptedResponse> register(@RequestBody SourceRequest request){UUID id=manage.register(request.toModel());return ResponseEntity.accepted().body(new SourceAcceptedResponse(id));}
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:19 @PutMapping("/{sourceId}") public ResponseEntity<Void> revise(@PathVariable UUID sourceId,@RequestBody SourceRequest request){manage.revise(sourceId,request.toModel());return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:20 @PostMapping("/analysis") public ResponseEntity<Void> analyzePendingSignals(){analysis.execute();return ResponseEntity.noContent().build();}
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminEditorialSourcesController.java:21 @GetMapping("/{sourceId}/signals") public SignalListResponse signals(@PathVariable UUID sourceId,@RequestParam(defaultValue="50") int limit){return new SignalListResponse(catalog.listSignals(sourceId,limit));}
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminImportPlacesController.java:28 @RequestMapping("/api/admin/import/places")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminImportPlacesController.java:56 @GetMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminImportPlacesController.java:63 @GetMapping("/{googlePlaceId}/preview")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminImportPlacesController.java:68 @PostMapping("/{googlePlaceId}/import")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticleGenerationController.java:27 @RequestMapping("/api/admin/studio/article-generations")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticleGenerationController.java:45 @PostMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticleGenerationController.java:55 @GetMapping("/{sagaId}")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticleGenerationController.java:60 @PutMapping("/{sagaId}/revision")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticleGenerationController.java:68 @PostMapping("/approvals/{token}/publish")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:19 @RequestMapping("/api/admin/studio/articles")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:44 @GetMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:49 @PutMapping("/{articleId}")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:62 @PostMapping
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:70 @DeleteMapping("/{articleId}")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:78 @PostMapping("/{articleId}/withdraw")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:86 @PutMapping("/{articleId}/featured")
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminStudioArticlesController.java:97 @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
src/main/java/com/nm/fragmentsclean/adminImportContext/adapters/primary/rest/AdminTopicCandidatesController.java:3 @RestController @RequestMapping("/api/admin/editorial/topic-candidates") public final class AdminTopicCandidatesController {private final DecideStudioTopicCandidate decisions;private final TopicCandidateStudioCatalog candidates;private final StartArticleGenerationFromTopicCandidate authoring;public AdminTopicCandidatesController(DecideStudioTopicCandidate decisions,TopicCandidateStudioCatalog candidates,StartArticleGenerationFromTopicCandidate authoring){this.decisions=decisions;this.candidates=candidates;this.authoring=authoring;}@GetMapping public java.util.List<TopicCandidateStudioCatalog.Item> list(){return candidates.list();}@PostMapping("/{id}/{decision:retain|defer|ignore}") public ResponseEntity<Void> decide(@PathVariable UUID id,@PathVariable String decision){decisions.execute(id,decision.toUpperCase());return ResponseEntity.accepted().build();}@PostMapping("/{id}/start-authoring") public ResponseEntity<com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationResult> start(@PathVariable UUID id,@RequestBody AuthoringRequest r,Authentication authentication){UUID operatorId=UUID.fromString(authentication.getName());return ResponseEntity.accepted().body(authoring.execute(id,r.locale(),operatorId,authentication.getName()));}public record AuthoringRequest(String locale){}}
src/main/java/com/nm/fragmentsclean/articleContext/read/adapters/primary/springboot/controllers/ArticleImageAssetsController.java:23 @GetMapping("/api/articles/image-assets/{fileName:.+}")
src/main/java/com/nm/fragmentsclean/articleContext/read/adapters/primary/springboot/controllers/ReadArticleController.java:12 @RequestMapping("/api/articles")
src/main/java/com/nm/fragmentsclean/articleContext/read/adapters/primary/springboot/controllers/ReadArticleController.java:21 @GetMapping("/{slug}")
src/main/java/com/nm/fragmentsclean/articleContext/read/adapters/primary/springboot/controllers/ReadArticleController.java:35 @GetMapping
src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/primary/springboot/controllers/WriteArticleController.java:14 @RequestMapping("/api/articles")
src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/primary/springboot/controllers/WriteArticleController.java:51 @PostMapping
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:29 @RequestMapping("/auth")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:45 @GetMapping("/google/studio/config")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:57 @PostMapping("/google/studio")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:79 @PostMapping("/google/mobile")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:98 @PostMapping("/apple/mobile")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:115 @PostMapping("/refresh")
src/main/java/com/nm/fragmentsclean/authenticationContext/write/adapters/primary/springboot/controllers/AuthWriteController.java:126 @PostMapping("/logout")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:74 @GetMapping("/api/admin/coffees")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:99 @DeleteMapping("/api/admin/coffees/{coffeeId}")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:110 @DeleteMapping("/api/admin/coffees/{coffeeId}/permanent")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:123 @PostMapping("/api/admin/coffees/{coffeeId}/publish")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:131 @PostMapping("/api/admin/coffees/{coffeeId}/unpublish")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:139 @PutMapping("/api/admin/coffees/{coffeeId}/details")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:151 @PutMapping("/api/admin/coffees/{coffeeId}/opening-hours")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:164 @PostMapping(value = "/api/admin/coffees/{coffeeId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:180 @DeleteMapping("/api/admin/coffees/{coffeeId}/photos/{photoId}")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/admin/AdminCoffeesReadController.java:193 @PutMapping("/api/admin/coffees/{coffeeId}/photos/order")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/controllers/CoffeeOpeningHoursReadController.java:19 @GetMapping("/api/coffees/opening-hours")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/controllers/CoffeePhotoAssetsController.java:22 @GetMapping("/api/coffees/photo-assets/{fileName:.+}")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/controllers/CoffeePhotosReadController.java:22 @GetMapping("/api/coffees/photos")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/controllers/CoffeeReadController.java:25 @GetMapping("/api/coffees")
src/main/java/com/nm/fragmentsclean/coffeeContext/read/adapters/primary/springboot/controllers/CoffeeReadController.java:40 @GetMapping("/api/coffees/{coffeeId}")
src/main/java/com/nm/fragmentsclean/coffeeContext/write/adapters/primary/springboot/controllers/WriteCoffeeController.java:13 @RequestMapping("/api/coffees")
src/main/java/com/nm/fragmentsclean/coffeeContext/write/adapters/primary/springboot/controllers/WriteCoffeeController.java:39 @PostMapping
src/main/java/com/nm/fragmentsclean/experienceContext/read/adapters/primary/springboot/controllers/ReadExperienceController.java:3 @RestController public final class ReadExperienceController{private final ListCoffeeExperiencesQueryHandler coffee;private final ListMyExperiencesQueryHandler mine;private final ListExperienceModerationReportsQueryHandler moderation;public ReadExperienceController(ListCoffeeExperiencesQueryHandler coffee,ListMyExperiencesQueryHandler mine,ListExperienceModerationReportsQueryHandler moderation){this.coffee=coffee;this.mine=mine;this.moderation=moderation;}@GetMapping("/api/coffees/{coffeeId}/experiences")ExperiencePage coffee(@PathVariable UUID coffeeId,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int limit,@AuthenticationPrincipal Jwt jwt){return coffee.handle(new ListCoffeeExperiencesQuery(user(jwt),coffeeId,cursor,limit));}@GetMapping("/api/users/me/experiences")ExperiencePage mine(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int limit,@AuthenticationPrincipal Jwt jwt){return mine.handle(new ListMyExperiencesQuery(user(jwt),cursor,limit));}@GetMapping("/api/admin/experience-moderation/reports")List<ExperienceModerationReportView> moderation(@RequestParam(defaultValue="OPEN")String status,@RequestParam(defaultValue="50")int limit){return moderation.handle(status,limit);}@ExceptionHandler(IllegalArgumentException.class)@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)Map<String,String> invalidQuery(IllegalArgumentException error){return Map.of("error","INVALID_QUERY","reason",error.getMessage());}private static UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceController.java:11 @PostMapping("/api/experiences")ResponseEntity<Void> create(@RequestBody CreateExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new CreateExperienceCommand(body.commandId(),body.experienceId(),user(jwt),body.coffeeId(),body.message(),body.publicationStatus(),body.at()));return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceController.java:12 @PatchMapping("/api/experiences/{id}")ResponseEntity<Void> update(@PathVariable UUID id,@RequestBody UpdateExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new UpdateExperienceCommand(body.commandId(),id,user(jwt),body.message(),body.at()));return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceController.java:13 @PostMapping("/api/experiences/{id}/publish")ResponseEntity<Void> publish(@PathVariable UUID id,@RequestBody ExperienceCommandRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new PublishExperienceCommand(body.commandId(),id,user(jwt),body.at()));return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceController.java:14 @DeleteMapping("/api/experiences/{id}")ResponseEntity<Void> delete(@PathVariable UUID id,@RequestBody ExperienceCommandRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new DeleteExperienceCommand(body.commandId(),id,user(jwt),body.at()));return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceController.java:15 @PostMapping("/api/experiences/{id}/reports")ResponseEntity<Void> report(@PathVariable UUID id,@RequestBody ReportExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new ReportExperienceCommand(body.commandId(),body.reportId(),id,user(jwt),body.reason(),body.details(),body.at()));return ResponseEntity.accepted().build();}
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceMediaController.java:16 @RequestMapping("/api/experiences/{experienceId}/media")
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceMediaController.java:27 @PostMapping("/upload-intents")
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceMediaController.java:34 @PostMapping("/{mediaId}/confirm")
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceMediaController.java:46 @DeleteMapping("/{mediaId}")
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/controllers/WriteExperienceModerationController.java:3 @RestController public final class WriteExperienceModerationController{private final CommandBus commands;public WriteExperienceModerationController(CommandBus commands){this.commands=commands;}@PostMapping("/api/admin/experience-moderation/reports/{reportId}/decision")ResponseEntity<Void> decide(@PathVariable UUID reportId,@RequestParam UUID experienceId,@RequestBody ModerateExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new ModerateExperienceCommand(body.commandId(),body.actionId(),reportId,experienceId,UUID.fromString(jwt.getSubject()),body.decision(),body.reason(),body.at()));return ResponseEntity.accepted().build();}}
src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/controllers/CommandStatusController.java:17 @RequestMapping("/commands")
src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/controllers/CommandStatusController.java:26 @GetMapping("/{commandId}")
src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/projectionSync/ProjectionSyncController.java:19 @GetMapping(path = { "/api/sync/events", "/api/admin/sync/events" }, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadCommentsController.java:13 @RequestMapping("/api/social/comments")
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadCommentsController.java:22 @GetMapping
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadLikeController.java:13 @RequestMapping("/api/social/targets")
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadLikeController.java:22 @GetMapping("/{targetId}/likes")
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadModerationController.java:20 @GetMapping("/api/social/blocks")
src/main/java/com/nm/fragmentsclean/socialContext/read/adapters/primary/springboot/controllers/ReadModerationController.java:24 @GetMapping("/api/admin/moderation/reports")
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteCommentController.java:16 @RequestMapping("/api/social/comments")
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteCommentController.java:25 @PostMapping
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteCommentController.java:47 @PutMapping
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteCommentController.java:65 @DeleteMapping
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteLikeController.java:14 @RequestMapping("/api/social/likes")
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteLikeController.java:22 @PostMapping
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteModerationController.java:19 @PostMapping("/api/social/comments/{commentId}/reports")
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteModerationController.java:27 @PostMapping("/api/social/blocks")
src/main/java/com/nm/fragmentsclean/socialContext/write/adapters/primary/springboot/controllers/WriteModerationController.java:34 @PostMapping("/api/admin/moderation/reports/{reportId}/decision")
src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/controllers/ReadTicketController.java:18 @RequestMapping("/api/tickets")
src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/controllers/ReadTicketController.java:27 @GetMapping("/{ticketId}/status")
src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/controllers/ReadTicketHistoryController.java:19 @RequestMapping("/api/users/me/tickets")
src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/controllers/ReadTicketHistoryController.java:27 @GetMapping
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:16 @RequestMapping("/api/admin/tickets")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:29 @GetMapping public List<TicketStatusView> list() { return reads.list(); }
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:30 @GetMapping("/{ticketId}") public ResponseEntity<TicketStatusView> get(@PathVariable UUID ticketId) {
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:34 @PutMapping("/{ticketId}")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:43 @DeleteMapping("/{ticketId}")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java:51 @PostMapping("/{ticketId}/verify")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/WriteTicketController.java:20 @RequestMapping("/api/tickets")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/WriteTicketController.java:38 @PostMapping("/verify")
src/main/java/com/nm/fragmentsclean/userApplicationContext/pass/adapters/primary/ReadPassController.java:19 @RequestMapping("/api/users/me/entitlements")
src/main/java/com/nm/fragmentsclean/userApplicationContext/pass/adapters/primary/ReadPassController.java:25 @GetMapping
src/main/java/com/nm/fragmentsclean/userApplicationContext/read/adapters/primary/springboot/controllers/ReadSavedCoffeeController.java:16 @RequestMapping("/api/users/me/saved-coffees")
src/main/java/com/nm/fragmentsclean/userApplicationContext/read/adapters/primary/springboot/controllers/ReadSavedCoffeeController.java:24 @GetMapping
src/main/java/com/nm/fragmentsclean/userApplicationContext/read/adapters/primary/springboot/controllers/ReadUserProfileController.java:15 @RequestMapping("/api/users/me")
src/main/java/com/nm/fragmentsclean/userApplicationContext/read/adapters/primary/springboot/controllers/ReadUserProfileController.java:23 @GetMapping
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteAccountController.java:14 @RequestMapping("/api/users/me")
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteAccountController.java:22 @DeleteMapping
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteAvatarController.java:5 @RestController @RequestMapping("/api/users/me/avatar")
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteAvatarController.java:6 public final class WriteAvatarController{private final IssueAvatarUploadIntent intents;private final ConfirmAvatarUpload confirmations;private final CommandBus commands;public WriteAvatarController(IssueAvatarUploadIntent intents,ConfirmAvatarUpload confirmations,CommandBus commands){this.intents=intents;this.confirmations=confirmations;this.commands=commands;}@PostMapping("/upload-intents")ResponseEntity<AvatarUploadIntent> intent(@RequestBody AvatarUploadIntentRequest body,@AuthenticationPrincipal Jwt jwt){if(body.mediaId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"mediaId is required");return ResponseEntity.status(201).body(intents.issue(body.mediaId(),user(jwt),body.contentType(),body.size()));}@PostMapping("/{mediaId}/confirm")ResponseEntity<Void> confirm(@PathVariable UUID mediaId,@RequestBody AvatarCommandRequest body,@AuthenticationPrincipal Jwt jwt){if(body.commandId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"commandId is required");try{confirmations.confirm(body.commandId(),mediaId,user(jwt),body.at());}catch(ImageUploadRejectedException rejected){throw new BusinessCommandRejectedException(rejected.code(),rejected.getMessage());}return ResponseEntity.accepted().build();}@DeleteMapping ResponseEntity<Void> remove(@RequestBody AvatarCommandRequest body,@AuthenticationPrincipal Jwt jwt){if(body.commandId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"commandId is required");commands.dispatch(new RemoveAvatarCommand(body.commandId(),user(jwt),body.at()));return ResponseEntity.accepted().build();}private static UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}}
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteSavedCoffeeController.java:17 @RequestMapping("/api/users/me/saved-coffees")
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteSavedCoffeeController.java:25 @PostMapping
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteUserProfileController.java:17 @RequestMapping("/api/users/me/profile")
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/controllers/WriteUserProfileController.java:25 @PatchMapping
```

Tâches planifiées :

```text
src/main/java/com/nm/fragmentsclean/articleContext/write/adapters/primary/springboot/scheduling/ScheduledArticleGenerationRequester.java:33 @Scheduled(
src/main/java/com/nm/fragmentsclean/editorialIntelligenceContext/write/adapters/primary/springboot/scheduling/AnalyzeNewEditorialSignalsJob.java:13 @Scheduled(
src/main/java/com/nm/fragmentsclean/editorialIntelligenceContext/write/adapters/primary/springboot/scheduling/ConsultDueEditorialSourcesJob.java:14 @Scheduled(fixedDelayString = "${fragments.editorial.discovery.schedule.delay-ms:900000}")
src/main/java/com/nm/fragmentsclean/editorialIntelligenceContext/write/adapters/primary/springboot/scheduling/EditorialPublicationScheduleJob.java:27 @Scheduled(fixedDelayString = "${fragments.editorial.planning.schedule.delay-ms:60000}")
src/main/java/com/nm/fragmentsclean/experienceContext/write/adapters/primary/springboot/scheduling/ExperienceMediaCleanupJob.java:3 @Component @ConditionalOnProperty(prefix="fragments.media.cleanup",name="enabled",havingValue="true") public final class ExperienceMediaCleanupJob{private final CleanExperienceMediaObjects cleanup;public ExperienceMediaCleanupJob(CleanExperienceMediaObjects cleanup){this.cleanup=cleanup;}@Scheduled(fixedDelayString="${fragments.media.cleanup.delay-ms:600000}")public void clean(){cleanup.run(100);}}
src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/primary/springboot/eventDispatcher/ScheduledOutboxEventDispatcher.java:21 @Scheduled(fixedDelayString = "${app.outbox.dispatcher.delay-ms:500}")
src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/scheduling/ScheduledTicketVerificationWorker.java:41 @Scheduled(fixedDelayString = "${ticketverify.worker.poll-ms:1000}")
src/main/java/com/nm/fragmentsclean/userApplicationContext/write/adapters/primary/springboot/scheduling/AvatarMediaCleanupJob.java:3 @Component @ConditionalOnProperty(prefix="fragments.media.cleanup",name="enabled",havingValue="true") public final class AvatarMediaCleanupJob{private final CleanAvatarMediaObjects cleanup;public AvatarMediaCleanupJob(CleanAvatarMediaObjects cleanup){this.cleanup=cleanup;}@Scheduled(fixedDelayString="${fragments.media.cleanup.delay-ms:600000}")public void clean(){cleanup.run(100);}}
```

## Annexe D — Reproduction de la sonde HMAC et des métriques

Sonde non committée, fake port sans secret réel. À compiler contre les classes compilées du backend puis lancer ; les types de la sonde ne déclenchent aucun accès distant.

```java
import java.time.*;
import java.util.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.*;
public class ApprovalPrecisionProbe {
 static class Repo implements ArticleReviewApprovalRepository {
  ArticleReviewApproval value;
  public Optional<ArticleReviewApproval> findBySagaAndRevision(UUID s, UUID r){return Optional.ofNullable(value);}
  public Optional<ArticleReviewApproval> findByTokenHash(String h){return Optional.ofNullable(value).filter(v->v.tokenHash().equals(h));}
  public void save(ArticleReviewApproval a){value=a;}
  public boolean consume(UUID id,Instant now){return false;}
 }
 public static void main(String[] args){
  for(String timestamp:List.of("2026-09-17T08:00:00Z","2026-09-17T08:00:00.123456Z")){
   var service=new ArticleReviewApprovalTokenService(new Repo(),new ArticleReviewApprovalProperties("audit-only-non-production-test-secret",Duration.ofHours(1)));
   var now=Instant.parse(timestamp);var token=service.issue(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),now);
   try{service.validate(token,now.plusSeconds(1));System.out.println(timestamp+" VALID");}
   catch(IllegalArgumentException e){System.out.println(timestamp+" REJECTED: "+e.getMessage());}
  }
 }
}
```

```bash
javac -cp target/classes -d /tmp /tmp/ApprovalPrecisionProbe.java
java -cp target/classes:/tmp ApprovalPrecisionProbe
```

Résultat : instant entier VALID ; instant .123456Z rejeté. L’exécution de session a utilisé le dossier temporaire d’audit plutôt que /tmp directement.

### Mesure globale, AST TypeScript et hotspots

Script temporaire exécuté : metrics.cjs. Chemins fixes de la session à adapter pour une autre machine. Ces scripts sont conservés ici pour reproductibilité, pas ajoutés comme outils applicatifs. Leurs biais de mesure sont explicités au §2.

```javascript
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto');
const ts=require('/Users/nicolasmaldiney/fragmentsCleanFront/node_modules/typescript');
const roots={backend:'/Users/nicolasmaldiney/fragmentsClean',mobile:'/Users/nicolasmaldiney/fragmentsCleanFront',studio:'/Users/nicolasmaldiney/fragments-admin',engine:'/Users/nicolasmaldiney/ticketverify-engine',flowatlas:'/Users/nicolasmaldiney/FlowAtlas'};
const result={date:new Date().toISOString(),method:'git tracked files; approximate nonblank non-comment physical lines; TS AST function spans/decision count; no generated/vendor coverage inferred',repos:{}};
for(const [name,root] of Object.entries(roots)){
 const files=cp.execFileSync('git',['ls-files','-z'],{cwd:root,encoding:'utf8'}).split('\0').filter(Boolean), stats=[],mods={},langs={},functions=[],imports=[],dups=new Map();let totalLines=0;
 for(const f of files){let raw;try{raw=fs.readFileSync(path.join(root,f))}catch{continue}if(raw.includes(0))continue;const ext=path.extname(f),text=raw.toString(),lines=text.split(/\r?\n/);if(lines.at(-1)==='')lines.pop();totalLines+=lines.length;const code=/\.(java|tsx?|jsx?|mjs|cjs|cpp|hpp|h|c|sh|sql|swift|kt|gradle)$/.test(f),test=/(^|\/)(tests?|__tests__)(\/|\.)|\.(test|spec)\./.test(f);const module=name==='backend'?(f.match(/src\/(?:main|test)\/java\/com\/nm\/fragmentsclean\/([^/]+)/)?.[1]||f.split('/')[0]):name==='mobile'?(f.match(/contextWL\/([^/]+)/)?.[1]||f.match(/features\/([^/]+)/)?.[1]||f.split('/')[0]):name==='studio'?(f.match(/src\/([^/]+)/)?.[1]||f.split('/')[0]):f.split('/')[0];
 let inComment=false,sloc=0;for(let l of lines){l=l.trim();if(inComment){if(l.includes('*/')){inComment=false;l=l.slice(l.indexOf('*/')+2).trim()}else continue}if(l.startsWith('/*')){if(!l.includes('*/'))inComment=true;continue}if(l&&!/^(\/\/|\*|#|--)/.test(l))sloc++}
 const row={file:f,lines:lines.length,sloc:code?sloc:0,test,module};stats.push(row);langs[ext||'(none)']=(langs[ext||'(none)']||0)+lines.length;mods[module]??={files:0,lines:0,prod:0,test:0};mods[module].files++;mods[module].lines+=lines.length;if(code)mods[module][test?'test':'prod']+=sloc;
 if(!code||test)continue;
 const normalized=lines.map(l=>l.trim()).filter(l=>l&&!l.startsWith('//'));for(let i=0;i+12<=normalized.length;i+=12){const block=normalized.slice(i,i+12).join('\n');const hash=crypto.createHash('sha256').update(block).digest('hex');if(!dups.has(hash))dups.set(hash,[]);dups.get(hash).push(f)}
 if(/\.[cm]?[jt]sx?$/.test(f)){const source=ts.createSourceFile(f,text,ts.ScriptTarget.Latest,true);function visit(n){if(ts.isFunctionLike(n)&&n.body){let decisions=1;function count(x){if([ts.SyntaxKind.IfStatement,ts.SyntaxKind.ConditionalExpression,ts.SyntaxKind.ForStatement,ts.SyntaxKind.ForOfStatement,ts.SyntaxKind.ForInStatement,ts.SyntaxKind.WhileStatement,ts.SyntaxKind.DoStatement,ts.SyntaxKind.CatchClause,ts.SyntaxKind.CaseClause].includes(x.kind))decisions++;if(ts.isBinaryExpression(x)&&[ts.SyntaxKind.AmpersandAmpersandToken,ts.SyntaxKind.BarBarToken,ts.SyntaxKind.QuestionQuestionToken].includes(x.operatorToken.kind))decisions++;if(x!==n.body&&ts.isFunctionLike(x))return;ts.forEachChild(x,count)}count(n.body);functions.push({file:f,line:source.getLineAndCharacterOfPosition(n.getStart()).line+1,name:n.name?.getText(source)||'(anonymous)',lines:source.getLineAndCharacterOfPosition(n.end).line-source.getLineAndCharacterOfPosition(n.getStart()).line+1,decisions})}if(ts.isImportDeclaration(n)&&ts.isStringLiteral(n.moduleSpecifier))imports.push({file:f,target:n.moduleSpecifier.text,typeOnly:!!n.importClause?.isTypeOnly});ts.forEachChild(n,visit)}visit(source)}
 }
 const changes=cp.execFileSync('git',['log','--since=2026-08-17','--pretty=format:','--name-only'],{cwd:root,encoding:'utf8'}).split('\n').filter(Boolean),freq={};for(const f of changes)freq[f]=(freq[f]||0)+1;
 result.repos[name]={root,sha:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),trackedFiles:files.length,textFiles:stats.length,totalLines,prodSloc:stats.filter(s=>!s.test).reduce((n,s)=>n+s.sloc,0),testSloc:stats.filter(s=>s.test).reduce((n,s)=>n+s.sloc,0),modules:mods,languages:langs,largest:stats.filter(s=>s.sloc&&!s.test).sort((a,b)=>b.lines-a.lines).slice(0,18),largestFunctions:functions.sort((a,b)=>b.lines-a.lines).slice(0,15),complexFunctions:[...functions].sort((a,b)=>b.decisions-a.decisions).slice(0,12),functions:functions.length,classes:files.filter(f=>f.endsWith('.java')).length,hooks:files.filter(f=>/\/use[A-Z][^/]*\.tsx?$/.test(f)).length,tsxFiles:files.filter(f=>f.endsWith('.tsx')).length,duplicateBlocks:[...dups.values()].filter(v=>new Set(v).size>1).map(v=>[...new Set(v)]).slice(0,20),hotspots:stats.filter(s=>s.sloc&&!s.test).map(s=>({...s,commits30d:freq[s.file]||0,score:s.lines*(freq[s.file]||0)})).sort((a,b)=>b.score-a.score).slice(0,12),imports};
}
fs.writeFileSync('/tmp/fragments-audit-20260917-5eDWJg/metrics.json',JSON.stringify(result,null,2));
for(const [name,r]of Object.entries(result.repos))console.log(JSON.stringify({name,sha:r.sha,files:r.trackedFiles,text:r.textFiles,lines:r.totalLines,prod:r.prodSloc,tests:r.testSloc,largest:r.largest.slice(0,5),complex:r.complexFunctions.slice(0,3)}));
```

### Attribution normalisée des modules

Script temporaire exécuté : module-metrics.cjs. Chemins fixes de la session à adapter pour une autre machine. Ces scripts sont conservés ici pour reproductibilité, pas ajoutés comme outils applicatifs. Leurs biais de mesure sont explicités au §2.

```javascript
const fs=require('fs'),cp=require('child_process'),path=require('path');const base='/tmp/fragments-audit-20260917-5eDWJg';const roots=require(base+'/metrics.json').repos;const result={};
for(const name of ['backend','mobile','studio','engine']){const root=roots[name].root;const groups={};for(const f of cp.execFileSync('git',['ls-files','-z'],{cwd:root,encoding:'utf8'}).split('\0').filter(Boolean)){let raw;try{raw=fs.readFileSync(path.join(root,f))}catch{continue}if(raw.includes(0))continue;const lines=raw.toString().split(/\r?\n/);if(lines.at(-1)==='')lines.pop();const code=/\.(java|tsx?|jsx?|mjs|cjs|cpp|hpp|h|c|sh|sql|swift|kt|gradle)$/.test(f),test=/(^|\/)(tests?|__tests__)(\/|\.)|\.(test|spec)\./.test(f);let mod;
if(name==='backend')mod=(f.match(/src\/(?:main|test)\/java\/com\/nm\/fragmentsclean\/([^/]+)/)?.[1]||f.split('/')[0]).replace(/ContextTest$/,'Context').replace(/^sharedKernelTest$/,'sharedKernel');else if(name==='mobile')mod=f.match(/contextW[Ll]\/([^/]+)/)?.[1]||f.match(/features\/([^/]+)/)?.[1]||f.split('/')[0];else if(name==='studio')mod=f.match(/src\/([^/]+)/)?.[1]||f.split('/')[0];else mod=f.split('/')[0];let inComment=false,sloc=0;for(let l of lines){l=l.trim();if(inComment){if(l.includes('*/')){inComment=false;l=l.slice(l.indexOf('*/')+2).trim()}else continue}if(l.startsWith('/*')){if(!l.includes('*/'))inComment=true;continue}if(l&&!/^(\/\/|\*|#|--)/.test(l))sloc++}groups[mod]??={files:0,lines:0,prod:0,test:0};groups[mod].files++;groups[mod].lines+=lines.length;if(code)groups[mod][test?'test':'prod']+=sloc;}result[name]=groups;}
fs.writeFileSync(base+'/module-metrics.json',JSON.stringify(result));console.log(JSON.stringify(result));
```

### Types, tailles de méthodes et décisions Java

Script temporaire exécuté : JavaMetrics.java. Chemins fixes de la session à adapter pour une autre machine. Ces scripts sont conservés ici pour reproductibilité, pas ajoutés comme outils applicatifs. Leurs biais de mesure sont explicités au §2.

```java
import javax.tools.*;import com.sun.source.util.*;import com.sun.source.tree.*;import java.nio.file.*;import java.util.*;
public class JavaMetrics {
 public static void main(String[] args)throws Exception{
  var compiler=ToolProvider.getSystemJavaCompiler();var manager=compiler.getStandardFileManager(null,null,null);
  var paths=Files.walk(Path.of(args[0])).filter(p->p.toString().endsWith(".java")).map(Path::toFile).toList();
  var task=(JavacTask)compiler.getTask(null,manager,d->{},List.of("-proc:none"),null,manager.getJavaFileObjectsFromFiles(paths));
  var trees=Trees.instance(task);var pos=trees.getSourcePositions();
  for(var unit:task.parse())new TreeScanner<Void,Void>(){
   public Void visitClass(ClassTree c,Void v){System.out.println("TYPE\t"+unit.getSourceFile().getName()+"\t"+c.getSimpleName()+"\t"+c.getKind());return super.visitClass(c,v);}
   public Void visitMethod(MethodTree m,Void v){if(m.getBody()!=null){int[] complexity={1};new TreeScanner<Void,Void>(){public Void scan(Tree t,Void p){if(t!=null&&Set.of(Tree.Kind.IF,Tree.Kind.CONDITIONAL_EXPRESSION,Tree.Kind.FOR_LOOP,Tree.Kind.ENHANCED_FOR_LOOP,Tree.Kind.WHILE_LOOP,Tree.Kind.DO_WHILE_LOOP,Tree.Kind.CATCH,Tree.Kind.CASE,Tree.Kind.CONDITIONAL_AND,Tree.Kind.CONDITIONAL_OR).contains(t.getKind()))complexity[0]++;return super.scan(t,p);}}.scan(m.getBody(),null);long first=unit.getLineMap().getLineNumber(pos.getStartPosition(unit,m)),last=unit.getLineMap().getLineNumber(pos.getEndPosition(unit,m));System.out.println("METHOD\t"+unit.getSourceFile().getName()+"\t"+m.getName()+"\t"+first+"\t"+(last-first+1)+"\t"+complexity[0]);}return super.visitMethod(m,v);}
  }.scan(unit,null);
 }
}
```

### Imports statiques résolus, SCC et inventaire lexical

Script temporaire exécuté : extra-metrics.cjs. Chemins fixes de la session à adapter pour une autre machine. Ces scripts sont conservés ici pour reproductibilité, pas ajoutés comme outils applicatifs. Leurs biais de mesure sont explicités au §2.

```javascript
const fs=require('fs'),path=require('path'),cp=require('child_process');
const base='/tmp/fragments-audit-20260917-5eDWJg';const data=JSON.parse(fs.readFileSync(base+'/metrics.json','utf8'));const out={};
for(const [name,r] of Object.entries(data.repos)){
 const files=cp.execFileSync('git',['ls-files','-z'],{cwd:r.root,encoding:'utf8'}).split('\0').filter(Boolean);const all=new Set(files);const graph=new Map();
 for(const i of r.imports){if(i.typeOnly)continue;let target=i.target.startsWith('.')?path.posix.normalize(path.posix.join(path.posix.dirname(i.file),i.target)):i.target.startsWith('@/')?i.target.slice(2):null;if(!target)continue;target=[target,...['.ts','.tsx','.js','/index.ts','/index.tsx'].map(e=>target+e)].find(f=>all.has(f));if(target){if(!graph.has(i.file))graph.set(i.file,[]);graph.get(i.file).push(target)}}
 let next=0,stack=[],on=new Set(),ids=new Map(),low=new Map(),cycles=[];function visit(v){ids.set(v,next);low.set(v,next++);stack.push(v);on.add(v);for(const w of graph.get(v)||[]){if(!ids.has(w)){visit(w);low.set(v,Math.min(low.get(v),low.get(w)))}else if(on.has(w))low.set(v,Math.min(low.get(v),ids.get(w)))}if(low.get(v)===ids.get(v)){let component=[],w;do{w=stack.pop();on.delete(w);component.push(w)}while(w!==v);if(component.length>1)cycles.push(component)}}for(const v of graph.keys())if(!ids.has(v))visit(v);
 const packagePath=path.join(r.root,name==='studio'?'fragments-studio/package.json':'package.json');let pkg={};if(fs.existsSync(packagePath))pkg=JSON.parse(fs.readFileSync(packagePath));
 let routes=[],scheduled=[],tables=[],todos=[];for(const f of files){if(!/\.(java|sql|ts|tsx)$/.test(f))continue;const lines=fs.readFileSync(path.join(r.root,f),'utf8').split('\n');lines.forEach((s,i)=>{if(/@(Get|Post|Put|Patch|Delete|Request)Mapping/.test(s))routes.push({file:f,line:i+1,text:s.trim()});if(/@Scheduled/.test(s))scheduled.push({file:f,line:i+1,text:s.trim()});if(/CREATE TABLE/i.test(s))tables.push({file:f,line:i+1,text:s.trim()});if(/TODO|FIXME|HACK|@Disabled|\b(?:it|test|describe)\.skip/.test(s))todos.push({file:f,line:i+1,text:s.trim()})})}
 out[name]={cycles,directDependencies:Object.keys(pkg.dependencies||{}).length,devDependencies:Object.keys(pkg.devDependencies||{}).length,routes,scheduled,tables,todos};
}
const java=fs.readFileSync(base+'/java-metrics.tsv','utf8').trim().split('\n').map(l=>l.split('\t'));out.java={types:java.filter(x=>x[0]==='TYPE').reduce((a,x)=>(a[x[3]]=(a[x[3]]||0)+1,a),{}),methods:java.filter(x=>x[0]==='METHOD').length,largestMethods:java.filter(x=>x[0]==='METHOD').sort((a,b)=>+b[4]-a[4]).slice(0,8),complexMethods:java.filter(x=>x[0]==='METHOD').sort((a,b)=>+b[5]-a[5]).slice(0,8)};
fs.writeFileSync(base+'/extra-metrics.json',JSON.stringify(out,null,2));for(const[n,r]of Object.entries(out))console.log(n,JSON.stringify(n==='java'?r:{cycles:r.cycles,direct:r.directDependencies,dev:r.devDependencies,mappings:r.routes.length,scheduled:r.scheduled.length,tables:r.tables.length,todos:r.todos.length}));
```

Ordre de reproduction : node metrics.cjs ; java JavaMetrics.java /Users/nicolasmaldiney/fragmentsClean/src/main/java > java-metrics.tsv ; node extra-metrics.cjs ; node module-metrics.cjs. Les scripts écrivent uniquement leurs JSON/TSV dans le répertoire temporaire indiqué ; ils n’altèrent pas les sources. La collecte temporelle est une mesure ponctuelle et dépend du checkout/lockfiles/registres.

## Clôture du rapport initial

Contrôles finaux : deux rapports présents et lisibles UTF-8, blocs de code équilibrés, chemins explicites src/main/src/test et chemins absolus de fichiers vérifiés présents. Le premier validateur temporaire de références avait une accolade excédentaire, corrigée ; sa première regex prenait les suffixes .json pour .js, corrigée également. Aucun constat produit n’en est tiré. git diff --check exécuté ; working trees mobile, Studio et FlowAtlas inchangés, backend uniquement docs/audit non suivi, moteur uniquement sa modification préexistante. Aucun commit créé.

Ce rapport est un diagnostic **NO-GO**, non une annonce de corrections. La Phase B attend l’accord sur la roadmap. Les traces de suppression/restauration précédemment suspendues restent ouvertes ; aucune branche suspendue n’a été fusionnée. Les fichiers applicatifs restent à leurs commits initiaux. Les limites du §12 sont des conditions explicites à lever pour conclure la qualification exhaustive.
