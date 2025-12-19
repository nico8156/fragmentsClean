package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.ProcessTicketVerificationEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketVerificationRequestsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TicketVerificationRequestsKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final ProcessTicketVerificationEventHandler handler;

    public TicketVerificationRequestsKafkaListener(ObjectMapper objectMapper,
                                                   ProcessTicketVerificationEventHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @KafkaListener(topics = {"ticket-verification-requested"}, groupId = "ticket-context-write")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();

        try {
            JsonNode root = objectMapper.readTree(payload);

            // Guard minimal: c'est un event ticket verify request ?
            if (!root.has("ticketId") || !root.has("userId")) {
                log.debug("[ticket-write] ignore non-ticket event: {}", payload);
                return;
            }

            // Si tu veux être strict: check event has imageRef or ocrText
            TicketVerifyAcceptedEvent evt = objectMapper.treeToValue(root, TicketVerifyAcceptedEvent.class);

            log.info("[ticket-write] received TicketVerifyAcceptedEvent ticketId={} userId={}",
                    evt.ticketId(), evt.userId());

            handler.handle(evt);

        } catch (Exception e) {
            log.error("[ticket-write] failed to handle ticket-verification-requested payload={}", payload, e);
        }
    }
}
