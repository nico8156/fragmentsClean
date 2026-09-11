package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.ContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ContentReport;
import jakarta.transaction.Transactional;

@Transactional
public class ReportCommentCommandHandler implements CommandHandler<ReportCommentCommand> {
    private final CommentRepository comments;
    private final ContentReportRepository reports;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;

    public ReportCommentCommandHandler(CommentRepository comments, ContentReportRepository reports,
                                       DomainEventPublisher events, DateTimeProvider clock) {
        this.comments = comments; this.reports = reports; this.events = events; this.clock = clock;
    }

    @Override public void execute(ReportCommentCommand command) {
        var existing = reports.byReporterAndComment(command.reporterId(), command.commentId());
        if (existing.isPresent()) {
            if (!existing.get().toSnapshot().reportId().equals(command.reportId())) {
                throw new BusinessCommandRejectedException("COMMENT_ALREADY_REPORTED", "Comment was already reported");
            }
            return;
        }
        var comment = comments.byId(command.commentId())
                .orElseThrow(() -> new BusinessCommandRejectedException("COMMENT_NOT_FOUND", "Comment does not exist"));
        var snapshot = comment.toSnapshot();
        if (snapshot.deletedAt() != null) throw new BusinessCommandRejectedException("COMMENT_NOT_FOUND", "Comment does not exist");
        if (snapshot.authorId().equals(command.reporterId())) {
            throw new BusinessCommandRejectedException("COMMENT_SELF_REPORT", "Own comment cannot be reported");
        }
        var now = clock.now();
        var report = ContentReport.create(command.reportId(), command.commentId(), snapshot.targetId(),
                snapshot.authorId(), command.reporterId(), command.reason(), command.details(), now);
        reports.save(report);
        report.registerCreatedEvent(command.commandId(), command.clientAt(), now);
        report.domainEvents().forEach(events::publish);
        report.clearDomainEvents();
    }
}
