package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record DeleteCoffeePhotoCommand(
		UUID commandId,
		UUID coffeeId,
		UUID photoId,
		Instant clientAt
) implements Command {
	public DeleteCoffeePhotoCommand {
		Objects.requireNonNull(commandId, "commandId is required");
		Objects.requireNonNull(coffeeId, "coffeeId is required");
		Objects.requireNonNull(photoId, "photoId is required");
	}
}
