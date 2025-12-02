package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;


public record CreateCoffeeCommand(
        double latitude, double longitude
) implements Command {
}
