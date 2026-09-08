package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublicationStatus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeUnpublishedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import java.time.Instant;
import java.util.UUID;

public class UnpublishCoffeeCommandHandler implements CommandHandler<UnpublishCoffeeCommand> {
    private final CoffeeRepository coffeeRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public UnpublishCoffeeCommandHandler(CoffeeRepository coffeeRepository, DomainEventPublisher eventPublisher,
                                         DateTimeProvider dateTimeProvider) {
        this.coffeeRepository = coffeeRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(UnpublishCoffeeCommand command) {
        var coffee = coffeeRepository.findById(new CoffeeId(command.coffeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Coffee not found: " + command.coffeeId()));
        if (coffee.isArchived()) throw new IllegalStateException("Archived coffee cannot be unpublished");
        if (coffee.publicationStatus() == CoffeePublicationStatus.DRAFT) return;
        Instant now = dateTimeProvider.now();
        coffee.unpublish(now);
        coffeeRepository.save(coffee);
        eventPublisher.publish(new CoffeeUnpublishedEvent(UUID.randomUUID(), command.commandId(),
                coffee.coffeeId(), coffee.version(), now, command.clientAt()));
    }
}
