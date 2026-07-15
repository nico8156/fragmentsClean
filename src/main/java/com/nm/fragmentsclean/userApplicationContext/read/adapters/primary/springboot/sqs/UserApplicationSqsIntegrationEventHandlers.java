package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.APP_USERS_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.userApplicationContext.read.projectors.SavedCoffeeCafeProjectionProjector;
import com.nm.fragmentsclean.userApplicationContext.read.projections.SavedCoffeeSetEventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationSqsIntegrationEventHandlers {
	private final SqsIntegrationEventPayloadReader payloadReader;

	public UserApplicationSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
		this.payloadReader = payloadReader;
	}

	@Bean
	SqsIntegrationEventHandler savedCoffeeSetSqsIntegrationEventHandler(SavedCoffeeSetEventHandler handler) {
		return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "user.saved_coffee.set",
				envelope -> handler.handle(payloadReader.read(envelope, SavedCoffeeSetEvent.class)));
	}

	@Bean
	SqsIntegrationEventHandler coffeeCreatedSavedCoffeeProjectionSqsIntegrationEventHandler(
			SavedCoffeeCafeProjectionProjector projector) {
		return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "coffee.saved_coffee_projection.created", envelope -> {
			CoffeeCreatedIntegrationEvent event = payloadReader.read(envelope, CoffeeCreatedIntegrationEvent.class);
			projector.upsert(event);
		});
	}

	@Bean
	SqsIntegrationEventHandler coffeeArchivedSavedCoffeeProjectionSqsIntegrationEventHandler(
			SavedCoffeeCafeProjectionProjector projector) {
		return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "coffee.saved_coffee_projection.archived", envelope -> {
			CoffeeLifecycleIntegrationEvent event = payloadReader.read(envelope, CoffeeLifecycleIntegrationEvent.class);
			projector.archive(event);
		});
	}

	@Bean
	SqsIntegrationEventHandler coffeeDeletedSavedCoffeeProjectionSqsIntegrationEventHandler(
			SavedCoffeeCafeProjectionProjector projector) {
		return new SimpleSqsIntegrationEventHandler(APP_USERS_EVENTS, "coffee.saved_coffee_projection.deleted", envelope -> {
			CoffeeLifecycleIntegrationEvent event = payloadReader.read(envelope, CoffeeLifecycleIntegrationEvent.class);
			projector.archive(event);
		});
	}

	private record SimpleSqsIntegrationEventHandler(
			String destination,
			String eventType,
			java.util.function.Consumer<IntegrationEventEnvelope> handler
	) implements SqsIntegrationEventHandler {

		@Override
		public SqsIntegrationEventRoute route() {
			return new SqsIntegrationEventRoute(destination, eventType);
		}

		@Override
		public void handle(IntegrationEventEnvelope envelope) {
			handler.accept(envelope);
		}
	}
}
