package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.platform.eventing.contracts.SocialCommentIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialLikeSetIntegrationEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;

final class SocialIntegrationEventAcl {
    private SocialIntegrationEventAcl() { }

    static CommentCreatedEvent created(SocialCommentIntegrationEvents.Created e) {
        return new CommentCreatedEvent(e.eventId(), e.commandId(), e.commentId(), e.targetId(), e.parentId(), e.authorId(),
                e.body(), ModerationStatus.valueOf(e.moderation()), e.version(), e.occurredAt(), e.clientAt());
    }
    static CommentUpdatedEvent updated(SocialCommentIntegrationEvents.Updated e) {
        return new CommentUpdatedEvent(e.eventId(), e.commandId(), e.commentId(), e.targetId(), e.authorId(), e.body(),
                ModerationStatus.valueOf(e.moderation()), e.version(), e.occurredAt(), e.clientAt());
    }
    static CommentDeletedEvent deleted(SocialCommentIntegrationEvents.Deleted e) {
        return new CommentDeletedEvent(e.eventId(), e.commandId(), e.commentId(), e.targetId(), e.authorId(),
                ModerationStatus.valueOf(e.moderation()), e.deletedAt(), e.version(), e.occurredAt(), e.clientAt());
    }
    static LikeSetEvent likeSet(SocialLikeSetIntegrationEvent e) {
        return new LikeSetEvent(e.eventId(), e.commandId(), e.likeId(), e.userId(), e.targetId(), e.active(), e.count(),
                e.version(), e.occurredAt(), e.clientAt());
    }
}
