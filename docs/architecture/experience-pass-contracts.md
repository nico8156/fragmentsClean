# Lot 02 — Experience, tickets et progression du Pass

Date : 11 septembre 2026. Cadrage par GPT-6 Astra High, modèle indiqué par
l'utilisateur. Ce document distingue décisions produit acquises, recommandations
d'architecture et propositions encore à valider. Aucun contrat décrit ici n'est
présenté comme déjà implémenté.

## Décisions produit acquises

- Une expérience peut être créée et publiée **sans ticket**.
- Le Pass sera repensé autour des visites et expériences ; les anciens seuils
  tickets/commentaires/likes ne sont pas la cible à conserver par défaut.
- Des tickets validés seront nécessaires pour atteindre certains niveaux du Pass.
- Le parcours complet expérience, médias, profil, modération et publication
  reste dans le périmètre. Le ticket n'en est plus une étape obligatoire.

L'absence de ticket n'empêchera dans aucun cas la publication d'une expérience.
Les niveaux concernés et les seuils exacts restent à valider avec la table proposée.

## Ce que le code établit aujourd'hui

| Source | Fait constaté | Conséquence |
| --- | --- | --- |
| `ticketContext/.../models/Ticket.java` | Identité utilisateur, résultat OCR et statut, sans `coffeeId` ni identité de reçu | Un ticket confirmé n'atteste pas une visite d'un café du catalogue |
| `ticketContext/.../gateways/TicketVerificationProvider.java` | Commerçant, adresse, montant, date, lignes ; pas d'identifiant de reçu fiable | Le nom, la date et le montant ne suffisent pas à rejeter automatiquement deux reçus comme identiques |
| `ticketContext/read/pass/PassProgressPolicy.java` | Niveaux 3/5/10 tickets avec commentaires/likes ; libellés de capacités débloquées | Redéfinir ensemble progression, vocabulaire et droits d'accès |
| `JdbcUserEntitlementsProjectionRepository.java` | SQL sur projections ticket et social | Ne pas étendre cette dette pour intégrer Experience |
| Mobile `passViewModel.ts` et `entitlementWl.reducer.ts` | Seuils de repli côté mobile | Retirer ces politiques locales lors du passage au nouveau contrat |
| Mobile `useTicketsHistory.ts` | Liste composée uniquement depuis le cache | Ajouter une lecture serveur paginée et son parcours mobile |
| `JdbcTicketStatusProjectionRepository.java` | Upserts acceptation/completion sans garde de version ; admin UPDATE sans insertion | Un événement ancien peut rétablir un ancien statut ; traiter ordre, tombstones et reconstruction avant la nouvelle consommation |
| Mobile `ticketRetrieval.ts` | `FAILED_FINAL` assimilé à `REJECTED`, `DELETED` sans mapping dédié | L'historique devra distinguer échec technique, refus et suppression |

FlowAtlas a retrouvé `ticketRetrieval` et son chemin vers gateway/reducer avec
une projection de 6 nœuds, 7 relations et 2 474 octets. Les constats Java,
autorisation et sémantique des statuts viennent d'une lecture directe du code.

## Frontières recommandées

| Concept | Propriétaire | Responsabilité |
| --- | --- | --- |
| Expérience personnelle d'un café | Nouveau `experienceContext` au lot 05 | Auteur, café, contenu, brouillon, publication, édition, suppression, décisions de visibilité |
| Ticket | `ticketContext` | Analyse, résultat, historique privé, doublons établis et éventuel rattachement café |
| Progression Pass du membre | `userApplicationContext`, sous-module Pass | Politique produit, contributions reçues, compteurs, niveaux et migration de politique |
| Commentaires et likes existants | `socialContext` | Conserver leur identité et leurs commandes pendant la transition |
| Catalogue café | `coffeeContext` | Existence, identité et disponibilité du lieu |
| Nom public/avatar | `userApplicationContext` | Profil ; références locales dans les consommateurs |
| Médias | BC propriétaire du contenu + adaptateurs techniques | Autorisation métier locale, stockage et transformations aux frontières |

