package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;

public interface JwtClaimsFactory {
    JwtClaims forAuthUser(AuthUser authUser);
}
