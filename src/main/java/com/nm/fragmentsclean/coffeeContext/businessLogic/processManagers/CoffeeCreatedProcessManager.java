package com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

import java.util.List;

public class CoffeeCreatedProcessManager implements EventHandler<CoffeeCreatedEvent> {
	private final List<EventHandler<CoffeeCreatedEvent>> policies;

	public CoffeeCreatedProcessManager(List<EventHandler<CoffeeCreatedEvent>> policies) {
		this.policies = List.copyOf(policies);
	}

	@Override
	public void handle(CoffeeCreatedEvent event) {
		policies.forEach(policy -> policy.handle(event));
	}
}
