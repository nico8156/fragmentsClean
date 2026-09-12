package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthUserCreatedEventHandler {

	private static final Logger log = LoggerFactory.getLogger(AuthUserCreatedEventHandler.class);

	private final AppUserRepository appUserRepository;
	private final DateTimeProvider dateTimeProvider;

	public AuthUserCreatedEventHandler(AppUserRepository appUserRepository,
			DateTimeProvider dateTimeProvider) {
		this.appUserRepository = appUserRepository;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Transactional
	public void handle(AuthUserCreatedIntegrationEvent event) {
		UUID authUserId = event.authUserId();
		Instant now = dateTimeProvider.now();

		Optional<AppUser> existing = appUserRepository.findById(authUserId);
		if (existing.isPresent()) {
			log.info("AppUser already exists for id={}, ignoring auth.user.created integration event", authUserId);
			return;
		}

		String displayName = firstNonBlank(
				event.displayName(),
				event.email(),
				"Utilisateur");

		String avatarUrl = blankToNull(event.avatarUrl());

		AppUser user = new AppUser(
				authUserId,
				authUserId,
				displayName,
				avatarUrl,
				now,
				now,
				0L);

		// Let persistence failures roll back and reach inbox retry. A concurrent
		// successful creation will be detected by findById on redelivery.
		appUserRepository.save(user);
		log.info("AppUser created from auth.user.created integration event. id={}", authUserId);
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && !v.trim().isEmpty())
				return v.trim();
		}
		return "Utilisateur";
	}

	private static String blankToNull(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}
}
