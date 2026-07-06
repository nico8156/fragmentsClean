package com.nm.fragmentsclean.coffeeContext.write.businessLogic.eventing;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CoffeeOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
	@Override
	public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
		if (event instanceof CoffeeCreatedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeeArchivedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeeDeletedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeeOpeningHoursImportedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeePhotosImportedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeePhotoAddedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		if (event instanceof CoffeePhotoDeletedEvent coffeeEvent) {
			return Optional.of(coffee(coffeeEvent.coffeeId()));
		}
		return Optional.empty();
	}

	private OutboxEventMetadata coffee(CoffeeId coffeeId) {
		String aggregateId = coffeeId.toString();
		return new OutboxEventMetadata("Coffee", aggregateId, "coffee:" + aggregateId);
	}
}
