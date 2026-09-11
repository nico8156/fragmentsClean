package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;
import java.time.Instant;import java.util.Objects;import java.util.UUID;
public record ExperienceCommandRequest(UUID commandId,Instant at){public ExperienceCommandRequest{Objects.requireNonNull(commandId);Objects.requireNonNull(at);}}
