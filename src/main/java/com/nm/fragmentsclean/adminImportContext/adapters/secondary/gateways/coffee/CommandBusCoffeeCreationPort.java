package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.coffee;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.CoffeeCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeeImportStatus;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeeGooglePlaceLookupPort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public class CommandBusCoffeeCreationPort implements CoffeeCreationPort {
	private final CommandBus commandBus;
	private final CoffeeGooglePlaceLookupPort coffeeGooglePlaceLookupPort;

	public CommandBusCoffeeCreationPort(CommandBus commandBus,
			CoffeeGooglePlaceLookupPort coffeeGooglePlaceLookupPort) {
		this.commandBus = commandBus;
		this.coffeeGooglePlaceLookupPort = coffeeGooglePlaceLookupPort;
	}

	@Override
	public CoffeeCreationResult createCoffee(CreateCoffeeCommand command) {
		if (command.googlePlaceId() != null
				&& coffeeGooglePlaceLookupPort.existsByGooglePlaceId(command.googlePlaceId())) {
			return new CoffeeCreationResult(
					null,
					command.googlePlaceId(),
					GooglePlaceCoffeeImportStatus.ALREADY_IMPORTED
			);
		}

		commandBus.dispatch(command);
		return new CoffeeCreationResult(
				command.coffeeId(),
				command.googlePlaceId(),
				GooglePlaceCoffeeImportStatus.IMPORTED
		);
	}
}
