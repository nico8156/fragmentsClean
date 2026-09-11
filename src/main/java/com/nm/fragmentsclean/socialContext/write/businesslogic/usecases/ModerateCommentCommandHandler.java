package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.ContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ContentReport;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.UUID;

@Transactional
public class ModerateCommentCommandHandler implements CommandHandler<ModerateCommentCommand> {
    private final CommentRepository comments;
    private final ContentReportRepository reports;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;

    public ModerateCommentCommandHandler(CommentRepository comments, ContentReportRepository reports,
                                         DomainEventPublisher events, DateTimeProvider clock) {
        this.comments = comments; this.reports = reports; this.events = events; this.clock = clock;
    }

    @Override public void execute(ModerateCommentCommand command) {
        if (command.moderation() != ModerationStatus.HIDDEN && command.moderation() != ModerationStatus.PUBLISHED) {
            throw new BusinessCommandRejectedException("MODERATION_DECISION_INVALID", "Unsupported moderation decision");
        }
        var report = reports.byId(command.reportId())
                .orElseThrow(() -> new BusinessCommandRejectedException("REPORT_NOT_FOUND", "Report does not exist"));
        if (!report.toSnapshot().commentId().equals(command.commentId())) {
            throw new BusinessCommandRejectedException("REPORT_COMMENT_MISMATCH", "Report does not belong to comment");
        }
        var comment = comments.byId(command.commentId())
                .orElseThrow(() -> new BusinessCommandRejectedException("COMMENT_NOT_FOUND", "Comment does not exist"));
        if (comment.toSnapshot().deletedAt() != null) {
            throw new BusinessCommandRejectedException("COMMENT_DELETED", "Deleted comment cannot be moderated");
        }
        if (command.reason() != null && command.reason().length() > 1000) {
            throw new BusinessCommandRejectedException("MODERATION_REASON_TOO_LONG", "Moderation reason exceeds 1000 characters");
        }
        var now = clock.now();
        var reportStatus = command.moderation() == ModerationStatus.HIDDEN ? ReportStatus.RESOLVED : ReportStatus.DISMISSED;
        boolean commentChanged = command.moderation() == ModerationStatus.HIDDEN ? comment.hide() : comment.restore();
        var reportsToClose = new LinkedHashMap<UUID, ContentReport>();
        reportsToClose.put(report.toSnapshot().reportId(), report);
        reports.openByComment(command.commentId()).forEach(openReport ->
                reportsToClose.put(openReport.toSnapshot().reportId(), openReport));
        boolean reportChanged = false;
        for (var reportToClose : reportsToClose.values()) {
            boolean changed = reportToClose.resolve(reportStatus, now);
            reportChanged |= changed;
            if (changed) reports.save(reportToClose);
        }
        if (!commentChanged && !reportChanged) return;
        comments.save(comment);
        comment.registerModeratedEvent(command.commandId(), command.actionId(), command.reportId(),
                command.operatorId(), reportStatus, command.reason(), command.clientAt(), now);
        comment.domainEvents().forEach(events::publish);
        comment.clearDomainEvents();
    }
}
