package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FakeGoogleAuthService implements GoogleAuthService {

    @Override
    public GoogleUserInfo exchangeCodeForUser(String authorizationCode) {
        String sub = "fake-google-sub-" + authorizationCode;
        String email = authorizationCode.toLowerCase(Locale.ROOT) + "@example.com";
        return new GoogleUserInfo(
                sub,
                email,
                true,
                "User " + authorizationCode,
                "https://example.com/avatar/" + authorizationCode
        );
    }
}
