package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Primary
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDomainEventPublisher.class);

    private final SpringOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DateTimeProvider dateTimeProvider;

    public OutboxDomainEventPublisher(
            SpringOutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            DateTimeProvider dateTimeProvider
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info(">>> OutboxDomainEventPublisher.publish called with {}", event.getClass().getName());
        try {
            String payloadJson = objectMapper.writeValueAsString(event);

            Instant occurredAt = event.occurredAt();
            Instant createdAt = dateTimeProvider.now();

            String eventType = event.getClass().getName();

            String aggregateType;
            String aggregateId;
            String streamKey;

            if (event instanceof ArticleCreatedEvent articleEvent) {
                aggregateType = "Article";
                aggregateId = articleEvent.articleId().toString();
                streamKey = "article:" + aggregateId;

            } else if (event instanceof CoffeeCreatedEvent coffeeEvent) {
                aggregateType = "Coffee";
                aggregateId = coffeeEvent.coffeeId().toString();
                streamKey = "coffee:" + aggregateId;

            } else if (event instanceof AuthUserCreatedEvent createdEvent) {
                aggregateType = "AuthUser";
                aggregateId = createdEvent.authUserId().toString();
                streamKey = "authUser:" + aggregateId;

            } else if (event instanceof AuthUserLoggedInEvent loginEvent) {
                aggregateType = "AuthUser";
                aggregateId = loginEvent.authUserId().toString();
                streamKey = "authUser:" + aggregateId;

            } else if (event instanceof AppUserProfileUpdatedEvent appEvent) {
                aggregateType = "AppUser";
                aggregateId = appEvent.userId().toString();
                streamKey = "appUser:" + aggregateId;

            } else if (event instanceof AppUserCreatedEvent appEvent) {
                aggregateType = "AppUser";
                aggregateId = appEvent.userId().toString();
                streamKey = "appUser:" + aggregateId;

                // 🔹 SOCIAL : Like + Comment → streamKey = social:<targetId>
            } else if (event instanceof LikeSetEvent likeEvent) {
                aggregateType = "Like";
                aggregateId = likeEvent.likeId().toString();
                streamKey = "user:" + likeEvent.userId().toString();

            } else if (event instanceof CommentCreatedEvent commentEvent) {
                aggregateType = "Comment";
                aggregateId = commentEvent.commentId().toString();
                streamKey = "user:" + commentEvent.authorId().toString();

            } else if (event instanceof CommentUpdatedEvent commentEvent) {
                aggregateType = "Comment";
                aggregateId = commentEvent.commentId().toString();
                streamKey = "user:" + commentEvent.authorId().toString();

            } else if (event instanceof CommentDeletedEvent commentEvent) {
                aggregateType = "Comment";
                aggregateId = commentEvent.commentId().toString();
                streamKey = "user:" + commentEvent.authorId().toString();

            } else {
                aggregateType = "Unknown";
                aggregateId = "unknown";
                streamKey = "global";

                log.warn("Persisting domain event of unknown type in outbox: {}",
                        event.getClass().getName());
            }


            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    event.eventId().toString(),
                    eventType,
                    aggregateType,
                    aggregateId,
                    streamKey,
                    payloadJson,
                    occurredAt,
                    createdAt,
                    OutboxStatus.PENDING,
                    0 // retryCount initial
            );

            outboxRepository.save(entity);

            log.info(">>> OutboxDomainEventPublisher persisted eventId={} aggregateId={}",
                    event.eventId(), aggregateId);

            log.debug("Persisted outbox event: eventType={} aggregateType={} aggregateId={}",
                    eventType, aggregateType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event {} for outbox", event, e);
            throw new RuntimeException("Failed to serialize domain event for outbox", e);
        }
    }
}
