package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake.*;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc.*;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceContentPolicy;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.util.*;import java.util.stream.Collectors;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.domain.EntityScan;import org.springframework.context.annotation.*;import org.springframework.data.jpa.repository.config.EnableJpaRepositories;import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EntityScan("com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa")
public class ExperienceContextWriteConfiguration{
    @Bean ExperienceContentPolicy experienceContentPolicy(@Value("${fragments.experience.moderation.forbidden-terms:}")String raw){return new ExperienceContentPolicy(Arrays.stream(("kill yourself,va te suicider,"+raw).split(",")).map(String::strip).filter(v->!v.isBlank()).collect(Collectors.toSet()));}
    @Bean @Profile("!fake") ExperienceRepository experienceRepository(SpringExperienceRepository repository){return new JpaExperienceRepository(repository);}
    @Bean @Profile("!fake") ExperienceReportRepository experienceReportRepository(SpringExperienceReportRepository repository){return new JpaExperienceReportRepository(repository);}
    @Bean @Profile("!fake") ExperienceCoffeeReference experienceCoffeeReference(JdbcTemplate jdbc){return new JdbcExperienceCoffeeReference(jdbc);}
    @Bean @Profile("!fake") ExperienceAccountDataEraser experienceAccountDataEraser(JdbcTemplate jdbc){return new JdbcExperienceAccountDataEraser(jdbc);}
    @Bean @Profile("fake") ExperienceRepository fakeExperienceRepository(){return new FakeExperienceRepository();}
    @Bean @Profile("fake") ExperienceReportRepository fakeExperienceReportRepository(){return new FakeExperienceReportRepository();}
    @Bean @Profile("fake") ExperienceCoffeeReference fakeExperienceCoffeeReference(){return id->true;}
    @Bean @Profile("fake") ExperienceAccountDataEraser fakeExperienceAccountDataEraser(){return id->{};}
    @Bean CreateExperienceCommandHandler createExperienceCommandHandler(ExperienceRepository r,ExperienceCoffeeReference c,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new CreateExperienceCommandHandler(r,c,p,e,d);}
    @Bean UpdateExperienceCommandHandler updateExperienceCommandHandler(ExperienceRepository r,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new UpdateExperienceCommandHandler(r,p,e,d);}
    @Bean PublishExperienceCommandHandler publishExperienceCommandHandler(ExperienceRepository r,ExperienceContentPolicy p,DomainEventPublisher e,DateTimeProvider d){return new PublishExperienceCommandHandler(r,p,e,d);}
    @Bean DeleteExperienceCommandHandler deleteExperienceCommandHandler(ExperienceRepository r,DomainEventPublisher e,DateTimeProvider d){return new DeleteExperienceCommandHandler(r,e,d);}
    @Bean ReportExperienceCommandHandler reportExperienceCommandHandler(ExperienceRepository r,ExperienceReportRepository reports,DomainEventPublisher e,DateTimeProvider d){return new ReportExperienceCommandHandler(r,reports,e,d);}
    @Bean ModerateExperienceCommandHandler moderateExperienceCommandHandler(ExperienceRepository r,ExperienceReportRepository reports,DomainEventPublisher e,DateTimeProvider d){return new ModerateExperienceCommandHandler(r,reports,e,d);}
    @Bean EraseExperienceAccountData eraseExperienceAccountData(ExperienceAccountDataEraser eraser,DomainEventPublisher e,DateTimeProvider d){return new EraseExperienceAccountData(eraser,e,d);}
}
