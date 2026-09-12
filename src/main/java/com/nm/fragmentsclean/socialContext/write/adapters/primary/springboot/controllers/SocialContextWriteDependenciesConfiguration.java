package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.SocialAccountDataEraser;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.EraseSocialAccountData;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcSocialAccountDataEraser;
import org.springframework.jdbc.core.JdbcTemplate;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeUserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.ContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.UserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaUserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringUserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentContentPolicy;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.ReportCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.SetUserBlockCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.ModerateCommentCommandHandler;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Bean
    SocialAccountDataEraser socialAccountDataEraser(JdbcTemplate jdbc) {
        return new JdbcSocialAccountDataEraser(jdbc);
    }

    @Bean
    EraseSocialAccountData eraseSocialAccountData(
            SocialAccountDataEraser eraser, DomainEventPublisher events, DateTimeProvider clock) {
        return new EraseSocialAccountData(eraser, events, clock);
    }

    @Bean
    CommentContentPolicy commentContentPolicy(
            @Value("${fragments.social.moderation.forbidden-terms:}") String rawTerms) {
        Set<String> terms = Arrays.stream(("kill yourself,va te suicider," + rawTerms).split(","))
                .map(String::strip).filter(value -> !value.isBlank()).collect(Collectors.toSet());
        return new CommentContentPolicy(terms);
    }

    // Profil "fake" : utilisé par défaut / pour les tests unitaires si tu veux
    @Bean
    @Profile("fake")
    public LikeRepository likeRepositoryFake() {
        return new FakeLikeRepository();
    }

    // Persistent adapters are the runtime default; fake is an explicit alternative.
    @Bean
    @Profile("!fake")
    public LikeRepository likeRepositoryJpa(SpringLikeRepository springLikeRepository) {
        return new JpaLikeRepository(springLikeRepository);
    }

    @Bean
    @Profile("fake")
    public CommentRepository fakeCommentRepository() {
        return new FakeCommentRepository();
    }

    @Bean @Profile("fake") public ContentReportRepository fakeContentReportRepository() { return new FakeContentReportRepository(); }
    @Bean @Profile("fake") public UserBlockRepository fakeUserBlockRepository() { return new FakeUserBlockRepository(); }

    @Bean @Profile("!fake")
    public ContentReportRepository jpaContentReportRepository(SpringContentReportRepository repository) {
        return new JpaContentReportRepository(repository);
    }
    @Bean @Profile("!fake")
    public UserBlockRepository jpaUserBlockRepository(SpringUserBlockRepository repository) {
        return new JpaUserBlockRepository(repository);
    }

    @Bean
    @Profile("!fake")
    public CommentRepository jpaCommentRepository(SpringCommentRepository springCommentRepository) {
        return new JpaCommentRepository(springCommentRepository);
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
            DateTimeProvider dateTimeProvider,
            CommentContentPolicy contentPolicy
    ) {
        return new CreateCommentCommandHandler(commentRepository, eventPublisher, dateTimeProvider, contentPolicy);
    }

    @Bean
    UpdateCommentCommandHandler updateCommentCommandHandler(
            CommentRepository commentRepository,
            DomainEventPublisher eventPublisher,
            DateTimeProvider dateTimeProvider,
            CommentContentPolicy contentPolicy
    ) {
        return new UpdateCommentCommandHandler(commentRepository, eventPublisher, dateTimeProvider, contentPolicy);
    }

    @Bean ReportCommentCommandHandler reportCommentCommandHandler(CommentRepository comments,
            ContentReportRepository reports, DomainEventPublisher events, DateTimeProvider clock) {
        return new ReportCommentCommandHandler(comments, reports, events, clock);
    }

    @Bean SetUserBlockCommandHandler setUserBlockCommandHandler(UserBlockRepository blocks,
            DomainEventPublisher events, DateTimeProvider clock) {
        return new SetUserBlockCommandHandler(blocks, events, clock);
    }

    @Bean ModerateCommentCommandHandler moderateCommentCommandHandler(CommentRepository comments,
            ContentReportRepository reports, DomainEventPublisher events, DateTimeProvider clock) {
        return new ModerateCommentCommandHandler(comments, reports, events, clock);
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
