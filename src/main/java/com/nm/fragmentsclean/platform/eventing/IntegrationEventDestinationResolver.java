package com.nm.fragmentsclean.platform.eventing;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.*;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import java.util.List;

public class IntegrationEventDestinationResolver {

  public List<String> destinationsFor(OutboxEventJpaEntity event) {
    String aggregateType = event.getAggregateType();
    String eventType = event.getEventType();

    if (eventType.endsWith("AppUserDeletionRequestedEvent")) {
      return List.of(APP_USERS_EVENTS, AUTH_USERS_EVENTS, DOMAIN_EVENTS, TICKET_EVENTS, EXPERIENCES_EVENTS);
    }
    if (eventType.endsWith("AccountDataErasedEvent")) {
      return List.of(APP_USERS_EVENTS);
    }

    if ("Ticket".equals(aggregateType)) {
      if (eventType.endsWith("TicketVerifyAcceptedEvent")) {
        return List.of(TICKET_EVENTS, TICKET_VERIFICATION_REQUESTED);
      }
      return List.of(TICKET_EVENTS, APP_USERS_EVENTS);
    }

    if ("Coffee".equals(aggregateType)) {
      if (eventType.endsWith("CoffeeCreatedEvent")
          || eventType.endsWith("CoffeeArchivedEvent")
          || eventType.endsWith("CoffeeDeletedEvent")) {
        return List.of(COFFEES_EVENTS, APP_USERS_EVENTS, EXPERIENCES_EVENTS);
      }
      if (eventType.endsWith("CoffeePublishedEvent") || eventType.endsWith("CoffeeUnpublishedEvent")) {
        return List.of(COFFEES_EVENTS, EXPERIENCES_EVENTS);
      }
      return List.of(COFFEES_EVENTS);
    }

    if ("Experience".equals(aggregateType)) {
      if (eventType.endsWith("ExperienceLifecycleChangedEvent")) return List.of(APP_USERS_EVENTS);
      return List.of(EXPERIENCES_EVENTS);
    }
    if ("ExperienceReport".equals(aggregateType)) return List.of(EXPERIENCES_EVENTS);
    if ("ExperienceMedia".equals(aggregateType)) return List.of(EXPERIENCES_EVENTS);
    if ("ExperienceAccountDeletion".equals(aggregateType)) return List.of(APP_USERS_EVENTS);
    if ("UserBlock".equals(aggregateType)) return List.of(DOMAIN_EVENTS, EXPERIENCES_EVENTS);
    if ("AppUser".equals(aggregateType)
        && (eventType.endsWith("AppUserCreatedEvent") || eventType.endsWith("AppUserProfileUpdatedEvent"))) {
      return List.of(APP_USERS_EVENTS, EXPERIENCES_EVENTS);
    }

    return switch (aggregateType) {
      case "Article", "ArticleAuthoringSaga" -> List.of(ARTICLES_EVENTS);
      case "AuthUser" -> List.of(AUTH_USERS_EVENTS);
      case "AppUser", "SavedCoffee" -> List.of(APP_USERS_EVENTS);
      case "Comment", "Like" -> List.of(DOMAIN_EVENTS);
      default -> List.of(DOMAIN_EVENTS);
    };
  }
}
