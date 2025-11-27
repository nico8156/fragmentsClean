package com.nm.fragmentsclean.authContext.adapters.primary.springboot.controllers.config;

import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeJwtTokenGenerator;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeOAuthIdTokenVerifier;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.OAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommandHandler;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;

// TODO: adapte ces imports selon là où tu mettras tes impls concrètes
// import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeIdentityRepository;
// import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.jpa.JpaIdentityRepository;
// import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.jpa.SpringIdentityRepository;
// import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.fake.FakeUserRepository;
// import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.jpa.JpaUserRepository;
// import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.jpa.SpringUserRepository;

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
//@EnableJpaRepositories({
//        "com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa",
//        "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa"
//})
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class AuthContextDependenciesConfiguration {

    @Bean
    @Profile("fake")
    public JwtTokenGenerator fakeJwtTokenGenerator() {
        return new FakeJwtTokenGenerator();
    }

    @Bean
    @Profile("fake")
    public OAuthIdTokenVerifier fakeOAuthIdTokenVerifier() {
        return new FakeOAuthIdTokenVerifier();
    }


    // ---------- Repositories : profils fake / database ----------

    @Bean
    @Profile("fake")
    public IdentityRepository fakeIdentityRepository() {
        // return new FakeIdentityRepository();
        throw new UnsupportedOperationException("TODO: impl fakeIdentityRepository()");
    }

    @Bean
    @Profile("database")
    public IdentityRepository jpaIdentityRepository(
            /*SpringIdentityRepository springIdentityRepository*/
    ) {
        // return new JpaIdentityRepository(springIdentityRepository);
        throw new UnsupportedOperationException("TODO: impl jpaIdentityRepository()");
    }

    @Bean
    @Profile("fake")
    public UserRepository fakeUserRepository() {
        // return new FakeUserRepository();
        throw new UnsupportedOperationException("TODO: impl fakeUserRepository()");
    }

    @Bean
    @Profile("database")
    public UserRepository jpaUserRepository(
            /*SpringUserRepository springUserRepository*/
    ) {
        // return new JpaUserRepository(springUserRepository);
        throw new UnsupportedOperationException("TODO: impl jpaUserRepository()");
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
