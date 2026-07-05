package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class ArchiveCoffeeCommandHandler implements CommandHandler<ArchiveCoffeeCommand> {

	private static final Logger log = LoggerFactory.getLogger(ArchiveCoffeeCommandHandler.class);

	private final CoffeeRepository coffeeRepository;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public ArchiveCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.coffeeRepository = coffeeRepository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(ArchiveCoffeeCommand command) {
		CoffeeId coffeeId = new CoffeeId(command.coffeeId());
		var coffee = coffeeRepository.findById(coffeeId);
		if (coffee.isEmpty()) {
			log.info("Coffee {} does not exist, archive command ignored.", coffeeId.value());
			return;
		}
		if (coffee.get().isArchived()) {
			log.info("Coffee {} is already archived, archive command ignored.", coffeeId.value());
			return;
		}

		Instant now = dateTimeProvider.now();
		coffee.get().archive(now);
		coffeeRepository.save(coffee.get());

		eventPublisher.publish(new CoffeeArchivedEvent(
				UUID.randomUUID(),
				command.commandId(),
				coffeeId,
				coffee.get().version(),
				now,
				command.clientAt()
		));
	}
}
