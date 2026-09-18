package com.nm.fragmentsclean.sharedKernel.projectionSync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncController;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncDispatcher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncSubscriber;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectionSyncControllerTest {
	private static final String USER_ID = "22222222-2222-2222-2222-222222222222";

	@Test
	void opens_user_sse_stream_scoped_to_authenticated_subject() throws Exception {
		var dispatcher = new FakeProjectionSyncDispatcher();
		MvcResult result = mockMvc(dispatcher).perform(get("/api/sync/events").principal(() -> USER_ID))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andReturn();

		MockHttpServletResponse response = result.getResponse();
		assertThat(response.getContentType()).startsWith("text/event-stream");
		assertThat(dispatcher.lastEventId).isNull();
		assertThat(dispatcher.subscriber).isEqualTo(ProjectionSyncSubscriber.user(USER_ID));
	}

	@Test
	void opens_admin_sse_stream_with_same_contract() throws Exception {
		var dispatcher = new FakeProjectionSyncDispatcher();
		MvcResult result = mockMvc(dispatcher).perform(get("/api/admin/sync/events"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andReturn();

		assertThat(result.getResponse().getContentType()).startsWith("text/event-stream");
		assertThat(dispatcher.subscriber).isEqualTo(ProjectionSyncSubscriber.admin());
	}

	@Test
	void forwards_last_event_id_to_dispatcher() throws Exception {
		var dispatcher = new FakeProjectionSyncDispatcher();
		mockMvc(dispatcher).perform(get("/api/sync/events")
						.principal(() -> USER_ID)
						.header("Last-Event-ID", "42"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted());

		assertThat(dispatcher.lastEventId).isEqualTo("42");
	}

	private MockMvc mockMvc(FakeProjectionSyncDispatcher dispatcher) {
		return MockMvcBuilders.standaloneSetup(new ProjectionSyncController(dispatcher)).build();
	}

	private static class FakeProjectionSyncDispatcher extends ProjectionSyncDispatcher {
		private String lastEventId;
		private ProjectionSyncSubscriber subscriber;

		FakeProjectionSyncDispatcher() {
			super(null, null, null, null);
		}

		@Override
		public SseEmitter openStream(String lastEventId, ProjectionSyncSubscriber subscriber) {
			this.lastEventId = lastEventId;
			this.subscriber = subscriber;
			var emitter = new SseEmitter(1_000L);
			try {
				emitter.send(SseEmitter.event()
						.name("sync.connected")
						.data(ProjectionSyncEvent.connected(Instant.parse("2026-07-03T10:00:00Z"))));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return emitter;
		}
	}
}
