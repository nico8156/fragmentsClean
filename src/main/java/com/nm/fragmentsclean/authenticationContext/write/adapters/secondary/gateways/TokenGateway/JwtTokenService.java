package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenHasher;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthRole;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;
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
import java.util.stream.Collectors;

@Primary
@Component
public class JwtTokenService implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final DateTimeProvider dateTimeProvider;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String issuer;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            DateTimeProvider dateTimeProvider,
            @Value("${auth.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl,
            @Value("${auth.jwt.refresh-token-ttl:P30D}") Duration refreshTokenTtl,
            @Value("${auth.jwt.issuer:https://auth.fragments}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.dateTimeProvider = dateTimeProvider;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.issuer = issuer;
    }

    @Override
    @Transactional
    public TokenPair generateTokensForUser(UUID appUserId, JwtClaims claims) {
        return issueTokens(appUserId, claims, UUID.randomUUID());
    }

    @Override
    @Transactional
    public TokenPair rotateTokensForUser(
            UUID appUserId, JwtClaims claims, UUID refreshTokenFamilyId) {
        return issueTokens(appUserId, claims, refreshTokenFamilyId);
    }

    private TokenPair issueTokens(
            UUID appUserId, JwtClaims claims, UUID refreshTokenFamilyId) {
        Instant now = dateTimeProvider.now();

        var accessTokenClaimsBuilder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(claims.issuedAt())
                .expiresAt(claims.expiresAt())
                .subject(claims.subject())
                .id(UUID.randomUUID().toString())
                .claim("roles", claims.roles()
                        .stream()
                        .map(AuthRole::name)
                        .collect(Collectors.toSet()))
                .claim("scopes", claims.scopes());
        // An authenticated Apple identity does not necessarily have an email.
        // Omit the optional claim instead of inventing an address or serializing null.
        if (claims.email() != null) accessTokenClaimsBuilder.claim("email", claims.email());
        JwtClaimsSet accessTokenClaims = accessTokenClaimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String accessToken = jwtEncoder
                .encode(JwtEncoderParameters.from(header, accessTokenClaims))
                .getTokenValue();

        Instant refreshExpiresAt = now.plus(refreshTokenTtl);
        String refreshTokenValue = "rft-" + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.createInFamily(
                appUserId,
                refreshTokenHasher.hash(refreshTokenValue),
                refreshExpiresAt,
                refreshTokenFamilyId
        );
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, refreshTokenValue);
    }
}
