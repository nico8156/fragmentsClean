# FlowAtlas — usage et évaluation pendant l’audit Fragments

## 1. Conclusion

**FlowAtlas a été réellement utilisé : 22 appels MCP find/context, sur Redux et quatre slices Java.** Il aide à localiser rapidement des chemins d’intention et de propagation, avec une sortie bornée. Il ne démontre ni atomicité, ni autorisation, ni idempotence, ni causalité runtime. Les défauts décisifs de l’audit ont nécessité la lecture des sources, tests et configurations.

Appréciation : **utile comme index architectural guidé ; insuffisant comme arbitre architectural ou preuve de sûreté**. Le support Redux est plus transversal que le Java actuel, mais présente des sur-approximations importantes. Les slices Java explicites donnent des frontières intéressantes sans prétendre couvrir le backend entier. Leur scope assumé est une protection utile, pas un défaut à « corriger » par un scan générique trompeur.

Les priorités d’amélioration sont la granularité des listeners, la provenance des relations et le sens de complete, avant une augmentation brute du nombre de nœuds reconnus.

Ce document porte exclusivement sur l’instrument. Les défauts produit et la roadmap Fragments restent dans [le rapport principal](2026-09-17-fragments-pre-release-audit.md).

## 2. Traçabilité et procédure

Date : **17 septembre 2026**, Europe/Paris.

| Élément | Version / branche / commit | État initial |
|---|---|---|
| FlowAtlas source local | package.json 0.1.0 ; main ; 0d362df634512bf7ee7790f7b146af0fd1f06d21 | propre |
| Backend analysé | main ;17125041c53075d369c9594012c9cea34dc91976 | propre |
| Mobile analysé | main ;f72396ec68075986fc36bb05907e091d5a475263 | propre |
| Studio inspecté traditionnellement | main ;4851f89c998fcc51e7691e100cbc38936c2d07d5 | propre |
| Moteur C++ atteint par la frontière Java | main ;cdebb4e33cc419f5111a2a93b9a4f4f82e1b2bb5 | modification préexistante smoke_contract.sh |

**Version du serveur MCP chargé : non attestée par un endpoint build-info.** Le SHA ci-dessus identifie le dépôt inspecté, pas une preuve cryptographique que le processus MCP a été compilé depuis ce SHA. Aucune version plus précise n’est inventée. Le changelog transmis antérieurement par l’utilisateur (953d45d) n’est pas présenté comme le HEAD d’aujourd’hui.

Instructions lues : AGENTS du dépôt FlowAtlas et .codex/skills/architecture-exploration/SKILL.md entièrement ; instruction annoncée à l’utilisateur. MCP privilégié ; **aucun scan CLI redondant**. Procédure :
- identifiant déjà connu → context ;
- inconnu → find puis context ;
- bornes de nœuds/arêtes/profondeur/octets explicites ;
- source puis tests pour confirmer les relations ;
- scope Java via requestPath explicite, jamais scan Java générique de tout Fragments.

Écart tracé : un context a utilisé l’identifiant supposé projectionUpdated, inexistant. Reprise par find('projection') puis context('projection.updated'). Ce n’est pas une preuve d’absence de SSE ; c’est une erreur d’identifiant de l’agent.

Requêtes Java consultées avant usage :
- tests/fixtures/fragments-java-http-command-request.json ;
- tests/fixtures/fragments-java-integration-event-request.json ;
- tests/fixtures/fragments-java-projection-request.json ;
- tests/fixtures/fragments-java-external-request.json.
La requête semantic a aussi été inspectée, mais n’a pas déclenché d’appel MCP dédié pendant cette session.

Les appels ont été consignés en temporaire dans /tmp/fragments-audit-20260917-5eDWJg/flowatlas-calls.json (21 premiers), puis le 22e conservé dans l’orchestration et transcrit intégralement ci-dessous. Ces fichiers temporaires ne sont pas commités. Aucun code FlowAtlas ou Fragments modifié.

## 3. Mesures objectives

| Mesure | Valeur observée |
|---|---:|
| Appels MCP |22|
| find |9|
| context |13|
| Appels TypeScript (adapter explicite ou défaut) |14|
| Appels Java |8|
| context avec projection exploitable |12|
| context en erreur d’identifiant |1|
| context explicitement tronqué |1 (outboxProcessOnce, maxNodes)|
| find sans résultat |2 (submitTicket ; TicketVerifyAccepted)|
| Durées cumulées des appels |276.907s|
| Médiane TypeScript |4.421s|
| Médiane Java |10.421s|
| Plus long appel |87.358s, context ProcessBuilder|
| Octets serializedBytes cumulés des context réussis |37503|

Durées mesurées par Date.now() avant/après chaque invocation MCP ; elles incluent orchestration/outil et ne sont pas un profil CPU du scanner. Des appels ont pu se chevaucher : **leur somme n’est pas le temps mural total de l’audit**. Pas de benchmark A/B avec le même agent sans FlowAtlas ; donc aucun pourcentage de temps/tokens économisés.

serializedBytes vient de la réponse : taille déclarée de JSON de contexte, pas tokens réellement consommés, ni volume des fichiers ultérieurement lus. Les context vont de1175 à10201octets, ce qui donne une borne utile de lecture mais ne démontre pas un gain total de contexte.

## 4. Couverture effective et validation dans le code

### 4.1 Soumission d’un ticket mobile

find('submitTicket',Event) vide ; find('ticket',Event,limit3) remonte d’abord les actions d’historique plutôt que l’intention de soumission. Après identification du nom réel dans les sources, context(uiTicketSubmitRequested) donne8nœuds/7arêtes :
intention → ticketSubmitUseCaseFactory → optimistic/enqueueCommitted/outboxProcessOnce → état et handler outbox.

Sources consultées : app/core-logic/contextWL/ticketWl/usecases/write/ticketSubmitWlUseCase.ts ; outboxWl/processOutbox.ts ; outboxWl/typeAction/outbox.actions.ts, tous dans fragmentsCleanFront. Sources confirmées : création d’identifiants, enqueue puis demande de traitement. Non démontré par le graphe : persistance disque avant perte du process, HTTP202, intervalle d’ACK et reconcile métier.

