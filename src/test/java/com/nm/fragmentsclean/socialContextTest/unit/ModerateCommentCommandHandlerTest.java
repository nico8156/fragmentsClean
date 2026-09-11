package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.*;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModerateCommentCommandHandlerTest {
    @Test void hides_comment_and_resolves_report_through_domain_transition() {
        var comments=new FakeCommentRepository(); var reports=new FakeContentReportRepository();
        var events=new FakeDomainEventPublisher(); var clock=new DeterministicDateTimeProvider();
        clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        UUID commentId=UUID.randomUUID(), target=UUID.randomUUID(), author=UUID.randomUUID(), reportId=UUID.randomUUID();
        comments.save(Comment.createNew(commentId,target,author,null,"content",clock.instantOfNow));
        reports.save(ContentReport.create(reportId,commentId,target,author,UUID.randomUUID(),ReportReason.SPAM,null,clock.instantOfNow));
        new ModerateCommentCommandHandler(comments,reports,events,clock).execute(new ModerateCommentCommand(
                UUID.randomUUID(),UUID.randomUUID(),reportId,commentId,UUID.randomUUID(),ModerationStatus.HIDDEN,
                "Confirmed spam",clock.instantOfNow));
        assertThat(comments.byId(commentId).orElseThrow().toSnapshot().moderation()).isEqualTo(ModerationStatus.HIDDEN);
        assertThat(reports.byId(reportId).orElseThrow().toSnapshot().status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(events.published).singleElement().isInstanceOf(CommentModeratedEvent.class);
    }

    @Test void closes_every_open_report_for_the_same_comment() {
        var comments=new FakeCommentRepository(); var reports=new FakeContentReportRepository();
        var events=new FakeDomainEventPublisher(); var clock=new DeterministicDateTimeProvider();
        clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        UUID commentId=UUID.randomUUID(), target=UUID.randomUUID(), author=UUID.randomUUID();
        comments.save(Comment.createNew(commentId,target,author,null,"content",clock.instantOfNow));
        var first=ContentReport.create(UUID.randomUUID(),commentId,target,author,UUID.randomUUID(),ReportReason.SPAM,null,clock.instantOfNow);
        var second=ContentReport.create(UUID.randomUUID(),commentId,target,author,UUID.randomUUID(),ReportReason.HARASSMENT,null,clock.instantOfNow);
        reports.save(first); reports.save(second);
        new ModerateCommentCommandHandler(comments,reports,events,clock).execute(new ModerateCommentCommand(
                UUID.randomUUID(),UUID.randomUUID(),first.toSnapshot().reportId(),commentId,UUID.randomUUID(),
                ModerationStatus.HIDDEN,"confirmed",clock.instantOfNow));
        assertThat(reports.allSnapshots()).allMatch(report -> report.status() == ReportStatus.RESOLVED);
    }

    @Test void restoring_a_hidden_comment_reclassifies_the_selected_closed_report() {
        var comments=new FakeCommentRepository(); var reports=new FakeContentReportRepository();
        var events=new FakeDomainEventPublisher(); var clock=new DeterministicDateTimeProvider();
        clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        UUID commentId=UUID.randomUUID(), target=UUID.randomUUID(), author=UUID.randomUUID(), reportId=UUID.randomUUID();
        var comment=Comment.createNew(commentId,target,author,null,"content",clock.instantOfNow);
        comment.hide(); comments.save(comment);
        var report=ContentReport.create(reportId,commentId,target,author,UUID.randomUUID(),ReportReason.SPAM,null,clock.instantOfNow);
        report.resolve(ReportStatus.RESOLVED,clock.instantOfNow); reports.save(report);

        new ModerateCommentCommandHandler(comments,reports,events,clock).execute(new ModerateCommentCommand(
                UUID.randomUUID(),UUID.randomUUID(),reportId,commentId,UUID.randomUUID(),ModerationStatus.PUBLISHED,
                "Restored after review",clock.instantOfNow));

        assertThat(comments.byId(commentId).orElseThrow().toSnapshot().moderation()).isEqualTo(ModerationStatus.PUBLISHED);
        assertThat(reports.byId(reportId).orElseThrow().toSnapshot().status()).isEqualTo(ReportStatus.DISMISSED);
    }
}
