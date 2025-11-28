package com.nm.fragmentsclean.authContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import java.time.Instant;

public interface JwtTokenGenerator {
    AppSessionTokens generateAccessToken(AppUser user, Identity identity, Instant now);

}
