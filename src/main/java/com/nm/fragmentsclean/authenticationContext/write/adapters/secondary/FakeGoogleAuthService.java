package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FakeGoogleAuthService implements GoogleAuthService {

    @Override
    public GoogleUserInfo exchangeCodeForUser(String code, String codeVerifier, String redirectUri) {
        // Pour l’instant on ignore code/codeVerifier
        String sub = "fake-google-sub-" + code;
        String email = code.toLowerCase(Locale.ROOT) + "@example.com";
        return new GoogleUserInfo(
                sub,
                email,
                true,
                "User " + code,
                "https://example.com/avatar/" + code
        );
    }
}
