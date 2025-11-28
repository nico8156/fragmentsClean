package com.nm.fragmentsclean.authContext.read.usecases;

import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;

import java.time.Instant;

public record GetCurrentUserResult(
        AppUserSnapshot userSnapshot,
        Instant serverTime
) {
}
