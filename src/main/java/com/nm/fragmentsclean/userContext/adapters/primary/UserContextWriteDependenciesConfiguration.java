//package com.nm.fragmentsclean.userContext.adapters.primary;
//
//import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.JpaIdentityRepository;
//import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
//import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
//import org.springframework.boot.autoconfigure.domain.EntityScan;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//
//@Configuration
//@EntityScan(basePackages = "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities")
//@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa")
//@ComponentScan(basePackages = {
//        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
//})
//public class UserContextWriteDependenciesConfiguration {
//    @Bean
//    @Profile("database")
//    public IdentityRepository indentityRepositoryJpa(SpringIdentityRepository springIdentityRepository){
//        return  new JpaIdentityRepository(springIdentityRepository);
//    }
//}
