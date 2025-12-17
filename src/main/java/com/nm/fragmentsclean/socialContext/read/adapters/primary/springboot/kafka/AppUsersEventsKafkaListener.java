package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.socialContext.read.projectors.UserSocialProjectionProjector;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppUsersEventsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AppUsersEventsKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final UserSocialProjectionProjector projector;

    public AppUsersEventsKafkaListener(ObjectMapper objectMapper,
                                       UserSocialProjectionProjector projector) {
        this.objectMapper = objectMapper;
        this.projector = projector;
    }

    @KafkaListener(topics = {"app-users-events"}, groupId = "social-context-read")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();
        log.info("[social-read] received on app-users-events: key={} value={}", record.key(), payload);

        try {
            AppUserCreatedEvent evt = objectMapper.readValue(payload, AppUserCreatedEvent.class);
            projector.upsert(evt.userId(), evt.displayName(), evt.avatarUrl(), evt.version(), evt.occurredAt());
            log.info("[social-read] upsert user_social_projection userId={} v={}", evt.userId(), evt.version());
            return;
        } catch (Exception e) {
            log.warn("[social-read] not AppUserCreatedEvent (or failed). payload={}", payload, e);
        }

        try {
            AppUserProfileUpdatedEvent evt = objectMapper.readValue(payload, AppUserProfileUpdatedEvent.class);
            projector.upsert(evt.userId(), evt.displayName(), evt.avatarUrl(), evt.version(), evt.occurredAt());
            log.info("[social-read] upsert user_social_projection userId={} v={}", evt.userId(), evt.version());
        } catch (Exception e) {
            log.error("[social-read] failed to handle app-users-events payload={}", payload, e);
        }
    }
}
