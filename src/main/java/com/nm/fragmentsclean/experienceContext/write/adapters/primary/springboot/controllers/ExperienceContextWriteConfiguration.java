package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake.*;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc.*;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceContentPolicy;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.PrivateImageStorageProperties;
import java.time.Duration;import java.util.*;import java.util.stream.Collectors;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.domain.EntityScan;import org.springframework.boot.context.properties.EnableConfigurationProperties;import org.springframework.context.annotation.*;import org.springframework.data.jpa.repository.config.EnableJpaRepositories;import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EntityScan("com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa")
@EnableConfigurationProperties(PrivateImageStorageProperties.class)
public class ExperienceContextWriteConfiguration{
    @Bean ExperienceContentPolicy experienceContentPolicy(@Value("${fragments.experience.moderation.forbidden-terms:}")String raw){return new ExperienceContentPolicy(Arrays.stream(("kill yourself,va te suicider,"+raw).split(",")).map(String::strip).filter(v->!v.isBlank()).collect(Collectors.toSet()));}
    @Bean @Profile("!fake") ExperienceRepository experienceRepository(SpringExperienceRepository repository){return new JpaExperienceRepository(repository);}
    @Bean @Profile("!fake") ExperienceReportRepository experienceReportRepository(SpringExperienceReportRepository repository){return new JpaExperienceReportRepository(repository);}
    @Bean @Profile("!fake") ExperienceMediaRepository experienceMediaRepository(SpringExperienceMediaRepository repository){return new JpaExperienceMediaRepository(repository);}
    @Bean @Profile("!fake") ExperienceCoffeeReference experienceCoffeeReference(JdbcTemplate jdbc){return new JdbcExperienceCoffeeReference(jdbc);}
    @Bean @Profile("!fake") ExperienceAccountDataEraser experienceAccountDataEraser(JdbcTemplate jdbc){return new JdbcExperienceAccountDataEraser(jdbc);}
    @Bean @Profile("fake") ExperienceRepository fakeExperienceRepository(){return new FakeExperienceRepository();}
    @Bean @Profile("fake") ExperienceReportRepository fakeExperienceReportRepository(){return new FakeExperienceReportRepository();}
    @Bean @Profile("fake") ExperienceMediaRepository fakeExperienceMediaRepository(){return new FakeExperienceMediaRepository();}
    @Bean @Profile("fake") ExperienceCoffeeReference fakeExperienceCoffeeReference(){return id->true;}
    @Bean @Profile("fake") ExperienceAccountDataEraser fakeExperienceAccountDataEraser(){return id->{};}
    @Bean CreateExperienceCommandHandler createExperienceCommandHandler(ExperienceRepository r,ExperienceCoffeeReference c,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new CreateExperienceCommandHandler(r,c,p,e,d);}
    @Bean UpdateExperienceCommandHandler updateExperienceCommandHandler(ExperienceRepository r,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new UpdateExperienceCommandHandler(r,p,e,d);}
    @Bean PublishExperienceCommandHandler publishExperienceCommandHandler(ExperienceRepository r,ExperienceMediaRepository m,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new PublishExperienceCommandHandler(r,m,p,e,d);}
    @Bean DeleteExperienceCommandHandler deleteExperienceCommandHandler(ExperienceRepository r,ExperienceMediaRepository m,DomainEventPublisher e,DateTimeProvider d){return new DeleteExperienceCommandHandler(r,m,e,d);}
    @Bean ReportExperienceCommandHandler reportExperienceCommandHandler(ExperienceRepository r,ExperienceReportRepository reports,DomainEventPublisher e,DateTimeProvider d){return new ReportExperienceCommandHandler(r,reports,e,d);}
    @Bean ModerateExperienceCommandHandler moderateExperienceCommandHandler(ExperienceRepository r,ExperienceReportRepository reports,DomainEventPublisher e,DateTimeProvider d){return new ModerateExperienceCommandHandler(r,reports,e,d);}
    @Bean EraseExperienceAccountData eraseExperienceAccountData(ExperienceAccountDataEraser eraser,DomainEventPublisher e,DateTimeProvider d){return new EraseExperienceAccountData(eraser,e,d);}
    @Bean RegisterExperienceMediaUploadIntent registerExperienceMediaUploadIntent(ExperienceRepository e,ExperienceMediaRepository m,PrivateMediaObjectKeys k,DateTimeProvider d,@Value("${fragments.experience.media.max-count:4}")int max){return new RegisterExperienceMediaUploadIntent(e,m,k,d,max);}
    @Bean IssueExperienceMediaUploadIntent issueExperienceMediaUploadIntent(RegisterExperienceMediaUploadIntent r,PrivateImageStore s,DateTimeProvider d,PrivateImageStorageProperties p){return new IssueExperienceMediaUploadIntent(r,s,d,p.getUploadTtl());}
    @Bean ConfirmExperienceMediaCommandHandler confirmExperienceMediaCommandHandler(ExperienceRepository e,ExperienceMediaRepository m,DomainEventPublisher p,DateTimeProvider d){return new ConfirmExperienceMediaCommandHandler(e,m,p,d);}
    @Bean ConfirmExperienceMediaUpload confirmExperienceMediaUpload(ExperienceMediaRepository m,PrivateImageStore s,PrivateMediaObjectKeys k,DurableCommandExecutor d,ConfirmExperienceMediaCommandHandler h){return new ConfirmExperienceMediaUpload(m,s,k,d,h);}
    @Bean DeleteExperienceMediaCommandHandler deleteExperienceMediaCommandHandler(ExperienceMediaRepository m,DomainEventPublisher e,DateTimeProvider d){return new DeleteExperienceMediaCommandHandler(m,e,d);}
    @Bean CompleteExperienceMediaDeletion completeExperienceMediaDeletion(ExperienceMediaRepository m,DomainEventPublisher e,DateTimeProvider d){return new CompleteExperienceMediaDeletion(m,e,d);}
    @Bean CleanExperienceMediaObjects cleanExperienceMediaObjects(ExperienceMediaRepository m,PrivateImageStore s,CompleteExperienceMediaDeletion c,DateTimeProvider d,@Value("${fragments.media.cleanup.pending-ttl:PT24H}")Duration ttl){return new CleanExperienceMediaObjects(m,s,c,d,ttl);}
}
