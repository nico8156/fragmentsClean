package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublishedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeOpeningHoursImportedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoDeletedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePublishedIntegrationEvent;

final class CoffeeIntegrationEventAcl {
    private CoffeeIntegrationEventAcl() { }

    static CoffeeArchivedEvent archived(CoffeeLifecycleIntegrationEvent e) {
        return new CoffeeArchivedEvent(e.eventId(), e.commandId(), new CoffeeId(e.coffeeId()), e.version(), e.occurredAt(), null);
    }
    static CoffeeDeletedEvent deleted(CoffeeLifecycleIntegrationEvent e) {
        return new CoffeeDeletedEvent(e.eventId(), e.commandId(), new CoffeeId(e.coffeeId()), e.version(), e.occurredAt(), null);
    }
    static CoffeePublishedEvent published(CoffeePublishedIntegrationEvent e) {
        return new CoffeePublishedEvent(e.eventId(), e.commandId(), new CoffeeId(e.coffeeId()), e.version(), e.occurredAt(), null);
    }
    static CoffeeOpeningHoursImportedEvent openingHours(CoffeeOpeningHoursImportedIntegrationEvent e) {
        return new CoffeeOpeningHoursImportedEvent(e.eventId(), e.commandId(), new CoffeeId(e.coffeeId()),
                new GooglePlaceId(e.googlePlaceId()), e.weekdayDescriptions(), e.version(), e.occurredAt(), e.clientAt());
    }
    static CoffeePhotoDeletedEvent photoDeleted(CoffeePhotoDeletedIntegrationEvent e) {
        return new CoffeePhotoDeletedEvent(e.eventId(), e.commandId(), new CoffeeId(e.coffeeId()), new PhotoId(e.photoId()),
                e.version(), e.occurredAt(), e.clientAt());
    }
}
