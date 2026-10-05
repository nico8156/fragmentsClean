package com.nm.fragmentsclean.mediaCatalogContext.read;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;
public record MediaCatalogAccountDataErasedEvent(UUID eventId, UUID requestId, UUID userId, String context, Instant occurredAt) implements DomainEvent { }
