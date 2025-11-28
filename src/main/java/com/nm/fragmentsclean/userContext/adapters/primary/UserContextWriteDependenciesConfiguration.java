package com.nm.fragmentsclean.userContext.adapters.primary;

import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.JpaUserRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.SpringUserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class UserContextWriteDependenciesConfiguration {

    @Bean
    public UserRepository userRepositoryJpa(SpringUserRepository springUserRepository){
        return new JpaUserRepository(springUserRepository);
    }
}
