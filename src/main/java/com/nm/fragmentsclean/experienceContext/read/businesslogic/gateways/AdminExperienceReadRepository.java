package com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways;
import java.util.*;
import com.nm.fragmentsclean.experienceContext.read.*;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
public interface AdminExperienceReadRepository {
 ExperiencePage search(SearchAdminExperiencesQuery query);
 Optional<AdminExperienceViews.Detail> byId(UUID id);
 Optional<AdminExperienceViews.Media> media(UUID id);
 AdminExperienceViews.Actions actions(UUID id,ExperienceCursor cursor,int limit);
}
