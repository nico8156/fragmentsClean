package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqsIntegrationEventRouterTest {

    @Test
    void dispatches_registered_handler_and_marks_message_processed() {
        var inbox = new FakeInboxMessageRepository();
        var handler = new RecordingHandler(route());
        var router = new SqsIntegrationEventRouter(inbox, List.of(handler));
        var envelope = envelope("event-1", "coffee.created");

        router.route(envelope);

        assertThat(handler.handled).containsExactly(envelope);
        assertThat(inbox.processed).containsExactly(envelope);
        assertThat(inbox.failed).isEmpty();
    }

    @Test
    void suppresses_processed_duplicate_without_dispatching_handler() {
        var inbox = new FakeInboxMessageRepository();
        inbox.claimResult = false;
        var handler = new RecordingHandler(route());
        var router = new SqsIntegrationEventRouter(inbox, List.of(handler));

        router.route(envelope("event-1", "coffee.created"));

        assertThat(handler.handled).isEmpty();
        assertThat(inbox.processed).isEmpty();
        assertThat(inbox.failed).isEmpty();
    }

    @Test
    void marks_message_failed_and_rethrows_when_handler_fails() {
        var inbox = new FakeInboxMessageRepository();
        var failure = new IllegalStateException("handler failed");
        var handler = new RecordingHandler(route());
        handler.failure = failure;
        var router = new SqsIntegrationEventRouter(inbox, List.of(handler));
        var envelope = envelope("event-1", "coffee.created");

        assertThatThrownBy(() -> router.route(envelope)).isSameAs(failure);

        assertThat(handler.handled).containsExactly(envelope);
        assertThat(inbox.processed).isEmpty();
        assertThat(inbox.failed).containsExactly(envelope);
    }

    @Test
    void ignores_unknown_route_after_claiming_message() {
        var inbox = new FakeInboxMessageRepository();
        var handler = new RecordingHandler(route());
        var router = new SqsIntegrationEventRouter(inbox, List.of(handler));
        var envelope = envelope("event-1", "coffee.unknown");

        router.route(envelope);

        assertThat(handler.handled).isEmpty();
        assertThat(inbox.processed).containsExactly(envelope);
        assertThat(inbox.failed).isEmpty();
    }

    @Test
    void rejects_duplicate_registered_routes_at_startup() {
        var first = new RecordingHandler(route());
        var second = new RecordingHandler(route());

        assertThatThrownBy(() -> new SqsIntegrationEventRouter(new FakeInboxMessageRepository(), List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate SQS integration event route");
    }

    private static SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute("coffees-events", "coffee.created");
    }

    private static IntegrationEventEnvelope envelope(String eventId, String eventType) {
        return new IntegrationEventEnvelope(
                eventId,
                eventType,
                1,
                "com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "coffees-events",
                "{}",
                Instant.parse("2026-07-05T10:00:00Z"));
    }

    private static class RecordingHandler implements SqsIntegrationEventHandler {
        private final SqsIntegrationEventRoute route;
        private final List<IntegrationEventEnvelope> handled = new ArrayList<>();
        private RuntimeException failure;

        private RecordingHandler(SqsIntegrationEventRoute route) {
            this.route = route;
        }

        @Override
        public SqsIntegrationEventRoute route() {
            return route;
        }

        @Override
        public void handle(IntegrationEventEnvelope envelope) {
            handled.add(envelope);
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static class FakeInboxMessageRepository extends InboxMessageRepository {
        private boolean claimResult = true;
        private final List<IntegrationEventEnvelope> processed = new ArrayList<>();
        private final List<IntegrationEventEnvelope> failed = new ArrayList<>();

        private FakeInboxMessageRepository() {
            super(null);
        }

        @Override
        public boolean claim(IntegrationEventEnvelope envelope) {
            return claimResult;
        }

        @Override
        public void markProcessed(IntegrationEventEnvelope envelope) {
            processed.add(envelope);
        }

        @Override
        public void markFailed(IntegrationEventEnvelope envelope, Exception error) {
            failed.add(envelope);
        }
    }
}
