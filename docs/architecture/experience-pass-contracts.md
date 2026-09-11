# Lot 02 — Experience, tickets et progression du Pass

Date : 11 septembre 2026. Cadrage 02A par GPT-6 Astra High puis implémentation
02B–02D par GPT-5.6 Sol High, modèles indiqués par l'utilisateur. Ce document
distingue le contrat produit validé, sa réalisation actuelle et ce qui dépend
encore du futur producteur `experienceContext`.

## Décisions produit acquises

- Une expérience peut être créée et publiée **sans ticket**.
- Le Pass sera repensé autour des visites et expériences ; les anciens seuils
  tickets/commentaires/likes ne sont pas la cible à conserver par défaut.
- Des tickets validés seront nécessaires pour atteindre certains niveaux du Pass.
- Le parcours complet expérience, médias, profil, modération et publication
  reste dans le périmètre. Le ticket n'en est plus une étape obligatoire.

L'absence de ticket n'empêchera dans aucun cas la publication d'une expérience.
Les seuils du Pass et la conservation des niveaux acquis après une suppression
ordinaire ont été validés ; fraude ou modération peuvent les révoquer.

## État initial constaté avant l'implémentation

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

## Pass : contrat produit validé

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

Seuils validés et codés dans la politique serveur version 2 :

| Niveau | Expériences publiées | Cafés distincts | Tickets validés |
| --- | ---: | ---: | ---: |
| Coffee Taster | 1 | 1 | 0 |
| Urban Explorer | 3 | 3 | 0 |
| Social Bean | 5 | 3 | 1 |
| Fragments Master | 10 | 5 | 3 |

Cette politique permet de commencer immédiatement, valorise découverte et
retours dans les lieux appréciés, et rend le dernier niveau explicite. Les deux
derniers niveaux exigent des tickets, tout en gardant un premier parcours
accessible sans scanner.

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

Règle validée pour les retraits : compteurs recalculés sur les contributions
actuellement admissibles ; badges déjà acquis conservés après suppression
ordinaire, révocables en cas de fraude ou décision de modération motivée.
La progression courante et les niveaux acquis sont distincts et persistés avec
la version de politique appliquée.
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

Le contrat primitif versionné `experience.lifecycle.changed` est inscrit
au catalogue et routé vers `app-users-events`. Son producteur sera livré avec
`experienceContext` au lot 05. Les événements ticket stables alimentent déjà le
Pass via outbox, route SQS/in-process et inbox. La fraîcheur mobile reste
`projection.updated` puis GET ; aucun état métier n'est envoyé directement en SSE.
Experience publie ses états de publication/modération et un motif factuel
(`PUBLISHED`, `AUTHOR_DELETED`, `MODERATION_HIDDEN`, `FRAUD_CONFIRMED`, etc.) ;
il ne publie ni une contribution Pass ni un ordre de révocation. La politique
Pass interprète elle-même ces faits.

Historique privé implémenté : `GET /api/users/me/tickets?cursor=...&limit=...`.
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
| 02A | Décisions Experience/Pass et contrat de transition | Validé par le produit et tracé ici |
| 02B | Projections ticket fiables + historique serveur/mobile | Implémenté : versions désordonnées, tombstones, PostgreSQL, pagination/intercalage, HTTP propriétaire, mapper, cache et états UI |
| 02C | Politique Pass validée, protection contre le rescan et nouveau propriétaire | Implémenté : domaine pur, contributions locales, empreinte OCR exacte, inbox, verrou utilisateur, identité immuable des sources, migration ancien profil, HTTP et mobile sans seuil local |
| 02D | Contrats Experience nécessaires au Pass | Contrat, catalogue, sérialisation, route et consommateur implémentés ; raccord au producteur réel différé au lot 05 |
| Conditionnelle | Justificatif ticket/café, si retenu | Référence locale, matching ambigu, collision réelle vs suspectée, révocation, pas de double comptage |

En l'absence d'Experience au lot 02, la verticale Experience → SQS/inbox → Pass
n'est pas annoncée comme validée contre un producteur inexistant. Le trajet ticket
stable → inbox → Pass → GET authentifié et le contrat Experience sont couverts ;
la verticale réelle Experience sera livrée et vérifiée au lot 05. Aucune donnée
synthétique n'est exposée comme activité réelle.

Les parcours critiques ajoutent perte réseau, redémarrage, socket absent,
commande rejetée explicitement et changements de compte. Les tests unitaires ne
remplacent ni les adaptateurs réels ni les verticales front et back. Suites de
régression et garde-fous DDD/hexa restent ceux du dépôt.

## Estimation et état

Premier point chronométré : 14:39 CEST, après exploration initiale. L'estimation
avant code fonctionnel était de 30–60 min pour 02A puis 1–2 jours concentrés pour
02B–02D. La fenêtre observée jusqu'à la première validation ciblée est proche
d'une heure, sans prétendre mesurer un temps actif : elle inclut échanges, analyse,
tests et attentes Testcontainers. Le socle d'événements, inbox et projections a
permis d'aller nettement plus vite que l'hypothèse prudente. Le justificatif
conditionnel demanderait toujours une estimation séparée après choix produit.

La fourchette globale initiale de 4–8 jours reste la référence historique. Après
les lots 00–02 locaux, la projection de l'implémentation restante passe à 3–6 jours
concentrés, hors délais TestFlight/App Review. L'objectif de quatre jours devient
plausible si les choix UI sont rapides et qu'aucun prérequis Apple/AWS ne bloque ;
médias, suppression de compte et modération restent les principales incertitudes.

État : 02A–02C implémentés localement ; 02D prêt côté consommateur et contrat.
L'historique privé est paginé serveur/mobile. Le Pass appartient désormais à
`userApplicationContext`, sans lecture SQL inter-BC en fonctionnement normal.
La migration SQL ponctuelle préserve les anciens niveaux et canonise les doublons
OCR exacts. Les tickets image sans texte OCR ne peuvent pas encore être dédupliqués
de façon fiable. Le domaine Experience lui-même reste le lot 05.

Sources principales :

- [Agrégat Ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/write/businesslogic/models/Ticket.java)
- [Politique Pass version 2](../../src/main/java/com/nm/fragmentsclean/userApplicationContext/pass/domain/PassProgressPolicy.java)
- [Projection locale du Pass](../../src/main/java/com/nm/fragmentsclean/userApplicationContext/pass/adapters/secondary/JdbcPassContributionStore.java)
- [Projections ticket](../../src/main/java/com/nm/fragmentsclean/ticketContext/read/adapters/secondary/repositories/JdbcTicketStatusProjectionRepository.java)
- [Contrats d'intégration ticket](../../src/main/java/com/nm/fragmentsclean/platform/eventing/contracts/TicketIntegrationEvents.java)
- [Historique mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/viewModel/useTicketsHistory.ts)
- [Présentation Pass mobile](/Users/nicolasmaldiney/fragmentsCleanFront/app/adapters/secondary/viewModel/passViewModel.ts)
