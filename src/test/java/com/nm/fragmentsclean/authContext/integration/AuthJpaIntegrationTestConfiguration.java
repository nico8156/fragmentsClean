package com.nm.fragmentsclean.authContext.integration;

import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.JpaIdentityRepository;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.JpaUserRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.SpringUserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@TestConfiguration
public class AuthJpaIntegrationTestConfiguration {

    @Bean
    public IdentityRepository identityRepository(SpringIdentityRepository springIdentityRepository){
        return new JpaIdentityRepository(springIdentityRepository);
    }
    @Bean
    public UserRepository userRepositoryJpa(SpringUserRepository springUserRepository){
        return new JpaUserRepository(springUserRepository);
    }

}