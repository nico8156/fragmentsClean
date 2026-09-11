package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService.AppleUserInfo;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.ProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;

public class CompleteAppleLogin {
  private static final String DEFAULT_DISPLAY_NAME = "Utilisateur Fragments";

  private final AuthUserRepository users;
  private final ProviderCredentialRepository credentials;
  private final TokenService tokens;
  private final DateTimeProvider clock;
  private final JwtClaimsFactory claims;
  private final DomainEventPublisher events;

  public CompleteAppleLogin(
      AuthUserRepository users,
      ProviderCredentialRepository credentials,
      TokenService tokens,
      DateTimeProvider clock,
      JwtClaimsFactory claims,
      DomainEventPublisher events) {
    this.users = users;
    this.credentials = credentials;
    this.tokens = tokens;
    this.clock = clock;
    this.claims = claims;
    this.events = events;
  }

  @Transactional
  public AppleLoginResult execute(AppleUserInfo profile) {
    var now = clock.now();
    AuthUser user =
        users
            .findByProviderAndProviderUserId(AuthProvider.APPLE, profile.sub())
            .map(
                existing -> {
                  existing.markLogin(now);
                  users.save(existing);
                  return existing;
                })
            .orElseGet(
                () -> {
                  var created =
                      AuthUser.createNew(
                          AuthProvider.APPLE,
                          profile.sub(),
                          profile.email(),
                          profile.emailVerified(),
                          normalizedDisplayName(profile.displayName()),
                          null,
                          now);
                  users.save(created);
                  return created;
                });

    credentials.save(user.id(), AuthProvider.APPLE, profile.providerRefreshToken());
    user.domainEvents().forEach(events::publish);
    user.clearDomainEvents();
    var pair = tokens.generateTokensForUser(user.id(), claims.forAuthUser(user));
    return new AppleLoginResult(
        pair.accessToken(),
        pair.refreshToken().token(),
        user.id(),
        user.displayName(),
        user.email(),
        null);
  }

  private static String normalizedDisplayName(String displayName) {
    if (displayName == null) return DEFAULT_DISPLAY_NAME;
    String normalized = displayName.strip().replaceAll("\\s+", " ");
    if (normalized.length() < 2
        || normalized.length() > 50
        || normalized.chars().anyMatch(Character::isISOControl)) {
      return DEFAULT_DISPLAY_NAME;
    }
    return normalized;
  }
}
