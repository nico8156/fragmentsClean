# Première tranche commune — 11 septembre 2026

Périmètre livré : isolation des comptes mobile et confidentialité des tickets,
avec correction adjacente de la permission de localisation. Cette tranche n'a
pas choisi la trajectoire ni le domaine Experience. Depuis, le produit a demandé
de réaliser les deux parcours ; le [plan unifié de l'audit](app-store-readiness-2026-09-11.md#plan-unifié-dintervention)
est mis à jour et attend sa validation. Les constats initiaux de cet audit restent
un état avant travaux.

## Backend

`GET /api/tickets/{ticketId}/status` conserve son URL et son DTO. Le contrôleur
transmet le sujet du JWT à `GetTicketStatusQuery`; le handler utilise une lecture
JDBC filtrée par `ticket_id` **et** `user_id`. Aucun propriétaire fourni par le
client n'est accepté. Absence de JWT : 401. Ticket étranger ou absent : 404 sans
corps. Les méthodes de lecture admin restent disponibles, sans modification
des règles d'accès Studio existantes.

Pas de changement de domaine, transport, table, index ou migration. La clé
primaire existante sur le ticket suffit pour cette lecture unitaire. Aucun SQL
cross-BC ajouté, aucune écriture depuis un contrôleur.

## Mobile

Les données privées et la navigation sont réinitialisées lors du changement
d'identité. Une génération de session protège des retours tardifs, y compris
A -> B -> A. Outbox, cache durable et curseur SSE ont des namespaces par compte.
La file A reste conservée pour A et n'est pas transmise sous B. Le démarrage
attend la restauration du compte ; les erreurs de stockage proposent un retry.
Les courses login/logout/refresh, abonnements localisation et flux SSE sont
également protégés. La permission `denied` ne devient plus `granted`.

Architecture et recette détaillées :
[isolation mobile](/Users/nicolasmaldiney/fragmentsCleanFront/docs/architecture/account-isolation.md).

Les anciens snapshots globaux sont **conservés mais non rechargés** : leur
propriétaire ne peut pas être déduit de façon fiable. Leur récupération reste
une décision explicite avant diffusion. Un ancien historique peut donc ne plus
apparaître dans l'UI ; il n'a pas été supprimé. Ne pas rétrograder vers l'ancien
runtime mobile en production : cela réintroduirait l'exposition inter-comptes.

## Vérifications

- Mobile : 232 tests réussis dans 56 suites Jest et TypeScript sans émission ; scénarios de
  changement de compte, persistance/recréation du store, requêtes tardives,
  panne de stockage, refresh après logout, SSE et reprise de commande.
- Backend : 41 tests ciblés réussis sur sept classes, dont quatre tests HTTP/JDBC
  avec PostgreSQL 13.1 réel via Testcontainers ; frontière des BC et sécurité
  admin incluses. Le test HTTP utilise le résolveur de principal Spring, sans
  simuler la validation cryptographique du JWT.
- Pas de modification du code Studio, pas de déploiement, pas de build natif,
  pas de recette iPhone effectuée dans cette tranche. Les conteneurs des tests
  sont temporaires et ne modifient pas la base applicative.

Commandes backend :

```sh
./mvnw -q -Dtest=TicketOwnershipQueryTest,TicketOwnershipHttpIT,BoundedContextArchitectureTest,VerifyTicketCommandHandlerTest,TicketVerificationProcessManagerTest,TicketProjectionSyncEventHandlerTest,AdminTokenSecurityTest test
```

## À décider ensuite

Le chantier distinct erreurs techniques/rejets métier/statuts reste à faire :
ne pas considérer tous les P0 de l'audit corrigés. Il inclut la conservation des
commandes sur erreur technique, les `REJECTED` persistés, le contrôle du
demandeur sur `/commands/{commandId}` et les succès sans nouvel événement.

Ce chantier est désormais le lot 01 du plan unifié, à démarrer seulement après
validation. Le choix des deux parcours est acquis ; restent les règles détaillées
Experience/commentaires/Pass, la preuve de visite, les tickets sans café connu et
la quantité de photos. La durée totale sera suivie par prévisions glissantes :
estimation initiale conservée, comparaison au réel, puis nouvelle projection du
reste à faire à chaque lot ou découverte importante. Ce suivi n'impose pas une
réduction de périmètre à quatre jours.
L'évaluation FlowAtlas figure dans l'audit ;
les preuves de confidentialité de cette tranche viennent du code et des tests,
pas d'une projection statique du graphe.
