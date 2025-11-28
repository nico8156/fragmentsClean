package com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.fake;

import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.OAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.VerifiedOAuthProfile;

public class FakeOAuthIdTokenVerifier implements OAuthIdTokenVerifier {

    @Override
    public VerifiedOAuthProfile verify(String provider, String idToken) {
        // Impl pour tests : tout est considéré comme valide.
        String safeProvider = (provider == null || provider.isBlank()) ? "fake-provider" : provider;

        String providerUserId = "fake-user-" + Math.abs(idToken != null ? idToken.hashCode() : 0);
        String email = providerUserId + "@example.test";
        boolean emailVerified = true;
        String displayName = "Fake " + capitalize(safeProvider);
        String avatarUrl = "https://example.com/avatar/" + providerUserId + ".png";
        String locale = "fr-FR";

        return new VerifiedOAuthProfile(
                safeProvider,
                providerUserId,
                email,
                emailVerified,
                displayName,
                avatarUrl,
                locale
        );
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
