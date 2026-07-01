package com.nm.fragmentsclean.sharedKernel.eventing;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventDestinationResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventEnvelopeFactoryTest {

    @Test
    void buildsStableEnvelopeWithoutLeakingJavaClassAsEventType() {
        var outbox = new OutboxEventJpaEntity(
                "evt-1",
                "com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent",
                "Ticket",
                "ticket-1",
                "user:user-1",
                "{\"commandId\":\"00000000-0000-0000-0000-000000000001\"}",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:01Z"),
                OutboxStatus.PENDING,
                0);

        var envelope = new IntegrationEventEnvelopeFactory()
                .from(outbox, IntegrationEventDestinations.TICKET_EVENTS);

        assertThat(envelope.eventType()).isEqualTo("ticket.verification.completed");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.sourceEventClassName()).endsWith("TicketVerificationCompletedEvent");
        assertThat(envelope.destination()).isEqualTo("ticket-events");
        assertThat(envelope.payloadJson()).contains("commandId");
    }

    @Test
    void routesTicketAcceptedToReadProjectionAndWorkerDestinations() {
        var outbox = new OutboxEventJpaEntity(
                "evt-2",
                "com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent",
                "Ticket",
                "ticket-1",
                "user:user-1",
                "{}",
                Instant.now(),
                Instant.now(),
                OutboxStatus.PENDING,
                0);

        assertThat(new IntegrationEventDestinationResolver().destinationsFor(outbox))
                .containsExactly("ticket-events", "ticket-verification-requested");
    }
}
