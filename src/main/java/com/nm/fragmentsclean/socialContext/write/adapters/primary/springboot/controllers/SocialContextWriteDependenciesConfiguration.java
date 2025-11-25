package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class SocialContextWriteDependenciesConfiguration {

    @Bean
    public LikeRepository likeRepository() {
        // pour l’instant on reste en fake ; tu pourras passer en JPA plus tard
        return new FakeLikeRepository();
    }

    @Bean
    public MakeLikeCommandHandler makeLikeCommandHandler(LikeRepository likeRepository,
                                                         DomainEventPublisher eventPublisher,
                                                         DateTimeProvider dateTimeProvider) {
        return new MakeLikeCommandHandler(likeRepository, eventPublisher, dateTimeProvider);
    }

}
