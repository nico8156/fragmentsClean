package com.nm.fragmentsclean.authenticationContext.read.projections;

import java.util.UUID;

public record AuthMeView(
        UUID userId,
        String displayName
) {
}
