package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class AddCoffeePhotoCommandHandler implements CommandHandler<AddCoffeePhotoCommand> {
	private final CoffeeRepository coffeeRepository;
	private final CoffeePhotoStorage photoStorage;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public AddCoffeePhotoCommandHandler(
			CoffeeRepository coffeeRepository,
			CoffeePhotoStorage photoStorage,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.coffeeRepository = coffeeRepository;
		this.photoStorage = photoStorage;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(AddCoffeePhotoCommand command) {
		var coffeeId = new CoffeeId(command.coffeeId());
		var coffee = coffeeRepository.findById(coffeeId)
				.orElseThrow(() -> new CoffeePhotoCommandException("Coffee does not exist: " + coffeeId.value()));

		var now = dateTimeProvider.now();
		var sourceName = "admin-upload/" + command.commandId() + "/" + command.fileName();
		var storedPhoto = photoStorage.store(
				coffeeId,
				coffee.googleId().orElse(null),
				new GooglePlacePhoto(sourceName, command.contentType(), command.bytes()));

		eventPublisher.publish(new CoffeePhotoAddedEvent(
				UUID.randomUUID(),
				command.commandId(),
				coffeeId,
				storedPhoto,
				coffee.version() + 1,
				now,
				command.clientAt()));
	}
}
