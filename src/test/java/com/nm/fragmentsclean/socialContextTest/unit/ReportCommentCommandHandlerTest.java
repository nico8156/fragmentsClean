package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.*;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportCommentCommandHandlerTest {
    private final UUID commentId=UUID.randomUUID(), authorId=UUID.randomUUID(), reporterId=UUID.randomUUID(), targetId=UUID.randomUUID();
    private final FakeCommentRepository comments = new FakeCommentRepository();
    private final FakeContentReportRepository reports = new FakeContentReportRepository();
    private final FakeDomainEventPublisher events = new FakeDomainEventPublisher();
    private final DeterministicDateTimeProvider clock = new DeterministicDateTimeProvider();

    @Test void persists_report_and_emits_fact() {
        clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        comments.save(Comment.createNew(commentId,targetId,authorId,null,"body",clock.instantOfNow));
        var command = new ReportCommentCommand(UUID.randomUUID(),UUID.randomUUID(),commentId,reporterId,
                ReportReason.SPAM,"Repeated links",clock.instantOfNow);
        new ReportCommentCommandHandler(comments,reports,events,clock).execute(command);
        assertThat(reports.allSnapshots()).singleElement().satisfies(report -> {
            assertThat(report.status()).isEqualTo(ReportStatus.OPEN);
            assertThat(report.reporterId()).isEqualTo(reporterId);
        });
        assertThat(events.published).singleElement().isInstanceOf(CommentReportedEvent.class);
    }

    @Test void rejects_self_report() {
        clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        comments.save(Comment.createNew(commentId,targetId,authorId,null,"body",clock.instantOfNow));
        assertThatThrownBy(() -> new ReportCommentCommandHandler(comments,reports,events,clock).execute(
                new ReportCommentCommand(UUID.randomUUID(),UUID.randomUUID(),commentId,authorId,
                        ReportReason.OTHER,null,clock.instantOfNow)))
                .isInstanceOf(BusinessCommandRejectedException.class)
                .extracting(e -> ((BusinessCommandRejectedException)e).rejectionCode()).isEqualTo("COMMENT_SELF_REPORT");
    }
}
