package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncRepository;

@Component
public class ProjectionSyncDispatcher {
	private final ProjectionSyncProperties properties;
	private final TaskScheduler taskScheduler;
	private final DateTimeProvider dateTimeProvider;
	private final ProjectionSyncRepository repository;

	public ProjectionSyncDispatcher(
			ProjectionSyncProperties properties,
			TaskScheduler projectionSyncTaskScheduler,
			DateTimeProvider dateTimeProvider,
			ProjectionSyncRepository repository) {
		this.properties = properties;
		this.taskScheduler = projectionSyncTaskScheduler;
		this.dateTimeProvider = dateTimeProvider;
		this.repository = repository;
	}

	public SseEmitter openStream(String lastEventId) {
		var emitter = new SseEmitter(properties.getTimeoutMs());
		var heartbeatTask = new AtomicReference<ScheduledFuture<?>>();
		var pollingTask = new AtomicReference<ScheduledFuture<?>>();
		var cursor = new AtomicLong(resolveInitialCursor(lastEventId));

		emitter.onCompletion(() -> cancelAll(heartbeatTask.get(), pollingTask.get()));
		emitter.onTimeout(() -> {
			cancelAll(heartbeatTask.get(), pollingTask.get());
			emitter.complete();
		});
		emitter.onError(error -> cancelAll(heartbeatTask.get(), pollingTask.get()));

		send(emitter, ProjectionSyncEvent.connected(dateTimeProvider.now()));
		replayAvailable(emitter, cursor);

		var scheduled = taskScheduler.scheduleAtFixedRate(
				() -> sendHeartbeat(emitter, heartbeatTask),
				Duration.ofMillis(properties.getHeartbeatIntervalMs()));
		heartbeatTask.set(scheduled);

		var polling = taskScheduler.scheduleAtFixedRate(
				() -> poll(emitter, cursor, pollingTask),
				Duration.ofMillis(properties.getPollIntervalMs()));
		pollingTask.set(polling);

		return emitter;
	}

	private long resolveInitialCursor(String lastEventId) {
		if (lastEventId == null || lastEventId.isBlank()) {
			return repository.currentOffset();
		}
		try {
			return Long.parseLong(lastEventId);
		} catch (NumberFormatException ignored) {
			return repository.currentOffset();
		}
	}

	private void poll(SseEmitter emitter, AtomicLong cursor, AtomicReference<ScheduledFuture<?>> pollingTask) {
		try {
			replayAvailable(emitter, cursor);
		} catch (RuntimeException error) {
			cancel(pollingTask.get());
			emitter.completeWithError(error);
		}
	}

	private void replayAvailable(SseEmitter emitter, AtomicLong cursor) {
		var events = repository.findAfter(cursor.get(), properties.getReplayBatchSize());
		for (ProjectionSyncEvent event : events) {
			send(emitter, event);
			cursor.set(Long.parseLong(event.id()));
		}
	}

	private void sendHeartbeat(SseEmitter emitter, AtomicReference<ScheduledFuture<?>> heartbeatTask) {
		try {
			send(emitter, ProjectionSyncEvent.heartbeat(dateTimeProvider.now()));
		} catch (RuntimeException error) {
			cancel(heartbeatTask.get());
			emitter.completeWithError(error);
		}
	}

	private void send(SseEmitter emitter, ProjectionSyncEvent event) {
		try {
			var builder = SseEmitter.event()
					.name(event.eventName())
					.reconnectTime(properties.getRetryMs())
					.data(event);
			if (event.id() != null && !event.id().isBlank()) {
				builder.id(event.id());
			}
			emitter.send(builder);
		} catch (IOException | IllegalStateException error) {
			throw new ProjectionSyncDeliveryException(error);
		}
	}

	private void cancel(ScheduledFuture<?> future) {
		if (future != null) {
			future.cancel(false);
		}
	}

	private void cancelAll(ScheduledFuture<?>... futures) {
		for (ScheduledFuture<?> future : futures) {
			cancel(future);
		}
	}

	private static class ProjectionSyncDeliveryException extends RuntimeException {
		ProjectionSyncDeliveryException(Throwable cause) {
			super(cause);
		}
	}
}
