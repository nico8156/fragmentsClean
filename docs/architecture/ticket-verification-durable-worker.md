# Vérification durable des tickets

Date : 12 septembre 2026.

## Décision

La vérification d'un ticket par le processus local `ticketverify` est un travail
long et externe. Elle ne s'exécute donc dans aucune transaction de base de
données. Le contexte ticket possède un processus durable distinct du statut de
commande et du résultat métier du ticket.

```text
ticket.verify.accepted:v1 reçu par SQS
-> inbox revendiquée avec lease
-> TicketVerificationProcessManager persiste un job PENDING
-> commit et suppression du message SQS

worker planifié
-> claim transactionnel du job et lease RUNNING
-> appel TicketVerificationProvider hors transaction
-> completion transactionnelle : Ticket + job + outbox event
```

Trois vérités restent séparées :

- le reçu de commande dit si l'intention de soumettre a été appliquée ou rejetée ;
- `TicketVerificationJob` coordonne l'exécution technique et ses reprises ;
- `Ticket.TicketStatus` décrit le résultat visible : `CONFIRMED`, `REJECTED` pour
  une décision métier, ou `FAILED` pour un échec technique final et relançable.

Un événement `FAILED_FINAL` n'est jamais converti en rejet métier. Sa projection
mobile produit `FAILED`, affiche une analyse interrompue et garde les refus dans
un compteur distinct.

## Invariants de reprise et de concurrence

- L'identifiant stable de l'événement accepté est le `jobId`; son insertion est
  idempotente et le `commandId` est également unique.
- Un job n'est réclamable que s'il est en attente, si son délai de retry est
  atteint ou si son lease `RUNNING` est expiré.
- Chaque claim incrémente la version et donne un propriétaire de lease unique.
- La completion compare état, propriétaire et version. Une réponse tardive d'un
  ancien worker est ignorée.
- Si un autre chemin a déjà rendu le ticket `CONFIRMED`, `REJECTED` ou `DELETED`,
  le job se termine sans événement contradictoire.
- Une panne inattendue laisse le job récupérable après expiration du lease.
- Une panne déclarée retryable est replanifiée avec délai; le nombre maximal de
  tentatives produit ensuite un résultat technique `FAILED_FINAL`.

L'inbox utilise elle aussi un lease durable. Un message `RECEIVED` encore actif
ne peut plus être traité simultanément; un lease expiré ou un statut `FAILED`
autorise une nouvelle revendication. `PROCESSED` reste terminal.

## Configuration

| Propriété | Défaut | Rôle |
| --- | ---: | --- |
| `ticketverify.worker.poll-ms` | 1000 | Intervalle du worker |
| `ticketverify.worker.batch-size` | 10 | Nombre maximal de jobs lus par passage |
| `ticketverify.worker.lease-seconds` | 30 | Fenêtre de propriété d'une tentative |
| `ticketverify.worker.retry-seconds` | 5 | Base du délai progressif |
| `ticketverify.worker.max-attempts` | 5 | Tentatives avant échec technique final |

Ces valeurs sont opérationnelles et ne sont pas des invariants métier. Le lease
doit rester supérieur à la durée normalement attendue du provider; une réponse
qui arrive après reprise par un autre worker sera volontairement ignorée.

## Déploiement et retour arrière

La migration additive
`db/release/2026-09-12-ticket-verification-jobs.sql` crée la table des jobs, ses
index et ajoute `lease_until` à l'inbox. Elle doit être appliquée avant le code.
Le rollback applicatif conserve la table et la colonne : aucune donnée de reprise
n'est supprimée. Après retour à l'ancienne version, arrêter le worker de la
nouvelle version; lors du redéploiement, les jobs non terminés reprennent selon
leur état et leur lease.

La suppression de compte efface les jobs du propriétaire avant les tickets. Le
job contient OCR et référence d'image : ces champs ne doivent jamais être loggés.

## Preuves automatisées

- domaine pur : transitions, leases actifs/expirés et propriétaire ;
- use cases avec fakes : intake idempotente, completion atomique, résultat
  technique distinct, réponse de worker obsolète et ticket déjà terminal ;
- worker : provider appelé sans transaction active et retry borné ;
- PostgreSQL/Testcontainers : round-trip, sélection des jobs dus, conflit de
  version et reprise après expiration ;
- inbox PostgreSQL : revendication concurrente, échec, expiration et terminalité ;
- architecture : frontières de bounded contexts inchangées.