`experienceContext` reste justifié sans ticket : son contenu, ses médias et son
cycle de publication constituent un concept autonome. Ne pas ajouter de
`visitContext` ou d'agrégat Visit à ce stade : aucune action distincte de
déclaration de visite n'a été demandée. « Visite » décrit ici ce que raconte
l'expérience, avec un niveau de preuve explicite si le produit en introduit un.

Le Pass appartient à l'identité produit, pas à la vérification OCR. Son déplacement
hors de `ticketContext` est une refonte réelle à tester. Son endpoint public peut
rester `/api/users/me/entitlements` avec un contrat versionné ; un seul propriétaire
serveur doit le servir après migration. Les références ticket/experience sont
alimentées par événements versionnés et inbox, jamais par SQL entre BC.

## Expérience : règles et parcours proposés

```text
Fiche café / historique personnel
  -> Partager mon expérience
  -> texte et/ou photo
  -> aperçu
  -> publication
  -> fiche café + mes expériences
```

Le scan peut offrir un raccourci vers ce parcours lorsque le café est connu ;
ce raccourci reste facultatif. Aucun écran « scanne pour pouvoir publier ».

Proposition de modèle minimal : `experienceId`, `authorId`, `coffeeId`, texte,
références média, date de visite éventuellement déclarée, dates serveur et version.
Séparer `publicationStatus` (`DRAFT`, `PUBLISHED`, `DELETED`) de
`moderationStatus` (`VISIBLE`, `HIDDEN`) : restaurer après modération ne doit pas
republier un contenu supprimé par son auteur.

- Identité auteur extraite de la session ; modification/suppression par auteur,
  masquage/restauration par le workflow de modération autorisé.
- Brouillon vide possible ; publication avec texte non blanc ou média disponible.
- Café existant via référence locale métier, distincte de la projection de feed.
  Référence encore non synchronisée : attente/retry explicite, pas faux rejet.
- Une personne peut raconter plusieurs visites du même café. Pas d'unicité
  globale par couple utilisateur/café ou par ticket pour les expériences libres.
- Le même `commandId` ne crée pas une seconde expérience ; les limites anti-spam
  relèvent de la politique de publication/modération, pas d'une preuve inventée.
- Date de visite déclarée distincte de `publishedAt`, toujours attribué au serveur.
- Les commentaires existants ne sont ni supprimés ni convertis automatiquement.
  Le rôle de leur interface à côté des expériences doit être décidé au lot 05.

Un ticket facultatif ou un badge « justificatif » n'est **pas encore une fonction
validée**. Si retenu : propriétaire identique, café rattaché avec preuve suffisante,
ticket admissible, référence locale versionnée et absence d'exposition OCR/paiement.
Une révocation retire le justificatif ; elle ne supprime pas mécaniquement une
expérience dont la publication est autorisée sans ticket. Les infractions au
contenu restent du ressort de la modération.

## Pass : proposition produit à discuter

Les niveaux indiqueraient une progression, sans conditionner l'accès aux actions
nécessaires pour les obtenir. Publier une expérience serait possible dès le
premier café. Les likes ne constitueraient plus une preuve de visite.

Trois compteurs distincts :

- `publishedExperiences` : expériences publiées et visibles ; brouillons,
  masquages et suppressions exclus. Une modification ne compte pas à nouveau.
- `distinctExperiencedCoffees` : cafés distincts avec au moins une expérience
  comptabilisée. Dix publications dans un café ne deviennent pas dix cafés.
- `validatedTickets` : tickets confirmés admissibles, non supprimés et sans
  doublon établi. Un rescan du même reçu ne doit pas ajouter de progression.

Proposition de seuils, **non validée et non codée** :