Aide réelle : entrée Redux trouvée et contour d’outbox rendu lisible sans charger toute la navigation mobile. Limite : find ne privilégie pas l’action d’intention que l’utilisateur décrit.

### 4.2 Likes et commentaires

find like renvoie des événements optimistes/reconcile/rollback ; context(likeOptimisticApplied) donne4/3, autour de likeToggleUseCaseFactory. Source réelle lue : likeWl/usecases/write/likePressedUseCase.ts. La relation handler→action→reducer est cohérente ; absence de sourceLocation du factory dans la projection impose une recherche traditionnelle.

Pour commentaires : context(commentsRetrievalPending) décrit correctement le thunk de récupération ; lecture de commentWl/usecases/read/commentRetrieval.ts confirme annulation via AbortController et protection de réponse stale. context(uiCommentCreateRequested)8/7 mène à commentCreateWlUseCase.ts et l’outbox. Les deux parcours sont distincts : le résultat find comment avait initialement surtout exposé le read path.

Pas de conclusion sur idempotence serveur ou absence de duplication dérivée de ces liens Redux : ces garanties ont été cherchées dans receipts/inbox/tests.

### 4.3 Outbox cliente

context(outboxProcessOnce,both,depth2,nodes25,edges40,bytes11000) atteint la limite de nœuds : complete=false, frontier fournie.25nœuds/24arêtes,10201octets. Le graphe met en évidence plusieurs commandes convergeant vers un même processeur et les gateways externes.

Approfondissement : lecture ciblée de processOutbox.ts:65-265 et outboxCommandHandlers.ts ; consultation séparée des intents ticket/comment/profile de la frontière. Pas d’expansion globale infinie. Les branches conservant les erreurs transitoires et celles supprimant les kinds inconnus ne sont pas représentées par des edges conditionnelles : le constat de conservation incomplète vient des sources.

Gain qualitatif : trouver les points de convergence. Coût : le factory sans sourceLocation et les nombreuses actions frontalières nécessitent rg pour revenir au fichier exact.

### 4.4 SSE puis snapshot

Le mauvais identifiant projectionUpdated retourne une erreur ; find projection expose l’événement externe 'projection.updated'. Son context renvoie **2nœuds/1arête, complete=true**, seulement event→listener.

Lecture de projectionSyncWl/usecases/projectionSyncListenerFactory.ts:142-225 confirme pourtant les dispatch articlesListRetrieval, scheduleCoffeeRefresh, commentRetrieval, coffeeExperiencesRetrieval, myExperiencesRetrieval, likesRetrieval, ticketRetrieval et entitlementsRetrieval. Ils passent par le helper interne routeProjectionUpdated avec dispatch en paramètre.

**Faux négatif observé dans ce contexte** : les récupérations de snapshots ne sont pas montrées. complete=true signifie graphe obtenu non tronqué, pas flux applicatif complet. Ce cas est particulièrement important : une lecture confiante de cette sortie aurait pu faire manquer l’amplification des GET et le filtrage privé insuffisant.

### 4.5 Entitlements mobile

context(entitlementsRetrieval)4nœuds/4arêtes montre le gateway et l’action hydrated. Source consultée : entitlementWl/usecases/read/entitlementRetrieval.ts. Deux edges DISPATCHES identiques vers hydrated sont retournées sans discriminant de site.

Deux dispatch possibles dans le code ne sont pas nécessairement une erreur de détection. **Ambiguïté de restitution** : le consommateur JSON ne sait pas si la relation est dupliquée par bug, représente deux callsites, ou deux branches. FlowAtlas ne démontre pas le calcul des droits backend : contributions et projections ont été inspectées par recherche classique.

### 4.6 Commande backend HTTP

find VerifyTicketCommand avec request Java HTTP, puis context canonique :
java-command:com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand.

4nœuds/3arêtes : POST /api/tickets/verify → méthode controller → commande → handler. Sources lues : WriteTicketController.java:38-57 ; VerifyTicketCommandHandler.java:34-86. Le typage de commande et la frontière HTTP sont correctement retrouvés dans ce scope.

La transaction handler, le fingerprint ticket, la persistance d’agrégat, la receipt et l’outbox ne sont pas prouvés par ces trois edges. Source et tests unit/web/infra nécessaires. La requête bornée a fait ce qu’elle promettait, pas un graphe de traitement complet.

### 4.7 Publication/consommation d’événements Java et inbox

find TicketVerifyAccepted Event vide (terme Java vs nom d’événement normalisé) ; find ticket renvoie integration:ticket-events:ticket.verify.accepted:v1. context3/2 relie StableEnvelopeOutboxEventSender#send, contrat stable et bean handler SQS TicketSqsIntegrationEventHandlers.

Sources vérifiées : platform/eventing/StableEnvelopeOutboxEventSender.java:37-48 ; IntegrationEventDestinationResolver.java ; ticketContext/read/adapters/primary/springboot/sqs/TicketSqsIntegrationEventHandlers.java. Le fanout ticket-events / ticket-verification-requested et la version1 sont cohérents.

Le request comprend des sources inbox/routeur, mais **la projection ne montre pas le protocole claim/lease/process/delete**. L’analyse traditionnelle a ensuite lu InboxMessageRepository.java:21-93, SqsIntegrationEventRouter.java:37-63 et SqsIntegrationEventConsumer.java:176-208. C’est elle qui a établi le scénario de suppression prématurée sous bail actif. FlowAtlas a donné une piste de chemin, pas découvert/prouvé le défaut d’atomicité.

### 4.8 Mise à jour des projections ticket

find du handler TicketVerifyAcceptedEventHandler puis context canonique donnent3nœuds/2arêtes :
handler DISPATCHES sync:tickets:entity ; événement sync UPDATES le State nommé tickets, référencé dans le handler Java.

