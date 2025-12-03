package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateCoffeeCommandHandler implements CommandHandler<CreateCoffeeCommand> {

    private static final Logger log = LoggerFactory.getLogger(CreateCoffeeCommandHandler.class);

    private final CoffeeRepository coffeeRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public CreateCoffeeCommandHandler(CoffeeRepository coffeeRepository,
                                      DomainEventPublisher eventPublisher,
                                      DateTimeProvider dateTimeProvider) {
        this.coffeeRepository = coffeeRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(CreateCoffeeCommand command) {
        Instant now = dateTimeProvider.now();

        // 1) Ids
        CoffeeId coffeeId = new CoffeeId(
                command.coffeeId() != null
                        ? command.coffeeId()
                        : UUID.randomUUID()
        );

        GooglePlaceId googlePlaceId = command.googlePlaceId() != null
                ? new GooglePlaceId(command.googlePlaceId())
                : null;

        // (optionnel) check de doublon par GooglePlaceId
        if (googlePlaceId != null && coffeeRepository.existsByGooglePlaceId(googlePlaceId)) {
            // à toi de décider : exception métier ? no-op ?
            log.warn("Coffee with googlePlaceId={} already exists, ignoring create.", googlePlaceId);
        }

        // 2) Value Objects
        CoffeeName name = new CoffeeName(command.name());

        Address address = new Address(
                command.addressLine1(),
                command.city(),
                command.postalCode(),
                command.country()
        );

        GeoPoint location = new GeoPoint(command.lat(), command.lon());

        PhoneNumber phone = command.phoneNumber() != null
                ? new PhoneNumber(command.phoneNumber())
                : null;

        WebsiteUrl website = command.website() != null
                ? new WebsiteUrl(command.website())
                : null;

        Set<Tag> tags = command.tags() != null
                ? command.tags().stream()
                .map(Tag::new)
                .collect(Collectors.toUnmodifiableSet())
                : Set.of();

        // 3) Créer l’agrégat Coffee
        Coffee coffee = Coffee.createNew(
                coffeeId,
                googlePlaceId,
                name,
                address,
                location,
                phone,
                website,
                tags,
                now
        );

        // 4) Persister
        coffeeRepository.save(coffee);

        // 5) Publier l’event
        CoffeeCreatedEvent event = new CoffeeCreatedEvent(
                UUID.randomUUID(),                  // eventId
                command.commandId(),                // commandId (du client)
                coffee.id(),
                googlePlaceId,
                name,
                address,
                location,
                phone,
                website,
                tags.stream().toList(),
                coffee.version(),
                now,                                // occurredAt
                command.clientAt()                  // clientAt
        );

        log.info("Publishing CoffeeCreatedEvent for coffeeId={} name={}", coffee.id(), coffee.name());
        eventPublisher.publish(event);

    }
}
