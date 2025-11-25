package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketOutboxEventSender implements OutboxEventSender {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketOutboxEventSender(SimpMessagingTemplate messagingTemplate,
                                      ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(OutboxEventJpaEntity outboxEvent) throws Exception {
        // En pratique tu peux soit :
        // - renvoyer payloadJson tel quel
        // - soit le désérialiser en DomainEvent / DTO
        var payloadJson = outboxEvent.getPayloadJson();

        // Ex: on route par streamKey = "likes:target:{targetId}"
        String destination = "/topic/" + outboxEvent.getStreamKey();

        messagingTemplate.convertAndSend(destination, payloadJson);
    }
}
