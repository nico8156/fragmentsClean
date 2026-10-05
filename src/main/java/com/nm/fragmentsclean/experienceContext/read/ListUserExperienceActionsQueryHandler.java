package com.nm.fragmentsclean.experienceContext.read;

import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceReadRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public final class ListUserExperienceActionsQueryHandler implements QueryHandler<ListUserExperienceActionsQuery,AdminExperienceViews.UserActions> {
 private final AdminExperienceReadRepository repository;
 public ListUserExperienceActionsQueryHandler(AdminExperienceReadRepository repository){this.repository=repository;}
 public AdminExperienceViews.UserActions handle(ListUserExperienceActionsQuery query){return repository.userActions(query);}
}
