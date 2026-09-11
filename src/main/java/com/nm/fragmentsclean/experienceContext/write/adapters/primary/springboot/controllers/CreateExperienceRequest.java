package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperiencePublicationStatus;
import java.time.Instant;import java.util.Objects;import java.util.UUID;
public record CreateExperienceRequest(UUID commandId,UUID experienceId,UUID coffeeId,String message,ExperiencePublicationStatus publicationStatus,Instant at){public CreateExperienceRequest{Objects.requireNonNull(commandId);Objects.requireNonNull(experienceId);Objects.requireNonNull(coffeeId);Objects.requireNonNull(publicationStatus);Objects.requireNonNull(at);}}
