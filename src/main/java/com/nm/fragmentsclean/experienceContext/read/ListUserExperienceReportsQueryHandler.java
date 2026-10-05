package com.nm.fragmentsclean.experienceContext.read;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceReadRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class ListUserExperienceReportsQueryHandler implements QueryHandler<ListUserExperienceReportsQuery,AdminExperienceViews.UserReports> {
 private final AdminExperienceReadRepository repository;
 public ListUserExperienceReportsQueryHandler(AdminExperienceReadRepository repository){this.repository=repository;}
 public AdminExperienceViews.UserReports handle(ListUserExperienceReportsQuery query){return repository.userReports(query);}
}
