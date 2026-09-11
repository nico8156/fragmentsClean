package com.nm.fragmentsclean.sharedKernel.eventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExperiencePassIntegrationContractTest {
    @Test
    void maps_only_primitive_progress_fields_into_the_versioned_envelope() throws Exception {
        UUID eventId = UUID.randomUUID(), experienceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID(), coffeeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-11T10:00:00Z");
        var outbox = new OutboxEventJpaEntity(eventId.toString(),
                "com.example.ExperienceLifecycleChangedEvent", "Experience",
                experienceId.toString(), "user:" + userId,
                """
                {"eventId":"%s","experienceId":"%s","userId":"%s","coffeeId":"%s",
                 "publicationStatus":"PUBLISHED","moderationStatus":"VISIBLE","reason":"PUBLISHED",
                 "version":2,"occurredAt":"%s","message":"must not cross boundary"}
                """.formatted(eventId, experienceId, userId, coffeeId, occurredAt),
                occurredAt, occurredAt, OutboxStatus.PENDING, 0);

        var envelope = new IntegrationEventEnvelopeFactory().from(outbox, "app-users-events");
        var payload = new ObjectMapper().findAndRegisterModules().readValue(
                envelope.payloadJson(), ExperienceIntegrationEvents.LifecycleChanged.class);

        assertThat(envelope.eventType()).isEqualTo("experience.lifecycle.changed");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(payload.experienceId()).isEqualTo(experienceId);
        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.coffeeId()).isEqualTo(coffeeId);
        assertThat(payload.publicationStatus()).isEqualTo("PUBLISHED");
        assertThat(payload.moderationStatus()).isEqualTo("VISIBLE");
        assertThat(payload.reason()).isEqualTo("PUBLISHED");
        assertThat(envelope.payloadJson()).doesNotContain("message");
    }
}
