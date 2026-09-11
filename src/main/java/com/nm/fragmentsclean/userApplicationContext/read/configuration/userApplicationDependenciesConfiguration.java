package com.nm.fragmentsclean.userApplicationContext.read.configuration;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcUserAccountDataEraser;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.JpaAccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.JpaSavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.SpringAccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.SpringSavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.SavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.UserAccountDataEraser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcessManager;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.RequestAccountDeletionCommandHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.SetSavedCoffeeCommandHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.UpdateAppUserProfileCommandHandler;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EntityScan(
    basePackages =
        "com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(
    basePackages =
        "com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(
    basePackages = {
      "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot",
      "com.nm.fragmentsclean.sharedKernel.adapters.secondary"
    })
public class userApplicationDependenciesConfiguration {

  @Bean
  public SavedCoffeeRepository savedCoffeeRepository(
      SpringSavedCoffeeRepository springSavedCoffeeRepository) {
    return new JpaSavedCoffeeRepository(springSavedCoffeeRepository);
  }

  @Bean
  AccountDeletionProcessRepository accountDeletionProcessRepository(
      SpringAccountDeletionProcessRepository repository) {
    return new JpaAccountDeletionProcessRepository(repository);
  }

  @Bean
  AuthUserCreatedEventHandler authUserCreatedEventHandler(
      AppUserRepository appUserRepository, DateTimeProvider dateTimeProvider) {
    return new AuthUserCreatedEventHandler(appUserRepository, dateTimeProvider);
  }

  @Bean
  SetSavedCoffeeCommandHandler setSavedCoffeeCommandHandler(
      SavedCoffeeRepository savedCoffeeRepository,
      DomainEventPublisher eventPublisher,
      DateTimeProvider dateTimeProvider) {
    return new SetSavedCoffeeCommandHandler(
        savedCoffeeRepository, eventPublisher, dateTimeProvider);
  }

  @Bean
  UpdateAppUserProfileCommandHandler updateAppUserProfileCommandHandler(
      AppUserRepository appUserRepository,
      DomainEventPublisher eventPublisher,
      DateTimeProvider dateTimeProvider) {
    return new UpdateAppUserProfileCommandHandler(
        appUserRepository, eventPublisher, dateTimeProvider);
  }

  @Bean
  RequestAccountDeletionCommandHandler requestAccountDeletionCommandHandler(
      AppUserRepository users,
      AccountDeletionProcessRepository processes,
      DomainEventPublisher events,
      DateTimeProvider clock) {
    return new RequestAccountDeletionCommandHandler(users, processes, events, clock);
  }

  @Bean
  UserAccountDataEraser userAccountDataEraser(JdbcTemplate jdbc) {
    return new JdbcUserAccountDataEraser(jdbc);
  }

  @Bean
  AccountDeletionProcessManager accountDeletionProcessManager(
      AccountDeletionProcessRepository processes,
      AppUserRepository users,
      UserAccountDataEraser eraser,
      DateTimeProvider clock) {
    return new AccountDeletionProcessManager(processes, users, eraser, clock);
  }
}
