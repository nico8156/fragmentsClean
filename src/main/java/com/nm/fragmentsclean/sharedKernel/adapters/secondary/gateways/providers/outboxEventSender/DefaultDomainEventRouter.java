package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.DomainEventRouter;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.springframework.stereotype.Component;

@Component
public class DefaultDomainEventRouter implements DomainEventRouter {

    @Override
    public EventRouting routingFor(DomainEvent event) {

        // 🔹 TICKET → Kafka (consumer OpenAI)
        if (event instanceof TicketVerifyAcceptedEvent) {
            return EventRouting.kafkaOnly();
        }
        if (event instanceof TicketVerificationCompletedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }
        // 🔹 AUTH / USER TECHNIQUE → Kafka uniquement
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

        // 🔹 ARTICLE / COFFEE → Kafka + WebSocket
        if (event instanceof ArticleCreatedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }
        if (event instanceof CoffeeCreatedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }
        if (event instanceof CoffeeOpeningHoursImportedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }
        if (event instanceof CoffeePhotosImportedEvent) {
            return EventRouting.kafkaAndWebSocket();
        }

        // 🔹 SOCIAL → all()
        if (event instanceof LikeSetEvent) {
            return EventRouting.all();
        }
        if (event instanceof CommentCreatedEvent) {
            return EventRouting.all();
        }
        if (event instanceof CommentUpdatedEvent) {
            return EventRouting.all();
        }
        if (event instanceof CommentDeletedEvent) {
            return EventRouting.all();
        }

        // 🔹 Par défaut : EventBus interne
        return EventRouting.eventBusOnly();
    }
}
