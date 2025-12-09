package com.nm.fragmentsclean.authenticationContext.read.configuration;

import com.nm.fragmentsclean.authenticationContext.read.GetMeQueryHandler;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway.DefaultJwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.JpaAuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.SpringAuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommandHandler;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot",
        "com.nm.fragmentsclean.sharedKernel.adapters.secondary"
})
public class authenticationDependenciesConfiguration {

    @Bean
    public AuthUserRepository authUserRepository(SpringAuthUserRepository springAuthUserRepository){
        return new JpaAuthUserRepository(springAuthUserRepository);
    }

    @Bean
    GoogleLoginCommandHandler googleLoginCommandHandler (
            DomainEventPublisher domainEventPublisher,
            GoogleAuthService googleAuthService,
            AuthUserRepository authUserRepository,
            AppUserRepository appUserRepository,
            TokenService tokenService,
            DateTimeProvider dateTimeProvider,
            JwtClaimsFactory jwtClaimsFactory
    ) {
        return new GoogleLoginCommandHandler(domainEventPublisher, googleAuthService, authUserRepository, appUserRepository, tokenService, dateTimeProvider, jwtClaimsFactory);
    }

    @Bean
    RefreshTokenCommandHandler refreshTokenCommandHandler(
            RefreshTokenRepository refreshTokenRepository,
            TokenService tokenService,
            DateTimeProvider dateTimeProvider,
            AppUserRepository appUserRepository,
            AuthUserRepository authUserRepository,
            JwtClaimsFactory jwtClaimsFactory
    ) {
        return new RefreshTokenCommandHandler(refreshTokenRepository, tokenService, dateTimeProvider, appUserRepository, authUserRepository, jwtClaimsFactory);
    }

    @Bean
    GetMeQueryHandler getMeQueryHandler(JdbcTemplate jdbcTemplate){
        return new GetMeQueryHandler(jdbcTemplate);
    }

    @Bean
    public JwtClaimsFactory jwtClaimsFactory(
            DateTimeProvider dateTimeProvider,
            @Value("${auth.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl
    ) {
        return new DefaultJwtClaimsFactory(dateTimeProvider, accessTokenTtl);
    }
}
