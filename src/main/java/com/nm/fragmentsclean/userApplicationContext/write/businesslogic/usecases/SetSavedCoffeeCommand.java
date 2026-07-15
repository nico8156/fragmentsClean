package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record SetSavedCoffeeCommand(
		String commandId,
		UUID savedCoffeeId,
		UUID userId,
		UUID coffeeId,
		boolean value,
		Instant clientAt
) implements Command {
}
