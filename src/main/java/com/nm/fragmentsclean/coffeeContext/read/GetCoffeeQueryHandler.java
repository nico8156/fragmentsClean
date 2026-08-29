package com.nm.fragmentsclean.coffeeContext.read;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public class GetCoffeeQueryHandler implements QueryHandler<GetCoffeeQuery, Optional<CoffeeSummaryView>> {

	private final CoffeeProjectionRepository repository;

	public GetCoffeeQueryHandler(CoffeeProjectionRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<CoffeeSummaryView> handle(GetCoffeeQuery query) {
		return repository.findById(query.coffeeId(), true);
	}
}
