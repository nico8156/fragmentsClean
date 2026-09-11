package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

import java.time.Instant;
import java.util.UUID;

public record SetSavedCoffeeCommand(
		String commandId,
		UUID savedCoffeeId,
		UUID userId,
		UUID coffeeId,
		boolean value,
		Instant clientAt
) implements AuthenticatedCommand {
	@Override
	public UUID receiptCommandId() {
		return UUID.fromString(commandId);
	}

	@Override
	public UUID requesterId() {
		return userId;
	}

	@Override
	public String receiptType() {
		return "user.saved_coffee.set.v1";
	}
}
