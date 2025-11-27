package com.nm.fragmentsclean.authContext.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import java.time.Instant;

public interface JwtTokenGenerator {
    AppSessionTokens generateAccessToken(AppUser user, Identity identity, Instant now);

}
