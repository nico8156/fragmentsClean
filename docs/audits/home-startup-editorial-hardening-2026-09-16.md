# Home, démarrage et rangs éditoriaux — 16 septembre 2026

## Constats de l'audit

Le contrôle staging a trouvé 16 cafés publiés (Rennes 6, Paris 9, Lorient 1)
et 10 articles publiés, dont deux à la une, rangs 3 et 4. Observations datées,
pas constantes produit. Le catalogue public est global ; la position trie les
cinq cafés du Home sans filtrer les villes disponibles. Un ancien snapshot local
peut précéder la réponse réseau. Le délai n'a pas été chronométré sur appareil.

`featuredRank` est cohérent entre serveur, Studio et mapper mobile. Deux rangs
attribués donnent deux articles dans le hero. Aucun remplissage artificiel à cinq
articles. « À lire ensuite » conserve les trois plus récents publiés non mis à
la une, hors article déjà dans le hero. Le grand visuel et son bandeau au scroll
restent inchangés.

## Corrections

### Mobile

- Lectures de démarrage indépendantes en parallèle après initialisation auth et
  restauration du compte. Ne plus rejouer le cache/outbox legacy par-dessus le
  snapshot géré du compte. Relancer si le compte change pendant le warmup.
- Après connexion/restauration post-démarrage, actualiser aussi les lectures Home.
- Précharger les images utiles : hero, « À lire ensuite », avatar, première photo
  des trois expériences et cinq cafés sélectionnés. Quatre chargements simultanés
  maximum et clés stables des médias privés conservées.
- Préparation visuelle plafonnée à 1,5 seconde, sans attente hors ligne. Ce plafond
  concerne l'affichage, pas la durée des requêtes ; le réseau peut rester lent.
- Pull-to-refresh avec indicateur et résultat visibles. Un échec partiel ne produit
  plus « À jour ». Indication séparée du chargement du catalogue.
- « Cafés près de toi », espace protégé pour « Voir la carte », liste exhaustive
  intitulée « Tous les cafés ».
- Vignettes « À lire ensuite » corrigées : l'objet métier `url` n'était pas une
  source native `uri`. Elles utilisent désormais expo-image et son cache disque.

### Studio

- Rangs occupés désactivés avec le titre de leur propriétaire.
- Réservations locales pendant les commandes et protection contre deux changements
  simultanés du même article ; les autres articles restent indépendants.
- Erreur par article, relecture du catalogue après conflit/rejet/non-confirmation.
  Ignorer les réponses de liste dépassées par une requête plus récente.
- Seul APPLIED confirme le changement. Le serveur reste arbitre entre opérateurs.

### Backend

- Port dépôt : verrou transactionnel de l'article avant modification. Port de
  disponibilité : sérialisation PostgreSQL des attributions d'un même rang,
  y compris lorsque la place est libre. Index unique existant conservé.
- Conflit métier : reçu admin REJECTED dans une transaction indépendante du
  rollback ; ce même identifiant rejeté ne peut ensuite réussir.
- Extension limitée aux commandes admin de rang, avec identifiants générés serveur.
  Les reçus ne sont pas accessibles à un demandeur mobile non autorisé. Les erreurs
  techniques ne sont pas transformées en rejets métier.
- Aucun SQL cross-BC, aucune mutation directe des projections depuis Studio,
  aucune nouvelle migration. Domaine décisionnaire, adaptateurs responsables des
  mécanismes de verrouillage. Contrats mobile et SSE inchangés.

## Vérifications locales

- Mobile : **92 suites / 350 tests**, TypeScript, ESLint ciblé, carte Redux générée
  et vérifiée. Démarrage parallèle, changement de compte pendant les lectures,
  restauration multi-villes, plan image borné, clés privées, concurrence et refresh
  partiel couverts avec ports fake/adaptateurs techniques.
- Studio : **34 fichiers / 159 tests**, unitaires, UI et verticales à gateway fake ;
  TypeScript, build OAuth/public et vérification du bundle sans secret. Le fichier
  `.env` opérateur n'a pas été changé.
- Backend : `ArticleEditorialCommandHandlersTest`,
  `ArticleFeaturedRankConcurrencyIT`, `JdbcArticleAggregateRepositoryIT`,
  `StructuredArticleProjectionIT`, `AdminTokenSecurityTest`,
  `BoundedContextArchitectureTest`, `ArticleFeaturedRankChangedEventHandlerTest`,
  `ArticleCurationIntegrationPayloadTest`, `PublicArticleSerializationContractTest`.
  PostgreSQL réel : un gagnant/un rejet pour un rang, reçu conservé après rollback,
  deux modifications du même article avec versions successives. Pas de relance de
  toute la suite backend pour cette tranche ; pas de recette multi-applications.

## Recette et livraison restantes

1. iPhone, démarrage après auth en réseau lent puis hors ligne : cache utilisable,
   catalogue global ensuite, préparation non bloquante, avatar/anciennes photos.
2. Tirer depuis le haut du hero : indicateur visible et succès/échec/hors ligne
   compréhensibles ; bandeau au scroll inchangé.
3. Petit écran et grande police : titre/action accessibles, vignettes affichées.
4. Deux opérateurs attribuent la même place : un seul propriétaire, erreur claire
   pour l'autre, catalogue actualisé, autres articles utilisables.
5. Mobile : rangs réellement attribués, retrait/archivage puis actualisation.

À la livraison initiale : travail local sur trois branches `fix/*`, sans déploiement
ni build TestFlight. Après autorisation complémentaire, les trois branches ont été
intégrées et poussées sur `main`, puis Studio publié sur S3/CloudFront ; voir le
[reçu de publication](../deployment/home-editorial-studio-2026-09-16.md).
Le backend et le nouveau build mobile restent à déployer/distribuer.
Aucune migration supplémentaire. Retour possible aux artefacts précédents sans
rollback de schéma, mais avec les protections précédentes moins fortes.

Le préchargement ne garantit pas toutes les images immédiatement. Des requêtes
peuvent finir après changement de compte sans valider le warmup du nouveau compte.
Le chantier distinct conservation/restauration et purge reste en pause, non corrigé.

## FlowAtlas et suivi

FlowAtlas TypeScript a aidé à recouper les chemins Redux cache/catalogue et les
nouveaux états image. Le graphe retrouve action → reducer ; le producteur utilisant
un prédicat de comparaison d'états nécessite encore lecture du code. Une frontière
de graphe complète ne prouve pas l'exhaustivité du comportement runtime. Aucun scan
Java générique : les fixtures Java disponibles ne couvrent pas cette attribution.

Session Codex courante, sans délégation ni changement de modèle demandé pendant
la tranche ; réglage exact modèle/effort non attesté par les outils. Travail local
le matin du 16 septembre. Déploiement et recette iPhone sont exclus du temps local :
ne pas confondre code/tests terminés et délai de validation de la release.
