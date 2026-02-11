package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler; // ✅ adapte le package si besoin
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthUserCreatedEventHandler implements EventHandler<AuthUserCreatedEvent> {

	private static final Logger log = LoggerFactory.getLogger(AuthUserCreatedEventHandler.class);

	private final AppUserRepository appUserRepository;
	private final DateTimeProvider dateTimeProvider;

	public AuthUserCreatedEventHandler(AppUserRepository appUserRepository,
			DateTimeProvider dateTimeProvider) {
		this.appUserRepository = appUserRepository;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void handle(AuthUserCreatedEvent event) {
		UUID authUserId = event.authUserId();
		Instant now = dateTimeProvider.now();

		Optional<AppUser> existing = appUserRepository.findById(authUserId);
		if (existing.isPresent()) {
			log.info("AppUser already exists for id={}, ignoring AuthUserCreatedEvent", authUserId);
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

		try {
			appUserRepository.save(user);
			log.info("AppUser created from AuthUserCreatedEvent. id={}, displayName={}", authUserId,
					displayName);
		} catch (DataIntegrityViolationException e) {
			String msg = String.valueOf(
					e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage()
							: e.getMessage());
			log.warn("AppUser creation raced for id={}, ignoring. msg={}", authUserId, msg);
		}
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
