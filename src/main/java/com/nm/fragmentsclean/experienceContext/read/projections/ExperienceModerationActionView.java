package com.nm.fragmentsclean.experienceContext.read.projections;
import java.time.Instant;import java.util.UUID;
public record ExperienceModerationActionView(UUID actionId,UUID operatorId,String decision,String reason,Instant occurredAt){}
