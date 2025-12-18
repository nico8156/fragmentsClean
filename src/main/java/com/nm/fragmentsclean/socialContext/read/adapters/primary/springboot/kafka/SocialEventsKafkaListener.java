package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.JsonNode;
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
    private final CommentCreatedEventHandler createdHandler;
    private final CommentUpdatedEventHandler updatedHandler;
    private final CommentDeletedEventHandler deletedHandler;

    public SocialEventsKafkaListener(ObjectMapper objectMapper,
                                     CommentCreatedEventHandler createdHandler,
                                     CommentUpdatedEventHandler updatedHandler,
                                     CommentDeletedEventHandler deletedHandler) {
        this.objectMapper = objectMapper;
        this.createdHandler = createdHandler;
        this.updatedHandler = updatedHandler;
        this.deletedHandler = deletedHandler;
    }

    @KafkaListener(topics = {"domain-events"}, groupId = "social-context-read")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();

        try {
            JsonNode root = objectMapper.readTree(payload);

            // ✅ GUARD: ignore tout ce qui n'est pas un event Comment
            // (ex: LikeSetEvent contient likeId, etc.)
            if (!root.has("commentId")) {
                log.debug("[social-read] ignore non-comment event on domain-events: {}", payload);
                return;
            }

            // DELETE
            if (root.has("deletedAt")) {
                CommentDeletedEvent evt = objectMapper.treeToValue(root, CommentDeletedEvent.class);
                deletedHandler.handle(evt);
                return;
            }

            // CREATE (parentId présent même si null)
            if (root.has("parentId")) {
                CommentCreatedEvent evt = objectMapper.treeToValue(root, CommentCreatedEvent.class);
                createdHandler.handle(evt);
                return;
            }

            // UPDATE
            CommentUpdatedEvent evt = objectMapper.treeToValue(root, CommentUpdatedEvent.class);
            updatedHandler.handle(evt);

        } catch (Exception e) {
            log.error("[social-read] failed to handle domain-events payload={}", payload, e);
        }
    }

}
