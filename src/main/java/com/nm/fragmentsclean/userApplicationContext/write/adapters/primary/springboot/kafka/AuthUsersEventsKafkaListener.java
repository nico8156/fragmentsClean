package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuthUsersEventsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AuthUsersEventsKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final AuthUserCreatedEventHandler handler;

    public AuthUsersEventsKafkaListener(ObjectMapper objectMapper,
                                        AuthUserCreatedEventHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @KafkaListener(
            topics = {"auth-users-events"},
            groupId = "user-application-context"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka listener received raw record on auth-users-events: key={}, value={}", record.key(), record.value());

        String payload = record.value();
        try {
            AuthUserCreatedEvent event =
                    objectMapper.readValue(payload, AuthUserCreatedEvent.class);

            log.info("Received AuthUserCreatedEvent from Kafka, authUserId={}, email={}",
                    event.authUserId(), event.email());

            handler.handle(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize AuthUserCreatedEvent from payload={}", payload, e);
        }
    }
}
