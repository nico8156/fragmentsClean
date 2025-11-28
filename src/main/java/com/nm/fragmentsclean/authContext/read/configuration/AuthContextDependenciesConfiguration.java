package com.nm.fragmentsclean.authContext.read.configuration;

import com.nm.fragmentsclean.authContext.read.usecases.GetCurrentUserQueryHandler;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.fake.FakeJwtTokenGenerator;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.fake.FakeOAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.JpaIdentityRepository;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.OAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.write.businesslogic.usecases.LoginCommandHandler;
import com.nm.fragmentsclean.authContext.write.businesslogic.usecases.RefreshSessionCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class AuthContextDependenciesConfiguration {

    @Bean
    public JwtTokenGenerator jwtTokenGenerator() {
        return new FakeJwtTokenGenerator();
    }

    @Bean
    public OAuthIdTokenVerifier oAuthIdTokenVerifier() {
        return new FakeOAuthIdTokenVerifier();
    }

    @Bean
    public IdentityRepository identityRepository(SpringIdentityRepository springIdentityRepository) {
        return new JpaIdentityRepository(springIdentityRepository);
    }

    @Bean
    public RefreshSessionCommandHandler refreshSessionCommandHandler(
            OAuthIdTokenVerifier oAuthIdTokenVerifier,
            IdentityRepository identityRepository,
            UserRepository userRepository,
            JwtTokenGenerator jwtTokenGenerator,
            DateTimeProvider dateTimeProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        return new RefreshSessionCommandHandler(
                oAuthIdTokenVerifier,
                identityRepository,
                userRepository,
                jwtTokenGenerator,
                dateTimeProvider,
                domainEventPublisher
        );
    }
    @Bean
    public LoginCommandHandler loginCommandHandler(
            OAuthIdTokenVerifier oAuthIdTokenVerifier,
            IdentityRepository identityRepository,
            UserRepository userRepository,
            JwtTokenGenerator jwtTokenGenerator,
            DateTimeProvider dateTimeProvider,
            DomainEventPublisher domainEventPublisher
    ){
        return new LoginCommandHandler(
                oAuthIdTokenVerifier,
                identityRepository,
                userRepository,
                jwtTokenGenerator,
                dateTimeProvider,
                domainEventPublisher
        );
    }

    @Bean
    public GetCurrentUserQueryHandler getCurrentUserQueryHandler(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            IdentityRepository identityRepository,
            DateTimeProvider dateTimeProvider
    ) {
        return new GetCurrentUserQueryHandler(
                currentUserProvider,
                userRepository,
                identityRepository,
                dateTimeProvider
        );
    }
}
