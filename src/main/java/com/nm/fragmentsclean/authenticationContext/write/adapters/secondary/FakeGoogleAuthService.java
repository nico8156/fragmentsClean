package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Profile("auth_test")
public class FakeGoogleAuthService implements GoogleAuthService {

    @Override
    public GoogleUserInfo exchangeCodeForUser(String authorizationCode) {

        // Permet de simuler des updates sans changer d'identité:
        // "userA"         -> identityKey = "userA", variant = "userA"
        // "userA#pic2"    -> identityKey = "userA", variant = "userA#pic2"
        // "userA#name2"   -> identityKey = "userA", variant = "userA#name2"
        String identityKey = authorizationCode;
        String variant = authorizationCode;

        int hash = authorizationCode.indexOf('#');
        if (hash > 0) {
            identityKey = authorizationCode.substring(0, hash);
        }

        String sub = "fake-google-sub-" + identityKey;
        String email = identityKey.toLowerCase(Locale.ROOT) + "@example.com";

        String name = "User " + identityKey;
        // Optionnel: si tu veux tester changement de nom aussi
        if (authorizationCode.contains("#name2")) {
            name = "User " + identityKey + " (v2)";
        }

        String avatarUrl = "https://example.com/avatar/" + variant;

        return new GoogleUserInfo(
                sub,
                email,
                true,
                name,
                avatarUrl
        );
    }
}
