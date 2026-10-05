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
    @Test void requires_a_reason_before_any_comment_or_report_change() {
        var comments=new FakeCommentRepository();var reports=new FakeContentReportRepository();var events=new FakeDomainEventPublisher();var clock=new DeterministicDateTimeProvider();
        UUID comment=UUID.randomUUID(),target=UUID.randomUUID(),author=UUID.randomUUID(),report=UUID.randomUUID();
        comments.save(Comment.createNew(comment,target,author,null,"content",clock.now()));
        reports.save(ContentReport.create(report,comment,target,author,UUID.randomUUID(),ReportReason.SPAM,null,clock.now()));
        var handler=new ModerateCommentCommandHandler(comments,reports,events,clock);
        for(var reason:java.util.Arrays.asList(null,"","   "))assertThatThrownBy(()->handler.execute(new ModerateCommentCommand(UUID.randomUUID(),UUID.randomUUID(),report,comment,UUID.randomUUID(),ModerationStatus.HIDDEN,reason,clock.now())))
            .isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class).hasMessageContaining("reason is required");
        assertThat(comments.byId(comment).orElseThrow().toSnapshot().moderation()).isEqualTo(ModerationStatus.PUBLISHED);
        assertThat(reports.byId(report).orElseThrow().toSnapshot().status()).isEqualTo(ReportStatus.OPEN);assertThat(events.published).isEmpty();
    }
    @Test void audits_a_new_review_even_when_the_selected_report_is_already_resolved() {
        var comments=new FakeCommentRepository();var reports=new FakeContentReportRepository();var events=new FakeDomainEventPublisher();var clock=new DeterministicDateTimeProvider();
        UUID comment=UUID.randomUUID(),target=UUID.randomUUID(),author=UUID.randomUUID(),report=UUID.randomUUID(),operator=UUID.randomUUID(),action=UUID.randomUUID();
        var content=Comment.createNew(comment,target,author,null,"content",clock.now());content.hide();comments.save(content);
        var reported=ContentReport.create(report,comment,target,author,UUID.randomUUID(),ReportReason.SPAM,null,clock.now());reported.resolve(ReportStatus.RESOLVED,clock.now());reports.save(reported);
        new ModerateCommentCommandHandler(comments,reports,events,clock).execute(new ModerateCommentCommand(UUID.randomUUID(),action,report,comment,operator,ModerationStatus.HIDDEN,"  Still spam  ",clock.now()));
        assertThat(events.published).singleElement().satisfies(value->{var audit=(CommentModeratedEvent)value;assertThat(audit.actionId()).isEqualTo(action);assertThat(audit.operatorId()).isEqualTo(operator);assertThat(audit.reason()).isEqualTo("Still spam");assertThat(audit.version()).isEqualTo(2);});
        assertThat(comments.byId(comment).orElseThrow().toSnapshot().moderation()).isEqualTo(ModerationStatus.HIDDEN);
    }

}
