package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxMessage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxRetryPolicy;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxDeliveryStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxEventDispatcherTest {
    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Test void a_delivery_failure_is_persisted_with_backoff_and_the_claim_is_not_completed() {
        var store = new RecordingStore(message());
        var dispatcher = dispatcher(store, event -> { throw new IllegalStateException("SQS unavailable"); });

        dispatcher.dispatchPending();

        assertThat(store.sent).isFalse();
        assertThat(store.failureCount).isEqualTo(1);
        assertThat(store.nextAttemptAt).isAfter(NOW);
        assertThat(store.error).contains("IllegalStateException", "SQS unavailable");
    }

    @Test void completion_failure_after_send_is_requeued_for_at_least_once_delivery() {
        var store = new RecordingStore(message());
        store.failCompletion = true;
        var delivered = new ArrayList<String>();
        var dispatcher = dispatcher(store, event -> delivered.add(event.eventId()));

        dispatcher.dispatchPending();

        assertThat(delivered).containsExactly("event-1");
        assertThat(store.failureCount).isEqualTo(1);
        assertThat(store.sent).isFalse();
    }

    @Test void an_event_is_quarantined_after_the_configured_number_of_failures() {
        var retried = new OutboxMessage(1, "event-1", "type", "aggregate", "aggregate-1", "stream-1",
                "{}", NOW, NOW, 2);
        var store = new RecordingStore(retried);
        var dispatcher = dispatcher(store, event -> { throw new IllegalArgumentException("poison"); });

        dispatcher.dispatchPending();

        assertThat(store.failureCount).isEqualTo(3);
        assertThat(store.terminal).isTrue();
    }

    private OutboxEventDispatcher dispatcher(RecordingStore store,
                                              com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender sender) {
        return new OutboxEventDispatcher(store, sender, () -> NOW,
                new OutboxRetryPolicy(3, Duration.ofSeconds(1), Duration.ofMinutes(1)),
                Duration.ofMinutes(2), 10, "worker-test");
    }

    private OutboxMessage message() {
        return new OutboxMessage(1, "event-1", "type", "aggregate", "aggregate-1", "stream-1",
                "{}", NOW, NOW, 0);
    }

    private static final class RecordingStore implements OutboxDeliveryStore {
        private final OutboxMessage message;
        private boolean sent;
        private boolean failCompletion;
        private int failureCount;
        private Instant nextAttemptAt;
        private String error;
        private boolean claimed;
        private boolean terminal;

        private RecordingStore(OutboxMessage message) { this.message = message; }

        @Override public List<OutboxMessage> claimDue(String owner, Instant now, Instant leaseUntil, int limit) {
            if (claimed) return List.of();
            claimed = true;
            return List.of(message);
        }

        @Override public boolean markSent(OutboxMessage ignored, String owner, Instant completedAt) {
            if (failCompletion) throw new IllegalStateException("database unavailable after send");
            sent = true;
            return true;
        }

        @Override public boolean recordFailure(long id, String owner, int failures, Instant retryAt,
                                               String errorMessage, boolean terminal) {
            failureCount = failures;
            nextAttemptAt = retryAt;
            error = errorMessage;
            this.terminal = terminal;
            return true;
        }
    }
}
