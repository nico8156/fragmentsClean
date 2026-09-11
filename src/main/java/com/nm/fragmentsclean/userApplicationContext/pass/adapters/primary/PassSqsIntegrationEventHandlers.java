package com.nm.fragmentsclean.userApplicationContext.pass.adapters.primary;

import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.userApplicationContext.pass.application.PassContributionProjector;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassProgressPolicy;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.APP_USERS_EVENTS;

@Configuration
public class PassSqsIntegrationEventHandlers {
    private final SqsIntegrationEventPayloadReader payloadReader;

    public PassSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Bean
    SqsIntegrationEventHandler passTicketCompletedHandler(PassContributionProjector projector,
                                                           ProjectionSyncPublisher sync) {
        return handler("ticket.verification.completed", envelope -> {
            var event = payloadReader.read(envelope, TicketIntegrationEvents.VerificationCompleted.class);
            publish(projector.ticketChanged(event.ticketId(), event.userId(), "APPROVED".equals(event.outcome()),
                    false, event.version(), event.occurredAt()), sync);
        });
    }

    @Bean
    SqsIntegrationEventHandler passTicketAdminUpdatedHandler(PassContributionProjector projector,
                                                              ProjectionSyncPublisher sync) {
        return handler("ticket.admin.updated", envelope -> {
            var event = payloadReader.read(envelope, TicketIntegrationEvents.AdminUpdated.class);
            publish(projector.ticketChanged(event.ticketId(), event.userId(), "CONFIRMED".equals(event.status()),
                    true, event.version(), event.occurredAt()), sync);
        });
    }

    @Bean
    SqsIntegrationEventHandler passTicketAdminDeletedHandler(PassContributionProjector projector,
                                                              ProjectionSyncPublisher sync) {
        return handler("ticket.admin.deleted", envelope -> {
            var event = payloadReader.read(envelope, TicketIntegrationEvents.AdminDeleted.class);
            publish(projector.ticketChanged(event.ticketId(), event.userId(), false,
                    true, event.version(), event.occurredAt()), sync);
        });
    }

    @Bean
    SqsIntegrationEventHandler passExperienceContributionHandler(PassContributionProjector projector,
                                                                  ProjectionSyncPublisher sync) {
        return handler("experience.lifecycle.changed", envelope -> {
            var event = payloadReader.read(envelope, ExperienceIntegrationEvents.LifecycleChanged.class);
            boolean active = PassProgressPolicy.countsExperience(
                    event.publicationStatus(), event.moderationStatus());
            publish(projector.experienceChanged(event.experienceId(), event.userId(), event.coffeeId(), active,
                    PassProgressPolicy.revokesAcquiredLevels(active, event.reason()),
                    event.version(), event.occurredAt()), sync);
        });
    }

    private SqsIntegrationEventHandler handler(String eventType, Consumer<IntegrationEventEnvelope> consumer) {
        return new SimpleHandler(new SqsIntegrationEventRoute(APP_USERS_EVENTS, eventType), consumer);
    }

    private void publish(Optional<PassSnapshot> applied, ProjectionSyncPublisher sync) {
        applied.ifPresent(pass -> sync.publish(ProjectionSyncEvent.projectionUpdated(
                "entitlements", "user", pass.userId().toString(), pass.version(), pass.updatedAt(),
                List.of("pass", "policy-v" + pass.policyVersion()))));
    }

    private record SimpleHandler(SqsIntegrationEventRoute route, Consumer<IntegrationEventEnvelope> consumer)
            implements SqsIntegrationEventHandler {
        @Override public void handle(IntegrationEventEnvelope envelope) { consumer.accept(envelope); }
    }
}
