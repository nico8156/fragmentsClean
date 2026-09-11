package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;
import java.time.Instant;import java.util.Objects;import java.util.UUID;
public record UpdateExperienceRequest(UUID commandId,String message,Instant at){public UpdateExperienceRequest{Objects.requireNonNull(commandId);Objects.requireNonNull(at);}}
