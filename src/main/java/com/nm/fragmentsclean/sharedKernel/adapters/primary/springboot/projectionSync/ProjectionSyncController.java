package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ProjectionSyncController {
	private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

	private final ProjectionSyncDispatcher dispatcher;

	public ProjectionSyncController(ProjectionSyncDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@GetMapping(path = { "/api/sync/events", "/api/admin/sync/events" }, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter events(@RequestHeader(name = LAST_EVENT_ID_HEADER, required = false) String lastEventId) {
		return dispatcher.openStream(lastEventId);
	}
}
