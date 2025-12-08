package com.nm.fragmentsclean.userApplicationContext.read.configuration;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.JpaAppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.SpringAppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot",
        "com.nm.fragmentsclean.sharedKernel.adapters.secondary"
})
public class userApplicationDependenciesConfiguration {

    @Bean
    public AppUserRepository appUserRepository(SpringAppUserRepository springAppUserRepository){
        return new JpaAppUserRepository(springAppUserRepository);
    }

}
