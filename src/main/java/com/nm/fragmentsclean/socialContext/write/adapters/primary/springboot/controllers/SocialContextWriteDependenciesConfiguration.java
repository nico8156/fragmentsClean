package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommandHandler;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class SocialContextWriteDependenciesConfiguration {

    // Profil "fake" : utilisé par défaut / pour les tests unitaires si tu veux
    @Bean
    @Profile("fake")
    public LikeRepository likeRepositoryFake() {
        return new FakeLikeRepository();
    }

    // Profil "database" : utilisé pour les tests avec Postgres / JPA
    @Bean
    @Profile("database")
    public LikeRepository likeRepositoryJpa(SpringLikeRepository springLikeRepository) {
        return new JpaLikeRepository(springLikeRepository);
    }
    @Bean
    public CommentRepository commentRepository() {
        return new FakeCommentRepository();
    }

    @Bean
    public MakeLikeCommandHandler makeLikeCommandHandler(LikeRepository likeRepository,
                                                         DomainEventPublisher eventPublisher,
                                                         DateTimeProvider dateTimeProvider) {
        return new MakeLikeCommandHandler(likeRepository, eventPublisher, dateTimeProvider);
    }
    @Bean
    CreateCommentCommandHandler createCommentCommandHandler(
            CommentRepository commentRepository,
            DomainEventPublisher eventPublisher,
            DateTimeProvider dateTimeProvider
    ) {
        return new CreateCommentCommandHandler(commentRepository, eventPublisher, dateTimeProvider);
    }

    @Bean
    UpdateCommentCommandHandler updateCommentCommandHandler(
            CommentRepository commentRepository,
            DomainEventPublisher eventPublisher,
            DateTimeProvider dateTimeProvider
    ) {
        return new UpdateCommentCommandHandler(commentRepository, eventPublisher, dateTimeProvider);
    }

    @Bean
    DeleteCommentCommandHandler deleteCommentCommandHandler(
            CommentRepository commentRepository,
            DomainEventPublisher eventPublisher,
            DateTimeProvider dateTimeProvider
    ) {
        return new DeleteCommentCommandHandler(commentRepository, eventPublisher, dateTimeProvider);
    }


}
