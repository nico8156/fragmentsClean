# Audit prépublication — serveur, mobile, Studio et FlowAtlas

Date : 11 septembre 2026. Constats et preuves de l'audit initial du code local.
Plan d'intervention révisé après la première tranche, à la demande du produit.

## Statut du document et décision de périmètre

Le périmètre global comprend désormais **les deux parcours** : nouveau parcours
Experience et préparation complète à la publication App Store. TestFlight est
une étape de validation de ce même produit, pas une alternative qui retire les
exigences de publication. Les anciennes trajectoires A/B sont remplacées par le
[plan unifié d'intervention](#plan-unifié-dintervention).

La première tranche est implémentée et testée localement : voir le
[bilan et ses limites](first-common-tranche-2026-09-11.md). Les constats et nombres
de tests des sections d'audit ci-dessous restent ceux d'avant cette tranche.
Le plan distingue donc corrections intégrées localement, validations restantes
et travaux non commencés. **Les correctifs P0 sont présents sur les branches de
release, mais ne sont ni déployés ni recettés sur appareil/environnement cible.**

Statut du plan révisé : **validé par l'utilisateur le 11 septembre 2026**.
Les lots 00 et 01 sont intégrés localement dans `release/app-store`. Aucun choix
métier des lots suivants n'est implicitement validé et aucun déploiement n'est
autorisé.

Lot 02 a été cadré avec Astra High puis implémenté avec Sol High. Décisions produit explicites : **expériences
possibles sans ticket**, **Pass repensé autour des visites/expériences**, avec
**tickets nécessaires pour certains niveaux**. Le [contrat du lot 02](../architecture/experience-pass-contracts.md)
fait foi : les seuils version 2 et la conservation des acquis ont été validés.

Le lot 03 est implémenté localement avec **GPT-5.6 Sol High** : nom public
offline-first, lecture de profil dans son contexte propriétaire, connexion Apple
native, credentials fournisseur chiffrés, suppression initiable dans l'app et
processus durable d'effacement par bounded context. Les prérequis Apple Developer,
les valeurs SSM et la recette sur build iOS signé restent externes et non réalisés.

Les lots 04 et 05 sont également implémentés localement avec **GPT-5.6 Sol
High**. `experienceContext` est maintenant le propriétaire des expériences texte
et de leur modération ; le mobile expose les parcours café et personnel sans
exiger de ticket, et Studio traite commentaires et expériences dans une file
unifiée. Les médias restent explicitement au lot 06.

## Conclusion

Fragments dispose déjà du socle nécessaire pour avancer vite : navigation,
carte clusterisée, fiche café, articles, Pass, commandes offline, cache durable,
SSE, SQS, stockage des assets et Studio. Le plan initial surestime le travail de
reconstruction de ces fondations. En revanche, il sous-estime certaines
dépendances du nouveau parcours : rattachement d'un ticket à un café,
isolation des comptes, publication de médias utilisateurs et modération.

L'objectif initial de quatre jours reste un repère d'ambition, pas une condition
qui autorise à retirer un parcours. Le produit demande maintenant de réaliser
l'ensemble et d'observer le temps réellement nécessaire. Le plan suit les lots,
leurs critères de sortie et les temps constatés ; aucune date de soumission
n'est garantie. Le calendrier Apple et les attentes externes sont distingués du
temps de développement.

Les expériences libres deviennent un contenu autonome, accessible sans ticket.
Elles ne doivent pas être présentées comme des visites vérifiées. Les commentaires
actuels restent distincts jusqu'à décision explicite de transition ; les contenus
publics imposent déjà de traiter la modération UGC.

## Périmètre et niveau de preuve

| Dépôt | Révision inspectée | Périmètre |
| --- | --- | --- |
| `fragmentsClean` | `18ae517` | Contrats, tickets, profil, social, sécurité, statut de commande, stockage, doctrine et déploiement |
| `fragmentsCleanFront` | `093fdc5` | Navigation, découverte, fiche, Home, profil, tickets, outbox, cache, SSE et configuration native |
| `fragments-admin` | `f33ffaf` | Application dans `fragments-studio`, publication café, édition et gestion des tickets |
| `FlowAtlas` | Checkout local | Guide d'exploration, chargeur TypeScript, MCP et projections réelles des deux clients |

Les trois worktrees produit étaient propres au début de l'audit. Seul ce rapport
est ajouté au serveur. Un script de diagnostic avec données synthétiques a été
créé dans `/private/tmp/fragments-release-audit-probe.cjs`.

Pas de déploiement, de modification AWS, de migration, ni de soumission EAS.
Pas de test sur appareil ou de capture visuelle : les constats UI viennent du
code. L'état AWS décrit dans les documents du dépôt n'a pas été revérifié en
ligne. Les exigences Apple ont été consultées sur les sources officielles.

## Vérifications exécutées

| Vérification | Résultat |
| --- | --- |
| Mobile : suite Jest complète | 52 suites, 209 tests réussis |
| Mobile : TypeScript `--noEmit` | Réussi |
| Mobile : `check-native-release-config.mjs` | Réussi |
| Studio : suite Vitest complète | 27 fichiers, 136 tests réussis |
| Studio : TypeScript `--noEmit` | Réussi |
| Backend : architecture BC, commandes tickets, process manager, SSE ticket, sécurité admin | 5 classes, 36 tests réussis |
| Diagnostic pur : permission refusée | Défaut reproduit : `denied` est stocké comme `granted` |
| Diagnostic pur : connexion A, ticket A, déconnexion, connexion B | Défaut reproduit : le ticket A reste dans l'état |

Classes backend exécutées : `BoundedContextArchitectureTest`,
`VerifyTicketCommandHandlerTest`, `TicketVerificationProcessManagerTest`,
`TicketProjectionSyncEventHandlerTest`, `AdminTokenSecurityTest`.
Les tests d'intégration PostgreSQL/LocalStack et les parcours Maestro n'ont pas
été exécutés. Ces résultats verts ne certifient donc ni la production ni les
scénarios manquants ci-dessous.

## Constats prioritaires

### P0 — Isolation des comptes côté mobile

État après tranche 00 : correction implémentée et testée localement ; récupération
des anciens snapshots et recette appareil encore à traiter. Constat initial :

Le cache durable utilise une clé globale `app.read-model-cache` et conserve
tickets, favoris et état social. La déconnexion vide l'authentification mais
ne réinitialise pas ces états privés. Les sélecteurs de tickets ne filtrent pas
par utilisateur. Le diagnostic des reducers reproduit la conservation du ticket
A après connexion de B.

L'outbox vérifie qu'une session existe, mais ne compare pas le propriétaire
de la commande au compte courant avant envoi. Les gateways utilisent le token
courant. Il existe donc également un risque d'envoyer une commande en attente
de A sous l'identité de B ; ce second effet est établi par lecture du chemin
d'envoi, pas par un test contre le serveur.

Correction cible : partitionner cache privé, outbox et curseur de synchronisation
par compte ; isoler les états au changement de session ; refuser l'envoi sous
un autre propriétaire ; protéger les réponses tardives de l'ancienne session.
Préserver les commandes de A pour son retour, sans les réattribuer ni les
supprimer silencieusement. Les données publiques peuvent rester communes.

Sources : [cache](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/gateways/storage/nativeReadModelCacheStorage.ts),
[snapshot](/Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/appWl/runtime/readModelCachePersistenceFactory.ts),
[store](/Users/nicolasmaldiney/fragmentsCleanFront/app/store/reduxStoreWl.ts),
[sélecteurs tickets](/Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/ticketWl/selector/ticket.selector.ts),
[envoi outbox](/Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/outboxWl/processOutbox.ts).

### P0 — Lecture d'un ticket sans contrôle du propriétaire

État après tranche 00 : contrôle propriétaire implémenté, testé en HTTP/JDBC avec
PostgreSQL réel ; pas encore déployé. Constat initial :

`GET /api/tickets/{ticketId}/status` exige une authentification, mais ni le
contrôleur, ni la query, ni le SQL ne comparent le propriétaire au sujet JWT.
Un utilisateur authentifié connaissant l'identifiant d'un autre ticket peut
emprunter ce chemin de lecture. La réponse inclut notamment OCR, montant,
commerçant, adresse et moyen de paiement. Un UUID ne remplace pas l'autorisation.

Correction cible : query avec identité authentifiée, filtre `ticket_id` +
`user_id` dans le repository, réponse non révélatrice pour un ticket étranger,
tests HTTP avec deux utilisateurs. Conserver un chemin admin séparé.

Sources : [contrôleur](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/primary/springboot/controllers/ReadTicketController.java),
[query handler](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/GetTicketStatusQueryHandler.java),
[SQL](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/secondary/repositories/JdbcTicketStatusReadRepository.java).

### P0 — Une erreur technique peut devenir un rejet métier

État après lot 01 : correction implémentée, testée et intégrée localement dans
`release/app-store` côté serveur et mobile ; migration de production et recette
sur environnement déployé restent à exécuter. Le constat initial était :

`WriteTicketController.verify` transforme toute exception du dispatch en HTTP
400. Le mapper mobile classe ce statut comme erreur `business`, ce qui autorise
le rollback et la suppression de la commande. Une panne de persistence pendant
le dispatch peut donc faire perdre une intention utilisateur.

Par ailleurs, le repository de statut inspecté sait enregistrer `APPLIED`, mais
aucun chemin d'enregistrement de `REJECTED` n'a été trouvé dans le code principal.
Une réponse de rejet perdue peut laisser le polling à `PENDING`. Le contrôleur
de statut reçoit aussi un JWT sans l'utiliser pour filtrer les résultats.

Correction cible : rejets métier explicites et durables, erreurs techniques
retryables, statut associé au demandeur, gestion des succès sans nouvel événement.
Ne pas confondre une commande de soumission appliquée avec un ticket finalement
refusé : le résultat de la vérification reste un état métier séparé.

Sources : [écriture ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/WriteTicketController.java),
[classification mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/outboxWl/gateway/gatewayError.ts),
[statuts](../../src/main/java/com/nm/fragmentsclean/sharedKernel/adapters/secondary/gateways/repositories/jdbc/CommandStatusRepository.java).

### P1 — Permission de localisation mal interprétée

État après tranche 00 : reducer corrigé, tests des trois valeurs réussis ; recette
sur appareil encore à effectuer. Constat initial :

Le reducer utilise la valeur truthy de la chaîne au lieu du statut lui-même.
`denied` et `undetermined` deviennent donc `granted`. Reproduction effectuée
sur le reducer réel. Corriger puis tester les trois valeurs et le parcours sans
localisation. Cela n'accorde pas la permission système, mais fausse l'état UI.

Source : [locationReducer](/Users/nicolasmaldiney/fragmentsCleanFront/app/core-logic/contextWL/locationWl/reducer/location.reducer.ts).

### P1 — Profil : domaine partiel, édition non branchée

`AppUser.updatePublicProfile` et `app.user.profile_updated` existent. L'écran
`EditProfileScreen` est en lecture seule ; aucun endpoint d'édition ni workflow
d'upload avatar utilisateur n'a été trouvé. La méthode actuelle ignore un avatar
null : la suppression doit être une intention explicite.

Le profil mobile passe par `/auth/me`, dont le handler lit directement
`app_users`, table du contexte applicatif. Pour l'édition, ajouter un read/write
contract propriétaire dans `userApplicationContext`, sans prolonger cette dette
SQL entre BC. Garder l'identité OAuth distincte du nom public choisi.

Sources : [AppUser](../../src/main/java/com/nm/fragmentsclean/userApplicationContext/write/businesslogic/models/AppUser.java),
[lecture actuelle](../../src/main/java/com/nm/fragmentsclean/authenticationContext/read/GetMeQueryHandler.java),
[écran](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/features/profile/screens/EditProfileScreen.tsx).

### P1 — Ticket validé ne signifie pas visite vérifiée d'un café

L'agrégat, le read model et `TicketIntegrationEvents.VerificationCompleted`
contiennent les données du commerçant mais aucun `coffeeId`. La création d'un
nouveau `ticketId` n'est pas une protection contre le rescan du même reçu physique.
Aucune déduplication de reçu métier n'a été identifiée dans ce chemin serveur.

Le mobile capture une photo, extrait le texte localement et envoie l'OCR. Le
provider câblé appelle le binaire `ticketverify` : la présence d'une ancienne
classe OpenAI ne prouve pas que le scan utilise actuellement une IA distante.
Le moteur externe n'a pas été audité. Un texte OCR accepté ne suffit pas à
promettre une preuve antifraude de présence.

L'historique mobile compose les tickets déjà connus localement. Le contrôleur
public n'expose pas de liste des tickets du compte : restauration sur nouveau
téléphone et sélection de tickets anciens demandent un endpoint dédié.

Sources : [contrats](../../src/main/java/com/nm/fragmentsclean/platform/eventing/contracts/TicketIntegrationEvents.java),
[Ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/businesslogic/models/Ticket.java),
[provider câblé](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/configuration/TicketWriteDependenciesConfiguration.java),
[historique mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/viewModel/useTicketsHistory.ts).

### P1 — Exceptions d'architecture dans le parcours ticket/admin

Le process manager ticket est `@Transactional` et appelle `provider.verify`
pendant cette transaction. Il faut éviter de recopier ce modèle pour le traitement
des médias : utiliser une prise de travail durable, exécution externe hors
transaction, puis completion idempotente.

`TicketAdminPanel` appelle directement le gateway depuis React et recharge
immédiatement après une réponse HTTP. Le contrôleur admin ticket accède directement
au read repository ; ses commandes génèrent un identifiant serveur qui n'est pas
retourné au client. Ce panneau ne constitue pas un modèle conforme pour la future
modération. Le workflow café est un meilleur exemple : intent, listener, commande,
polling du statut, puis refresh.

Sources : [process manager](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/businesslogic/processManagers/TicketVerificationProcessManager.java),
[contrôleur admin](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/adapters/primary/springboot/controllers/AdminTicketController.java),
[panneau tickets](/Users/nicolasmaldiney/fragments-admin/fragments-studio/src/ticketAdminContext/ui/TicketAdminPanel.tsx),
[workflow café](/Users/nicolasmaldiney/fragments-admin/fragments-studio/src/existingCoffeesContext/write/listeners/existingCoffeesListeners.ts).

## Écarts App Store

Les commentaires publics sont déjà de l'UGC. Aucun parcours complet signalement,
blocage, filtrage et traitement opérateur n'a été trouvé. Le statut de modération
d'un commentaire ne remplace pas ce parcours. Apple demande ces moyens ainsi
qu'un contact publié : [règle 1.2](https://developer.apple.com/app-store/review/guidelines/#user-generated-content).

L'application propose Google pour le compte principal. Aucune option Apple
n'a été trouvée et aucune exception évidente ne ressort du produit. Prévoir
Sign in with Apple ou démontrer une option équivalente conforme ; le texte
actuel impose des caractéristiques de confidentialité, pas seulement un nom de
fournisseur : [règle 4.8](https://developer.apple.com/app-store/review/guidelines/#login-services).

Aucun parcours de suppression du compte n'a été trouvé. Il doit être initiable
dans l'app et traiter les données associées, dont l'UGC. Un traitement différé
est permis avec information et confirmation ; imposer un email au support ne
suffit pas pour ce type d'app. Sources : [suppression de compte](https://developer.apple.com/support/offering-account-deletion-in-your-app/).

Prévoir le lien de confidentialité accessible dans l'app, les déclarations de
données et un accès de revue opérationnel : [règles de revue](https://developer.apple.com/app-store/review/guidelines/).
Les URLs et informations effectivement configurées dans App Store Connect n'ont
pas été inspectées.

Depuis le 28 avril 2026, Apple demande Xcode 26+ avec le SDK iOS 26+ pour les
uploads App Store Connect. Le contrôle natif local réussi ne vérifie pas le
SDK du build EAS : [exigence SDK](https://developer.apple.com/news/upcoming-requirements/).
La version minimale iOS ciblée par l'app est une question distincte.

## Ce qui est réutilisable pour la passe visuelle

| Surface | Existant | Travail ciblé |
| --- | --- | --- |
| Navigation | React Navigation, quatre tabs, stack racine et stack profil ; Expo Router sert d'entrée | Floating tab bar, padding partagé, accessibilité, clavier ; fiche et scan sont déjà hors tabs |
| Carte | Clusters, zoom, sélection, bottom sheet et mode liste | Nom multiligne, aperçu photo, CTA distincts fiche/itinéraire, erreur/vide/loading explicites |
| Fiche café | Actions, photos, infos, tags, likes, favoris, commentaires | Réordonner, filtrer les tags techniques, horaires repliables, lecture accessible |
| Home | Articles réels et hero | Retirer `dataForPacks` factice et les actions vides ; composer cafés, articles et progression réels |
| Profil | Avatar fournisseur, nom public, anneaux Pass partagés | Édition réelle, états de sauvegarde, éventuellement avatar choisi |
| Studio | Import, édition café/photos/horaires, articles, opérations et SSE | Modération et éventuelle résolution de rattachement ticket/café |

Versions déclarées mobile : Expo `~54.0.37`, React Native `0.81.5`, React `19.1.0`,
React Navigation 7, Reanimated `~4.1.1`, bottom sheet `^5.2.6`. Blur, haptics,
image picker, expo-image et clusterer sont déjà déclarés. Aucune montée majeure
n'est justifiée par la seule refonte visuelle.

Le `useCafeFull` actuel ne lance pas de lecture : il compose le store. La fiche
affiche un skeleton tant qu'elle ne trouve pas de café, sans état d'échec de
chargement dédié. Un deep link vers un café absent du cache doit disposer d'un
chargement ciblé et d'un résultat « introuvable ».

Le réglage Notifications est actuellement un simple état React local. Le cacher
ou le rendre fonctionnel évite une préférence sans effet.

Sources principales : [navigation](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/navigation/RootNavigator.tsx),
[Home](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/features/home/screens/HomeScreen.tsx),
[Map](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/features/map/screens/MapScreen.tsx),
[fiche](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/features/cafes/screens/CafeDetailsScreen.tsx),
[paramètres](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/primary/react/features/profile/screens/AppSettingsScreen.tsx).

## Découpage métier recommandé pour les expériences

Décision du lot 02 : une expérience peut être publiée sans ticket.
`experienceContext` reste recommandé pour son cycle autonome de contenu,
brouillon, publication, médias et modération. Le ticket conserve son propre
parcours et participe à certains niveaux du Pass. Voir le
[contrat détaillé](../architecture/experience-pass-contracts.md).

| Concept | Propriétaire proposé | Frontière |
| --- | --- | --- |
| Vérification du ticket et déduplication | `ticketContext` | Résultat de vérification distinct d'une preuve de visite ; progression par événements, pas d'accès aux tables d'un autre BC |
| Référence café | Référence locale du consommateur | Alimentée par événements café ; le rattachement d'un ticket reste conditionnel à une fonction justificatif validée |
| Expérience et références de ses médias | `experienceContext` | Auteur, café, publication non vide et visibilité ; aucun ticket requis |
| Progression Pass | Sous-module de `userApplicationContext` | Politique versionnée, contributions locales ticket/experience, niveaux ; SQL transverse supprimé du fonctionnement normal |
| Nom et avatar publics | `userApplicationContext` | Projection locale du profil dans les consommateurs |
| Commentaire, like du lieu | `socialContext` | Ne deviennent pas automatiquement des visites |
| Favori privé | `userApplicationContext` | Le produit possède déjà `savedCoffeeWl`, distinct du Like |
| Stockage d'objets | Adaptateurs techniques | S3 ne devient pas un domaine ni une table métier partagée sans propriétaire |

Contrats de progression implémentés côté consommateur : faits versionnés de contribution Experience
(id, auteur, café, admissibilité, version) et de résultat ticket. Ne pas transmettre
l'OCR ou les informations de paiement au Pass ou à Experience. Les références
métier consommées sont distinctes des vues de feed et des compteurs affichés.

Définir retrait de contribution, ticket supprimé/corrigé, utilisateur supprimé,
café archivé, concurrence et replay désordonné. La révocation d'un justificatif
facultatif ne supprime pas automatiquement une expérience libre. Aucun rattachement
des anciens tickets à partir du seul nom du commerçant.

Le Pass actuel compte commentaires et likes. Sa refonte est demandée par le
produit : versionner la politique, prévoir les acquis existants et retirer les
seuils de repli mobiles. Les seuils nouveaux restent à valider ; aucune conversion
automatique de commentaire en expérience ou visite vérifiée.

## Médias : socle existant et travail restant

Les assets café/article ont déjà stockage S3 et résolution des URLs. Les chemins
inspectés utilisent des uploads serveur et des signatures de lecture. Ils ne
constituent pas un protocole d'upload direct utilisateur confirmé.

Pour une première tranche : une photo maximum, format final normalisé, miniature
et avatar carré. L'owner métier autorise l'intention ; le mobile conserve un fichier
local durable et un état d'upload ; le serveur valide et transforme l'objet avant
de le rendre disponible. Publication seulement avec média disponible. Reprise
après expiration d'URL, annulation, nettoyage et remplacement d'avatar doivent
être des cas normaux, sans gros blob dans l'outbox JSON.

Une URL présignée est réutilisable jusqu'à expiration et peut remplacer l'objet
ciblé. Prévoir un emplacement temporaire puis une référence finale protégée,
ou un mécanisme équivalent qui évite de modifier un média après validation.
La confirmation cliente seule ne prouve ni le type réel, ni la taille, ni
l'absence d'EXIF : [comportement S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html).

## Contrats et persistence à prévoir

| Capacité | Contrat actuel | Delta envisagé |
| --- | --- | --- |
| Cafés | `/api/coffees`, détail, photos, horaires | Réutiliser ; pas de `homeContext` |
| Profil | `/auth/me` | Lecture propriétaire et commande d'édition avec `commandId` |
| Tickets | `/api/tickets/verify`, `/{id}/status` | Autorisation propriétaire, historique paginé, rattachement métier |
| Social | `/api/social/comments`, likes | Signalement, blocage, masquage/restauration et lectures filtrées |
| Expériences | Absent | Commandes + lectures café/moi, pagination stable, SSE de projection |
| Médias utilisateurs | Absent | Intention, validation asynchrone, état disponible/rejeté, suppression |
| Compte | Login/refresh/me | Connexion conforme et suppression coordonnée des données/sessions |

Le serveur utilise `schema.sql` ; aucune migration Flyway n'a été trouvée.
Le profil prod désactive l'initialisation SQL. Introduire une baseline et des
migrations ordonnées avant des données durables, avec restauration testée ;
ne pas déduire du démarrage local que le déploiement appliquera le nouveau schéma.

Migrations possibles, selon périmètre retenu : propriétaire/statut des commandes,
rattachement ticket et déduplication, expériences et références locales,
métadonnées média par propriétaire, signalements/blocages/actions, suppression de
compte. Ajouter les index de pagination et contraintes d'unicité avec les slices.
Déployer de façon additive ; un rollback applicatif ne doit pas supprimer les
données créées par la version précédente.

## FlowAtlas : résultat de l'évaluation

Oui, le MCP est utilisable immédiatement sur les deux clients TypeScript.
Le guide [architecture-exploration](/Users/nicolasmaldiney/FlowAtlas/.codex/skills/architecture-exploration/SKILL.md)
a été appliqué : découverte ciblée, contexte borné, lecture des sources, puis arrêt
de l'expansion lorsque le territoire utile est identifié.

| Essai réel | Résultat | Enseignement |
| --- | --- | --- |
| Mobile `uiLikeToggleRequested` | 10 nœuds, 11 relations, 3 615 octets, projection complète aux limites demandées | Très utile pour retrouver optimisme, état Like et entrée outbox |
| Studio `existingCoffeePublishRequested` | 13 nœuds, 23 relations, 6 969 octets ; budgets atteints, 12 relations de frontière omises | Bon point d'entrée ; factory regroupant plusieurs opérations, pas un scénario isolé |
| Mobile `projectionSyncEventReceived` | Événement vers état technique uniquement | Ce Redux event n'est pas le déclencheur des GET ; ceux-ci partent aussi du callback |
| Mobile `projection.updated`, puis handler de sync | Réception détectée ; sorties du helper de routage absentes | Le code contient des refresh que le graphe ne restitue pas ici |
| Mobile `ticketRetrieval` | 6 nœuds, 7 relations, 2 474 octets | A ciblé le gateway, le thunk et le reducer avant l'ajout de l'historique paginé |
| Mobile `entitlementsRetrieval` | 4 nœuds, 3 relations, 1 651 octets | A confirmé le chemin handler → gateway → action → reducer ; la lecture directe a ensuite révélé l'ancien fallback UI hors de ce trajet |
| Lot 03 mobile `profileUpdateRequested` | 21 nœuds, 27 relations, 7 362 octets, projection complète | Retrouve précisément listener, optimisme, outbox, rollback/reconciliation et reducer |
| Lot 03 mobile `accountDeletionRequested` | 21 nœuds, 37 relations, 9 397 octets, projection complète | Retrouve confirmation/view-model, listener, statuts, sign-out et nettoyages Redux ; utile pour contrôler les fuites inter-comptes |
| Recherches initiales `State:coffee`, `Handler:Home` | Aucun résultat | Recherche lexicale des nœuds ; les états peuvent s'appeler `cfState`, les écrans ne sont pas des handlers |

Les premières recherches vides ne signifiaient donc pas que le connecteur était
en panne. Les résultats sont dans `structuredContent` ; lire seulement le message
texte d'accusé de réception conduit à des appels répétés inutiles.

Limites : analyse TypeScript/Redux, pas d'adaptateur Java/Spring dans la version
inspectée ; pas de preuve des autorisations, transactions, SQL, comportement
runtime ou rendu visuel. `complete: true` signifie complétude de la projection
selon les limites de l'outil, pas complétude de la fonctionnalité dans le code.
Les handlers agrégés peuvent mêler des opérations voisines sans prouver leur
causalité. Les liens et chemins manquants doivent rester explicitement inconnus.
Sur le lot 03, la projection n'a pas montré l'appel HTTP concret de suppression
porté par `UserRepo`, ni le routage HTTP de `User.Profile.Update` derrière le
processeur d'outbox générique. Elle ne peut pas non plus prouver le bouton natif
Apple, SecureStore, les garanties backend Java ou les suppressions SQL.

Économie attendue : moins de recherches globales et de fichiers ouverts sur une
intervention Redux ciblée. Aucun pourcentage de coût ou de vitesse n'est établi
par cet audit. Le dépôt FlowAtlas documente un essai antérieur avec 21,9 % de
tokens d'entrée en moins, mais il compare MCP et CLI sur une tâche et ne permet
pas de promettre ce gain pour Fragments ni sur la facturation réelle.

Méthode proposée pour les prochains lots : un événement d'entrée connu → une
projection de 8–16 Ko maximum → sources et tests utiles → recherche complémentaire
sur les frontières absentes. Utiliser directement `rg` pour une modification UI
locale ou du Java. Le MCP conserve des graphes vérifiés en session et peut
réutiliser jusqu'à quatre projets ; il reste nécessaire de revalider après edits.
Pour faire évoluer FlowAtlas, les gains les plus concrets seraient : résoudre
les appels de gateway selon le `kind` d'une commande générique, inclure les
déclencheurs écran/view-model, distinguer les branches provider et modéliser les
frontières natives. L'adaptateur Java annoncé complétera surtout l'analyse des
controllers, handlers, ports/adapters et événements ; transactions et SQL réel
devront malgré tout rester prouvés par les tests.

Sources : [README FlowAtlas](/Users/nicolasmaldiney/FlowAtlas/README.md),
[évaluation antérieure](/Users/nicolasmaldiney/FlowAtlas/docs/evaluations/codex-mcp-exploration.md),
[composition MCP](/Users/nicolasmaldiney/FlowAtlas/src/mcp.ts).

## Plan unifié d'intervention

### Cible et règles de livraison

Livrer un même produit complet : découverte → café → expérience texte/photo,
avec un parcours parallèle de scan/historique de tickets ; expériences et tickets
alimentent une progression Pass selon la politique validée. Profil, modération,
confidentialité, suppression du compte et exploitation font partie du produit.
Le parcours passe ensuite par TestFlight,
corrections de recette et préparation de la soumission App Store.

Ne pas supprimer silencieusement expériences, photos, avatar ou conformité pour
tenir une date. Une livraison texte avant les médias est un jalon technique,
pas une réduction du périmètre final. Toute modification de périmètre sera
soumise au produit. Likes d'expérience, réponses imbriquées, feed algorithmique
et nouvelles fonctions sociales restent des extensions à valider séparément.

« Livré localement », « testé sur appareil », « disponible sur TestFlight » et
« prêt à soumettre » sont des statuts différents. Une suite verte ne suffit pas
à franchir les trois derniers. Les builds partagés, mutations AWS, migrations
d'environnement et soumissions doivent avoir leur périmètre d'autorisation
explicite ; le démarrage du lot 02 autorise son travail local, pas un déploiement.

### Ordre des lots et dépendances

| Lot | Intervention | Dépôts | Dépendances et sortie attendue |
| --- | --- | --- | --- |
| 00 | Isolation comptes/outbox/cache/SSE, propriétaire ticket, permission | Serveur + mobile | Implémenté et testé localement ; reprise legacy et recette appareil restent ouvertes |
| 01 | P0 erreurs techniques, rejets métier, vérité et autorisation des statuts | Serveur + mobile + contrats admin concernés | Intégré localement ; aucun rollback sur panne, rejet durable, idempotence et statut réservé au demandeur |
| 02 | Expériences libres, nouveau Pass, déduplication et historique tickets | Serveur + mobile | Implémenté localement côté historique/Pass/contrats ; producteur Experience attendu au lot 05 ; justificatif facultatif non retenu à ce stade |
| 03 | Identité, profil éditable et cycle de suppression du compte | Serveur + mobile | Implémenté et testé localement ; activation Apple Developer/SSM et recette iOS signée externes ; extension Experience/media obligatoire aux lots 05/06 |
| 04 | Modération de l'UGC existant et outillage opérateur | Serveur + mobile + Studio | Implémenté et testé localement ; signalement, blocage/déblocage, filtrage, masquage/restauration et historique opérateur utilisables. Contact public réel, opérateur/SLA et recette déployée restent externes |
| 05 | Expériences texte de bout en bout | Serveur + mobile + Studio | Implémenté et testé localement ; brouillon, publication sans ticket, édition, suppression, listes café/moi, synchronisation, modération, Pass et suppression de compte intégrés |
| 06 | Photos d'expérience et avatar choisi | Serveur + mobile + Studio | 03/05 ; pipeline média sécurisé, upload/reprise, validation réelle, remplacement, modération et nettoyage |
| 07 | Fondations UI, floating tab bar, carte et fiche café | Mobile, contrats serveur si manque constaté | Fondations préparables après 01 ; intégration finale avec 05/06, navigation et états complets |
| 08 | Home réel et cohérence profil/Pass/expériences | Mobile + lectures serveur utiles | 02/03/05/06/07 ; contenu réel, progression cohérente, sections utiles sans localisation ou activité communautaire |
| 09 | Durcissement transversal, récupération des données et préparation exploitation | Serveur + mobile + Studio | 01 à 08 ; sécurité, offline, accessibilité, migrations/restauration, observabilité et recette complète |
| 10 | TestFlight, corrections et préparation App Store | Ensemble + ressources de revue | 09 ; parcours réels validés, blocages levés, build et dossier de revue cohérents |

Les dépendances fixent ce qui doit être disponible, pas un nombre de jours par
ligne. Après validation, relever tôt les prérequis externes Apple, signature,
build natif, support, confidentialité et AWS pour éviter de les découvrir au lot
10. Un build interne diagnostique peut précéder la fin des fonctionnalités ; il
ne vaut pas validation de release. Les finitions UI indépendantes pourront être
intercalées après accord sur les écrans, sans court-circuiter les P0.

### Lot 01 — Erreurs techniques, rejets métier et statuts canoniques

Ce lot est distinct de l'isolation déjà réalisée. Il est implémenté, validé et
intégré localement dans les branches `release/app-store` backend et mobile.
Le produit a autorisé le démarrage du lot 02.
Il doit couvrir les commandes mobiles existantes (tickets, commentaires, likes,
favoris), inventorier les consommateurs Studio du statut et fournir un contrat
réutilisable par profil, expériences, médias et modération.

Travaux backend :

- Supprimer les conversions générales d'exceptions techniques en rejets métier.
  Définir un résultat de rejet explicite et un code stable, sans détails internes
  ni secrets. Une classe d'exception ou un HTTP 4xx indistinct ne suffit pas à
  prouver un rejet métier.
- Associer chaque reçu de commande à son demandeur authentifié et à son type ;
  définir la détection d'une réutilisation du `commandId` avec une autre intention
  ou un autre propriétaire. Le demandeur ne vient jamais d'un champ client.
- Persister `REJECTED` de manière durable, même si la transaction métier est
  annulée. Définir les frontières transactionnelles et tester les courses ; une
  erreur d'infrastructure ne doit pas être enregistrée comme rejet métier.
- Enregistrer `APPLIED` pour une commande réussie sans nouvel événement métier,
  notamment une intention déjà satisfaite ; ne pas fabriquer un événement pour
  obtenir un statut. Aligner la signification et le moment du statut avec le
  contrat canonique et les consommateurs existants.
- Protéger `GET /commands/{commandId}` par le demandeur, via query/handler/read
  repository. Définir une réponse non révélatrice pour un id étranger ou inconnu,
  sans fuite du motif ni rollback indu du mobile ; droits admin explicites et
  séparés, sans confondre jeton opérateur et identité utilisateur.
- Traiter les reçus existants sans propriétaire par une stratégie de compatibilité
  ou backfill vérifiable. Ne pas attribuer leur propriété au premier demandeur.
  Fixer la rétention compatible avec les commandes longtemps offline.
- Avant le premier changement de schéma, cadrer baseline et migrations ordonnées
  sur le `schema.sql` existant, tester base neuve et base existante, documenter
  déploiement additif et restauration. Ne pas supposer que la prod applique le DDL
  au démarrage ; choisir et documenter le mécanisme reproductible.

Travaux mobile et contrats consommateurs :

- Erreur réseau, timeout, offline, 5xx, perte de socket : garder optimisme et
  commande, reprendre avec backoff et **le même `commandId`**. Une session expirée
  suspend/reprend l'envoi ; elle ne constitue pas un refus métier de l'intention.
- Rollback uniquement sur rejet métier explicite du backend ou statut canonique
  `REJECTED`. Retirer les heuristiques fondées sur un mot dans un message ou sur
  tous les 4xx. `PENDING` ou réponse non disponible ne signifie pas rejet.
- Garder `/commands/{commandId}` comme source canonique ; ACK socket opportuniste,
  SSE réservé à la fraîcheur des projections, suivi de GET pour les snapshots.
- Auditer retry/reconciliation Studio pour les contrats modifiés et vérifier
  l'absence de régression ; ne pas forcer un modèle mobile offline à l'admin.

Critères de sortie et tests obligatoires :

| Scénario | Résultat attendu |
| --- | --- |
| Panne DB, timeout ou HTTP 5xx à la soumission | Aucun rollback ni abandon de l'intention ; reprise avec le même id |
| Session expirée ou socket absent | Pas de rejet inventé ; reprise/polling après authentification valide |
| Rejet métier puis réponse HTTP perdue | Polling retrouve `REJECTED` et son code stable ; rollback une seule fois |
| Succès puis réponse perdue, double tap, retry concurrent | Effet métier unique et statut cohérent |
| Intention déjà satisfaite, aucun nouvel événement | `APPLIED` durable, pas d'attente infinie à `PENDING` |
| B demande le statut de A, ou rejoue son id | Pas de divulgation ni d'exécution sous B ; tests HTTP avec deux identités |
| Id inconnu, reçu ancien sans propriétaire | Comportement documenté, non révélateur, aucune réattribution implicite |
| Redémarrage entre écriture, statut et propagation | Pas de faux succès/rejet ; reprise idempotente démontrée |
| Soumission ticket appliquée puis vérification du reçu refusée | `APPLIED` de la commande et résultat métier du ticket restent distincts |

Preuves attendues : tests fake-first des règles, MockMvc des contrats, PostgreSQL
réel pour transaction/concurrence/reprise, tests mobile outbox/watchdog et
compatibilité Studio ; revue des frontières DDD et de l'absence d'appels réseau
dans les transactions. Ce lot est clos seulement sur ces preuves, pas par une
modification isolée de `WriteTicketController`.

Preuves obtenues à la clôture locale : 325 tests backend verts, dont transactions,
concurrence, migration legacy, HTTP à deux identités, no-op et séparation du
résultat ticket ; 59 suites et 240 tests mobile verts ; TypeScript et ESLint sans
erreur ; 27 fichiers/136 tests, contrat et build Studio verts. La recette sur un
backend réellement migré reste une preuve de déploiement du lot 09, pas une raison
de confondre implémentation locale et production.

### Lots 02 à 06 — Parcours métier complet et conformité intégrée

**02 — Expériences libres et nouveau Pass.** Publication autorisée sans ticket ;
certains niveaux du Pass nécessitent des tickets. Seuils et conservation des acquis
validés. Historique serveur paginé et consommation mobile livrés, projections
ticket fiabilisées face au replay, déduplication OCR exacte et retrait des
contributions définis. La progression est déplacée vers son propriétaire produit
avec références locales et événements primitifs versionnés.
Un justificatif ticket/café, s'il est retenu, aura ses propres règles de
rattachement/révocation ; il ne conditionne pas les expériences libres.

Preuves locales du lot 02 : 313 tests backend distincts verts sur la régression
complète et les suites ciblées PostgreSQL de migration, verrou, ordre/replay et identité des
sources ; verticale enveloppe ticket stable → inbox → projection Pass → GET JWT ;
60 suites et 246 tests mobile verts ; TypeScript, ESLint sans erreur (16 warnings
préexistants), carte Redux et configuration native de release validés. La
verticale issue d'un vrai producteur Experience reste explicitement une preuve
du lot 05.

**03 — Identité et compte.** Implémenté localement. Le profil produit et sa query
JDBC sont dans `userApplicationContext`; l'ancien `/auth/me` transverse est retiré.
Le nom public suit une commande offline-first avec validation domaine, outbox,
rejet métier et réconciliation. Sign in with Apple utilise le bouton Expo natif,
une validation/exchange backend, des credentials de révocation chiffrés AES-GCM
et une configuration SSM documentée. La suppression confirmée crée un processus
durable : chaque contexte actuel efface ses propres données, publie son
acquittement via outbox/SQS/inbox, puis le coordinateur termine sous verrou. Les
refresh tokens sont révoqués, l'identité est anonymisée et un ancien access token
est refusé après traitement auth. `APPLIED` signifie demande durablement acceptée,
pas effacement global terminé. Experience et médias devront rejoindre ce même
processus aux lots 05/06 ; avatar reste au lot 06.

Preuves locales du lot 03 : 339 tests backend distincts verts (suite complète,
puis test de migration additive ajouté et exécuté), dont
domaine/use cases, adaptateurs Apple, chiffrement, HTTP/JWT, PostgreSQL,
transactional outbox, SQS/inbox, duplication et verticale complète de suppression ;
64 suites et 255 tests mobile verts ; TypeScript, configuration native, carte
Redux et lint sans erreur nouvelle (un warning historique). L'activation réelle
du capability Apple, les secrets SSM et une recette de connexion/suppression sur
build iOS signé restent des prérequis externes.

**04 — Modération existante.** Implémenté localement sur les commentaires déjà
publics : politique de contenu, motifs de signalement, masquage immédiat pour le
signalant, blocage/déblocage et lectures filtrées, y compris cache/offline. Studio
dispose d'une file avec aperçu/auteur/motif/compteur, décisions masquer/restaurer,
motif opérateur, statut canonique et historique d'opération. Les opérations suivent
UI → use case/listener → commande du `socialContext` propriétaire → outbox/SQS →
projection ; aucun contrôleur admin ne mute une table ou un read model métier.
Le runbook documente la prise en charge quotidienne et urgente, mais le vrai
contact public, l'opérateur désigné et la preuve de traitement sur environnement
déployé restent des prérequis humains, pas des valeurs à inventer dans le code.

Preuves locales du lot 04 : 354 tests backend verts, dont règles de domaine,
HTTP/JWT, PostgreSQL, transactional outbox, projection idempotente, restauration,
effacement de compte et contrats Studio ; 67 suites et 264 tests mobile verts,
dont outbox offline-first, classification des erreurs HTTP, filtrage personnel et
rafraîchissement SSE par identité ; 30 fichiers et 143 tests Studio verts, dont
gateway HTTP, workflow de décision/statut et rendu de la file. TypeScript, contrat
OpenAPI généré, build Studio de production, configuration native, carte Redux et
lint sont validés ; le lint mobile conserve un unique warning historique sans
nouvelle erreur.

Traçabilité Git locale : backend `4273d42` fusionné dans `release/app-store` par
`319f70f` ; mobile `dff610a` fusionné dans `release/app-store` par `c7fdce3` ;
Studio `78b3d72` fusionné dans `main` par `32a91d9`. Aucun push ni déploiement
n'est inclus dans cette clôture.

**05 — Expériences texte.** Implémenté localement. `experienceContext` possède
les agrégats Experience et ExperienceReport, les commandes authentifiées et les
projections JDBC. Une expérience peut être brouillonnée puis publiée, modifiée
ou supprimée sans ticket ; auteur et café sont des invariants et l'existence du
café repose sur une référence locale alimentée par événements. Les faits primitifs
versionnés passent par outbox, destinations SQS et inbox ; les décisions reçues
hors ordre ne peuvent pas rétablir un ancien état. Les handlers de query et de
projection dépendent de ports, jamais de JDBC.

Le mobile ajoute `experienceWl`, son mapping transport explicite, lectures café/
moi, cache privé, SSE→GET, optimisme, outbox durable et réconciliation canonique.
La fiche café permet brouillon ou publication, puis édition/suppression ; la liste
personnelle expose les deux états. Une panne technique conserve la commande et
seul un rejet explicite déclenche le rollback. Signalement et blocage masquent
immédiatement le contenu. Studio agrège les files commentaire/expérience et route
la décision vers le contexte propriétaire. L'effacement Experience participe au
processus de suppression du compte et le producteur réel alimente désormais le
Pass.

Preuves locales du lot 05 : 370 tests Maven verts (0 échec, 2 tests
d'infrastructure ignorés lorsque le socket Docker n'était pas accessible), puis
8 tests d'intégration PostgreSQL/Testcontainers verts ciblant Experience,
migration réentrante et suppression de compte. Ils couvrent les invariants et
handlers, HTTP/JWT, création sans ticket, cycle brouillon→publication→
suppression, refus d'un autre propriétaire, outbox/enveloppe/routage/inbox,
pagination, replay et ordre de modération, contribution Pass, signalement/blocage,
file Studio et effacement de compte. Le mobile valide 70 suites et 270 tests,
TypeScript, lint (aucune erreur, un avertissement historique), carte Redux et
configuration native. Studio valide 30 fichiers et 144 tests, le contrat OpenAPI
et le build de production en mode OAuth sûr. Aucun média, déploiement, migration
d'environnement ni recette appareil n'est revendiqué dans ce lot.

Traçabilité Git locale lot 05 : backend `e6bc6c5` fusionné dans
`release/app-store` par `f718240` ; mobile `a9423d5` fusionné dans
`release/app-store` par `30efde5` ; Studio `4b85a50` fusionné dans `main` par
`eddf3d5`. Aucun push ni déploiement n'est inclus dans cette clôture.

**06 — Photos et avatar.** Réutiliser les adaptateurs techniques S3, mais garder
les références et autorisations chez leurs propriétaires métier. Livrer upload
présigné vers un emplacement temporaire privé, contrôle serveur taille/type réel,
normalisation/redimensionnement/suppression EXIF, référence finale protégée et
statut vérifiable. Pas de publication avec un média non disponible. Le mobile
conserve le fichier durablement, affiche aperçu/progression, gère annulation,
retry, URL expirée, passage offline et changement de compte. Brancher édition et
suppression de l'avatar, propagation du profil, modération des médias, remplacement
de l'ancien objet et nettoyages différés/orphelins/suppression du compte. Valider
IAM minimal, CORS et absence d'accès public implicite. La quantité de photos est
à confirmer ; commencer techniquement par une photo ne décide pas du plafond final.

### Lots 07 à 10 — Expérience utilisateur et publication

**07 — Navigation, carte, fiche.** Conserver React Navigation et les clusters
existants. Finaliser tokens/contraste, floating tab bar avec safe areas, fallback
opaque, clavier, VoiceOver et réduction des effets. Clarifier sélection carte,
bottom sheet, fiche et itinéraire ; préserver zoom/position au retour. Gérer
localisation refusée, chargement/erreur/vide et noms longs. Réorganiser la fiche
identité/actions/caractéristiques/expériences/infos/contenu lié ; filtrer les tags
techniques, replier les horaires, traiter photos absentes et deep links hors cache.

**08 — Home et cohérence produit.** Composer articles, cafés, progression et
expériences réels ; pas de `homeContext` ni de nouveau maître des données. Prévoir
nouvel utilisateur, utilisateur actif, aucune localisation et aucun contenu
communautaire. Retirer packs factices, actions vides et préférences sans effet,
sans retirer une fonctionnalité attendue sans accord. Finaliser profil/avatar,
historique personnel et Pass suivant les règles du lot 02 ; chaque section doit
avoir une destination utile et des états de chargement/erreur explicites.

**09 — Durcissement et exploitation.** Tester le parcours complet sans socket,
offline/reconnexion, fermeture/reprise de l'app, deux comptes, réseau lent et
session expirée. Contrôler accessibilité, performances listes/carte/images,
pagination/index, rate limits, validations, logs sans secrets, SQS/DLQ/replay,
inbox/outbox, sauvegarde/restauration, migrations et nettoyage S3. Résoudre la
récupération des snapshots legacy conservés au lot 00, avec preuve de propriété
et accord explicite avant toute opération irréversible. Retester la suppression
du compte avec expériences, avatar, médias, caches et sessions réellement présents.
Les restrictions de blocage/masquage doivent survivre à une relecture depuis le
cache. Fermer les écarts constatés ; une simple checklist ne constitue pas la preuve.

**10 — TestFlight puis App Store.** Valider un build natif compatible avec les
exigences à revérifier au moment de la soumission. Recette sur petit/grand iPhone
et versions iOS retenues, puis campagne TestFlight et corrections. Préparer compte
de revue, ticket/QR exploitable, environnement disponible, procédure de scan,
captures réelles, support, confidentialité, déclarations de données et textes de
permissions. Vérifier les exigences Apple applicables et la cohérence entre app,
backend, Studio, politiques et dossier. La soumission n'est proposée qu'après
levée des blocages et accord explicite ; l'acceptation Apple reste externe.

### Décisions ciblées à prendre avant les lots concernés

Le choix « faire les deux parcours » et les trois décisions du lot 02 sont acquis.
Restent à valider avant les implémentations concernées :

- Avant 02/05 : seuils exacts du nouveau Pass, conservation des acquis, rôle futur
  des commentaires et éventuel justificatif facultatif ticket/café.
- Lot 03 : nom public, anonymisation et fournisseurs sont implémentés ; les
  informations de support/confidentialité restent à finaliser avant diffusion.
- Avant 04 : responsabilité de modération, motifs/actions et traitement opérateur.
- Avant 06 : formats réellement supportés, quantité/taille des photos, coût et
  paramètres d'exploitation S3 ; photos et avatar restent dans le périmètre.
- Avant 07/08 : validation des écrans/parcours de référence et textes produit.
- Avant diffusion : récupération des données legacy, environnements, matrice
  appareil et autorisations de build/déploiement/soumission.

### Suivi du temps, de l'avancement et prévision glissante

Ne pas inventer la durée déjà écoulée de la tranche 00 : elle n'a pas été mesurée
de façon fiable. À chaque prochain lot, relever début/fin, sessions actives,
attentes de tests/builds ou d'accès, retour produit et corrections de recette.
Les durées écoulées ne sont pas automatiquement du temps actif ; les attentes
simultanées ne doivent pas être additionnées deux fois.

| Lot | État au moment de cette révision | Temps constaté | Preuve / reste à faire |
| --- | --- | --- | --- |
| 00 | Implémenté et testé localement | Non mesuré | 232 tests mobile, 41 backend ciblés, TypeScript/carte Redux OK ; legacy et appareil ouverts |
| 01 | Clos et intégré localement ; validation produit en cours | 43 min de fenêtre observée, de 13:47 à 14:30 CEST ; temps actif non instrumenté séparément | Reçu durable, isolation, migration/backfill, contrats mobile et compatibilité Studio ; déploiement non réalisé |
| 02 | Clos et intégré localement ; Astra High puis Sol High | Fenêtre observée d'environ 14:39 à 15:54 CEST, incluant échanges, implémentation et attentes de tests ; temps actif non isolé | Backend `3ff0ca8`/merge `94d8082`, mobile `588f825`/merge `e4952fc` ; producteur Experience différé au lot 05 |
| 03 | Implémenté et testé localement ; Sol High | Fenêtre observée d'environ 86 min, de 15:54 à 17:20 CEST, incluant exploration, implémentation et attentes de tests ; temps actif non isolé | Backend/mobile complets dans le périmètre actuel ; capability/SSM/build iOS externes, avatar et extensions Experience/media différés explicitement |
| 04 | Clos et intégré localement ; GPT-5.6 Sol High | Fenêtre observée d'environ 75 min, de 17:20 à 18:35 CEST ; temps actif non isolé | Verticale commentaire/blocage/Studio complète ; exploitation humaine et recette déployée externes |
| 05 | Clos et intégré localement ; GPT-5.6 Sol High | Fenêtre observée d'environ 1 h 45, de 18:35 à 20:20 CEST, incluant implémentation, corrections, Docker/builds et validations ; temps actif non isolé | Verticale Experience texte complète sur serveur/mobile/Studio ; médias, environnement déployé et recette appareil restent hors lot |
| 06 à 08 | Planifiés, non démarrés | Non démarré | Médias/avatar puis navigation/carte/fiche et Home |
| 09 | Planifié, non démarré | Non démarré | Preuves de durcissement et recette intégrée |
| 10 | Planifié, non démarré | Non démarré | TestFlight, corrections, dossier et autorisation de soumission |

Le suivi fonctionne en boucle : **projection → réalisation observée → analyse
des écarts → nouvelle projection**. Il ne faut ni attendre la fin du chantier
pour estimer, ni conserver une première estimation devenue irréaliste.

Au démarrage de chaque lot, après autorisation de l'implémentation :

- Enregistrer une estimation initiale sous forme de fourchette, son périmètre,
  ses hypothèses, dépendances et niveau de confiance ; séparer effort actif et
  délai calendaire, qui dépend aussi de la disponibilité et des attentes externes.
- Ouvrir une ligne de suivi du lot et une projection du reste du programme.
  Les lots lointains gardent une fourchette plus large lorsque leurs contrats ou
  prérequis ne sont pas encore assez connus.

À chaque fin de lot, et en cours de lot si une découverte change sensiblement
la durée ou les dépendances :

- Comparer l'estimation initiale au travail constaté et estimer le reste à faire
  du lot en cours ; pour un lot non fini, comparer au total réestimé, pas au seul
  temps déjà consommé. Inclure tests, corrections et recette, pas seulement le code.
- Expliquer l'écart : complexité, fondation réutilisée, régression, attente
  externe, décision produit ou changement de périmètre. Garder ce dernier visible
  plutôt que le présenter comme une simple variation de vitesse.
- Reprojeter le programme à partir du reste du lot et des lots suivants, de leurs
  dépendances, des attentes connues et d'une marge d'incertitude explicite. Ne pas
  extrapoler mécaniquement la vitesse d'un correctif à un chantier média ou de revue.
- Présenter une fenêtre révisée de fin de développement/recette et, séparément,
  les attentes TestFlight/App Review. Si les disponibilités ne permettent pas une
  date crédible, présenter d'abord une fourchette d'effort restant et les conditions
  nécessaires pour la convertir en calendrier.
- Soumettre au produit tout arbitrage de périmètre ou de priorité qui dépasse
  le plan validé ; une nouvelle projection n'autorise pas à retirer une fonction.

Conserver chaque projection datée : ne pas écraser la référence initiale avec
la dernière estimation, sous peine de masquer les écarts. Le bilan de chaque lot
lie les preuves (tests, captures si UI), les risques et la projection révisée.

Gabarit du journal à compléter sans valeurs inventées :

| Révision / date | Lot et périmètre | Estimation initiale du lot | Réel actif / attentes | Reste du lot réestimé | Projection du reste global | Écart, hypothèses et confiance |
| --- | --- | --- | --- | --- | --- | --- |
| Prévision 0 — 2026-09-11 13:47 CEST | 01, erreurs/rejets/statuts | Non consignée avant le premier patch ; ne pas reconstruire a posteriori | Fenêtre écoulée mesurée depuis 13:47, incluant compilation et attentes Testcontainers ; temps actif non chronométré | Revue, documentation et intégration Git | 4 à 8 jours concentrés d'implémentation locale pour 02 à 09, puis délais TestFlight/App Review séparés | Estimation encore peu fiable : le socle existant a accéléré 01, mais Experience, médias et modération sont plus larges |
| Prévision 1 — 2026-09-11 14:30 CEST | 01 clos et intégré localement | Référence ci-dessus conservée | Fenêtre 43 min, incluant deux suites backend complètes, tests ciblés/Testcontainers, mobile, Studio et intégration Git ; temps actif non isolé | Zéro en implémentation locale ; migration/recette reportées au durcissement | 4 à 8 jours concentrés d'implémentation locale pour 02 à 09, puis délais TestFlight/App Review séparés | 01 a été plus rapide grâce au socle existant et aux tests ; confiance faible à moyenne, sans extrapolation aux décisions Experience, médias, UGC et natif |
| Prévision 2 — lot 02, premier point 2026-09-11 14:39 CEST, avant code fonctionnel | 02A cadrage, 02B historique, 02C Pass/doublons, 02D contrats | 30–60 min de cadrage ; 1–2 jours concentrés d'implémentation après validation | Exploration démarrée ; temps actif non isolé | Seuils/migration à valider puis slices testées | Référence 4–8 jours conservée, à recalculer après cadrage et première slice ; TestFlight/App Review séparés | Ticket retiré du chemin obligatoire ; nouvelle politique Pass, ownership et replay ajoutent du travail ; confiance faible |
| Prévision 3 — lot 02, clôture 2026-09-11 15:54 CEST | 02A–02D, hors producteur Experience du lot 05 | Référence 1–2 jours après cadrage | Fenêtre observée d'environ 75 min depuis le premier point, incluant échanges et attentes Testcontainers ; temps actif non isolé | Zéro dans le périmètre 02 ; raccord producteur au lot 05 | 3–6 jours concentrés pour 03 à 09, puis délais TestFlight/App Review séparés | Socle eventing/inbox/projection largement réutilisable ; objectif quatre jours plausible, confiance moyenne-faible car médias, UGC, Apple et recette native restent plus incertains |
| Prévision 4 — lot 03, clôture locale 2026-09-11 17:20 CEST | Profil, Apple, suppression coordonnée des données actuelles | Incluse dans la référence globale 3–6 jours ; pas de fourchette isolée enregistrée avant code | Fenêtre observée d'environ 86 min depuis la clôture 02, incluant exploration, dépendance Expo, implémentation et suites complètes ; temps actif non isolé | Zéro pour le code local du périmètre actuel ; activation Apple/SSM/recette signée externes ; extensions Experience/media prévues | 2–5 jours concentrés pour 04 à 09, puis délais TestFlight/App Review séparés | Forte réutilisation command/outbox/SQS/inbox ; Apple et le processus transverse ont néanmoins demandé une vraie verticale. Confiance moyenne-faible avant UGC/médias/UI |
| Prévision 5 — lot 04, point avant suites complètes 2026-09-11 18:10 CEST | Modération commentaires, blocage personnel et file Studio | Incluse dans la référence globale 2–5 jours ; pas de fourchette isolée enregistrée avant code | Fenêtre observée d'environ 50 min depuis 17:20, incluant implémentation, génération de contrat, attentes Docker et tests ciblés ; temps actif non isolé | Suites complètes, revue architecturale et intégration Git | 1,5–4 jours concentrés pour 05 à 09, puis TestFlight/App Review séparés | Le socle command/outbox/projection a fortement accéléré la verticale ; médias et recette native restent les plus incertains. Confiance moyenne-faible |
| Prévision 6 — lot 04, clôture locale 2026-09-11 18:35 CEST | Modération commentaires, blocage personnel et file Studio | Référence 2–5 jours globale conservée ; aucune estimation isolée reconstruite | Fenêtre observée d'environ 75 min depuis 17:20, incluant trois suites complètes, contrôles statiques/build et attentes Docker ; temps actif non isolé | Zéro pour le code local du périmètre ; contact/support réel, opérateur/SLA, migration et recette déployée externes | 1,5–3,5 jours concentrés pour 05 à 09, puis TestFlight/App Review séparés | Réutilisation forte du pipeline de commandes et des projections ; Experience texte est maintenant la prochaine incertitude métier, puis médias et recette native. Confiance moyenne |
| Prévision 7 — lot 05, démarrage 2026-09-11 vers 18:35 CEST | Experience texte serveur/mobile/Studio, Pass, modération et suppression | 1 h 30 à 3 h de fenêtre locale annoncée avant implémentation | Implémentation et validations en cours ; temps actif non isolé des attentes Docker/build | Suites complètes, documentation et intégration Git | 1 à 3 jours concentrés pour 06 à 09, puis TestFlight/App Review séparés | Les contrats Pass/modération/outbox existants accélèrent le lot ; les médias/S3 et la recette native ne doivent pas être extrapolés à cette vitesse. Confiance moyenne |
| Prévision 8 — lot 05, clôture locale 2026-09-11 vers 20:20 CEST | Experience texte serveur/mobile/Studio, Pass, modération et suppression | Référence initiale de 1 h 30 à 3 h | Fenêtre observée d'environ 1 h 45 depuis 18:35, dans la fourchette, incluant corrections, attentes Docker et builds ; temps actif non isolé | Zéro pour le périmètre texte local ; médias et recette déployée restent explicitement ouverts | 1 à 2,5 jours concentrés pour 06 à 09, puis TestFlight/App Review séparés | Réutilisation forte des pipelines command/outbox/inbox, Pass et modération ; le pipeline S3, les traitements d'image et la recette native du lot 06 concentrent désormais l'incertitude. Confiance moyenne |
| Prévision 9 — lot 06, démarrage 2026-09-11 20:45 CEST | Photo d'expérience et avatar, stockage privé, reprise, modération et effacement | 2 h 30 à 5 h de fenêtre locale pour code et preuves automatisées ; AWS réel et recette appareil séparés | Exploration et cadrage démarrés ; temps actif non isolé | Verticales backend/mobile/Studio, tests, documentation et intégration Git | 0,75 à 2 jours concentrés pour 07 à 09 après le lot 06, puis TestFlight/App Review séparés | Une photo par expérience dans l'UI V1, collection ordonnée et plafond serveur configurables ; avatar unique. JPEG/PNG seulement tant que HEIC n'est pas décodé et normalisé côté serveur. Confiance moyenne-faible avant preuve du pipeline objet |
| Prévision 10 — lot 06, clôture locale 2026-09-11 22:05 CEST | Photo d'expérience et avatar sur backend/mobile/Studio, stockage privé, reprise, modération, SSE et effacement | Référence initiale de 2 h 30 à 5 h | Fenêtre observée d'environ 1 h 20 depuis 20:45, sous la fourchette, incluant implémentation, revue de fraîcheur SSE, trois suites complètes, builds et attentes Docker ; temps actif non isolé | Zéro pour l'implémentation locale ; IAM/CORS S3 réel, migration d'environnement et recette appareil/réseau lent restent au durcissement | 0,5 à 1,5 jour concentré pour 07 à 09, puis TestFlight/App Review séparés | Le socle outbox/command status/projections et les adapters Expo ont accéléré la verticale. L'absence de preuve AWS réelle interdit d'extrapoler la vitesse locale au déploiement. Confiance moyenne |
| Prévision 11 — lot 07, clôture locale 2026-09-11 22:24 CEST | Navigation flottante, sélection carte, itinéraire et hiérarchie de fiche café | 0,5 à 1,5 jour pour une passe UI contenue sans changement de contrat | Fenêtre observée d'environ 19 min depuis la clôture 06, incluant inventaire, implémentation, documentation, TypeScript, lint et deux régressions mobiles complètes ; temps actif non isolé | Zéro pour le code local ; contrôle sur appareils petit/grand iPhone et VoiceOver restent à la recette native | 0,25 à 1 jour concentré pour 08 à 09, puis TestFlight/App Review séparés | Le périmètre a été volontairement borné aux composants et lectures existants : ni refonte du Home, ni nouveau contexte, ni contrat backend. La vitesse ne préjuge pas de la recette native ni du durcissement réseau. Confiance moyenne |
| Prévisions suivantes — à chaque point de contrôle | Lot en cours ou terminé | Référence conservée | À mesurer | À réestimer, zéro seulement si clos | Nouvelle fourchette datée | Causes des écarts et changements depuis la projection précédente |

La tranche 00 reste « durée non mesurée » et ne sert pas de donnée de vitesse
inventée. Ce mécanisme permet de constater
progressivement si l'ensemble converge plus vite ou plus lentement que prévu,
sans annoncer une date garantie ni réduire implicitement le périmètre.

### Méthode d'intervention

Chaque tranche fonctionnelle reste bornée, avec changements séparés par dépôt,
contrats, tests, revue des frontières et stratégie de déploiement/retour arrière.
Utiliser les orchestrateurs `.agents` appropriés : backend command/query/
projection/SQS/external-adapter/process-manager ; mobile optimistic-command/
read-feature/bootstrap ; Studio command-workflow/projection-sync/security.
Préserver les modifications locales existantes ; pas de commit ou PR global
mélangeant des décisions indépendantes.

FlowAtlas sert à cibler les parcours Redux mobile/Studio : projection courte,
lecture des sources utiles, revalidation après modification. Ses limites sur
callbacks, middleware et Java imposent une vérification directe du code et des
tests pour autorisation, transactions et concurrence. Ne pas refaire un audit
global à chaque slice et ne pas promettre de pourcentage d'économie non mesuré.

Retour lot 04 : FlowAtlas a reconstitué le parcours mobile
`uiCommentReportRequested -> listener -> optimistic state/outbox -> processOutbox ->
CommentsWlGateway` en 22 nœuds et 41 arêtes, contexte complet (9 491 octets).
Il a donc permis de vérifier rapidement que l'intention de modération réutilise
bien le pipeline offline-first existant. La projection mobile
`blockedUsersRetrieval` (4 nœuds, 3 arêtes, 2 013 octets) retrouve le cycle du
thunk, mais pas les dispatches ni l'appel gateway contenus dans son callback.
Sur Studio, le graphe de la décision
retrouve correctement événements, listener et état (7 nœuds, 11 arêtes), mais
ne matérialise pas l'appel au `ModerationGateway` pourtant effectué par le
listener. Les évolutions prioritaires sont donc la résolution des appels externes
via paramètres de factory/interfaces injectées et l'analyse du corps des callbacks
`createAsyncThunk` ; la future couverture Java devra
ensuite relier contrôleur, commande, handler, agrégat, outbox, SQS et projection.

Retour lot 05 : la projection `uiExperienceCreateRequested` est complète selon
FlowAtlas avec 7 nœuds, 9 relations et 3 239 octets ; elle retrouve l'intention,
le listener, les quatre mutations optimistes et le reducer. Elle ne relie toutefois
pas `enqueueCommitted`, appelé dans une fonction locale `enqueue` du listener.
La projection centrée sur `enqueueCommitted` (20 nœuds, 19 relations, 6 550
octets) retrouve les producteurs directs historiques, mais omet elle aussi le
listener Experience, alors que la carte Redux statique et le test vertical le
voient. C'est un cas de test précis à ajouter à FlowAtlas : propagation inter-
procédurale d'un `api.dispatch` à travers un helper local capturant `api`.

Sur Studio, `moderationDecisionRequested` reste une projection complète de 10
nœuds, 15 relations et 4 328 octets. Elle confirme événements/listener/reducers,
mais ne montre ni le déclencheur React ni la branche `ModerationGateway` qui route
COMMENT et EXPERIENCE vers deux endpoints. Les prochaines améliorations utiles
sont donc la résolution des appels de ports injectés, des branches discriminées
par union TypeScript et des dispatches indirects. Même avec cette limite,
FlowAtlas réduit bien la lecture initiale Redux ; le code et les tests restent la
preuve nécessaire pour les transactions Java et les frontières réseau.

Retour lot 06 : la projection avatar Redux est complète avec 23 nœuds et 30
relations. Elle suit l'intention UI, le listener, la mutation optimiste, l'outbox,
le retry et le watchdog, ce qui a permis de contrôler rapidement le maintien du
fichier local pendant une incertitude technique. Elle ne matérialise toutefois
pas l'appel externe effectué via le port polymorphe `UserRepo`. Cette limite
rejoint le retour des lots précédents : la prochaine évolution la plus rentable
est de résoudre les appels d'interfaces/factories injectées et de les classer
comme frontières externes. La future analyse Java devra en priorité relier
controller, use case, transaction, aggregate, outbox, SQS/inbox et projection.

### Modèles Codex et niveau d'exigence

Le modèle est choisi et annoncé avant chaque lot afin d'utiliser le niveau le
moins coûteux qui conserve la fiabilité attendue. La référence est la
[documentation officielle OpenAI](https://learn.chatgpt.com/fr-FR/docs/models) :
Sol pour le travail complexe et ouvert, Terra pour le travail quotidien bien
cadré, Luna pour les transformations claires et répétables, Astra pour les
workflows les plus difficiles demandant un jugement soutenu. Le niveau de
raisonnement monte seulement avec la complexité et les compromis du lot.

| Lot | Modèle proposé | Raisonnement | Motif et point de contrôle |
| --- | --- | --- | --- |
| Audit et plan initial | GPT-6 Astra | High | Architecture, arbitrages et périmètre transverse ; réalisé |
| 00 — isolation/confidentialité | GPT-6 Astra | High | Investigation et sécurisation transverse ; réalisé |
| 01 — erreurs/rejets/statuts | GPT-5.6 Sol | High | Transactions, concurrence, sécurité et contrats offline ; utilisé du début d'implémentation à la revue et l'intégration |
| 02 — contrats Experience/Pass/historique | GPT-6 Astra pour 02A ; GPT-5.6 Sol pour 02B–02D | High | Astra pour les décisions produit et frontières ; Sol sélectionné par l'utilisateur pour l'historique, la politique, les migrations, les tests et l'intégration |
| 03 — identité/profil/suppression | GPT-5.6 Sol | High | Cycle de vie transverse et conformité ; Astra seulement pour une revue ciblée si nécessaire |
| 04 — modération UGC | GPT-5.6 Sol | High | Autorisations, cohérence serveur/mobile/Studio et exploitation |
| 05 — expériences texte | GPT-5.6 Sol | High | Verticale métier complète DDD/CQRS/offline |
| 06 — médias/avatar/S3 | GPT-5.6 Sol | High | Sécurité objet, reprise et nettoyage ; revue Astra ciblée possible |
| 07 — navigation/carte/fiche | GPT-5.6 Terra ou Sol | Medium à High | Choisir Terra si les écrans et contrats sont figés, Sol si des interactions transverses restent ouvertes |
| 08 — Home/cohérence produit | GPT-5.6 Terra ou Sol | Medium à High | Dépend du niveau de composition et des contrats restants |
| 09 — durcissement | GPT-5.6 Sol | High | Analyse des courses, sécurité, migrations et reprise |
| 10 — TestFlight/App Store | GPT-5.6 Sol, revue Astra ciblée | High | Corrections de release et jugement final ; pas d'Ultra par défaut |

Le choix sera réévalué avant chaque lot avec la projection de temps. Un lot peut
passer à Terra Medium lorsque ses décisions sont figées et ses critères de sortie
sont mécaniques. Luna reste réservé aux tâches réellement répétitives et bornées,
jamais à la conception de domaine, à la concurrence, aux autorisations ou aux
transactions. Une revue par un modèle plus fort est déclenchée par le risque,
pas systématiquement ; le modèle effectivement utilisé est enregistré dans le
journal du lot avec tout changement en cours d'exécution.

### Politique de tests et garde-fous architecturaux du chantier

La vitesse ne justifie aucune baisse de la politique existante. Chaque lot doit
prouver les couches qu'il modifie avec une pyramide cohérente :

- Tests unitaires purs du domaine et des reducers/selectors pour invariants,
  transitions et cas limites ; ports métier testés avec des fakes nommés.
- Tests de use cases/handlers/listeners avec fakes, sans mocks comme modèle par
  défaut du métier. Les mocks restent aux frontières techniques et natives.
- Tests d'infrastructure des adaptateurs réels : PostgreSQL/Testcontainers pour
  SQL, transactions, verrous et migrations ; sérialisation/enveloppes, inbox,
  outbox, SQS/LocalStack et S3 lorsque ces composants sont concernés.
- Tests HTTP MockMvc des contrats, authentification, autorisation, réponses non
  révélatrices et mapping des erreurs. Tests des adapters HTTP mobile/Studio.
- Tests verticaux complets front et back sur chaque parcours critique, avec la
  vraie composition applicative pertinente : intention UI, listener/use case,
  optimisme/outbox, contrat HTTP, command handler, persistence/statut, polling,
  projection puis GET/reconciliation. Un test unitaire de chaque extrémité ne
  remplace pas une preuve verticale.
- Tests de reprise : duplication, ordre, concurrence, redelivery, redémarrage,
  absence de socket, offline/timeout/5xx, session expirée et changement de compte.
  Les builds et recettes appareil complètent les tests automatisés lorsqu'un
  comportement natif est concerné.
- Suites de régression existantes serveur, mobile et Studio, vérifications de
  types/lint/cartes générées, puis tests ciblés et complets proportionnés au
  risque. Aucun test ne doit être neutralisé, rendu moins strict ou remplacé par
  une assertion cosmétique pour obtenir du vert.

Le respect architectural est un critère de livraison au même niveau que le
fonctionnel. DDD et hexagone restent rigoureux : ownership explicite par bounded
context, invariants dans agrégats/modèles de domaine, application orchestration
dans handlers/use cases, ports au centre, adapters aux frontières, contrôleurs
minces et dépendances orientées vers le cœur. Aucun aggregate, entity, repository,
handler, DTO interne ou table métier d'un autre BC n'est importé ou manipulé.
Les échanges inter-BC passent par contrats d'intégration primitifs versionnés,
outbox/SQS/inbox et projections locales ; une ACL primitive temporaire doit être
explicite, documentée et ne devient jamais une commodité silencieuse.

CQRS reste strict : JPA pour les écritures métier, JDBC/SQL explicite pour les
vues, aucun agrégat chargé sur le read side, aucune projection écrite depuis un
contrôleur ou utilisée comme invariant d'écriture. Les événements sont des faits,
les commandes des intentions et le statut de commande un reçu technique ; aucun
de ces trois concepts ne sert à simuler l'autre. Les appels externes ne tiennent
pas une transaction ouverte. Les workflows longs utilisent un processus durable,
pas `PENDING`, SSE ou WebSocket comme état métier. Kafka et Redis ne sont pas
introduits. Toute solution qui exige une perversion de ces règles est arrêtée,
documentée et remplacée par une alternative conforme avant de poursuivre.

**État actuel : lots 00 à 07 implémentés et testés localement ; le lot 07 est
prêt à intégrer. Il a utilisé GPT-5.6 Terra Medium, conformément au choix de
modèle : changements UI bornés, contrats et navigation métier figés. Il livre
une barre d’onglets flottante accessible, un parcours carte explicite et une
fiche café réordonnée, sans nouveau contexte ni changement backend. Les
configurations Apple/SSM et IAM/CORS S3 réelles, la recette native petit/grand
iPhone et VoiceOver, les migrations d'environnement et tout déploiement restent
explicitement ouverts.**
