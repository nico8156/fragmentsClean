package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test") // ou "dev"
public class FakeTokenService implements TokenService {

    @Override
    public TokenPair generateTokensForUser(UUID appUserId) {
        String access = "access-" + appUserId;
        String refresh = "refresh-" + appUserId;
        return new TokenPair(access, refresh);
    }
}
