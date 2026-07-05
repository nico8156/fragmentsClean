package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class DeleteCoffeePhotoCommandHandler implements CommandHandler<DeleteCoffeePhotoCommand> {
	private final CoffeeRepository coffeeRepository;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public DeleteCoffeePhotoCommandHandler(
			CoffeeRepository coffeeRepository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.coffeeRepository = coffeeRepository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(DeleteCoffeePhotoCommand command) {
		var coffeeId = new CoffeeId(command.coffeeId());
		var coffee = coffeeRepository.findById(coffeeId)
				.orElseThrow(() -> new CoffeePhotoCommandException("Coffee does not exist: " + coffeeId.value()));
		var photoId = new PhotoId(command.photoId());
		var now = dateTimeProvider.now();

		eventPublisher.publish(new CoffeePhotoDeletedEvent(
				UUID.randomUUID(),
				command.commandId(),
				coffeeId,
				photoId,
				coffee.version() + 1,
				now,
				command.clientAt()));
	}
}
