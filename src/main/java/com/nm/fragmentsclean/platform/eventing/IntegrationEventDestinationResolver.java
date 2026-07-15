package com.nm.fragmentsclean.platform.eventing;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

import java.util.List;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.*;

public class IntegrationEventDestinationResolver {

    public List<String> destinationsFor(OutboxEventJpaEntity event) {
        String aggregateType = event.getAggregateType();
        String eventType = event.getEventType();

        if ("Ticket".equals(aggregateType)) {
            if (eventType.endsWith("TicketVerifyAcceptedEvent")) {
                return List.of(TICKET_EVENTS, TICKET_VERIFICATION_REQUESTED);
            }
            return List.of(TICKET_EVENTS);
        }

        if ("Coffee".equals(aggregateType)) {
            if (eventType.endsWith("CoffeeCreatedEvent")
                    || eventType.endsWith("CoffeeArchivedEvent")
                    || eventType.endsWith("CoffeeDeletedEvent")) {
                return List.of(COFFEES_EVENTS, APP_USERS_EVENTS);
            }
            return List.of(COFFEES_EVENTS);
        }

        return switch (aggregateType) {
            case "Article" -> List.of(ARTICLES_EVENTS);
            case "AuthUser" -> List.of(AUTH_USERS_EVENTS);
            case "AppUser", "SavedCoffee" -> List.of(APP_USERS_EVENTS);
            case "Comment", "Like" -> List.of(DOMAIN_EVENTS);
            default -> List.of(DOMAIN_EVENTS);
        };
    }
}
