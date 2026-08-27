package com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGoogleOpeningHoursForCoffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGooglePhotosForCoffee;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;

/** Starts coffee enrichment from the stable SQS contract when the local event bus is disabled. */
public class CoffeeCreatedIntegrationEnrichmentHandler {
    private final ImportGoogleOpeningHoursForCoffee openingHoursImporter;
    private final ImportGooglePhotosForCoffee photosImporter;

    public CoffeeCreatedIntegrationEnrichmentHandler(
            ImportGoogleOpeningHoursForCoffee openingHoursImporter,
            ImportGooglePhotosForCoffee photosImporter) {
        this.openingHoursImporter = openingHoursImporter;
        this.photosImporter = photosImporter;
    }

    public void handle(CoffeeCreatedIntegrationEvent event) {
        openingHoursImporter.handle(event);
        photosImporter.handle(event);
    }
}
