package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;


public record CreateCoffeeCommand(
        double latitude, double longitude
) implements Command {
}
