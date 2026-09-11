package com.nm.fragmentsclean.experienceContext.read.projections;
import java.time.Instant;import java.util.List;import java.util.UUID;
public record ExperienceModerationReportView(UUID reportId,UUID experienceId,UUID coffeeId,UUID authorId,String authorName,UUID reporterId,String reason,String details,String status,String content,long reportCount,Instant createdAt,List<ExperienceMediaView> media,List<ExperienceModerationActionView> actions){}
