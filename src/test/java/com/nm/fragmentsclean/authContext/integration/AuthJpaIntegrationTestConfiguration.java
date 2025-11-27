package com.nm.fragmentsclean.authContext.integration;

import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.JpaIdentityRepository;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthJpaIntegrationTestConfiguration {

    @Bean
    public IdentityRepository identityRepository(SpringIdentityRepository springIdentityRepository) {
        return new JpaIdentityRepository(springIdentityRepository);
    }
}
