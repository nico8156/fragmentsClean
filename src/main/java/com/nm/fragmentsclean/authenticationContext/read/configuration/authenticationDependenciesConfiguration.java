package com.nm.fragmentsclean.authenticationContext.read.configuration;

import com.nm.fragmentsclean.authenticationContext.read.AuthAccountStatusReader;
import com.nm.fragmentsclean.authenticationContext.read.adapters.secondary.JdbcAuthAccountStatusReader;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway.DefaultJwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.credentials.EncryptedJdbcProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.credentials.ProviderCredentialCipher;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.ProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.AppleLoginCommandHandler;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.CompleteAppleLogin;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.CompleteAuthenticationAccountDataErasure;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.EraseAuthenticationAccountData;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommandHandler;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.LogoutCommandHandler;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@EntityScan(
    basePackages =
        "com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(
    basePackages =
        "com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(
    basePackages = {
      "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot",
      "com.nm.fragmentsclean.sharedKernel.adapters.secondary"
    })
public class authenticationDependenciesConfiguration {

  @Bean
  AuthAccountStatusReader authAccountStatusReader(JdbcTemplate jdbc) {
    return new JdbcAuthAccountStatusReader(jdbc);
  }

  @Bean
  GoogleLoginCommandHandler googleLoginCommandHandler(
      DomainEventPublisher domainEventPublisher,
      GoogleAuthService googleAuthService,
      AuthUserRepository authUserRepository,
      TokenService tokenService,
      DateTimeProvider dateTimeProvider,
      JwtClaimsFactory jwtClaimsFactory) {
    return new GoogleLoginCommandHandler(
        domainEventPublisher,
        googleAuthService,
        authUserRepository,
        tokenService,
        dateTimeProvider,
        jwtClaimsFactory);
  }

  @Bean
  public RefreshTokenCommandHandler refreshTokenCommandHandler(
      RefreshTokenRepository refreshTokenRepository,
      TokenService tokenService,
      DateTimeProvider dateTimeProvider,
      AuthUserRepository authUserRepository,
      JwtClaimsFactory jwtClaimsFactory) {
    return new RefreshTokenCommandHandler(
        refreshTokenRepository,
        tokenService,
        dateTimeProvider,
        authUserRepository,
        jwtClaimsFactory);
  }

  @Bean
  LogoutCommandHandler logoutCommandHandler(RefreshTokenRepository refreshTokenRepository) {
    return new LogoutCommandHandler(refreshTokenRepository);
  }

  @Bean
  ProviderCredentialCipher providerCredentialCipher(
      @Value("${auth.provider-credential-encryption-key:}") String key) {
    return new ProviderCredentialCipher(key);
  }

  @Bean
  ProviderCredentialRepository providerCredentialRepository(
      JdbcTemplate jdbc, ProviderCredentialCipher cipher) {
    return new EncryptedJdbcProviderCredentialRepository(jdbc, cipher);
  }

  @Bean
  CompleteAppleLogin completeAppleLogin(
      AuthUserRepository users,
      ProviderCredentialRepository credentials,
      TokenService tokens,
      DateTimeProvider clock,
      JwtClaimsFactory claims,
      DomainEventPublisher events) {
    return new CompleteAppleLogin(users, credentials, tokens, clock, claims, events);
  }

  @Bean
  AppleLoginCommandHandler appleLoginCommandHandler(
      AppleAuthService apple, CompleteAppleLogin completion) {
    return new AppleLoginCommandHandler(apple, completion);
  }

  @Bean
  CompleteAuthenticationAccountDataErasure completeAuthenticationAccountDataErasure(
      AuthUserRepository users,
      RefreshTokenRepository tokens,
      ProviderCredentialRepository credentials,
      DomainEventPublisher events,
      DateTimeProvider clock) {
    return new CompleteAuthenticationAccountDataErasure(users, tokens, credentials, events, clock);
  }

  @Bean
  EraseAuthenticationAccountData eraseAuthenticationAccountData(
      AuthUserRepository users,
      ProviderCredentialRepository credentials,
      AppleAuthService apple,
      CompleteAuthenticationAccountDataErasure completion) {
    return new EraseAuthenticationAccountData(users, credentials, apple, completion);
  }

  @Bean
  public JwtClaimsFactory jwtClaimsFactory(
      DateTimeProvider dateTimeProvider,
      @Value("${auth.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl) {
    return new DefaultJwtClaimsFactory(dateTimeProvider, accessTokenTtl);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
