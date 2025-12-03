package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeCommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class CreateCommentCommandHandlerTest {
    private final UUID COMMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID TARGET_ID  = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID CMD_ID     = UUID.fromString("44444444-4444-4444-4444-444444444444");

    FakeCommentRepository commentRepository = new FakeCommentRepository();
    FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
    DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

    CreateCommentCommandHandler handler;

    @BeforeEach
    void setup() {
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:00:00Z");
        handler = new CreateCommentCommandHandler(commentRepository, domainEventPublisher, dateTimeProvider);
    }

    @Test
    void should_create_new_comment_and_publish_event() {
        // WHEN
        handler.execute(new CreateCommentCommand(
                CMD_ID,
                COMMENT_ID,
                USER_ID,
                TARGET_ID,
                null, // pas de parent
                "Hello fragments !",
                Instant.parse("2023-10-01T09:59:00Z")
        ));

        // THEN : état
        var snaps = commentRepository.allSnapshots();
        assertThat(snaps).hasSize(1);
        var snap = snaps.getFirst();
        assertThat(snap.commentId()).isEqualTo(COMMENT_ID);
        assertThat(snap.targetId()).isEqualTo(TARGET_ID);
        assertThat(snap.authorId()).isEqualTo(USER_ID);
        assertThat(snap.parentId()).isNull();
        assertThat(snap.body()).isEqualTo("Hello fragments !");
        assertThat(snap.createdAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(snap.moderation()).isEqualTo(ModerationStatus.PUBLISHED);
        assertThat(snap.version()).isEqualTo(0L); // première version, pas d’edit

        // THEN : event
        assertThat(domainEventPublisher.published).hasSize(1);
        var evt = (CommentCreatedEvent) domainEventPublisher.published.getFirst();

        assertThat(evt.commandId()).isEqualTo(CMD_ID);
        assertThat(evt.commentId()).isEqualTo(COMMENT_ID);
        assertThat(evt.targetId()).isEqualTo(TARGET_ID);
        assertThat(evt.parentId()).isNull();
        assertThat(evt.authorId()).isEqualTo(USER_ID);
        assertThat(evt.body()).isEqualTo("Hello fragments !");
        assertThat(evt.moderation()).isEqualTo(ModerationStatus.PUBLISHED);
        assertThat(evt.version()).isEqualTo(0L);
        assertThat(evt.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(evt.clientAt()).isEqualTo(Instant.parse("2023-10-01T09:59:00Z"));
    }
}
