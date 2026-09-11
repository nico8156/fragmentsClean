package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

final class ExperienceEvents {
    private ExperienceEvents() { }
    static void publish(AggregateRoot aggregate, DomainEventPublisher publisher) {
        aggregate.domainEvents().forEach(publisher::publish);
        aggregate.clearDomainEvents();
    }
}
