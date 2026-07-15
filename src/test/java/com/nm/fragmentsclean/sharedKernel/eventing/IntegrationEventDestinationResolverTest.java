package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinationResolver;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationEventDestinationResolverTest {

    private final IntegrationEventDestinationResolver resolver = new IntegrationEventDestinationResolver();

    @Test
    void routes_context_events_to_sqs_destinations() {
        assertThat(destinations("AuthUser", "com.example.AuthUserCreatedEvent"))
                .containsExactly("auth-users-events");
        assertThat(destinations("AppUser", "com.example.AppUserCreatedEvent"))
                .containsExactly("app-users-events");
        assertThat(destinations("Coffee", "com.example.CoffeeCreatedEvent"))
                .containsExactly("coffees-events", "app-users-events");
        assertThat(destinations("Article", "com.example.ArticleCreatedEvent"))
                .containsExactly("articles-events");
        assertThat(destinations("Comment", "com.example.CommentCreatedEvent"))
                .containsExactly("domain-events");
        assertThat(destinations("Like", "com.example.LikeSetEvent"))
                .containsExactly("domain-events");
    }

    @Test
    void routes_ticket_verify_accepted_to_read_projection_and_worker_queue() {
        assertThat(destinations("Ticket", "com.example.TicketVerifyAcceptedEvent"))
                .containsExactly("ticket-events", "ticket-verification-requested");
    }

    @Test
    void routes_ticket_completed_to_ticket_projection_queue_only() {
        assertThat(destinations("Ticket", "com.example.TicketVerificationCompletedEvent"))
                .containsExactly("ticket-events");
    }

    private List<String> destinations(String aggregateType, String eventType) {
        var event = new OutboxEventJpaEntity();
        event.setAggregateType(aggregateType);
        event.setEventType(eventType);
        return resolver.destinationsFor(event);
    }
}
