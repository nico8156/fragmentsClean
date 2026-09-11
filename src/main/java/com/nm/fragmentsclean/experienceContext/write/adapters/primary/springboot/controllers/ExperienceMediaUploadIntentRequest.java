package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import java.util.UUID;

public record ExperienceMediaUploadIntentRequest(UUID mediaId, String contentType, long size) {}