Source lue : ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java:27 et ticketContext/read/adapters/secondary/repositories/JdbcTicketStatusProjectionRepository.java:28-59. Le handler met d’abord à jour la projection DB puis émet la notification de fraîcheur.

**Ambiguïté importante** : l’edge notification UPDATES tickets pourrait être lu comme la cause d’une écriture de projection, alors que le code écrit la projection avant de notifier. Le State ici est référencé dans une source Java, pas un reducer Redux : aucune liaison Java→Redux n’a été détectée par cet appel. La relation relève des conventions de projection/request ; documenter son sens évite de transformer une association statique en ordre d’exécution.

### 4.9 Process manager ticket → processus natif

context direct local-process:java.lang.ProcessBuilder avec request external, conformément à l’identifiant fourni dans le cadrage.2nœuds/1arête ; relation CALLS_EXTERNAL vers processus local correctement identifiée depuis le worker.

Sources lues : ScheduledTicketVerificationWorker.java:55-76 et ProcessBuilderTicketVerificationProvider.java:35-135. Bonne détection de frontière malgré l’indirection provider. Le premier appel observé dure87.358s. On ne peut pas attribuer précisément ce temps au scan, Maven/classpath, cache ou transport sans télémétrie interne.

La limite de timeout incomplet (stdin écrit avant waitFor) n’est pas visible dans le graphe ; la source révèle aussi que imageRef n’est pas traitée par le moteur text-only.

### 4.10 Parcours supplémentaire : édition profil / avatar

context(profileUpdateRequested,depth3) :10nœuds/18arêtes, complete=true. Source lue intégralement : userWl/usecases/profile/profileUpdateListenerFactory.ts.

Ce factory enregistre **plusieurs listeners distincts** :
- profileUpdateRequested modifie le nom, enqueue UserProfileUpdate ;
- avatarAttachRequested modifie l’avatar, enqueue UserAvatarAttach ;
- avatarRemoveRequested fait une autre commande.

La projection depuis profileUpdateRequested traverse le **factory commun**, puis montre avatarUpdateOptimistic comme dispatch aval. Ce n’est pas une causalité réelle du changement de nom. **Sur-approximation confirmée / faux positif causal** si l’agent interprète un chemin du graphe comme un scénario d’exécution. Les edges répétées enqueue/outboxProcessOnce n’identifient pas les listeners/sites.

Ce cas fournit un test de régression concret pour améliorer FlowAtlas : séparer les enregistrements listen au sein du factory, conserver factory comme conteneur et non unique Handler exécuté par toutes actions.

### 4.11 Studio et C++ : limites de couverture de l’instrument

Studio a été inspecté/testé dans l’audit général, mais aucun appel MCP Studio spécifique n’a été effectué ; ne pas prétendre que ses selectors/editorial/calendar ont été analysés par FlowAtlas. Le C++ n’est vu que comme frontière de processus ; CMake/CTest et sources sont hors graphe Java. Les politiques IAM/S3, procédures restore, migrations, entitlements backend complets et HMAC ont nécessité exploration classique.

## 5. Registre des qualités, lacunes et cas trompeurs

| ID | Nature | Preuve reproductible | Impact agent |
|---|---|---|---|
|FA-01|détection utile|context ticket intent, like/comment, outbox|localise convergences sans ouvrir tout Redux|
|FA-02|détection utile|Java HTTPcommande et processus local|retrouve frontière/adaptateur concret|
|FA-03|faux négatif observé|context projection.updated seulement2/1 alors que helper dispatch plusieurs snapshots|risque de conclure trop tôt absence de propagation|
|FA-04|sur-approximation causale|profileUpdateRequested→factory→avatarUpdateOptimistic|changement de nom ne déclenche pas avatar|
|FA-05|ambiguïté|entitlement hydrated et profile enqueue edges identiques|callsite/branche non identifiable|
|FA-06|provenance manquante|factories Redux sans sourceLocation|retour à rg et fichiers requis|
|FA-07|ambiguïté de contrat|complete=true malgré absence des helpers et transports aval|confusion graphe non tronqué / analyse sémantique complète|
|FA-08|ambiguïté de modèle|sync:tickets:entity UPDATES tickets (State Java)|association de projection pouvant être confondue avec ordre causal ; le code écrit puis notifie|
|FA-09|limite de scope Java|projection outbox→handler ne rend pas inbox/commit/delete|idempotence/distribué non auditable depuis cette vue seule|
|FA-10|ergonomie find|submitTicket et TicketVerifyAccepted vides ; ticket favorise history|noms canoniques et synonymes difficiles à deviner|
|FA-11|performance à instrumenter|Java87s/35s et TS3–15s dans cette session|besoin budget/état/cache observable|
|FA-12|traçabilité build|pas SHA du serveur MCP attesté|sourceHEAD peut différer du scanner réellement chargé|

FA-03/04 sont appuyés sur sources lues, pas une impression esthétique. FA-08 reste une ambiguïté de représentation configurée, pas une affirmation que le scanner exécute mal le système. Pour FA-11, aucune campagne répétée ne permet une loi de performance.

## 6. Commandes exactes et résultats essentiels

Les noms ci-dessous correspondent aux outils MCP appelés via tools.mcp__flowatlas__flowatlas_find_nodes / flowatlas_get_context. **Aucune commande scan CLI exécutée** dans cette session. Les durées sont les appels réels, pas une estimation.

