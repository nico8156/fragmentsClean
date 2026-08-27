package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CoffeePublishedEvent(UUID eventId, UUID commandId, CoffeeId coffeeId, int version,
                                   Instant occurredAt, Instant clientAt) implements DomainEvent { }
