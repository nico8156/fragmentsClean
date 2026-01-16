package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;

@Component
public class AuthUserCreatedEventHandler implements EventHandler<AuthUserCreatedEvent> {

	private static final Logger log = LoggerFactory.getLogger(AuthUserCreatedEventHandler.class);

	private final AppUserRepository appUserRepository;
	private final DomainEventPublisher domainEventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public AuthUserCreatedEventHandler(
			AppUserRepository appUserRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.appUserRepository = appUserRepository;
		this.domainEventPublisher = domainEventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void handle(AuthUserCreatedEvent event) {
		var now = dateTimeProvider.now();
		UUID userId = event.authUserId(); // 🔥 clé primaire

		// ✅ Idempotence applicative (plus robuste que findByAuthUserId)
		if (appUserRepository.findById(userId).isPresent()) {
			log.info("AppUser already exists id={}, ignoring AuthUserCreatedEvent", userId);
			return;
		}

		var displayName = event.email();
		String avatarUrl = null;

		// ✅ AppUser.id = authUserId
		var created = AppUser.createNew(
				userId, // authUserId (si tu le gardes)
				displayName,
				avatarUrl,
				now);

		try {
			appUserRepository.save(created);
		} catch (DataIntegrityViolationException e) {
			// ✅ Idempotence DB en cas de race / double delivery
			log.warn("AppUser creation raced for id={}, ignoring. msg={}", userId, e.getMessage());
			return;
		}

		created.domainEvents().forEach(domainEventPublisher::publish);
		created.clearDomainEvents();

		log.info("AppUser created from AuthUserCreatedEvent. id={}", userId);
	}
}
