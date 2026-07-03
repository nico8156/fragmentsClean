package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;

public interface CoffeeCreationPort {
	void createCoffee(CreateCoffeeCommand command);
}
