package com.nm.fragmentsclean.authContext.adapters.primary.springboot.controllers.config;

import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeJwtTokenGenerator;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeOAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.JpaIdentityRepository;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.OAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.JpaUserRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.SpringUserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.entities",
        "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities"
})
@EnableJpaRepositories(basePackages = {
        "com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa",
        "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa"
})
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class AuthContextDependenciesConfiguration {

    // ---------- Tokens (fake pour l’instant, mais globaux) ----------

    @Bean
    public JwtTokenGenerator jwtTokenGenerator() {
        return new FakeJwtTokenGenerator();
    }

    @Bean
    public OAuthIdTokenVerifier oAuthIdTokenVerifier() {
        return new FakeOAuthIdTokenVerifier();
    }

    // ---------- Repositories : JPA partout pour l’instant ----------

    @Bean
    @Profile("database")
    public IdentityRepository identityRepository(SpringIdentityRepository springIdentityRepository) {
        return new JpaIdentityRepository(springIdentityRepository);
    }

    @Bean
    @Profile("database")
    public UserRepository userRepository(SpringUserRepository springUserRepository) {
        return new JpaUserRepository(springUserRepository);
    }

    // ---------- Command Handler ----------

    @Bean
    public RefreshSessionCommandHandler refreshSessionCommandHandler(
            OAuthIdTokenVerifier oAuthIdTokenVerifier,
            IdentityRepository identityRepository,
            UserRepository userRepository,
            JwtTokenGenerator jwtTokenGenerator,
            DateTimeProvider dateTimeProvider
    ) {
        return new RefreshSessionCommandHandler(
                oAuthIdTokenVerifier,
                identityRepository,
                userRepository,
                jwtTokenGenerator,
                dateTimeProvider
        );
    }
}
