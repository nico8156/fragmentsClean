package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcCommentProjectionRepository;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCreatedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentDeletedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentUpdatedEventHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;

class CommentProjectionSyncEventHandlerTest {

	@Test
	void created_applies_projection_then_publishes_target_comments_sync_event() {
		var repository = new RecordingCommentProjectionRepository();
		var publisher = new RecordingProjectionSyncPublisher();
		var handler = new CommentCreatedEventHandler(repository, publisher);
		var event = commentCreatedEvent();

		handler.handle(event);

		assertThat(repository.createdEvents).containsExactly(event);
		assertProjectionUpdated(publisher.events.getFirst(), event.targetId(), event.version(), event.occurredAt(), "created");
	}

	@Test
	void updated_applies_projection_then_publishes_target_comments_sync_event() {
		var repository = new RecordingCommentProjectionRepository();
		var publisher = new RecordingProjectionSyncPublisher();
		var handler = new CommentUpdatedEventHandler(repository, publisher);
		var event = commentUpdatedEvent();

		handler.handle(event);

		assertThat(repository.updatedEvents).containsExactly(event);
		assertProjectionUpdated(publisher.events.getFirst(), event.targetId(), event.version(), event.occurredAt(), "updated");
	}

	@Test
	void deleted_applies_projection_then_publishes_target_comments_sync_event() {
		var repository = new RecordingCommentProjectionRepository();
		var publisher = new RecordingProjectionSyncPublisher();
		var handler = new CommentDeletedEventHandler(repository, publisher);
		var event = commentDeletedEvent();

		handler.handle(event);

		assertThat(repository.deletedEvents).containsExactly(event);
		assertProjectionUpdated(publisher.events.getFirst(), event.targetId(), event.version(), event.occurredAt(), "deleted");
	}

	private void assertProjectionUpdated(
			ProjectionSyncEvent event,
			UUID targetId,
			long version,
			Instant changedAt,
			String hint) {
		assertThat(event.eventName()).isEqualTo("projection.updated");
		assertThat(event.projection()).isEqualTo("comments");
		assertThat(event.scope()).isEqualTo("target");
		assertThat(event.entityId()).isEqualTo(targetId.toString());
		assertThat(event.version()).isEqualTo(version);
		assertThat(event.changedAt()).isEqualTo(changedAt);
		assertThat(event.hints()).containsExactly(hint);
	}

	private CommentCreatedEvent commentCreatedEvent() {
		return new CommentCreatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				targetId(),
				null,
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				"hello",
				ModerationStatus.PUBLISHED,
				3L,
				Instant.parse("2026-07-05T08:00:00Z"),
				Instant.parse("2026-07-05T07:59:59Z"));
	}

	private CommentUpdatedEvent commentUpdatedEvent() {
		return new CommentUpdatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbc"),
				UUID.fromString("11111111-1111-1111-1111-111111111112"),
				targetId(),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				"edited",
				ModerationStatus.PUBLISHED,
				4L,
				Instant.parse("2026-07-05T08:01:00Z"),
				Instant.parse("2026-07-05T08:00:59Z"));
	}

	private CommentDeletedEvent commentDeletedEvent() {
		return new CommentDeletedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaac"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbd"),
				UUID.fromString("11111111-1111-1111-1111-111111111113"),
				targetId(),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				ModerationStatus.SOFT_DELETED,
				Instant.parse("2026-07-05T08:02:00Z"),
				5L,
				Instant.parse("2026-07-05T08:02:01Z"),
				Instant.parse("2026-07-05T08:01:59Z"));
	}

	private UUID targetId() {
		return UUID.fromString("33333333-3333-3333-3333-333333333333");
	}

	private static class RecordingCommentProjectionRepository extends JdbcCommentProjectionRepository {
		private final List<CommentCreatedEvent> createdEvents = new ArrayList<>();
		private final List<CommentUpdatedEvent> updatedEvents = new ArrayList<>();
		private final List<CommentDeletedEvent> deletedEvents = new ArrayList<>();

		private RecordingCommentProjectionRepository() {
			super(null);
		}

		@Override
		public void apply(CommentCreatedEvent event) {
			createdEvents.add(event);
		}

		@Override
		public void apply(CommentUpdatedEvent event) {
			updatedEvents.add(event);
		}

		@Override
		public void apply(CommentDeletedEvent event) {
			deletedEvents.add(event);
		}
	}

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<ProjectionSyncEvent> events = new ArrayList<>();

		@Override
		public void publish(ProjectionSyncEvent event) {
			events.add(event);
		}
	}
}
