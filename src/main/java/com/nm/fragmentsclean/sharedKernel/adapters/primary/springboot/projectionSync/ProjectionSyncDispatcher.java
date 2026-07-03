package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;

@Component
public class ProjectionSyncDispatcher {
	private final ProjectionSyncProperties properties;
	private final TaskScheduler taskScheduler;
	private final DateTimeProvider dateTimeProvider;

	public ProjectionSyncDispatcher(
			ProjectionSyncProperties properties,
			TaskScheduler projectionSyncTaskScheduler,
			DateTimeProvider dateTimeProvider) {
		this.properties = properties;
		this.taskScheduler = projectionSyncTaskScheduler;
		this.dateTimeProvider = dateTimeProvider;
	}

	public SseEmitter openStream(String lastEventId) {
		var emitter = new SseEmitter(properties.getTimeoutMs());
		var heartbeatTask = new AtomicReference<ScheduledFuture<?>>();

		emitter.onCompletion(() -> cancel(heartbeatTask.get()));
		emitter.onTimeout(() -> {
			cancel(heartbeatTask.get());
			emitter.complete();
		});
		emitter.onError(error -> cancel(heartbeatTask.get()));

		send(emitter, ProjectionSyncEvent.connected(dateTimeProvider.now()));

		var scheduled = taskScheduler.scheduleAtFixedRate(
				() -> sendHeartbeat(emitter, heartbeatTask),
				Duration.ofMillis(properties.getHeartbeatIntervalMs()));
		heartbeatTask.set(scheduled);

		return emitter;
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

	private static class ProjectionSyncDeliveryException extends RuntimeException {
		ProjectionSyncDeliveryException(Throwable cause) {
			super(cause);
		}
	}
}