| Niveau | Expériences publiées | Cafés distincts | Tickets validés |
| --- | ---: | ---: | ---: |
| Coffee Taster | 1 | 1 | 0 |
| Urban Explorer | 3 | 3 | 0 |
| Social Bean | 5 | 3 | 1 |
| Fragments Master | 10 | 5 | 3 |

Cette proposition permet de commencer immédiatement, valorise découverte et
retours dans les lieux appréciés, et rend le dernier niveau explicite. Elle
reste ajustable avant implémentation. Les deux derniers niveaux exigeraient
ainsi des tickets, tout en gardant un premier parcours accessible sans scanner.

Ne pas nommer `verifiedVisits` un compteur calculé à partir des expériences
libres. Le compteur de tickets n'est pas additionné aux expériences pour
prétendre compter des visites uniques.
Un rattachement explicite est nécessaire pour calculer une union dédupliquée.

Les niveaux peuvent exiger simultanément des expériences et des tickets sans
prétendre que ces tickets concernent les mêmes cafés. Un éventuel lien justificatif
à une expérience constitue une fonction distincte, encore à confirmer.

La protection contre le rescan reste nécessaire pour la progression : empreinte
d'entrée normalisée/versionnée pour le rejeu identique et identité de reçu fiable
si disponible. Une similarité commerçant/date/montant crée au plus une suspicion,
pas une preuve permettant un rejet automatique. Prévoir contrainte d'unicité
concurrente, traitement privé d'un conflit entre comptes et résolution d'ambiguïté.
L'empreinte OCR seule ne garantit pas l'unicité du reçu physique ; cette limite
doit rester visible tant que le moteur ne fournit pas de preuve plus forte.

Proposition pour les retraits : compteurs recalculés sur les contributions
actuellement admissibles ; badges déjà acquis conservés après suppression
ordinaire, révocables en cas de fraude ou décision de modération motivée.
Cette conservation, encore à valider, exige une distinction entre progression
courante et niveaux acquis, persistés avec la version de politique appliquée.
Un blocage personnel n'est pas une décision de modération globale et ne modifie
pas le Pass public de l'auteur.

Migration : préserver les anciennes données et niveaux avec leur `policyVersion`.
Ne pas convertir un ancien commentaire en expérience, ni un ticket non rattaché
en café visité. Documenter avant bascule les changements visibles pour un ancien
utilisateur ; pas de perte silencieuse de progression acquise.

## Contrats techniques à préparer

Experience publiera des faits primitifs versionnés : création, publication,
édition, suppression, masquage et restauration. Les consommateurs de progression
reçoivent seulement identité, auteur, café, admissibilité de la contribution et
version ; aucun besoin de texte, photo, OCR ou paiement pour compter.

Le sous-module Pass garde une contribution locale par source et identifiant,
avec version et admissibilité. Une suppression conserve un tombstone. Un événement
plus ancien ne réactive pas la contribution ; deux événements concurrents sont
sérialisés par utilisateur ou protégés par verrou optimiste et retry transactionnel.
La révision du Pass est propre au Pass, pas un maximum des versions des tickets.

Les noms et payloads définitifs seront inscrits au catalogue d'événements avant
le premier producteur. Déclarer les nouvelles destinations SQS/DLQ et consommateurs
inbox ; ne pas modifier silencieusement un payload existant. La fraîcheur mobile
reste `projection.updated` puis GET ; aucun état métier envoyé directement en SSE.

Historique privé proposé : `GET /api/users/me/tickets?cursor=...&limit=...`.
Query/handler/port de lecture JDBC, demandeur issu du JWT, liste résumé sans OCR,
référence d'image ni paiement. Le détail propriétaire existant reste distinct.
Pagination keyset avec ordre immuable et départage unique : ne pas prendre
`occurred_at`, modifié par les corrections admin, comme clé stable. Un ordinal
de première matérialisation de la projection est une option simple ; il décrit
l'ordre d'ajout à l'historique, pas la date réelle de visite. Cursor borné/versionné,
filtre propriétaire sur chaque page et `limit + 1` pour la suite.

