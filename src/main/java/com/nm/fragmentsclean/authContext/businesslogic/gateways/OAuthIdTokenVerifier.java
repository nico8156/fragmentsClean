package com.nm.fragmentsclean.authContext.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.businesslogic.models.VerifiedOAuthProfile;

public interface OAuthIdTokenVerifier {
    VerifiedOAuthProfile verify(String provider, String idToken);

}
