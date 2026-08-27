package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.ImportedGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

public class ImportGooglePlaceCoffee {
	private final PreviewGooglePlaceCoffee previewGooglePlaceCoffee;
	private final CoffeeCreationPort coffeeCreationPort;
	private final UuidGenerator uuidGenerator;
	private final DateTimeProvider dateTimeProvider;

	public ImportGooglePlaceCoffee(PreviewGooglePlaceCoffee previewGooglePlaceCoffee,
			CoffeeCreationPort coffeeCreationPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider) {
		this.previewGooglePlaceCoffee = previewGooglePlaceCoffee;
		this.coffeeCreationPort = coffeeCreationPort;
		this.uuidGenerator = uuidGenerator;
		this.dateTimeProvider = dateTimeProvider;
	}

	public ImportedGooglePlaceCoffee execute(String googlePlaceId) {
		var preview = previewGooglePlaceCoffee.execute(googlePlaceId);
		UUID commandId = uuidGenerator.generate();
		UUID coffeeId = uuidGenerator.generate();
		Instant now = dateTimeProvider.now();

		var command = new CreateCoffeeCommand(
				commandId,
				coffeeId,
				preview.googlePlaceId(),
				preview.name(),
				preview.addressLine1(),
				preview.city(),
				preview.postalCode(),
				preview.country(),
				preview.latitude(),
				preview.longitude(),
				preview.phoneNumber(),
				preview.website(),
				List.of("google-places"),
				now,
				"DRAFT"
		);

		var result = coffeeCreationPort.createCoffee(command);

		return new ImportedGooglePlaceCoffee(commandId, result.coffeeId(), result.googlePlaceId(), result.status());
	}
}
