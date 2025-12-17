package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.DomainEventRouter;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.springframework.stereotype.Component;

@Component
public class DefaultDomainEventRouter implements DomainEventRouter {

    @Override
    public EventRouting routingFor(DomainEvent event) {

        // 🔹 AUTH / USER TECHNIQUE → Kafka uniquement (projections, userContext & co)
        if (event instanceof AuthUserCreatedEvent) {
            return EventRouting.kafkaOnly();
        }

        if (event instanceof AuthUserLoggedInEvent) {
            return EventRouting.kafkaOnly();
        }

        if (event instanceof AppUserCreatedEvent) {
            return EventRouting.kafkaOnly();
        }
        if (event instanceof AppUserProfileUpdatedEvent) {
            return EventRouting.kafkaOnly();
        }

        // 🔹 ARTICLE / COFFEE → projections + temps réel (feed, carte, etc.)
        if (event instanceof ArticleCreatedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }

        if (event instanceof CoffeeCreatedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }

        // 🔹 SOCIAL (likes + comments) → projections + temps réel UI
        if (event instanceof LikeSetEvent) {
            return EventRouting.all();
        }

        if (event instanceof CommentCreatedEvent) {
            return EventRouting.kafkaOnly();
        }

        if (event instanceof CommentUpdatedEvent) {
            return EventRouting.all();
        }

        if (event instanceof CommentDeletedEvent) {
            return EventRouting.all();
        }
//        if (event instanceof LikeSetEvent) {
//            return EventRouting.kafkaAndWebSocket();
//        }
//
//        if (event instanceof CommentCreatedEvent) {
//            return EventRouting.kafkaAndWebSocket();
//        }
//
//        if (event instanceof CommentUpdatedEvent) {
//            return EventRouting.kafkaAndWebSocket();
//        }
//
//        if (event instanceof CommentDeletedEvent) {
//            return EventRouting.kafkaAndWebSocket();
//        }

        // 🔹 Par défaut : seulement EventBus interne
        return EventRouting.eventBusOnly();
    }
}
