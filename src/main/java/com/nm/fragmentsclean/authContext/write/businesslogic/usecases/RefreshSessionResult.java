package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;


import java.time.Instant;
import java.util.List;

public record RefreshSessionResult(
        AppUserSnapshot user,
        AppSessionTokens tokens,
        String provider,
        List<String> scopes,
        Instant serverTime
) {
}
