package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;

class ProjectionSyncSseEventTest {
	@Test
	void public_transport_payload_does_not_disclose_internal_audience_or_recipient() throws Exception {
		var internal = ProjectionSyncEvent.userProjectionUpdated(
				"user-private",
				"tickets",
				"entity",
				"ticket-1",
				3L,
				Instant.parse("2026-09-17T10:00:00Z"),
				List.of("status"));

		String json = new ObjectMapper().findAndRegisterModules()
				.writeValueAsString(ProjectionSyncSseEvent.from(internal));

		assertThat(json).contains("\"projection\":\"tickets\"");
		assertThat(json).doesNotContain("user-private", "recipientId", "audience");
	}
}
