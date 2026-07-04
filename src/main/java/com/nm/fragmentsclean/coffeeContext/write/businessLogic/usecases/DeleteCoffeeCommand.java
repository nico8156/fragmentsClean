package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record DeleteCoffeeCommand(
		UUID commandId,
		UUID coffeeId,
		Instant clientAt
) implements Command {
}
