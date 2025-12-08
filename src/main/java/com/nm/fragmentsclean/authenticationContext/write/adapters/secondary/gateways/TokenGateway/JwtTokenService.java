package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Primary
@Component
public class JwtTokenService implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DateTimeProvider dateTimeProvider;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            DateTimeProvider dateTimeProvider,
            @Value("${auth.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl,
            @Value("${auth.jwt.refresh-token-ttl:P30D}") Duration refreshTokenTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.dateTimeProvider = dateTimeProvider;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    @Transactional
    public TokenPair generateTokensForUser(UUID appUserId) {
        Instant now = dateTimeProvider.now();

        Instant accessExpiresAt = now.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fragments-auth")
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .subject(appUserId.toString())
                .id(UUID.randomUUID().toString())          // 👈 jti random à chaque token
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String accessToken = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        Instant refreshExpiresAt = now.plus(refreshTokenTtl);
        String refreshTokenValue = "rft-" + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.createNew(
                appUserId,
                refreshTokenValue,
                refreshExpiresAt
        );
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

}