Mobile : read use case, gateway avec mapping explicite, liste paginée en cache
privé, loading/empty/error/offline, refresh et chargement suivant depuis le VM.
Fusion des tickets locaux en attente sans écraser l'outbox ni le détail par une
réponse résumé. Les requêtes tardives d'une ancienne session sont ignorées.
Prévoir des tombstones/synchronisation pour retirer un ticket supprimé du cache ;
l'absence d'un ticket dans une page ne prouve pas sa suppression.

## Découpage d'implémentation et tests

| Slice | Livraison | Preuves obligatoires |
| --- | --- | --- |
| 02A | Décisions Experience/Pass et contrat de transition | Revue de ce document avec le produit ; pas de migration fonctionnelle |
| 02B | Projections ticket fiables + historique serveur/mobile | Versions désordonnées, suppression/replay, PostgreSQL, pagination/intercalage, HTTP deux identités, mapper, reducer et parcours VM → lecture → rendu d'état |
| 02C | Politique Pass validée, protection contre le rescan et nouveau propriétaire | Domaine pur, fakes de contributions, identité de reçu, inbox duplicate/redelivery, concurrence PostgreSQL, migration ancien profil, HTTP et consommation mobile sans seuil local |
| 02D | Contrats Experience nécessaires au Pass | Schémas primitifs/versionnés, sérialisation, architecture ; raccord effectif au producteur au lot 05 |
| Conditionnelle | Justificatif ticket/café, si retenu | Référence locale, matching ambigu, collision réelle vs suspectée, révocation, pas de double comptage |

En l'absence d'Experience au lot 02, ne pas annoncer 02C/02D validés de bout en
bout contre un producteur inexistant. Les tests fake-first préparent le contrat ;
la verticale réelle Experience → SQS/inbox → Pass → GET/mobile sera livrée et
vérifiée au lot 05. Aucune donnée synthétique ne sera exposée comme activité réelle.

Les parcours critiques ajoutent perte réseau, redémarrage, socket absent,
commande rejetée explicitement et changements de compte. Les tests unitaires ne
remplacent ni les adaptateurs réels ni les verticales front et back. Suites de
régression et garde-fous DDD/hexa restent ceux du dépôt.

## Estimation et état

Premier point chronométré : 14:39 CEST, après exploration initiale. Ne pas inventer
un temps actif depuis le changement de modèle. Estimation avant code fonctionnel :
30–60 min pour le cadrage 02A ; 1–2 jours concentrés pour 02B–02D après validation,
avec confiance faible tant que seuils, migration et politique de doublons sont ouverts.
Le justificatif conditionnel demanderait une estimation séparée après choix produit.

La fourchette globale précédente de 4–8 jours est conservée comme référence,
pas confirmée par cette exploration. Le retrait du ticket obligatoire réduit les
dépendances de publication ; la nouvelle politique Pass et la fiabilisation des
projections ajoutent du travail réel. Reprojeter après 02A et première slice testée.

État : exploration effectuée, règles produit ci-dessus enregistrées, propositions
révisables. Historique, refonte Pass et Experience non implémentés dans cette slice.

Sources principales :

- [Agrégat Ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/businesslogic/models/Ticket.java)
- [Politique Pass actuelle](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/pass/PassProgressPolicy.java)
- [Lecture actuelle des compteurs](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/secondary/repositories/JdbcUserEntitlementsProjectionRepository.java)
- [Projections ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/secondary/repositories/JdbcTicketStatusProjectionRepository.java)
- [Contrats d'intégration ticket](../../src/main/java/com/nm/fragmentsclean/platform/eventing/contracts/TicketIntegrationEvents.java)
- [Historique mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/viewModel/useTicketsHistory.ts)
- [Présentation Pass mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/viewModel/passViewModel.ts)
