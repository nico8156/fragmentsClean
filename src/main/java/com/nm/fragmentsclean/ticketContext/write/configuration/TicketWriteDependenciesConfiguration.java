package com.nm.fragmentsclean.ticketContext.write.configuration;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.JpaTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.ticketEngine.ProcessBuilderTicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.ProcessTicketVerificationEventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommandHandler;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa")
public class TicketWriteDependenciesConfiguration {

	@Bean
	@ConditionalOnMissingBean(TicketRepository.class)
	public TicketRepository ticketRepository(SpringTicketRepository springTicketRepository,
			ObjectMapper objectMapper) {
		return new JpaTicketRepository(springTicketRepository, objectMapper);
	}

	@Bean
	VerifyTicketCommandHandler verifyTicketCommandHandler(
			TicketRepository ticketRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new VerifyTicketCommandHandler(ticketRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	TicketVerificationCompletedEventHandler ticketVerificationCompletedEventHandler(
			JdbcTicketStatusProjectionRepository jdbcTicketStatusProjectionRepository) {
		return new TicketVerificationCompletedEventHandler(jdbcTicketStatusProjectionRepository);
	}

	@Bean
	ProcessTicketVerificationEventHandler processTicketVerificationEventHandler(
			TicketRepository ticketRepository,
			TicketVerificationProvider ticketVerificationProvider,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new ProcessTicketVerificationEventHandler(ticketRepository, ticketVerificationProvider,
				domainEventPublisher, dateTimeProvider);
	}

	@Bean
	public TicketVerificationProvider ticketVerificationProvider(
			ObjectMapper objectMapper,
			@Value("${ticket.verify.binary-path}") String binaryPath,
			@Value("${ticketverify.timeout-ms:1500}") long timeoutMs) {
		return new ProcessBuilderTicketVerificationProvider(
				objectMapper,
				List.of(binaryPath),
				Duration.ofMillis(timeoutMs));
	}
}
