package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;


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
        String payloadJson = outboxEvent.getPayloadJson();
        String destination = "/topic/" + outboxEvent.getStreamKey();

        messagingTemplate.convertAndSend(destination, payloadJson);
    }
}
