package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.util.LinkedHashSet;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDetailsEditedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhoneNumber;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.WebsiteUrl;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class EditCoffeeDetailsCommandHandler implements CommandHandler<EditCoffeeDetailsCommand> {
    private final CoffeeRepository coffees;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;

    public EditCoffeeDetailsCommandHandler(CoffeeRepository coffees, DomainEventPublisher events, DateTimeProvider clock) {
        this.coffees = coffees;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(EditCoffeeDetailsCommand command) {
        var coffee = coffees.findById(new CoffeeId(command.coffeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Coffee not found: " + command.coffeeId()));
        var now = clock.now();
        var tags = new LinkedHashSet<Tag>();
        if (command.tags() != null) command.tags().stream().map(String::trim).filter(value -> !value.isBlank())
                .map(Tag::new).forEach(tags::add);
        coffee.editDetails(new CoffeeName(command.name()),
                new Address(command.addressLine1(), command.city(), command.postalCode(), command.country()),
                new GeoPoint(command.latitude(), command.longitude()), nullablePhone(command.phoneNumber()),
                nullableWebsite(command.website()), tags, now);

        coffees.save(coffee);
        events.publish(new CoffeeDetailsEditedEvent(UUID.randomUUID(), command.commandId(), coffee.coffeeId(),
                coffee.version(), now, command.clientAt()));
    }

    private PhoneNumber nullablePhone(String value) {
        return value == null || value.isBlank() ? null : new PhoneNumber(value.trim());
    }

    private WebsiteUrl nullableWebsite(String value) {
        return value == null || value.isBlank() ? null : new WebsiteUrl(value.trim());
    }
}
