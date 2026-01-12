package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeCommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

public class DeleteCommentCommandHandlerTest {
	private final UUID COMMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private final UUID CMD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	FakeCommentRepository commentRepository = new FakeCommentRepository();
	FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
	DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

	DeleteCommentCommandHandler handler;

	@BeforeEach
	void setup() {
		dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
		handler = new DeleteCommentCommandHandler(commentRepository, domainEventPublisher, dateTimeProvider);

		Comment initial = Comment.createNew(
				COMMENT_ID,
				TARGET_ID,
				USER_ID,
				null,
				"Body to delete",
				Instant.parse("2023-10-01T10:00:00Z"));
		commentRepository.save(initial);
	}

	@Test
	void should_soft_delete_comment_and_publish_event() {
		// WHEN
		handler.execute(new DeleteCommentCommand(
				CMD_ID,
				COMMENT_ID,
				Instant.parse("2023-10-01T10:59:00Z")));

		// THEN : état
		var snap = commentRepository.allSnapshots().getFirst();
		assertThat(snap.deletedAt()).isEqualTo(dateTimeProvider.instantOfNow);
		assertThat(snap.moderation()).isEqualTo(ModerationStatus.SOFT_DELETED);
		assertThat(snap.version()).isEqualTo(1L);

		// THEN : event
		assertThat(domainEventPublisher.published).hasSize(1);
		var evt = (CommentDeletedEvent) domainEventPublisher.published.getFirst();
		assertThat(evt.commentId()).isEqualTo(COMMENT_ID);
		assertThat(evt.deletedAt()).isEqualTo(dateTimeProvider.instantOfNow);
		assertThat(evt.moderation()).isEqualTo(ModerationStatus.SOFT_DELETED);
	}

	@Test
	void should_be_idempotent_when_already_deleted() {
		// GIVEN : déjà supprimé
		handler.execute(new DeleteCommentCommand(
				CMD_ID,
				COMMENT_ID,
				Instant.parse("2023-10-01T10:59:00Z")));
		domainEventPublisher.published.clear();

		// WHEN : delete à nouveau
		handler.execute(new DeleteCommentCommand(
				UUID.fromString("55555555-5555-5555-5555-555555555555"),
				COMMENT_ID,
				Instant.parse("2023-10-01T11:01:00Z")));

		// THEN : pas de nouvel event, version reste 1
		assertThat(domainEventPublisher.published).isEmpty();
		var snap = commentRepository.allSnapshots().getFirst();
		assertThat(snap.version()).isEqualTo(1L);
	}
}
