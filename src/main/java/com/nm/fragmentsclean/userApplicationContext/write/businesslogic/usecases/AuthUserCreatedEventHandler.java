package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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

		// Idempotence applicative : si déjà créé, on ne fait rien
		if (appUserRepository.findByAuthUserId(event.authUserId()).isPresent()) {
			log.info("AppUser already exists for authUserId={}, ignoring AuthUserCreatedEvent",
					event.authUserId());
			return;
		}

		// Création "minimal profile"
		var displayName = event.email(); // fallback
		String avatarUrl = null;

		var created = AppUser.createNew(
				event.authUserId(),
				displayName,
				avatarUrl,
				now);

		try {
			appUserRepository.save(created);
		} catch (DataIntegrityViolationException e) {
			// Idempotence DB (unique auth_user_id) en cas de race / replay
			log.warn("AppUser creation raced/replayed for authUserId={}, ignoring. msg={}",
					event.authUserId(), e.getMessage());
			return;
		}

		created.domainEvents().forEach(domainEventPublisher::publish);
		created.clearDomainEvents();

		log.info("AppUser created from AuthUserCreatedEvent. authUserId={} appUserId={}",
				event.authUserId(), created.id());
	}
}