| # | Outil | Entrée | ms | Résultat |
|---|---|---|---:|---|
|1|find|submitTicket|13124|find ; détail args/résultat ci-dessous|
|2|find|ticket|3556|find ; détail args/résultat ci-dessous|
|3|find|like|14601|find ; détail args/résultat ci-dessous|
|4|find|comment|4892|find ; détail args/résultat ci-dessous|
|5|context|likeOptimisticApplied|3536|4n/3e, complete=true, 1608B|
|6|context|commentsRetrievalPending|4936|3n/2e, complete=true, 1411B|
|7|context|outboxProcessOnce|3431|25n/24e, complete=false, 10201B|
|8|context|uiTicketSubmitRequested|4132|8n/7e, complete=true, 2759B|
|9|context|uiCommentCreateRequested|3552|8n/7e, complete=true, 2767B|
|10|context|projectionUpdated|4526|erreur identifiant|
|11|context|entitlementsRetrieval|4612|4n/4e, complete=true, 1787B|
|12|find|projection|4037|find ; détail args/résultat ci-dessous|
|13|context|projection.updated|4316|2n/1e, complete=true, 1175B|
|14|context|local-process:java.lang.ProcessBuilder|87358|2n/1e, complete=true, 1879B|
|15|find|VerifyTicketCommand|24708|find ; détail args/résultat ci-dessous|
|16|context|java-command:com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand|35836|4n/3e, complete=true, 3889B|
|17|find|TicketVerifyAccepted|10453|find ; détail args/résultat ci-dessous|
|18|find|TicketVerifyAcceptedEventHandler|10316|find ; détail args/résultat ci-dessous|
|19|find|ticket|9502|find ; détail args/résultat ci-dessous|
|20|context|integration:ticket-events:ticket.verify.accepted:v1|10222|3n/2e, complete=true, 2900B|
|21|context|com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)|10390|3n/2e, complete=true, 2454B|
|22|context|profileUpdateRequested|4871|10n/18e, complete=true, 4673B|

### Arguments littéraux

```json
[
  {
    "call": 1,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "submitTicket",
      "kind": "Event",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "limit": 5
    },
    "elapsedMs": 13124
  },
  {
    "call": 2,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "ticket",
      "kind": "Event",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "limit": 3
    },
    "elapsedMs": 3556
  },
  {
    "call": 3,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "like",
      "kind": "Event",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "limit": 3
    },
    "elapsedMs": 14601
  },
  {
    "call": 4,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "comment",
      "kind": "Event",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "limit": 3
    },
    "elapsedMs": 4892
  },
  {
    "call": 5,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "likeOptimisticApplied",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "both",
      "maxDepth": 2,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 11000
    },
    "elapsedMs": 3536
  },
  {
    "call": 6,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "commentsRetrievalPending",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "both",
      "maxDepth": 2,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 11000
    },
    "elapsedMs": 4936
  },
  {
    "call": 7,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "outboxProcessOnce",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "both",
      "maxDepth": 2,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 11000
    },
    "elapsedMs": 3431
  },
  {
    "call": 8,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "uiTicketSubmitRequested",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "downstream",
      "maxDepth": 3,
      "maxNodes": 22,
      "maxEdges": 35,
      "maxBytes": 8000
    },
    "elapsedMs": 4132
  },
  {
    "call": 9,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "uiCommentCreateRequested",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "downstream",
      "maxDepth": 3,
      "maxNodes": 22,
      "maxEdges": 35,
      "maxBytes": 8000
    },
    "elapsedMs": 3552
  },
  {
    "call": 10,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "projectionUpdated",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "downstream",
      "maxDepth": 3,
      "maxNodes": 22,
      "maxEdges": 35,
      "maxBytes": 8000
    },
    "elapsedMs": 4526
  },
  {
    "call": 11,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "entitlementsRetrieval",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "downstream",
      "maxDepth": 3,
      "maxNodes": 22,
      "maxEdges": 35,
      "maxBytes": 8000
    },
    "elapsedMs": 4612
  },
  {
    "call": 12,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "projection",
      "kind": "Event",
      "limit": 5,
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront"
    },
    "elapsedMs": 4037
  },
  {
    "call": 13,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "projection.updated",
      "adapter": "typescript",
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "direction": "both",
      "maxDepth": 3,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 10000
    },
    "elapsedMs": 4316
  },
  {
    "call": 14,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "local-process:java.lang.ProcessBuilder",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-external-request.json",
      "direction": "both",
      "maxDepth": 3,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 10000
    },
    "elapsedMs": 87358
  },
  {
    "call": 15,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "VerifyTicketCommand",
      "kind": "Event",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-http-command-request.json",
      "limit": 3
    },
    "elapsedMs": 24708
  },
  {
    "call": 16,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "java-command:com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand",
      "adapter": "java",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-http-command-request.json",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "direction": "both",
      "maxDepth": 3,
      "maxNodes": 20,
      "maxEdges": 35,
      "maxBytes": 9000
    },
    "elapsedMs": 35836
  },
  {
    "call": 17,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "TicketVerifyAccepted",
      "kind": "Event",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-integration-event-request.json",
      "limit": 5
    },
    "elapsedMs": 10453
  },
  {
    "call": 18,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "TicketVerifyAcceptedEventHandler",
      "kind": "Handler",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-projection-request.json",
      "limit": 5
    },
    "elapsedMs": 10316
  },
  {
    "call": 19,
    "tool": "flowatlas_find_nodes",
    "arguments": {
      "query": "ticket",
      "kind": "Event",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-integration-event-request.json",
      "limit": 5
    },
    "elapsedMs": 9502
  },
  {
    "call": 20,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "integration:ticket-events:ticket.verify.accepted:v1",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-integration-event-request.json",
      "direction": "both",
      "maxDepth": 3,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 10000
    },
    "elapsedMs": 10222
  },
  {
    "call": 21,
    "tool": "flowatlas_get_context",
    "arguments": {
      "nodeId": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
      "adapter": "java",
      "projectPath": "/Users/nicolasmaldiney/fragmentsClean",
      "requestPath": "/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-projection-request.json",
      "direction": "both",
      "maxDepth": 3,
      "maxNodes": 25,
      "maxEdges": 40,
      "maxBytes": 10000
    },
    "elapsedMs": 10390
  },
  {
    "call": 22,
    "tool": "flowatlas_get_context",
    "arguments": {
      "projectPath": "/Users/nicolasmaldiney/fragmentsCleanFront",
      "adapter": "typescript",
      "nodeId": "profileUpdateRequested",
      "direction": "downstream",
      "maxDepth": 3,
      "maxNodes": 20,
      "maxEdges": 25,
      "maxBytes": 10000
    },
    "elapsedMs": 4871
  }
]
```

