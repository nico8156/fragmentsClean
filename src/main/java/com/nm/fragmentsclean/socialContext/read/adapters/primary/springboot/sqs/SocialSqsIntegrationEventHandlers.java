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
import com.nm.fragmentsclean.socialContext.read.projections.ModerationProjectionEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialCommentIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialLikeSetIntegrationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.util.List;

@Configuration
public class SocialSqsIntegrationEventHandlers {

    private final SqsIntegrationEventPayloadReader payloadReader;

    public SocialSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Bean
    SqsIntegrationEventHandler socialCommentCreatedSqsIntegrationEventHandler(CommentCreatedEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.created",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.Created.class);barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.authorId(),()->handler.handle(SocialIntegrationEventAcl.created(event)));});
    }

    @Bean
    SqsIntegrationEventHandler socialCommentUpdatedSqsIntegrationEventHandler(CommentUpdatedEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.updated",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.Updated.class);barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.authorId(),()->handler.handle(SocialIntegrationEventAcl.updated(event)));});
    }

    @Bean
    SqsIntegrationEventHandler socialCommentDeletedSqsIntegrationEventHandler(CommentDeletedEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.deleted",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.Deleted.class);barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.authorId(),()->handler.handle(SocialIntegrationEventAcl.deleted(event)));});
    }

    @Bean
    SqsIntegrationEventHandler socialLikeSetSqsIntegrationEventHandler(LikeSetEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.like.set",
                envelope -> {var event=payloadReader.read(envelope,SocialLikeSetIntegrationEvent.class);barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.userId(),()->handler.handle(SocialIntegrationEventAcl.likeSet(event)));});
    }

    @Bean SqsIntegrationEventHandler socialCommentReportedSqsIntegrationEventHandler(ModerationProjectionEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.reported",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.Reported.class);barrier.ifAllActive(AccountErasureBarrier.Scope.SOCIAL,List.of(event.authorId(),event.reporterId()),()->handler.handle(SocialIntegrationEventAcl.reported(event)));});
    }

    @Bean SqsIntegrationEventHandler socialCommentModeratedSqsIntegrationEventHandler(ModerationProjectionEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.comment.moderated",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.Moderated.class);barrier.ifAllActive(AccountErasureBarrier.Scope.SOCIAL,List.of(event.authorId(),event.operatorId()),()->handler.handle(SocialIntegrationEventAcl.moderated(event)));});
    }

    @Bean SqsIntegrationEventHandler socialUserBlockChangedSqsIntegrationEventHandler(ModerationProjectionEventHandler handler,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(DOMAIN_EVENTS, "social.user_block.changed",
                envelope -> {var event=payloadReader.read(envelope,SocialCommentIntegrationEvents.UserBlockChanged.class);barrier.ifAllActive(AccountErasureBarrier.Scope.SOCIAL,List.of(event.blockerId(),event.blockedUserId()),()->handler.handle(SocialIntegrationEventAcl.blockChanged(event)));});
    }

    @Bean
    SqsIntegrationEventHandler appUserCreatedSocialProjectionSqsIntegrationEventHandler(
            UserSocialProjectionProjector projector,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "app.user.created", envelope -> {
            AppUserCreatedIntegrationEvent event = payloadReader.read(envelope, AppUserCreatedIntegrationEvent.class);
            barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.userId(),()->projector.upsert(event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt()));
        });
    }

    @Bean
    SqsIntegrationEventHandler appUserProfileUpdatedSocialProjectionSqsIntegrationEventHandler(
            UserSocialProjectionProjector projector,AccountErasureBarrier barrier) {
        return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "app.user.profile_updated", envelope -> {
            AppUserProfileUpdatedIntegrationEvent event = payloadReader.read(envelope, AppUserProfileUpdatedIntegrationEvent.class);
            barrier.ifActive(AccountErasureBarrier.Scope.SOCIAL,event.userId(),()->projector.upsert(event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt()));
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
