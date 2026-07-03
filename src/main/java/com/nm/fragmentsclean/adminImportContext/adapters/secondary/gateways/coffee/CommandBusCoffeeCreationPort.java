package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.coffee;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public class CommandBusCoffeeCreationPort implements CoffeeCreationPort {
	private final CommandBus commandBus;

	public CommandBusCoffeeCreationPort(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@Override
	public void createCoffee(CreateCoffeeCommand command) {
		commandBus.dispatch(command);
	}
}
