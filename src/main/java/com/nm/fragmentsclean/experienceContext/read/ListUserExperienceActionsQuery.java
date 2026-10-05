package com.nm.fragmentsclean.experienceContext.read;

import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record ListUserExperienceActionsQuery(UUID authorId, ExperienceCursor cursor, int limit)
 implements Query<AdminExperienceViews.UserActions> {
 public ListUserExperienceActionsQuery {
  if(authorId==null || limit<1 || limit>100)throw new IllegalArgumentException("Invalid author or page size");
 }
}
