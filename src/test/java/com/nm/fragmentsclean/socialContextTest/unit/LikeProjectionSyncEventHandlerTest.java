package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSetEventHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LikeProjectionSyncEventHandlerTest {

	@Test
	void like_set_applies_projection_then_publishes_target_likes_sync_event() {
		var repository = new RecordingLikeProjectionRepository();
		var publisher = new RecordingProjectionSyncPublisher(repository.operations);
		var handler = new LikeSetEventHandler(repository, publisher);
		var event = likeSetEvent();

		handler.handle(event);

		assertThat(repository.operations).containsExactly("projection", "sync");
		assertThat(repository.events).containsExactly(event);
		assertThat(publisher.events).hasSize(1);
		ProjectionSyncEvent syncEvent = publisher.events.getFirst();
		assertThat(syncEvent.eventName()).isEqualTo("projection.updated");
		assertThat(syncEvent.projection()).isEqualTo("likes");
		assertThat(syncEvent.scope()).isEqualTo("target");
		assertThat(syncEvent.entityId()).isEqualTo(event.targetId().toString());
		assertThat(syncEvent.version()).isEqualTo(event.version());
		assertThat(syncEvent.changedAt()).isEqualTo(event.occurredAt());
		assertThat(syncEvent.hints()).containsExactly("set");
	}

	private LikeSetEvent likeSetEvent() {
		return new LikeSetEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				UUID.fromString("33333333-3333-3333-3333-333333333333"),
				true,
				1L,
				3L,
				Instant.parse("2026-07-05T08:00:00Z"),
				Instant.parse("2026-07-05T07:59:59Z"));
	}

	private static class RecordingLikeProjectionRepository extends JdbcLikeProjectionRepository {
		private final List<String> operations = new ArrayList<>();
		private final List<LikeSetEvent> events = new ArrayList<>();

		private RecordingLikeProjectionRepository() {
			super(null);
		}

		@Override
		public void apply(LikeSetEvent event) {
			operations.add("projection");
			events.add(event);
		}
	}

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<String> operations;
		private final List<ProjectionSyncEvent> events = new ArrayList<>();

		private RecordingProjectionSyncPublisher(List<String> operations) {
			this.operations = operations;
		}

		@Override
		public void publish(ProjectionSyncEvent event) {
			operations.add("sync");
			events.add(event);
		}
	}
}
