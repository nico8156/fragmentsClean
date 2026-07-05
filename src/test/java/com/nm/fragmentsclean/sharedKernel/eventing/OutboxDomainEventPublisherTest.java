package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.platform.eventing.DefaultOutboxEventMetadataResolver;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.OutboxDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class OutboxDomainEventPublisherTest {

	@Test
	void persists_coffee_photos_imported_as_coffee_aggregate() {
		var saved = new AtomicReference<OutboxEventJpaEntity>();
		var repository = outboxRepositoryCapturing(saved);
		var publisher = new OutboxDomainEventPublisher(
				repository,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				() -> Instant.parse("2026-07-04T10:30:00Z"),
				new DefaultOutboxEventMetadataResolver());
		var event = new CoffeePhotosImportedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("places/google-1"),
				List.of(new ImportedCoffeePhoto(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
						"https://cdn.fragments.test/photo-1.jpg")),
				12,
				Instant.parse("2026-07-04T10:20:00Z"),
				Instant.parse("2026-07-04T10:19:59Z"));

		publisher.publish(event);

		assertThat(saved.get()).isNotNull();
		assertThat(saved.get().getAggregateType()).isEqualTo("Coffee");
		assertThat(saved.get().getAggregateId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getStreamKey()).isEqualTo("coffee:11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getEventType()).endsWith("CoffeePhotosImportedEvent");
	}

	@Test
	void persists_coffee_archived_as_coffee_aggregate() {
		var saved = new AtomicReference<OutboxEventJpaEntity>();
		var repository = outboxRepositoryCapturing(saved);
		var publisher = new OutboxDomainEventPublisher(
				repository,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				() -> Instant.parse("2026-07-04T11:00:00Z"),
				new DefaultOutboxEventMetadataResolver());
		var event = new CoffeeArchivedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				13,
				Instant.parse("2026-07-04T10:59:00Z"),
				Instant.parse("2026-07-04T10:58:59Z"));

		publisher.publish(event);

		assertThat(saved.get()).isNotNull();
		assertThat(saved.get().getAggregateType()).isEqualTo("Coffee");
		assertThat(saved.get().getAggregateId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getStreamKey()).isEqualTo("coffee:11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getEventType()).endsWith("CoffeeArchivedEvent");
	}

	@Test
	void persists_coffee_deleted_as_coffee_aggregate() {
		var saved = new AtomicReference<OutboxEventJpaEntity>();
		var repository = outboxRepositoryCapturing(saved);
		var publisher = new OutboxDomainEventPublisher(
				repository,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				() -> Instant.parse("2026-07-04T11:00:00Z"),
				new DefaultOutboxEventMetadataResolver());
		var event = new CoffeeDeletedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				13,
				Instant.parse("2026-07-04T10:59:00Z"),
				Instant.parse("2026-07-04T10:58:59Z"));

		publisher.publish(event);

		assertThat(saved.get()).isNotNull();
		assertThat(saved.get().getAggregateType()).isEqualTo("Coffee");
		assertThat(saved.get().getAggregateId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getStreamKey()).isEqualTo("coffee:11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getEventType()).endsWith("CoffeeDeletedEvent");
	}

	@Test
	void persists_admin_photo_added_as_coffee_aggregate() {
		var saved = new AtomicReference<OutboxEventJpaEntity>();
		var repository = outboxRepositoryCapturing(saved);
		var publisher = new OutboxDomainEventPublisher(
				repository,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				() -> Instant.parse("2026-07-05T10:00:00Z"),
				new DefaultOutboxEventMetadataResolver());
		var event = new CoffeePhotoAddedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new ImportedCoffeePhoto(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
						"s3://bucket/photo-1.jpg"),
				14,
				Instant.parse("2026-07-05T09:59:00Z"),
				Instant.parse("2026-07-05T09:58:59Z"));

		publisher.publish(event);

		assertThat(saved.get()).isNotNull();
		assertThat(saved.get().getAggregateType()).isEqualTo("Coffee");
		assertThat(saved.get().getAggregateId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getStreamKey()).isEqualTo("coffee:11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getEventType()).endsWith("CoffeePhotoAddedEvent");
	}

	@Test
	void persists_admin_photo_deleted_as_coffee_aggregate() {
		var saved = new AtomicReference<OutboxEventJpaEntity>();
		var repository = outboxRepositoryCapturing(saved);
		var publisher = new OutboxDomainEventPublisher(
				repository,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				() -> Instant.parse("2026-07-05T10:00:00Z"),
				new DefaultOutboxEventMetadataResolver());
		var event = new CoffeePhotoDeletedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new PhotoId(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")),
				15,
				Instant.parse("2026-07-05T09:59:00Z"),
				Instant.parse("2026-07-05T09:58:59Z"));

		publisher.publish(event);

		assertThat(saved.get()).isNotNull();
		assertThat(saved.get().getAggregateType()).isEqualTo("Coffee");
		assertThat(saved.get().getAggregateId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getStreamKey()).isEqualTo("coffee:11111111-1111-1111-1111-111111111111");
		assertThat(saved.get().getEventType()).endsWith("CoffeePhotoDeletedEvent");
	}

	private static SpringOutboxEventRepository outboxRepositoryCapturing(AtomicReference<OutboxEventJpaEntity> saved) {
		return (SpringOutboxEventRepository) Proxy.newProxyInstance(
				SpringOutboxEventRepository.class.getClassLoader(),
				new Class<?>[]{SpringOutboxEventRepository.class},
				(proxy, method, args) -> {
					if ("save".equals(method.getName())) {
						saved.set((OutboxEventJpaEntity) args[0]);
						return args[0];
					}
					if ("toString".equals(method.getName())) {
						return "capturing-outbox-repository";
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}
}
