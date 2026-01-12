package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeCommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

public class UpdateCommentCommandHandlerTest {
	private final UUID COMMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private final UUID CMD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	FakeCommentRepository commentRepository = new FakeCommentRepository();
	FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
	DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

	UpdateCommentCommandHandler handler;

	@BeforeEach
	void setup() {
		dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
		handler = new UpdateCommentCommandHandler(commentRepository, domainEventPublisher, dateTimeProvider);

		// seed : commentaire existant
		Comment initial = Comment.createNew(
				COMMENT_ID,
				TARGET_ID,
				USER_ID,
				null,
				"Old body",
				Instant.parse("2023-10-01T10:00:00Z"));
		commentRepository.save(initial);
	}

	@Test
	void should_update_body_and_publish_event() {
		// WHEN
		handler.execute(new UpdateCommentCommand(
				CMD_ID,
				COMMENT_ID,
				"New content",
				Instant.parse("2023-10-01T10:04:00Z")));

		// THEN : état
		var snap = commentRepository.allSnapshots().getFirst();
		assertThat(snap.body()).isEqualTo("New content");
		assertThat(snap.version()).isEqualTo(1L);
		assertThat(snap.editedAt()).isEqualTo(dateTimeProvider.instantOfNow);

		// THEN : event
		assertThat(domainEventPublisher.published).hasSize(1);
		var evt = (CommentUpdatedEvent) domainEventPublisher.published.getFirst();
		assertThat(evt.commentId()).isEqualTo(COMMENT_ID);
		assertThat(evt.body()).isEqualTo("New content");
		assertThat(evt.version()).isEqualTo(1L);
		assertThat(evt.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
	}

	@Test
	void should_be_idempotent_when_body_unchanged() {
		// WHEN : même body
		handler.execute(new UpdateCommentCommand(
				CMD_ID,
				COMMENT_ID,
				"Old body",
				Instant.parse("2023-10-01T10:04:00Z")));

		// THEN : pas d’event
		assertThat(domainEventPublisher.published).isEmpty();
		var snap = commentRepository.allSnapshots().getFirst();
		assertThat(snap.version()).isEqualTo(0L);
	}
}
