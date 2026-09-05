# Contrats d’intégration et ACL entrantes

## Frontière imposée

Un `DomainEvent` appartient au modèle qui l’émet. Il peut être sérialisé dans
l’outbox locale, mais il n’est jamais le contrat lu depuis SQS.

```text
DomainEvent producteur
-> outbox transactionnelle
-> IntegrationEventPayloadMapper
-> contrat public v1, transport-neutral et primitive-only
-> enveloppe stable
-> SQS
-> SqsIntegrationEventPayloadReader
-> ACL du contexte consommateur
-> message/événement local
-> handler ou process manager
```

Les contrats publics sont dans `platform/eventing/contracts`. Ils ne dépendent
d’aucun bounded context, aggregate, enum métier ou value object. Une rupture de
forme exige un nouveau numéro de version dans l’enveloppe et une stratégie de
compatibilité du consommateur.

## Responsabilités

- L’outbox conserve le fait métier interne avec la transaction du producteur.
- `IntegrationEventPayloadMapper` aplatit value objects et enums en primitives.
- L’adaptateur primaire SQS ne contient pas de décision métier.
- L’ACL entrante traduit le contrat externe vers le langage local du contexte.
- Le handler conserve transaction, idempotence et règles de projection.
- L’inbox supprime les doublons techniques ; la projection reste monotone et
  rejouable au niveau métier.

Pour une propagation write/read au sein du même bounded context, l’ACL peut
reconstruire son événement local afin de réutiliser le handler de projection.
Pour une communication entre deux bounded contexts, elle ne reconstruit jamais
le type métier du producteur : elle alimente une projection ou référence locale.

## Garde-fous

`BoundedContextArchitectureTest` interdit à `payloadReader.read(...)` de cibler
une classe située dans un package `write/.../models`. Les tests de round-trip
valident aussi les formes publiques complexes (article, social et ticket).

Les nouveaux flux suivent les orchestrateurs SQS consumer et projection sous
`.agents/backend/orchestrators`.
