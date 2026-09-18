package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import java.security.Principal;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncSubscriber;

@RestController
public class ProjectionSyncController {
	private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

	private final ProjectionSyncDispatcher dispatcher;

	public ProjectionSyncController(ProjectionSyncDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@GetMapping(path = "/api/sync/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter userEvents(
			Principal principal,
			@RequestHeader(name = LAST_EVENT_ID_HEADER, required = false) String lastEventId) {
		return dispatcher.openStream(lastEventId, ProjectionSyncSubscriber.user(principal.getName()));
	}

	@GetMapping(path = "/api/admin/sync/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter adminEvents(
			@RequestHeader(name = LAST_EVENT_ID_HEADER, required = false) String lastEventId) {
		return dispatcher.openStream(lastEventId, ProjectionSyncSubscriber.admin());
	}
}
