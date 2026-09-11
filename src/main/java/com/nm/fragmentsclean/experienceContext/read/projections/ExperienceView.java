package com.nm.fragmentsclean.experienceContext.read.projections;
import java.time.Instant;import java.util.List;import java.util.UUID;
public record ExperienceView(UUID experienceId,UUID coffeeId,UUID authorId,String authorName,String avatarUrl,String message,String publicationStatus,String moderationStatus,Instant createdAt,Instant updatedAt,long version,List<ExperienceMediaView> media){}
