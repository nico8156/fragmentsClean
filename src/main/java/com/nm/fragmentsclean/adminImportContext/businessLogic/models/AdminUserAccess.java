package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.UUID;

public record AdminUserAccess(UUID userId, String email, Instant grantedAt, UUID grantedBy, boolean bootstrap) {
}
