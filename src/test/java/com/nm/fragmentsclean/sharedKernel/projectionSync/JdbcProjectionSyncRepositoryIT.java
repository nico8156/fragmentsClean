package com.nm.fragmentsclean.sharedKernel.projectionSync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncRepository;

@SpringBootTest
@ActiveProfiles("database")
class JdbcProjectionSyncRepositoryIT extends TestContainers {
	@Autowired
	ProjectionSyncRepository repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void reset() {
		jdbcTemplate.execute("TRUNCATE TABLE projection_sync_events RESTART IDENTITY");
	}

	@Test
	void append_persists_projection_sync_event_and_assigns_offset() {
		ProjectionSyncEvent stored = repository.append(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				"coffee-1",
				42L,
				Instant.parse("2026-07-03T10:00:00Z"),
				List.of("summary")));

		assertThat(stored.id()).isEqualTo("1");
		assertThat(repository.currentOffset()).isEqualTo(1);

		List<ProjectionSyncEvent> replay = repository.findAfter(0, 10);
		assertThat(replay).hasSize(1);
		assertThat(replay.getFirst().id()).isEqualTo("1");
		assertThat(replay.getFirst().eventName()).isEqualTo("projection.updated");
		assertThat(replay.getFirst().projection()).isEqualTo("coffees");
		assertThat(replay.getFirst().scope()).isEqualTo("entity");
		assertThat(replay.getFirst().entityId()).isEqualTo("coffee-1");
		assertThat(replay.getFirst().version()).isEqualTo(42L);
		assertThat(replay.getFirst().hints()).containsExactly("summary");
	}

	@Test
	void find_after_replays_only_events_after_last_event_id() {
		repository.append(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				"coffee-1",
				1L,
				Instant.parse("2026-07-03T10:00:00Z"),
				List.of("summary")));
		repository.append(ProjectionSyncEvent.projectionUpdated(
				"articles",
				"collection",
				null,
				2L,
				Instant.parse("2026-07-03T10:00:01Z"),
				List.of()));

		List<ProjectionSyncEvent> replay = repository.findAfter(1, 10);

		assertThat(replay).hasSize(1);
		assertThat(replay.getFirst().id()).isEqualTo("2");
		assertThat(replay.getFirst().projection()).isEqualTo("articles");
	}
}