### Résultats de recherche conservés

```json
[
  {
    "query": "submitTicket",
    "adapter": "typescript",
    "result": {
      "schemaVersion": 1,
      "query": "submitTicket",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 5
      },
      "matches": []
    }
  },
  {
    "query": "ticket",
    "adapter": "typescript",
    "result": {
      "schemaVersion": 1,
      "query": "ticket",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 3
      },
      "matches": [
        {
          "id": "ticketHistoryFailed",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/ticketWl/reducer/ticketWl.reducer.ts",
            "line": 25
          }
        },
        {
          "id": "ticketHistoryReceived",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/ticketWl/reducer/ticketWl.reducer.ts",
            "line": 20
          }
        },
        {
          "id": "ticketHistoryRequested",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/ticketWl/reducer/ticketWl.reducer.ts",
            "line": 19
          }
        }
      ]
    }
  },
  {
    "query": "like",
    "adapter": "typescript",
    "result": {
      "schemaVersion": 1,
      "query": "like",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 3
      },
      "matches": [
        {
          "id": "likeOptimisticApplied",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/likeWl/typeAction/likeWl.action.ts",
            "line": 10
          }
        },
        {
          "id": "likeReconciled",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/likeWl/typeAction/likeWl.action.ts",
            "line": 14
          }
        },
        {
          "id": "likeRollback",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/likeWl/typeAction/likeWl.action.ts",
            "line": 15
          }
        }
      ]
    }
  },
  {
    "query": "comment",
    "adapter": "typescript",
    "result": {
      "schemaVersion": 1,
      "query": "comment",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 3
      },
      "matches": [
        {
          "id": "commentsRetrievalCancelled",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/commentWl/usecases/read/commentRetrieval.ts",
            "line": 23
          }
        },
        {
          "id": "commentsRetrievalFailed",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/commentWl/usecases/read/commentRetrieval.ts",
            "line": 19
          }
        },
        {
          "id": "commentsRetrievalPending",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/commentWl/usecases/read/commentRetrieval.ts",
            "line": 6
          }
        }
      ]
    }
  },
  {
    "query": "projection",
    "adapter": "typescript",
    "result": {
      "schemaVersion": 1,
      "query": "projection",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 5
      },
      "matches": [
        {
          "id": "projection.updated",
          "kind": "Event",
          "source": "external-protocol",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/projectionSyncWl/usecases/projectionSyncListenerFactory.ts",
            "line": 144
          }
        },
        {
          "id": "projectionSyncDisconnected",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/projectionSyncWl/typeAction/projectionSync.action.ts",
            "line": 23
          }
        },
        {
          "id": "projectionSyncDisconnectRequested",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/projectionSyncWl/typeAction/projectionSync.action.ts",
            "line": 11
          }
        },
        {
          "id": "projectionSyncEnsureConnectedRequested",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/projectionSyncWl/typeAction/projectionSync.action.ts",
            "line": 7
          }
        },
        {
          "id": "projectionSyncEventReceived",
          "kind": "Event",
          "sourceLocation": {
            "file": "app/core-logic/contextWL/projectionSyncWl/typeAction/projectionSync.action.ts",
            "line": 19
          }
        }
      ]
    }
  },
  {
    "query": "VerifyTicketCommand",
    "adapter": "java",
    "result": {
      "schemaVersion": 1,
      "query": "VerifyTicketCommand",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 3
      },
      "matches": [
        {
          "id": "java-command:com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand",
          "kind": "Event",
          "sourceLocation": {
            "file": "com/nm/fragmentsclean/ticketContext/write/businesslogic/usecases/VerifyTicketCommand.java",
            "line": 8
          }
        }
      ]
    }
  },
  {
    "query": "TicketVerifyAccepted",
    "adapter": "java",
    "result": {
      "schemaVersion": 1,
      "query": "TicketVerifyAccepted",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 5
      },
      "matches": []
    }
  },
  {
    "query": "TicketVerifyAcceptedEventHandler",
    "adapter": "java",
    "result": {
      "schemaVersion": 1,
      "query": "TicketVerifyAcceptedEventHandler",
      "request": {
        "kinds": [
          "Handler"
        ],
        "limit": 5
      },
      "matches": [
        {
          "id": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
          "kind": "Handler",
          "sourceLocation": {
            "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
            "line": 27
          }
        }
      ]
    }
  },
  {
    "query": "ticket",
    "adapter": "java",
    "result": {
      "schemaVersion": 1,
      "query": "ticket",
      "request": {
        "kinds": [
          "Event"
        ],
        "limit": 5
      },
      "matches": [
        {
          "id": "integration:ticket-events:ticket.verify.accepted:v1",
          "kind": "Event",
          "sourceLocation": {
            "file": "com/nm/fragmentsclean/platform/eventing/IntegrationEventDestinationResolver.java",
            "line": 23
          }
        },
        {
          "id": "integration:ticket-verification-requested:ticket.verify.accepted:v1",
          "kind": "Event",
          "sourceLocation": {
            "file": "com/nm/fragmentsclean/platform/eventing/IntegrationEventDestinationResolver.java",
            "line": 23
          }
        }
      ]
    }
  }
]
```

### Projections représentatives pour régression de l’outil

Ces quatre réponses bornées sont transcrites pour conserver les relations discutées, sans refaire un scan :
SSE incomplet sémantiquement ; outbox Java sans protocole inbox ; projection ticket configurée ; factory profil sur-approximé.

