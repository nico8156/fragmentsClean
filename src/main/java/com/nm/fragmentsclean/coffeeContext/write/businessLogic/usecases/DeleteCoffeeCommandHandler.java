package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class DeleteCoffeeCommandHandler implements CommandHandler<DeleteCoffeeCommand> {

	private static final Logger log = LoggerFactory.getLogger(DeleteCoffeeCommandHandler.class);

	private final CoffeeRepository coffeeRepository;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public DeleteCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.coffeeRepository = coffeeRepository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(DeleteCoffeeCommand command) {
		CoffeeId coffeeId = new CoffeeId(command.coffeeId());
		var coffee = coffeeRepository.findById(coffeeId);
		if (coffee.isEmpty()) {
			log.info("Coffee {} does not exist, delete command ignored.", coffeeId.value());
			return;
		}

		Instant now = dateTimeProvider.now();
		int nextVersion = coffee.get().version() + 1;
		coffeeRepository.deleteById(coffeeId);

		eventPublisher.publish(new CoffeeDeletedEvent(
				UUID.randomUUID(),
				command.commandId(),
				coffeeId,
				nextVersion,
				now,
				command.clientAt()
		));
	}
}
