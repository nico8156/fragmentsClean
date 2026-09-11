package com.nm.fragmentsclean.experienceContext.read;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;import java.util.UUID;
public record ListMyExperiencesQuery(UUID userId,String cursor,int limit)implements Query<ExperiencePage>{}