```json
[
  {
    "focus": "projection.updated",
    "result": {
      "schemaVersion": 2,
      "focus": {
        "id": "projection.updated",
        "kind": "Event",
        "source": "external-protocol",
        "sourceLocation": {
          "file": "app/core-logic/contextWL/projectionSyncWl/usecases/projectionSyncListenerFactory.ts",
          "line": 144
        }
      },
      "request": {
        "direction": "both",
        "maxDepth": 3,
        "maxNodes": 25,
        "maxEdges": 40,
        "maxBytes": 10000
      },
      "complete": true,
      "frontierComplete": true,
      "omittedFrontierCount": 0,
      "limitsReached": [],
      "returned": {
        "nodes": 2,
        "edges": 1
      },
      "frontier": [],
      "projection": {
        "nodes": [
          {
            "id": "projection.updated",
            "kind": "Event",
            "source": "external-protocol",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/projectionSyncWl/usecases/projectionSyncListenerFactory.ts",
              "line": 144
            }
          },
          {
            "id": "projectionSyncListenerFactory",
            "kind": "Handler"
          }
        ],
        "edges": [
          {
            "source": "projectionSyncListenerFactory",
            "target": "projection.updated",
            "kind": "LISTENS_TO"
          }
        ]
      },
      "serializedBytes": 1175
    }
  },
  {
    "focus": "integration:ticket-events:ticket.verify.accepted:v1",
    "result": {
      "schemaVersion": 2,
      "focus": {
        "id": "integration:ticket-events:ticket.verify.accepted:v1",
        "kind": "Event",
        "sourceLocation": {
          "file": "com/nm/fragmentsclean/platform/eventing/IntegrationEventDestinationResolver.java",
          "line": 23
        }
      },
      "request": {
        "direction": "both",
        "maxDepth": 3,
        "maxNodes": 25,
        "maxEdges": 40,
        "maxBytes": 10000
      },
      "complete": true,
      "frontierComplete": true,
      "omittedFrontierCount": 0,
      "limitsReached": [],
      "returned": {
        "nodes": 3,
        "edges": 2
      },
      "frontier": [],
      "projection": {
        "nodes": [
          {
            "id": "integration:ticket-events:ticket.verify.accepted:v1",
            "kind": "Event",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/platform/eventing/IntegrationEventDestinationResolver.java",
              "line": 23
            }
          },
          {
            "id": "com.nm.fragmentsclean.platform.eventing.StableEnvelopeOutboxEventSender#send(com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity)",
            "kind": "Handler",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/platform/eventing/StableEnvelopeOutboxEventSender.java",
              "line": 48
            }
          },
          {
            "id": "com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.sqs.TicketSqsIntegrationEventHandlers#ticketVerifyAcceptedReadSqsIntegrationEventHandler(com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler)",
            "kind": "Handler",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/sqs/TicketSqsIntegrationEventHandlers.java",
              "line": 27
            }
          }
        ],
        "edges": [
          {
            "source": "com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.sqs.TicketSqsIntegrationEventHandlers#ticketVerifyAcceptedReadSqsIntegrationEventHandler(com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler)",
            "target": "integration:ticket-events:ticket.verify.accepted:v1",
            "kind": "LISTENS_TO",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/sqs/TicketSqsIntegrationEventHandlers.java",
              "line": 27
            }
          },
          {
            "source": "com.nm.fragmentsclean.platform.eventing.StableEnvelopeOutboxEventSender#send(com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity)",
            "target": "integration:ticket-events:ticket.verify.accepted:v1",
            "kind": "DISPATCHES",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/platform/eventing/StableEnvelopeOutboxEventSender.java",
              "line": 48
            }
          }
        ]
      },
      "serializedBytes": 2900
    }
  },
  {
    "focus": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
    "result": {
      "schemaVersion": 2,
      "focus": {
        "id": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
        "kind": "Handler",
        "sourceLocation": {
          "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
          "line": 27
        }
      },
      "request": {
        "direction": "both",
        "maxDepth": 3,
        "maxNodes": 25,
        "maxEdges": 40,
        "maxBytes": 10000
      },
      "complete": true,
      "frontierComplete": true,
      "omittedFrontierCount": 0,
      "limitsReached": [],
      "returned": {
        "nodes": 3,
        "edges": 2
      },
      "frontier": [],
      "projection": {
        "nodes": [
          {
            "id": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
            "kind": "Handler",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
              "line": 27
            }
          },
          {
            "id": "sync:tickets:entity",
            "kind": "Event",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
              "line": 33
            }
          },
          {
            "id": "tickets",
            "kind": "State",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
              "line": 30
            }
          }
        ],
        "edges": [
          {
            "source": "com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler#handle(com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent)",
            "target": "sync:tickets:entity",
            "kind": "DISPATCHES",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
              "line": 33
            }
          },
          {
            "source": "sync:tickets:entity",
            "target": "tickets",
            "kind": "UPDATES",
            "sourceLocation": {
              "file": "com/nm/fragmentsclean/ticketContext/read/projections/TicketVerifyAcceptedEventHandler.java",
              "line": 33
            }
          }
        ]
      },
      "serializedBytes": 2454
    }
  },
  {
    "focus": "profileUpdateRequested",
    "result": {
      "schemaVersion": 2,
      "focus": {
        "id": "profileUpdateRequested",
        "kind": "Event",
        "sourceLocation": {
          "file": "app/core-logic/contextWL/userWl/typeAction/user.action.ts",
          "line": 61
        }
      },
      "request": {
        "direction": "downstream",
        "maxDepth": 3,
        "maxNodes": 20,
        "maxEdges": 25,
        "maxBytes": 10000
      },
      "complete": true,
      "frontierComplete": true,
      "omittedFrontierCount": 0,
      "limitsReached": [],
      "returned": {
        "nodes": 10,
        "edges": 18
      },
      "frontier": [],
      "projection": {
        "nodes": [
          {
            "id": "profileUpdateRequested",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/userWl/typeAction/user.action.ts",
              "line": 61
            }
          },
          {
            "id": "profileUpdateListenerFactory",
            "kind": "Handler"
          },
          {
            "id": "avatarUpdateOptimistic",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/userWl/typeAction/user.action.ts",
              "line": 86
            }
          },
          {
            "id": "enqueueCommitted",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/outboxWl/typeAction/outbox.actions.ts",
              "line": 35
            }
          },
          {
            "id": "outboxProcessOnce",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/outboxWl/typeAction/outbox.actions.ts",
              "line": 9
            }
          },
          {
            "id": "profileUpdateOptimistic",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/userWl/typeAction/user.action.ts",
              "line": 65
            }
          },
          {
            "id": "profileUpdateRejectedLocally",
            "kind": "Event",
            "sourceLocation": {
              "file": "app/core-logic/contextWL/userWl/typeAction/user.action.ts",
              "line": 70
            }
          },
          {
            "id": "authReducer",
            "kind": "State"
          },
          {
            "id": "outboxWlReducer",
            "kind": "State"
          },
          {
            "id": "processOutboxFactory",
            "kind": "Handler"
          }
        ],
        "edges": [
          {
            "source": "processOutboxFactory",
            "target": "outboxProcessOnce",
            "kind": "LISTENS_TO"
          },
          {
            "source": "enqueueCommitted",
            "target": "outboxWlReducer",
            "kind": "UPDATES"
          },
          {
            "source": "avatarUpdateOptimistic",
            "target": "authReducer",
            "kind": "UPDATES"
          },
          {
            "source": "profileUpdateRejectedLocally",
            "target": "authReducer",
            "kind": "UPDATES"
          },
          {
            "source": "profileUpdateOptimistic",
            "target": "authReducer",
            "kind": "UPDATES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "profileUpdateRequested",
            "kind": "LISTENS_TO"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "profileUpdateRejectedLocally",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "profileUpdateRejectedLocally",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "profileUpdateOptimistic",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "enqueueCommitted",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "outboxProcessOnce",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "profileUpdateRejectedLocally",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "avatarUpdateOptimistic",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "enqueueCommitted",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "outboxProcessOnce",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "avatarUpdateOptimistic",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "enqueueCommitted",
            "kind": "DISPATCHES"
          },
          {
            "source": "profileUpdateListenerFactory",
            "target": "outboxProcessOnce",
            "kind": "DISPATCHES"
          }
        ]
      },
      "serializedBytes": 4673
    }
  }
]
```

