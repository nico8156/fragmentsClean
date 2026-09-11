package com.nm.fragmentsclean.experienceContext.read;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

public final class ListMyExperiencesQueryHandler
    implements QueryHandler<ListMyExperiencesQuery, ExperiencePage> {
  private final ExperienceReadRepository repository;

  public ListMyExperiencesQueryHandler(ExperienceReadRepository repository) {
    this.repository = repository;
  }

  @Override
  public ExperiencePage handle(ListMyExperiencesQuery query) {
    return repository.listForUser(query.userId(), query.cursor(), query.limit());
  }
}
