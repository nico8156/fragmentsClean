package com.nm.fragmentsclean.authContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.VerifiedOAuthProfile;

public interface OAuthIdTokenVerifier {
    VerifiedOAuthProfile verify(String provider, String idToken);

}
