package com.nm.fragmentsclean.experienceContext.read.projections;
import java.time.Instant;import java.util.List;
public record ExperiencePage(List<ExperienceView> items,String nextCursor,Instant serverTime){}
