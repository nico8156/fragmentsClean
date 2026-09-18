package com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxMessage;


/**
 * Port d'envoi d'un événement de l'outbox vers l'extérieur :
 * - integration messaging ====> stable envelopes through IntegrationMessagePublisher
 */

public interface OutboxEventSender {
    void send(OutboxMessage event) throws Exception;
}
