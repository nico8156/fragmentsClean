package com.nm.fragmentsclean.experienceContext.read.configuration;

import com.nm.fragmentsclean.experienceContext.read.ListCoffeeExperiencesQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.ListExperienceModerationReportsQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.ListMyExperiencesQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExperienceContextReadConfiguration {
  @Bean
  ListCoffeeExperiencesQueryHandler listCoffeeExperiencesQueryHandler(ExperienceReadRepository repository) {
    return new ListCoffeeExperiencesQueryHandler(repository);
  }

  @Bean
  ListMyExperiencesQueryHandler listMyExperiencesQueryHandler(ExperienceReadRepository repository) {
    return new ListMyExperiencesQueryHandler(repository);
  }

  @Bean
  ListExperienceModerationReportsQueryHandler listExperienceModerationReportsQueryHandler(ExperienceReadRepository repository) {
    return new ListExperienceModerationReportsQueryHandler(repository);
  }
}
