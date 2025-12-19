package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketEventsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventsKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final TicketVerifyAcceptedEventHandler acceptedHandler;
    private final TicketVerificationCompletedEventHandler completedHandler;

    public TicketEventsKafkaListener(
            ObjectMapper objectMapper,
            TicketVerifyAcceptedEventHandler acceptedHandler,
            TicketVerificationCompletedEventHandler completedHandler
    ) {
        this.objectMapper = objectMapper;
        this.acceptedHandler = acceptedHandler;
        this.completedHandler = completedHandler;
    }

    @KafkaListener(topics = {"ticket-events"}, groupId = "ticket-context-read")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();

        try {
            JsonNode root = objectMapper.readTree(payload);

            // Guard: ticket events only
            if (!root.has("ticketId") || !root.has("userId")) {
                log.debug("[ticket-read] ignore non-ticket event: {}", payload);
                return;
            }

            // Completed has "outcome"
            if (root.has("outcome")) {
                TicketVerificationCompletedEvent evt =
                        objectMapper.treeToValue(root, TicketVerificationCompletedEvent.class);
                completedHandler.handle(evt);
                return;
            }

            // Accepted (ANALYZING) has "status" (and no outcome)
            if (root.has("status")) {
                TicketVerifyAcceptedEvent evt =
                        objectMapper.treeToValue(root, TicketVerifyAcceptedEvent.class);
                acceptedHandler.handle(evt);
                return;
            }

            log.debug("[ticket-read] ignore unknown ticket payload: {}", payload);

        } catch (Exception e) {
            log.error("[ticket-read] failed to handle ticket-events payload={}", payload, e);
        }
    }
}
