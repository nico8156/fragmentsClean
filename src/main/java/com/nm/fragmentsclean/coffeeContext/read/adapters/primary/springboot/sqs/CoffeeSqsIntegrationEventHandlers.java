package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.COFFEES_EVENTS;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeArchivedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedIntegrationEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeOpeningHoursImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoAddedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotosImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePublishedEventHandler;
import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeDeletedMediaCleanupHandler;
import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeCreatedIntegrationEnrichmentHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosImportedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeOpeningHoursImportedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoDeletedIntegrationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoffeeSqsIntegrationEventHandlers {

    private final SqsIntegrationEventPayloadReader payloadReader;

    public CoffeeSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Bean
    SqsIntegrationEventHandler coffeeCreatedSqsIntegrationEventHandler(
            CoffeeCreatedIntegrationEventHandler coffeeCreatedIntegrationEventHandler,
            CoffeeCreatedIntegrationEnrichmentHandler coffeeCreatedIntegrationEnrichmentHandler) {
        return readAndHandle("coffee.created", CoffeeCreatedIntegrationEvent.class, event -> {
            coffeeCreatedIntegrationEventHandler.handle(event);
            coffeeCreatedIntegrationEnrichmentHandler.handle(event);
        });
    }

    @Bean
    SqsIntegrationEventHandler coffeeArchivedSqsIntegrationEventHandler(CoffeeArchivedEventHandler handler) {
        return readAndHandle("coffee.archived", CoffeeLifecycleIntegrationEvent.class,
                event -> handler.handle(CoffeeIntegrationEventAcl.archived(event)));
    }

    @Bean
    SqsIntegrationEventHandler coffeePublishedSqsIntegrationEventHandler(CoffeePublishedEventHandler handler) {
        return readAndHandle("coffee.published", CoffeePublishedIntegrationEvent.class,
                event -> handler.handle(CoffeeIntegrationEventAcl.published(event)));
    }

    @Bean
    SqsIntegrationEventHandler coffeeDeletedSqsIntegrationEventHandler(CoffeeDeletedEventHandler handler,
            CoffeeDeletedMediaCleanupHandler mediaCleanupHandler) {
        return readAndHandle("coffee.deleted", CoffeeLifecycleIntegrationEvent.class, integrationEvent -> {
            var event = CoffeeIntegrationEventAcl.deleted(integrationEvent);
            handler.handle(event);
            mediaCleanupHandler.handle(event);
        });
    }

    @Bean
    SqsIntegrationEventHandler coffeeOpeningHoursImportedSqsIntegrationEventHandler(
            CoffeeOpeningHoursImportedEventHandler handler) {
        return readAndHandle("coffee.opening_hours_imported", CoffeeOpeningHoursImportedIntegrationEvent.class,
                event -> handler.handle(CoffeeIntegrationEventAcl.openingHours(event)));
    }

    @Bean
    SqsIntegrationEventHandler coffeePhotosImportedSqsIntegrationEventHandler(CoffeePhotosImportedEventHandler handler) {
        return readAndHandle("coffee.photos_imported", CoffeePhotosImportedIntegrationEvent.class, handler::handle);
    }

    @Bean
    SqsIntegrationEventHandler coffeePhotoAddedSqsIntegrationEventHandler(CoffeePhotoAddedEventHandler handler) {
        return readAndHandle("coffee.photo_added", CoffeePhotoAddedIntegrationEvent.class, handler::handle);
    }

    @Bean
    SqsIntegrationEventHandler coffeePhotoDeletedSqsIntegrationEventHandler(CoffeePhotoDeletedEventHandler handler) {
        return readAndHandle("coffee.photo_deleted", CoffeePhotoDeletedIntegrationEvent.class,
                event -> handler.handle(CoffeeIntegrationEventAcl.photoDeleted(event)));
    }

    private <T> SqsIntegrationEventHandler readAndHandle(
            String eventType,
            Class<T> eventClass,
            java.util.function.Consumer<T> handler) {
        return new SimpleCoffeeSqsIntegrationEventHandler(eventType,
                envelope -> handler.accept(payloadReader.read(envelope, eventClass)));
    }

    private record SimpleCoffeeSqsIntegrationEventHandler(
            String eventType,
            java.util.function.Consumer<IntegrationEventEnvelope> handler
    ) implements SqsIntegrationEventHandler {

        @Override
        public SqsIntegrationEventRoute route() {
            return new SqsIntegrationEventRoute(COFFEES_EVENTS, eventType);
        }

        @Override
        public void handle(IntegrationEventEnvelope envelope) {
            handler.accept(envelope);
        }
    }
}
