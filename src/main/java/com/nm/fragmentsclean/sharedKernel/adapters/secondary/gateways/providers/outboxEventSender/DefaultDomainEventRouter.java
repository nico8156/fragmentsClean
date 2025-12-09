package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.DomainEventRouter;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class DefaultDomainEventRouter implements DomainEventRouter {

    @Override
    public EventRouting routingFor(DomainEvent event) {

        // 🔹 Events d’auth / user → Kafka
        if (event instanceof AuthUserCreatedEvent) {
            return EventRouting.kafkaOnly();
        }

        if (event instanceof AppUserCreatedEvent) {
            return EventRouting.kafkaOnly();
        }

        // 🔹 Plus tard : events sociaux avec temps réel
        // if (event instanceof UserLikedCafeEvent e) {
        //     return EventRouting.kafkaAndWebSocket();
        // }

        // 🔹 Par défaut : seulement EventBus interne
        return EventRouting.eventBusOnly();
    }
}
