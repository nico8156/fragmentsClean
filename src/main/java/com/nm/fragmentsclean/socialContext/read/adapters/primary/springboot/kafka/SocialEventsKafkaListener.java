package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCreatedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentDeletedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentUpdatedEventHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SocialEventsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(SocialEventsKafkaListener.class);

    private final ObjectMapper objectMapper;

    private final CommentCreatedEventHandler commentCreatedHandler;
    private final CommentUpdatedEventHandler commentUpdatedHandler;
    private final CommentDeletedEventHandler commentDeletedHandler;

    public SocialEventsKafkaListener(
            ObjectMapper objectMapper,
            CommentCreatedEventHandler commentCreatedHandler,
            CommentUpdatedEventHandler commentUpdatedHandler,
            CommentDeletedEventHandler commentDeletedHandler
    ) {
        this.objectMapper = objectMapper;
        this.commentCreatedHandler = commentCreatedHandler;
        this.commentUpdatedHandler = commentUpdatedHandler;
        this.commentDeletedHandler = commentDeletedHandler;
    }

    @KafkaListener(topics = {"domain-events"}, groupId = "social-context-read")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();
        log.info("[social-read] received on social-events: key={} value={}", record.key(), payload);

        // On route par type d'event : on essaye de désérialiser successivement.
        // (Simple, aligné avec ton style actuel.)
        if (tryHandle(payload, CommentCreatedEvent.class, evt -> commentCreatedHandler.handle(evt))) return;
        if (tryHandle(payload, CommentUpdatedEvent.class, evt -> commentUpdatedHandler.handle(evt))) return;
        if (tryHandle(payload, CommentDeletedEvent.class, evt -> commentDeletedHandler.handle(evt))) return;

        log.warn("[social-read] unhandled social-events payload={}", payload);
    }

    private <T> boolean tryHandle(String payload, Class<T> clazz, java.util.function.Consumer<T> consumer) {
        try {
            T evt = objectMapper.readValue(payload, clazz);
            consumer.accept(evt);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
