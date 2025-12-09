package com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.EventRouting;

public interface DomainEventRouter {
    EventRouting routingFor(DomainEvent event);

}
