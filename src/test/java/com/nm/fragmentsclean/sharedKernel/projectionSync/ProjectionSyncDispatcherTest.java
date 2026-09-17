package com.nm.fragmentsclean.sharedKernel.projectionSync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncSubscriber;

class ProjectionSyncDispatcherTest {
	@Test
	void opening_stream_schedules_heartbeat() {
		var scheduler = new RecordingTaskScheduler();
		var properties = new ProjectionSyncProperties();
		properties.setTimeoutMs(10_000);
		properties.setHeartbeatIntervalMs(12_345);
		properties.setRetryMs(2_000);

		var dispatcher = new ProjectionSyncDispatcher(
				properties,
				scheduler,
				() -> Instant.parse("2026-07-03T10:00:00Z"),
				new FakeProjectionSyncRepository(12));

		var emitter = dispatcher.openStream(null, ProjectionSyncSubscriber.user("user-1"));

		assertThat(emitter).isNotNull();
		assertThat(scheduler.intervals).contains(Duration.ofMillis(12_345));
		assertThat(scheduler.task).isNotNull();
	}

	@Test
	void opening_stream_with_last_event_id_replays_from_cursor() {
		var scheduler = new RecordingTaskScheduler();
		var properties = new ProjectionSyncProperties();
		properties.setTimeoutMs(10_000);
		properties.setHeartbeatIntervalMs(60_000);
		properties.setPollIntervalMs(60_000);
		properties.setReplayBatchSize(10);
		var repository = new FakeProjectionSyncRepository(2);
		repository.events.add(ProjectionSyncEvent.publicProjectionUpdated(
				"coffees",
				"entity",
				"coffee-3",
				3L,
				Instant.parse("2026-07-03T10:00:03Z"),
				List.of("summary")).withId("3"));

		var dispatcher = new ProjectionSyncDispatcher(
				properties,
				scheduler,
				() -> Instant.parse("2026-07-03T10:00:00Z"),
				repository);

		dispatcher.openStream("2", ProjectionSyncSubscriber.user("user-1"));

		assertThat(repository.lastFindAfter).isEqualTo(2);
	}

	@Test
	void user_subscriber_cannot_receive_another_users_or_admin_events() {
		var subscriber = ProjectionSyncSubscriber.user("user-1");
		Instant now = Instant.parse("2026-07-03T10:00:00Z");

		assertThat(subscriber.mayReceive(ProjectionSyncEvent.publicProjectionUpdated(
				"coffees", "entity", "coffee-1", 1L, now, List.of()))).isTrue();
		assertThat(subscriber.mayReceive(ProjectionSyncEvent.userProjectionUpdated(
				"user-1", "tickets", "entity", "ticket-1", 1L, now, List.of()))).isTrue();
		assertThat(subscriber.mayReceive(ProjectionSyncEvent.userProjectionUpdated(
				"user-2", "tickets", "entity", "ticket-2", 1L, now, List.of()))).isFalse();
		assertThat(subscriber.mayReceive(ProjectionSyncEvent.adminProjectionUpdated(
				"moderation", "report", "report-1", 1L, now, List.of()))).isFalse();
	}

	@Test
	void admin_subscriber_receives_public_and_admin_but_not_user_private_events() {
		var subscriber = ProjectionSyncSubscriber.admin();
		Instant now = Instant.parse("2026-07-03T10:00:00Z");

		assertThat(subscriber.mayReceive(ProjectionSyncEvent.publicProjectionUpdated(
				"coffees", "entity", "coffee-1", 1L, now, List.of()))).isTrue();
		assertThat(subscriber.mayReceive(ProjectionSyncEvent.userProjectionUpdated(
				"user-1", "tickets", "entity", "ticket-1", 1L, now, List.of()))).isFalse();
		assertThat(subscriber.mayReceive(ProjectionSyncEvent.adminProjectionUpdated(
				"moderation", "report", "report-1", 1L, now, List.of()))).isTrue();
	}

	@Test
	void filtered_events_advance_the_global_cursor() {
		var scheduler = new RecordingTaskScheduler();
		var properties = new ProjectionSyncProperties();
		properties.setTimeoutMs(10_000);
		properties.setHeartbeatIntervalMs(60_000);
		properties.setPollIntervalMs(60_000);
		properties.setReplayBatchSize(10);
		var repository = new FakeProjectionSyncRepository(0);
		repository.events.add(ProjectionSyncEvent.userProjectionUpdated(
				"user-2", "tickets", "entity", "ticket-2", 1L,
				Instant.parse("2026-07-03T10:00:01Z"), List.of()).withId("1"));

		var dispatcher = new ProjectionSyncDispatcher(
				properties, scheduler, () -> Instant.parse("2026-07-03T10:00:00Z"), repository);

		dispatcher.openStream("0", ProjectionSyncSubscriber.user("user-1"));
		scheduler.pollingTask.run();

		assertThat(repository.findAfterCursors).containsExactly(0L, 1L);
	}

	private static class RecordingTaskScheduler implements TaskScheduler {
		private Runnable task;
		private Runnable pollingTask;
		private final List<Duration> intervals = new ArrayList<>();

		@Override
		public ScheduledFuture<?> schedule(Runnable task, java.time.Instant startTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.time.Instant startTime, Duration period) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
			this.task = task;
			if (intervals.size() == 1) {
				this.pollingTask = task;
			}
			this.intervals.add(period);
			return new CompletedScheduledFuture();
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.time.Instant startTime, Duration delay) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
			throw new UnsupportedOperationException();
		}
	}

	private static class FakeProjectionSyncRepository implements ProjectionSyncRepository {
		private final List<ProjectionSyncEvent> events = new ArrayList<>();
		private final long currentOffset;
		private long lastFindAfter = -1;
		private final List<Long> findAfterCursors = new ArrayList<>();

		FakeProjectionSyncRepository(long currentOffset) {
			this.currentOffset = currentOffset;
		}

		@Override
		public ProjectionSyncEvent append(ProjectionSyncEvent event) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ProjectionSyncEvent> findAfter(long lastEventId, int limit) {
			this.lastFindAfter = lastEventId;
			this.findAfterCursors.add(lastEventId);
			return events.stream()
					.filter(event -> Long.parseLong(event.id()) > lastEventId)
					.limit(limit)
					.toList();
		}

		@Override
		public long currentOffset() {
			return currentOffset;
		}
	}

	private static class CompletedScheduledFuture implements ScheduledFuture<Object> {
		@Override
		public long getDelay(java.util.concurrent.TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(java.util.concurrent.Delayed other) {
			return 0;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return true;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Object get() {
			return null;
		}

		@Override
		public Object get(long timeout, java.util.concurrent.TimeUnit unit) {
			return null;
		}
	}
}