## 7. Rescans, redondance et coût de contexte

22 invocations, pas22 scans internes prouvés. Aucune télémétrie cacheHit/scanId/fichiers reparsés fournie dans les réponses conservées ; **impossible de compter les rescans internes**. Le changement de requestPath Java est intentionnel : HTTP, transport, projection et processus ont des scopes différents.

Redondances agent :
- premier find submitTicket mal aligné sur le nom réel ;
- projectionUpdated tenté avant de récupérer l’identifiant canonique ;
- recherche événement Java par nom de classe puis par nom métier.
Ces trois détours sont documentés, pas présentés comme panne du scanner.

Les context courts limitent effectivement l’entrée de texte architectural ; néanmoins les sources et tests restent nécessaires. Plusieurs handlers sans sourceLocation ont ajouté des rg. La reconstruction de chemins Java et le ciblage Redux ont été utiles ; les défaillances de précision obligent à conserver un esprit critique, ce qui réduit un éventuel gain de temps brut.

**Mesuré :** nombre d’appels, arguments, durées, nœuds/arêtes/octetspayload, erreurs/limites explicites.
**Estimé :** aucune économie chiffrée.
**Qualitatif :** gain d’orientation réel sur Redux ; Java utile pour jalonner une verticale et montrer une frontière externe ; gain limité pour transactions/invariants/permissions/ops.
**Non établi :** coût tokens facturé, accélération moyenne, proportion de code explorée, recall/precision globale du scanner. Quelques exemples ne suffisent pas à calculer ces métriques.

## 8. Recommandations priorisées pour FlowAtlas

### A. Défauts ou résultats trompeurs

1. **Séparer les registrations Redux listener.** HandlerId stable par action matcher + site ; factory comme conteneur. Test exact sur profileUpdateListenerFactory : le nom affiché ne doit pas conduire à avatarAttach.
2. **Traiter les helpers dispatch paramétrés** avec portée bornée et provenance : routeProjectionUpdated doit montrer les snapshot actions, pas inventer toutes les branches comme toujours exécutées. Distinguer edges conditionnelles.
3. **Distinguer les sens de complétude.** completeProjectionWithinBudget, sourceScopeComplete, unresolvedRelations et unsupportedPatterns ; ne pas conserver un seul booléen interprétable comme certitude totale.
4. **Ne pas nommer UPDATES une invalidation de fraîcheur indirecte** sans médiation ; préférer INVALIDATES/TRIGGERS_REFRESH et lien READ_SNAPSHOT, ou qualifier explicitement « convention de projection ».
5. **Dédupliquer ou rendre explicites les callsites** : edgeId/site/span/condition ; mêmes endpoints ≠ même appel.

### B. Fiabilité pour l’usage agent

- sourceLocation obligatoire quand disponible pour Handler/State/External ; plages start/end, méthode et confiance de résolution.
- SHA outil, SHA/propreté projet, requestHash, configHash, date du scan, version adapters/schema dans chaque réponse.
- provenance extracted/inferred/configured pour chaque edge ; les contrats explicitement mappés par fixture ne doivent pas passer pour des faits extraits.
- raison d’absence et patterns non supportés, plus distinction node absent / hors scope / non reconnu / budget épuisé.
- tests de précision négatifs autant que fixtures positives ; comparer au code réel avec helpers, multiple registrations, generics/DI et transactions.
- possibilité de retourner un petit « packet de preuves » : fragments source minimaux + références tests, sans générer de verdict de qualité.

### C. Adaptateurs ou conventions à reconnaître

