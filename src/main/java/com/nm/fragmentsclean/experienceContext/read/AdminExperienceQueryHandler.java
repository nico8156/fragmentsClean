package com.nm.fragmentsclean.experienceContext.read;
import java.util.*;import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class AdminExperienceQueryHandler implements QueryHandler<SearchAdminExperiencesQuery,ExperiencePage>{
 private final AdminExperienceReadRepository repository;
 public AdminExperienceQueryHandler(AdminExperienceReadRepository repository){this.repository=repository;}
 public ExperiencePage handle(SearchAdminExperiencesQuery query){return repository.search(query);}
 public Optional<AdminExperienceViews.Detail> byId(UUID id){return repository.byId(id);}
 public Optional<AdminExperienceViews.Media> media(UUID id){return repository.media(id);}
 public AdminExperienceViews.Actions actions(UUID id,String cursor,int limit){if(limit<1||limit>100)throw new IllegalArgumentException("Invalid page size");return repository.actions(id,ExperienceCursor.parse(cursor),limit);}
}
