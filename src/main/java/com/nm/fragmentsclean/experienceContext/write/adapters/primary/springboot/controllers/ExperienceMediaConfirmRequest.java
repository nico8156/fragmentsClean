package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.UUID;

public record ExperienceMediaConfirmRequest(UUID commandId, Instant at) {}
