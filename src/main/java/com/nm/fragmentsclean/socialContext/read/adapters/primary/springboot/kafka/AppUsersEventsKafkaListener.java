package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.socialContext.read.projectors.UsersPublicProjectionProjector;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppUsersEventsKafkaListener {

    private final ObjectMapper objectMapper;
    private final UsersPublicProjectionProjector projector;

    public AppUsersEventsKafkaListener(ObjectMapper objectMapper,
                                       UsersPublicProjectionProjector projector) {
        this.objectMapper = objectMapper;
        this.projector = projector;
    }

    @KafkaListener(topics = {"app-users-events"}, groupId = "social-context-read")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();

        // Try created
        try {
            AppUserCreatedEvent evt = objectMapper.readValue(payload, AppUserCreatedEvent.class);
            projector.upsert(
                    evt.userId(),
                    evt.displayName(),
                    evt.avatarUrl(),
                    "fr-FR",
                    evt.version(),
                    evt.occurredAt()
            );
            return;
        } catch (Exception ignored) {}

        // Try updated
        try {
            AppUserProfileUpdatedEvent evt = objectMapper.readValue(payload, AppUserProfileUpdatedEvent.class);
            projector.upsert(
                    evt.userId(),
                    evt.displayName(),
                    evt.avatarUrl(),
                    "fr-FR",
                    evt.version(),
                    evt.occurredAt()
            );
        } catch (Exception ignored) {
            // optionnel: log debug
        }
    }
}
