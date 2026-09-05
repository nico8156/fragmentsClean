package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.platform.eventing.contracts.SavedCoffeeSetIntegrationEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;

final class UserApplicationIntegrationEventAcl {
    private UserApplicationIntegrationEventAcl() { }

    static SavedCoffeeSetEvent savedCoffeeSet(SavedCoffeeSetIntegrationEvent e) {
        return new SavedCoffeeSetEvent(e.eventId(), e.commandId(), e.savedCoffeeId(), e.userId(), e.coffeeId(),
                e.active(), e.version(), e.occurredAt(), e.clientAt());
    }
}
