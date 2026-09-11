package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.platform.eventing.contracts.SocialCommentIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.SocialLikeSetIntegrationEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentReportedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentModeratedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlockChangedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportReason;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus;

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
    static CommentReportedEvent reported(SocialCommentIntegrationEvents.Reported e) {
        return new CommentReportedEvent(e.eventId(), e.commandId(), e.reportId(), e.commentId(), e.targetId(),
                e.authorId(), e.reporterId(), ReportReason.valueOf(e.reason()), e.details(),
                ReportStatus.valueOf(e.status()), e.version(), e.occurredAt(), e.clientAt());
    }
    static CommentModeratedEvent moderated(SocialCommentIntegrationEvents.Moderated e) {
        return new CommentModeratedEvent(e.eventId(), e.commandId(), e.actionId(), e.reportId(), e.commentId(),
                e.targetId(), e.authorId(), e.operatorId(), ModerationStatus.valueOf(e.moderation()),
                ReportStatus.valueOf(e.reportStatus()), e.reason(), e.version(), e.occurredAt(), e.clientAt());
    }
    static UserBlockChangedEvent blockChanged(SocialCommentIntegrationEvents.UserBlockChanged e) {
        return new UserBlockChangedEvent(e.eventId(), e.commandId(), e.blockId(), e.blockerId(), e.blockedUserId(),
                e.active(), e.version(), e.occurredAt(), e.clientAt());
    }
}
