package com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;


/**
 * Port d'envoi d'un événement de l'outbox vers l'extérieur :
 * - integration messaging ====> stable envelopes through IntegrationMessagePublisher
 * - WebSocket ====> opportunistic client ACK sender
 * - SSE
 * - autre...
 */

public interface OutboxEventSender {
    void send(OutboxEventJpaEntity event) throws Exception;
}
