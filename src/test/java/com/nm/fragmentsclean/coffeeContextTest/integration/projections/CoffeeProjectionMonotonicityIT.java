package com.nm.fragmentsclean.coffeeContextTest.integration.projections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.JdbcCoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublicationStatus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.sql.init.mode=always")
@Import({JdbcCoffeeProjectionRepository.class, CoffeeCreatedEventHandler.class,
		CoffeeProjectionMonotonicityIT.FailingSyncConfiguration.class})
class CoffeeProjectionMonotonicityIT extends TestContainers {

	private static final UUID COFFEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Autowired
	CoffeeProjectionRepository repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	CoffeeCreatedEventHandler createdEventHandler;

	@Autowired
	FailingProjectionSyncPublisher syncPublisher;

	@BeforeEach
	void resetProjection() {
		syncPublisher.fail = false;
		jdbcTemplate.update("DELETE FROM coffee_summaries_projection WHERE id = ?", COFFEE_ID);
		jdbcTemplate.update("DELETE FROM coffee_projection_checkpoints WHERE coffee_id = ?", COFFEE_ID);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void projection_and_checkpoint_are_rolled_back_when_sync_append_fails() {
		syncPublisher.fail = true;

		assertThatThrownBy(() -> createdEventHandler.handle(createdEvent(1, CoffeePublicationStatus.PUBLISHED)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("sync append failed");

		assertThat(repository.findAll(false)).isEmpty();
		Integer checkpoints = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM coffee_projection_checkpoints WHERE coffee_id = ?",
				Integer.class,
				COFFEE_ID);
		assertThat(checkpoints).isZero();
	}

	@Test
	void published_received_before_created_materializes_a_published_summary_at_latest_version() {
		var published = repository.markPublishedIfNewer(COFFEE_ID, 2, instant(2));
		var created = repository.applyIfNewer(createdEvent(1));

		assertThat(published.applied()).isTrue();
		assertThat(created.applied()).isTrue();
		assertThat(repository.findAll(false))
				.singleElement()
				.satisfies(view -> {
					assertThat(view.id()).isEqualTo(COFFEE_ID);
					assertThat(view.publicationStatus()).isEqualTo("PUBLISHED");
					assertThat(view.version()).isEqualTo(2);
				});
	}

	@Test
	void stale_created_received_after_delete_cannot_resurrect_the_projection() {
		repository.applyIfNewer(createdEvent(1));
		repository.deleteIfNewer(COFFEE_ID, 3, instant(3));

		var replay = repository.applyIfNewer(createdEvent(1));

		assertThat(replay.applied()).isFalse();
		assertThat(repository.findAll(false)).isEmpty();
		assertThat(checkpointVersion()).isEqualTo(3);
	}

	@Test
	void stale_lifecycle_event_cannot_downgrade_a_newer_projection() {
		repository.applyIfNewer(createdEvent(1));
		repository.markPublishedIfNewer(COFFEE_ID, 4, instant(4));

		var staleArchive = repository.markArchivedIfNewer(COFFEE_ID, 3, instant(3));

		assertThat(staleArchive.applied()).isFalse();
		assertThat(repository.findAll(false))
				.singleElement()
				.satisfies(view -> {
					assertThat(view.publicationStatus()).isEqualTo("PUBLISHED");
					assertThat(view.version()).isEqualTo(4);
				});
	}

	private CoffeeCreatedEvent createdEvent(int version) {
		return createdEvent(version, CoffeePublicationStatus.DRAFT);
	}

	private CoffeeCreatedEvent createdEvent(int version, CoffeePublicationStatus publicationStatus) {
		return new CoffeeCreatedEvent(
				UUID.randomUUID(),
				UUID.randomUUID(),
				new CoffeeId(COFFEE_ID),
				new GooglePlaceId("google-place-monotonic"),
				new CoffeeName("Monotonic Coffee"),
				new Address("1 rue Example", "Rennes", "35000", "FR"),
				new GeoPoint(48.11, -1.67),
				null,
				null,
				List.of(),
				publicationStatus,
				version,
				instant(version),
				instant(version));
	}

	private Instant instant(int version) {
		return Instant.parse("2026-08-29T10:00:0" + version + "Z");
	}

	private long checkpointVersion() {
		Long version = jdbcTemplate.queryForObject(
				"SELECT latest_version FROM coffee_projection_checkpoints WHERE coffee_id = ?",
				Long.class,
				COFFEE_ID);
		return version == null ? -1 : version;
	}

	@TestConfiguration
	static class FailingSyncConfiguration {
		@Bean
		@Primary
		FailingProjectionSyncPublisher failingProjectionSyncPublisher() {
			return new FailingProjectionSyncPublisher();
		}

	}

	static class FailingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private boolean fail;

		@Override
		public void publish(ProjectionSyncEvent event) {
			if (fail) throw new IllegalStateException("sync append failed");
		}
	}
}
