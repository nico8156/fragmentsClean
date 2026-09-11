package com.nm.fragmentsclean.experienceContext.read.configuration;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceProjectionEventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExperienceProjectionConfiguration {
  @Bean
  ExperienceProjectionEventHandler experienceProjectionEventHandler(
      ExperienceProjectionRepository repository, ProjectionSyncPublisher sync) {
    return new ExperienceProjectionEventHandler(repository, sync);
  }
}