- Spring @Transactional/proxy/REQUIRES_NEW ; annotations de scheduling et propagation de scope ; signaler seulement la frontière connue, pas garantir une transaction distribuée.
- JDBC/JPA claims, locks, CAS/version et outbox write : facts transactionnels avec provenance. Le trio claim→effect→ack mérite une vue dédiée.
- SQS receive/delete/visibility, fanout/queue/DLQ et inbox consumer identity ; reconnaître la différence BUSY et PROCESSED lorsque typée.
- Process managers : état/owner/lease/deadline, fonctions de claim/complete, états terminaux, external calls.
- Redux account scope, storage outbox versions, Auth lifecycle, SSE→thunk→snapshot→reducer.
- Contrats HTTP/OpenAPI/backend→mapper mobile/Studio ; lier sémantiquement sans scanner tous les repos par défaut.
- C++ subprocess contract, GitHub Actions/Compose/CloudFormation : souhaitables après fiabilisation des adaptateurs existants, pas priorité à un énorme graphe infra.

### D. Améliorer find

- Recherche sur noms qualifiés/canoniques et noms source/alias ; suggérer uiTicketSubmitRequested pour submit ticket sans inventer de résultat.
- Prioriser intents/use cases entrants quand la requête vise une action ; garder tri déterministe mais montrer score/matchReason.
- Sur zéro résultat, proposer proches/out-of-scope avec coût borné ; exposer les namespaces Event/Handler reconnus.
- Filtrer read/write, contexte/fichier et type de frontière ; pagination stable.
- Préférer un identifiant copiable unique ; un nom Java source et integration event doivent rester deux objets liés, pas forcément fusionnés.

### E. Améliorer context

- Développer une frontière précise par id/site plutôt que rescanner large.
- Résumer frontières non résolues même lorsque budget non atteint ; sourceScope doit lister exclusions.
- Option chemin entre deux nœuds avec preuves ; ne pas présenter un chemin factory sur-approximé comme exécution.
- Vue ownership et frontières BC à partir d’un contrat local explicite ; autoriser exceptions documentées, pas verdict DDD automatique.
- Option inclure erreurs/retry/no-op/timeout/ack comme catégories d’effets avec preuves.

### F. JSON

- Conserver schemaVersion et budgets explicites (déjà utile).
- Ajouter provenance, diagnosticCode, unsupportedReason, confidence non numérique arbitraire, callsiteId, condition et type de relation sémantique.
- Ajouter scanId/cacheHit/timings par phase/fichiers analysés/réutilisés, sha sources/outil ; harmoniser forme erreurs vs succès.
- Séparer edges uniques et sites multiples ; garder serializedBytes mais documenter exactement ce qu’il mesure.
- Inclure frontierCount et clés de continuation déterministes ; pas d’explosion de contexte via diagnostics verbeux.

### G. Performance

- Instrumenter avant d’optimiser : parse/classpath/Maven/projection/serialization/cache ; rendre visible si le serveur travaille encore.
- Cache par SHA+requestHash+config, invalidation fichiers explicite ; réutiliser le même graphe pour find/context.
- Statut/progress/cancel sur Java lent, afin que l’agent puisse donner une progression honnête sans supposer un blocage.
- Benchmarks fixes Fragments Redux/Java : cold/warm, plusieurs répétitions, latence p50/p95 et taille mémoire ; gains non mesurés ici.

### H. Souhaitable seulement

Comparaison de deux versions de graphe pour une PR, export d’un diagramme compact, liens IDE, annotations de review et catalogue de conventions. Ces fonctions ne compensent pas une relation fausse ou sans source.

## 9. Bilan final et limites

FlowAtlas a contribué à **l’orientation**, surtout l’entrée ticket/comment/like/outbox et les frontières commande/SQS/processus. La lecture traditionnelle a établi les constats sécurité, HMAC, refresh, retention/restore, contention, erreurs de lease et politiques AWS. Il serait trompeur de les attribuer entièrement à FlowAtlas.

Ce compte-rendu d’utilisation initial était achevé pour les 22 appels consignés ;
**ce n’est pas un audit interne complet du logiciel FlowAtlas**. Pas de benchmark
contrôlé, pas de preuve du SHA du processus MCP, pas de mesure globale des faux
positifs/négatifs. Le dépôt FlowAtlas est resté inchangé. Ces exemples et
arguments littéraux permettent maintenant une campagne de régression ciblée et
mesurable.

## 10. Reprise P1 du 18 septembre 2026 — scanner Java mis à jour

Trois appels MCP supplémentaires ont été effectués avec
`fragments-java-external-request.json` : deux `find` identiques (le premier a été
répété uniquement parce que l'enveloppe annonçait `structuredContent` sans
l'imprimer), puis un `context` :

```json
{"query":"ProcessBuilder","projectPath":"/Users/nicolasmaldiney/fragmentsClean","adapter":"java","requestPath":"/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-external-request.json","kind":"External","limit":5}
```

```json
{"nodeId":"local-process:java.lang.ProcessBuilder","projectPath":"/Users/nicolasmaldiney/fragmentsClean","adapter":"java","requestPath":"/Users/nicolasmaldiney/FlowAtlas/tests/fixtures/fragments-java-external-request.json","direction":"both","maxDepth":4,"maxNodes":30,"maxEdges":50,"maxBytes":20000}
```

Résultat objectif : `find` pointe désormais la ligne 85 du provider modifié.
`context` retourne `schemaVersion: 2`, `complete: true`,
`frontierComplete: true`, aucune limite atteinte, 2 nœuds, 1 arête et 1879
octets sérialisés. L'ajout de la complétude de frontière, des limites atteintes
et du volume sérialisé répond directement à une faiblesse relevée dans le
rapport initial.

Le point restant ambigu est la contraction du chemin : l'arête relie le worker
planifié directement à `ProcessBuilder`, alors que la source traverse
`TicketVerificationProvider` puis
`ProcessBuilderTicketVerificationProvider`. Cette projection est utile pour
localiser rapidement le risque externe, mais insuffisante pour analyser la
deadline, le bornage stdin/stdout/stderr et le nettoyage du processus. Ces
preuves ont encore nécessité la lecture des sources et l'exécution de faux
binaires hostiles. Le gain qualitatif Java est donc meilleur pour l'orientation
et la complétude déclarée, sans permettre un gain de temps chiffré honnête.
