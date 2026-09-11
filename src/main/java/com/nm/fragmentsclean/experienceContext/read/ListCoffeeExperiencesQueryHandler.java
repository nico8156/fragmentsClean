package com.nm.fragmentsclean.experienceContext.read;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

public final class ListCoffeeExperiencesQueryHandler
    implements QueryHandler<ListCoffeeExperiencesQuery, ExperiencePage> {
  private final ExperienceReadRepository repository;

  public ListCoffeeExperiencesQueryHandler(ExperienceReadRepository repository) {
    this.repository = repository;
  }

  @Override
  public ExperiencePage handle(ListCoffeeExperiencesQuery query) {
    return repository.listForCoffee(query.requesterId(), query.coffeeId(), query.cursor(), query.limit());
  }
}
