package com.nm.fragmentsclean.sharedKernel.projectionSync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncProperties;

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
				() -> Instant.parse("2026-07-03T10:00:00Z"));

		var emitter = dispatcher.openStream(null);

		assertThat(emitter).isNotNull();
		assertThat(scheduler.interval).isEqualTo(Duration.ofMillis(12_345));
		assertThat(scheduler.task).isNotNull();
	}

	private static class RecordingTaskScheduler implements TaskScheduler {
		private Runnable task;
		private Duration interval;

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
			this.interval = period;
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
