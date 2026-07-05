package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record AddCoffeePhotoCommand(
		UUID commandId,
		UUID coffeeId,
		String fileName,
		String contentType,
		byte[] bytes,
		Instant clientAt
) implements Command {
	public AddCoffeePhotoCommand {
		Objects.requireNonNull(commandId, "commandId is required");
		Objects.requireNonNull(coffeeId, "coffeeId is required");
		if (fileName == null || fileName.isBlank()) {
			fileName = "admin-upload";
		}
		fileName = fileName.trim();
		contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
		bytes = bytes == null ? new byte[0] : bytes.clone();
		if (bytes.length == 0) {
			throw new IllegalArgumentException("photo bytes are required");
		}
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
