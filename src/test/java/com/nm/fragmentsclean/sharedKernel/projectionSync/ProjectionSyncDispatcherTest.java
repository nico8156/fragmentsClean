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

		var emitter = dispatcher.openStream(null);

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
		repository.events.add(ProjectionSyncEvent.projectionUpdated(
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

		dispatcher.openStream("2");

		assertThat(repository.lastFindAfter).isEqualTo(2);
	}

	private static class RecordingTaskScheduler implements TaskScheduler {
		private Runnable task;
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
