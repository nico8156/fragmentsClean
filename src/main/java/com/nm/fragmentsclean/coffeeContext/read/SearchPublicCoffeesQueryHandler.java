package com.nm.fragmentsclean.coffeeContext.read;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public class SearchPublicCoffeesQueryHandler implements QueryHandler<SearchPublicCoffeesQuery, CoffeeCataloguePage> {
	private final CoffeeProjectionRepository repository;

	public SearchPublicCoffeesQueryHandler(CoffeeProjectionRepository repository) {
		this.repository = repository;
	}

	@Override
	public CoffeeCataloguePage handle(SearchPublicCoffeesQuery query) {
		return repository.searchPublished(query.search(), query.cursor(), query.limit());
	}
}
