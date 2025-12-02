package com.nm.fragmentsclean.sharedKernel.businesslogic.models.event;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public interface EventBus {

    /**
     * Publie un événement de domaine sur le bus.
     * Tous les EventHandler<E> enregistrés pour ce type seront invoqués.
     */
    <E extends DomainEvent> void publish(E event);
}
