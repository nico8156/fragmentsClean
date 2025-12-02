package com.nm.fragmentsclean.sharedKernel.businesslogic.models.event;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public interface EventHandler<E extends DomainEvent> {

    /**
     * Logique métier à exécuter quand l’événement est publié.
     */
    void handle(E event);
}
