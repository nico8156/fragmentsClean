Résumé pipeline LIKE complet (avec ta contrainte “void HTTP”)


UI → uiLikeToggleRequested(targetId)

Use case Redux :

calcule commandId, likeId, userId, at

push dans Outbox : LikeAddCmd ou LikeRemoveCmd

applique l’optimisme (update immédiat du state)

déclenche outboxProcessOnce()

Outbox front :

sérialise la commande,

fait POST /api/social/likes,

ne lit que le status HTTP (200/204 = OK réseau),

ne reçoit pas d’ACK métier ici.


Back :

controller → MakeLikeCommandHandler

agrégat Like applique la mutation, enregistre un LikeSetEvent

handler publie LikeSetEvent via DomainEventPublisher → Outbox DB

Outbox back :

OutboxEventDispatcher envoie le LikeSetEvent sur le stream/socket



Front (sync listener) :

reçoit LikeSetEvent sur WS/SSE

outbox runtime :

resolve(commandId) → “ah, c’est l’ACK de cette commande”

marque la commande comme confirmée (succès)

store likes :

met à jour count, me, version, lastFetchedAt, etc.

HTTP = juste “commande envoyée”,
Socket/stream = ACK + vérité serveur delta.


Récap ultra concret du flow LIKE

Pour être sûr qu’on est synchronisés :

Front :

enfile une commande dans son Outbox (LikeAdd ou LikeRemove),

envoie un POST /api/social/likes (sans attendre d’ACK métier),

applique l’optimisme.

Back – HTTP :

reçoit la requête,

construit MakeLikeCommand,

commandBus.dispatch(command),

répond 204/202.

Back – Domaine :

MakeLikeCommandHandler charge/crée Like,

Like.set(...) met à jour l’état + registerEvent(new LikeSetEvent(...)),

handler persiste l’agrégat,

handler fait domainEventPublisher.publish(event).

Back – Outbox (publisher) :

OutboxDomainEventPublisher.publish(event) sérialise l’event,

crée un OutboxEventJpaEntity (status = PENDING).

Back – Outbox (dispatcher) :

toutes les X ms, OutboxEventDispatcher lit les PENDING,

pour chaque : eventSender.send(outboxEvent) :

en dev : log

en prod : WebSocket → front

marque l’outbox event en SENT.

Front – Socket :

reçoit un message LikeSetEvent (ou un enveloppe stream),

retrouve la commande par commandId,

marque la commande comme confirmée,

met à jour LikesStateWl avec la vérité serveur.