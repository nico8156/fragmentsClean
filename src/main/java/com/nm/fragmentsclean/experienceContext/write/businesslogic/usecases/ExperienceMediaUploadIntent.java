package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ExperienceMediaUploadIntent(UUID mediaId, boolean uploadRequired, String uploadUrl,
    String method, Map<String, String> headers, Instant expiresAt) {}
