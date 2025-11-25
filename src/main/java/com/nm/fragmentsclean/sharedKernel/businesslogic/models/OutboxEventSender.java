package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;


/**
 * Port d'envoi d'un événement de l'outbox vers l'extérieur :
 * - Kafka ====> KafkaOutboxEventSender
 * - WebSocket ====> WebSocketOutboxEventSender
 * - SSE
 * - autre...
 */

public interface OutboxEventSender {
    void send(OutboxEventJpaEntity event) throws Exception;
}
