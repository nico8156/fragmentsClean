package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.APP_USERS_EVENTS;
import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.DOMAIN_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserProfileUpdatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.socialContext.read.projectors.UserSocialProjectionProjector;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCreatedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentDeletedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentUpdatedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSetEventHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocialSqsIntegrationEventHandlers {

    private final SqsIntegrationEventPayloadReader payloadReader;

    public SocialSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Bean
    SqsIntegrationEventHandler socialCommentCreatedSqsIntegrationEventHandler(CommentCreatedEventHandler handler) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.created",
                envelope -> handler.handle(payloadReader.read(envelope, CommentCreatedEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler socialCommentUpdatedSqsIntegrationEventHandler(CommentUpdatedEventHandler handler) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.updated",
                envelope -> handler.handle(payloadReader.read(envelope, CommentUpdatedEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler socialCommentDeletedSqsIntegrationEventHandler(CommentDeletedEventHandler handler) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.deleted",
                envelope -> handler.handle(payloadReader.read(envelope, CommentDeletedEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler socialLikeSetSqsIntegrationEventHandler(LikeSetEventHandler handler) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.like.set",
                envelope -> handler.handle(payloadReader.read(envelope, LikeSetEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler appUserCreatedSocialProjectionSqsIntegrationEventHandler(
            UserSocialProjectionProjector projector) {
        return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "app.user.created", envelope -> {
            AppUserCreatedIntegrationEvent event = payloadReader.read(envelope, AppUserCreatedIntegrationEvent.class);
            projector.upsert(event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt());
        });
    }

    @Bean
    SqsIntegrationEventHandler appUserProfileUpdatedSocialProjectionSqsIntegrationEventHandler(
            UserSocialProjectionProjector projector) {
        return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "app.user.profile_updated", envelope -> {
            AppUserProfileUpdatedIntegrationEvent event = payloadReader.read(envelope, AppUserProfileUpdatedIntegrationEvent.class);
            projector.upsert(event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt());
        });
    }

    private record SimpleSqsIntegrationEventHandler(
            String destination,
            String eventType,
            java.util.function.Consumer<IntegrationEventEnvelope> handler
    ) implements SqsIntegrationEventHandler {

        @Override
        public SqsIntegrationEventRoute route() {
            return new SqsIntegrationEventRoute(destination, eventType);
        }

        @Override
        public void handle(IntegrationEventEnvelope envelope) {
            handler.accept(envelope);
        }
    }
}
