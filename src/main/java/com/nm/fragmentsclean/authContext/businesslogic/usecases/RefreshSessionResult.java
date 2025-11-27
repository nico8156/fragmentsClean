package com.nm.fragmentsclean.authContext.businesslogic.usecases;

import com.nm.fragmentsclean.authContext.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;


import java.util.List;

public record RefreshSessionResult(
        AppUserSnapshot user,
        AppSessionTokens tokens,
        String provider,
        List<String> scopes
) {
}
