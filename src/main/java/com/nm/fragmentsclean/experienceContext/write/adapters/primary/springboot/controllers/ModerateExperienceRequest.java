package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceModerationStatus;import java.time.Instant;import java.util.Objects;import java.util.UUID;
public record ModerateExperienceRequest(UUID commandId,UUID actionId,ExperienceModerationStatus decision,String reason,Instant at){public ModerateExperienceRequest{Objects.requireNonNull(commandId);Objects.requireNonNull(actionId);Objects.requireNonNull(decision);Objects.requireNonNull(at);}}
