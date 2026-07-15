package com.nm.fragmentsclean.userApplicationContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories.JdbcSavedCoffeeProjectionRepository;
import com.nm.fragmentsclean.userApplicationContext.read.projections.GetSavedCoffeesQuery;
import com.nm.fragmentsclean.userApplicationContext.read.projections.SavedCoffeeListView;
import org.springframework.stereotype.Component;

@Component
public class GetSavedCoffeesQueryHandler implements QueryHandler<GetSavedCoffeesQuery, SavedCoffeeListView> {
	private final JdbcSavedCoffeeProjectionRepository repository;
	private final DateTimeProvider dateTimeProvider;

	public GetSavedCoffeesQueryHandler(
			JdbcSavedCoffeeProjectionRepository repository,
			DateTimeProvider dateTimeProvider) {
		this.repository = repository;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public SavedCoffeeListView handle(GetSavedCoffeesQuery query) {
		return repository.findForUser(query.userId(), dateTimeProvider.now());
	}
}
